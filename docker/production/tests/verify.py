"""Pruebas locales opt-in. Solo fixtures propias; nunca AWS, targets ni OpenAI.

Requiere imágenes construidas y Python con cryptography. Conserva volumen PG de prueba.
"""
import base64
import datetime
import hashlib
import http.client
import json
import os
from pathlib import Path
import re
import secrets
import socket
import ssl
import subprocess
import tempfile
import time
import uuid

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID

ROOT = Path(__file__).resolve().parents[3]
COMPOSE = ROOT / 'docker/production/compose.yml'
NODE = 'node:22-alpine@sha256:0a7108bf6c7bf5de370ffb1a3ed6be93d405b43ff159f681a8d18c0e2bc2e402'


def run(args, *, env=None, stdin=None, check=True):
    result = subprocess.run(args, cwd=ROOT, env=env, input=stdin, capture_output=True, text=True)
    if check and result.returncode:
        # No volcar logs/env/comandos: pueden contener material de fixture o configuración.
        raise AssertionError('Command failed: ' + args[0] + ' ' + args[1] + '; code=' + str(result.returncode))
    return result


def main():
    token = uuid.uuid4().hex[:10]
    project, volume = 'apr-' + token, 'apr-data-' + token
    counter, proxy, network = 'apr-counter-' + token, 'apr-proxy-' + token, 'apr-net-' + token
    work = Path(tempfile.mkdtemp(prefix='apr-'))
    evidence = ROOT / 'openspec/changes/aws-production-readiness/evidence'
    evidence.mkdir(parents=True, exist_ok=True)
    results = {'project': project, 'fixtureDirectory': str(work), 'retainedVolume': volume, 'checks': []}
    env = os.environ.copy()
    # No propagar ninguna clave evaluator del operador a fixtures.
    env.pop('EVALUATOR_API_KEY', None)
    env.update(SECRETS_DIR=str(work).replace('\\', '/'), TLS_DIR=str(work / 'tls').replace('\\', '/'),
               PUBLIC_HOSTNAME='scanner.test', PUBLIC_ORIGIN='https://scanner.test:18443',
               INGRESS_BIND_ADDRESS='127.0.0.1', INGRESS_HTTPS_PORT='18443', PGDATA_VOLUME=volume,
               BACKEND_CONTAINER_NAME=project + '-backend')
    env['BACKEND_IMAGE'] = run(['docker', 'image', 'inspect', 'llm-scanner-backend:production', '--format', '{{.Id}}']).stdout.strip()
    env['INGRESS_IMAGE'] = run(['docker', 'image', 'inspect', 'llm-scanner-ingress:production', '--format', '{{.Id}}']).stdout.strip()
    (work / 'empty-vars').write_text('')
    compose = ['docker', 'compose', '--env-file', str(work / 'empty-vars'), '-p', project, '-f', str(COMPOSE)]
    password = secrets.token_urlsafe(32)
    auth = 'Basic ' + base64.b64encode(('fixture:' + password).encode()).decode()
    for name in ('admin-password', 'app-password-pg', 'target-aes'):
        value = base64.b64encode(secrets.token_bytes(32)).decode() if name == 'target-aes' else secrets.token_urlsafe(32)
        (work / name).write_text(value, encoding='ascii')
    (work / 'app-password-backend').write_bytes((work / 'app-password-pg').read_bytes())
    # SHA solo para credencial sintética efímera; producción exige bcrypt fuerte externo.
    (work / 'htpasswd').write_text('fixture:{SHA}' + base64.b64encode(hashlib.sha1(password.encode()).digest()).decode() + '\n')
    tls = work / 'tls/active'
    tls.mkdir(parents=True)
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    subject = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'scanner.test')])
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (x509.CertificateBuilder().subject_name(subject).issuer_name(subject).public_key(key.public_key())
            .serial_number(x509.random_serial_number()).not_valid_before(now - datetime.timedelta(minutes=1))
            .not_valid_after(now + datetime.timedelta(days=1))
            .add_extension(x509.SubjectAlternativeName([x509.DNSName('scanner.test')]), critical=False)
            .sign(key, hashes.SHA256()))
    (tls / 'fullchain.pem').write_bytes(cert.public_bytes(serialization.Encoding.PEM))
    (tls / 'privkey.pem').write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
                                                     serialization.NoEncryption()))
    context = ssl.create_default_context(cafile=str(tls / 'fullchain.pem'))

    def connect():
        sock = socket.create_connection(('127.0.0.1', 18443), timeout=6)
        return context.wrap_socket(sock, server_hostname='scanner.test')

    def request(path, method='GET', body=None, authenticated=True, extra=None):
        conn = http.client.HTTPSConnection('127.0.0.1', 18443, timeout=8, context=context)
        conn.sock = connect()
        headers = {'Host': 'scanner.test:18443', 'Origin': env['PUBLIC_ORIGIN']}
        if authenticated:
            headers['Authorization'] = auth
        if extra:
            headers.update(extra)
        conn.request(method, path, body=body, headers=headers)
        response = conn.getresponse()
        answer = response.status, dict((k.lower(), v) for k, v in response.getheaders()), response.read()
        conn.close()
        return answer

    def record(name, passed):
        results['checks'].append({'name': name, 'pass': bool(passed)})
        if not passed:
            raise AssertionError(name)
        print('PASS ' + name, flush=True)

    def wait_https():
        for _ in range(90):
            try:
                if request('/api/projects')[0] == 200:
                    return
            except (OSError, http.client.HTTPException):
                pass
            time.sleep(2)
        raise AssertionError('Readiness timeout')

    def pg(sql):
        # Contraseña exclusivamente del fichero de fixture dentro de su servicio; nunca argv/log.
        shell = 'export PGPASSWORD="$(cat /run/secrets/admin-password)"; exec psql -X -q -t -A -v ON_ERROR_STOP=1 -U scanner_bootstrap -d llm_scanner_seguridad_pro'
        return run(compose + ['exec', '-T', 'postgres', 'sh', '-c', shell], env=env, stdin=sql).stdout.strip()

    try:
        config = json.loads(run(compose + ['config', '--format', 'json'], env=env).stdout)
        record('exactly-three-services', set(config['services']) == {'ingress', 'backend', 'postgres'})
        record('no-backend-or-database-published-port', all(not config['services'][s].get('ports') for s in ('backend', 'postgres')))
        record('proxy-outside-data-network', 'data' not in config['services']['ingress']['networks'])
        record('data-network-internal', config['networks']['data']['internal'])
        record('no-evaluator-or-demo', 'EVALUATOR_API_KEY' not in config['services']['backend']['environment']
               and config['services']['backend']['environment']['EVALUATOR_EXPORT_ENABLED'] == 'false')
        record('ingress-fails-without-secrets', run(['docker', 'run', '--rm', '--network', 'none',
               '-e', 'PUBLIC_HOSTNAME=scanner.test', '-e', 'PUBLIC_ORIGIN=' + env['PUBLIC_ORIGIN'],
               env['INGRESS_IMAGE']], check=False).returncode != 0)
        record('backend-rejects-local-profile', run(['docker', 'run', '--rm', '--network', 'none',
               '-e', 'SPRING_PROFILES_ACTIVE=local', env['BACKEND_IMAGE']], check=False).returncode != 0)
        run(['docker', 'volume', 'create', volume])
        run(compose + ['up', '-d', '--no-build'], env=env)
        wait_https()
        record('nginx-configuration-valid', run(compose + ['exec', '-T', 'ingress', 'nginx', '-t', '-c', '/tmp/nginx.conf'], env=env).returncode == 0)
        record('angular-static', request('/')[0] == 200 and b'<app-root' in request('/')[2])
        asset = re.findall(rb'<script[^>]+src="([^"]+)"', request('/')[2])[0].decode()
        record('built-javascript-authenticated', request('/' + asset, authenticated=False)[0] == 401)
        record('spa-deep-link', request('/projects/fixture/scans/fixture')[0] == 200 and b'<app-root' in request('/projects/fixture/scans/fixture')[2])
        for path in ('/', '/api/projects', '/projects/fixture', '/favicon.ico'):
            record('auth-required-' + path, request(path, authenticated=False)[0] == 401)
        record('bad-auth-denied', request('/api/projects', extra={'Authorization': 'Basic Zm9vOmJhcg=='})[0] == 401)
        record('canonical-host-only', request('/', extra={'Host': 'foreign.test'})[0] == 421)
        record('api-json-not-spa', request('/api/projects')[1].get('content-type', '').startswith('application/json'))
        for service, port in (('backend', '8080/tcp'), ('postgres', '5432/tcp')):
            identity = run(compose + ['ps', '-q', service], env=env).stdout.strip()
            ports = json.loads(run(['docker', 'inspect', '--format', '{{json .NetworkSettings.Ports}}', identity]).stdout)
            record('runtime-private-' + service, not ports.get(port))
        record('ingress-cannot-reach-pg', run(compose + ['exec', '-T', 'ingress', 'sh', '-c',
               'nc -z -w 1 postgres 5432'], env=env, check=False).returncode != 0)
        record('runtime-export-and-demo-disabled', run(compose + ['exec', '-T', 'backend', 'sh', '-c',
               'test "$EVALUATOR_EXPORT_ENABLED" = false && test "$TARGET_LOCAL_DEMO_ENABLED" = false && test -z "${EVALUATOR_API_KEY+x}"'],
               env=env).returncode == 0)
        status, headers, body = request('/api/not-a-route')
        record('unknown-api-no-spa', status == 404 and b'<app-root' not in body)
        record('foreign-origin-denied', request('/api/projects', 'POST', '{}', extra={'Origin': 'https://foreign.test'})[0] == 403)
        record('missing-origin-denied', request('/api/projects', 'POST', '{}', extra={'Origin': ''})[0] == 403)
        status, headers, body = request('/api/projects', 'POST', json.dumps({'name': 'APR fixture ' + token}), extra={'Content-Type': 'application/json'})
        record('authorized-write-and-location', status == 201 and headers.get('location', '').startswith('/api/projects/'))
        saved = json.loads(body)['id']
        prefix, suffix = '{"message":"{{PROMPT}}","padding":"', '"}'
        padding_bytes = 32768 - len(prefix) - len(suffix)
        template = prefix + '\\\\' * (padding_bytes // 2) + 'x' * (padding_bytes % 2) + suffix
        headers = {}
        remaining = 16384 - 20 * 128
        for number in range(20):
            header_name = ('X-Fixture-' + str(number)).ljust(128, 'a')
            size = remaining // (20 - number)
            headers[header_name] = 'y' * size
            remaining -= size
        largest_target = {'name': 'N' * 120, 'url': 'https://target.example/' + 'a' * (2048 - len('https://target.example/')),
                          'method': 'POST', 'contentType': 'application/json', 'requestTemplate': template,
                          'promptPlaceholder': '{{PROMPT}}', 'responsePath': '$.' + 'a' * 1022,
                          'headers': headers, 'authType': 'NONE', 'timeoutMs': 60000}
        record('maximum-valid-target-accepted', request('/api/projects/' + saved + '/target', 'POST',
               json.dumps(largest_target), extra={'Content-Type': 'application/json'})[0] == 201)
        role = pg("SELECT rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication FROM pg_roles WHERE rolname='scanner_app';")
        record('app-no-global-privileges', role == 'f')
        history = pg("SELECT string_agg(version,',' ORDER BY installed_rank) FROM flyway_schema_history WHERE success;")
        record('clean-flyway-official-history', history == '1,2,3,4,5,6,7')
        record('fixed-test-catalog', pg('SELECT count(*) FROM test_cases;') == '4')
        run(compose + ['restart', 'backend'], env=env)
        wait_https()
        record('restart-retains-project', request('/api/projects/' + saved)[0] == 200)
        record('restart-no-duplicate-migrations', pg('SELECT count(*) FROM flyway_schema_history;') == '7')
        backend_before = run(['docker', 'inspect', '--format', '{{.State.StartedAt}}', env['BACKEND_CONTAINER_NAME']]).stdout.strip()
        pg_id = run(compose + ['ps', '-q', 'postgres'], env=env).stdout.strip()
        pg_before = run(['docker', 'inspect', '--format', '{{.State.StartedAt}}', pg_id]).stdout.strip()
        renewed = (x509.CertificateBuilder().subject_name(subject).issuer_name(subject).public_key(key.public_key())
                   .serial_number(x509.random_serial_number()).not_valid_before(now - datetime.timedelta(minutes=1))
                   .not_valid_after(now + datetime.timedelta(days=2))
                   .add_extension(x509.SubjectAlternativeName([x509.DNSName('scanner.test')]), critical=False)
                   .sign(key, hashes.SHA256()))
        (tls / 'fullchain.pem').write_bytes(renewed.public_bytes(serialization.Encoding.PEM))
        run(compose + ['exec', '-T', 'ingress', 'nginx', '-t', '-c', '/tmp/nginx.conf'], env=env)
        run(compose + ['exec', '-T', 'ingress', 'nginx', '-c', '/tmp/nginx.conf', '-s', 'reload'], env=env)
        context = ssl.create_default_context(cafile=str(tls / 'fullchain.pem'))
        time.sleep(1)
        with connect() as tls_socket:
            served = x509.load_der_x509_certificate(tls_socket.getpeercert(binary_form=True))
        record('tls-reload-new-serial-and-expiry', served.serial_number == renewed.serial_number
               and served.not_valid_after_utc == renewed.not_valid_after_utc)
        record('tls-reload-preserves-auth', request('/', authenticated=False)[0] == 401 and request('/')[0] == 200)
        record('tls-reload-no-backend-or-pg-restart', backend_before == run(['docker', 'inspect', '--format', '{{.State.StartedAt}}',
               env['BACKEND_CONTAINER_NAME']]).stdout.strip() and pg_before == run(['docker', 'inspect', '--format',
               '{{.State.StartedAt}}', pg_id]).stdout.strip())
        # Solo la BD efímera nueva de este test; se restaura el checksum antes de cerrar.
        original_checksum = pg("SELECT checksum FROM flyway_schema_history WHERE version='1';")
        pg("UPDATE flyway_schema_history SET checksum=0 WHERE version='1';")
        run(compose + ['stop', 'ingress', 'backend'], env=env)
        run(compose + ['up', '-d', 'backend'], env=env)
        time.sleep(15)
        inspect = json.loads(run(['docker', 'inspect', env['BACKEND_CONTAINER_NAME']]).stdout)[0]
        record('checksum-mismatch-fails-readiness', inspect['RestartCount'] > 0 or inspect['State']['Status'] != 'running')
        run(compose + ['stop', 'backend'], env=env)
        pg("UPDATE flyway_schema_history SET checksum=" + original_checksum + " WHERE version='1';")
        run(compose + ['stop'], env=env)

        # Segunda fixture: mismo ingress, upstream contador sin secretos ni PostgreSQL.
        fixture_script = Path(__file__).with_name('counter.js')
        run(['docker', 'network', 'create', network])
        run(['docker', 'run', '-d', '--name', counter, '--network', network, '--network-alias', 'backend',
             '-v', str(fixture_script) + ':/counter.js:ro', NODE, 'node', '/counter.js'])
        run(['docker', 'run', '-d', '--name', proxy, '--network', network, '-p', '127.0.0.1:18443:8443',
             '-e', 'PUBLIC_HOSTNAME=scanner.test', '-e', 'PUBLIC_ORIGIN=' + env['PUBLIC_ORIGIN'],
             '-v', str(work / 'htpasswd') + ':/run/auth/htpasswd:ro', '-v', str(work / 'tls') + ':/run/tls:ro',
             '--tmpfs', '/tmp:uid=101,gid=101,size=4m', '--tmpfs', '/var/cache/nginx:uid=101,gid=101,size=64m',
             '--read-only', '--cap-drop', 'ALL', '--security-opt', 'no-new-privileges', env['INGRESS_IMAGE']])
        wait_https()

        def count():
            code = "require('http').get('http://localhost:8080/fixture-count',r=>r.on('data',d=>process.stdout.write(d)))"
            return json.loads(run(['docker', 'exec', counter, 'node', '-e', code]).stdout)

        baseline = count()['writes']
        record('counter-rejects-unauthenticated-write', request('/api/projects', 'POST', '{}', authenticated=False)[0] == 401)
        record('counter-rejects-foreign-origin', request('/api/projects', 'POST', '{}', extra={'Origin': 'https://foreign.test'})[0] == 403)
        record('denied-writes-never-reach-upstream', count()['writes'] == baseline)
        record('oversized-content-length-413', request('/api/projects', 'POST', b'x' * (1048576 + 1))[0] == 413)
        record('oversized-not-forwarded', count()['writes'] == baseline)
        sock = connect()
        sock.sendall(('POST /api/projects HTTP/1.1\r\nHost: scanner.test\r\nAuthorization: ' + auth +
                      '\r\nOrigin: ' + env['PUBLIC_ORIGIN'] + '\r\nTransfer-Encoding: chunked\r\n\r\n').encode())
        try:
            for _ in range(17):
                chunk = b'x' * 65536
                sock.sendall(b'10000\r\n' + chunk + b'\r\n')
            sock.sendall(b'0\r\n\r\n')
            reply = sock.recv(4096)
        except (OSError, ssl.SSLError):
            reply = b''
        sock.close()
        record('chunked-not-forwarded', count()['writes'] == baseline)
        record('chunked-overflow-413', b'413' in reply)
        record('compressed-rejected', request('/api/projects', 'POST', b'fixture', extra={'Content-Encoding': 'gzip'})[0] == 415)
        record('compressed-not-forwarded', count()['writes'] == baseline)
        sock = connect()
        sock.sendall(('POST /api/projects HTTP/1.1\r\nHost: scanner.test\r\nAuthorization: ' + auth +
                      '\r\nOrigin: ' + env['PUBLIC_ORIGIN'] + '\r\nContent-Length: 8\r\n\r\nx').encode())
        sock.settimeout(20)
        started = time.monotonic()
        reply = sock.recv(4096)
        elapsed = time.monotonic() - started
        sock.close()
        record('idle-body-timeout-15s', 14 <= elapsed <= 20 and (b'408' in reply or not reply))
        record('idle-body-not-forwarded', count()['writes'] == baseline)
        held = []
        try:
            for _ in range(8):
                pending = connect()
                pending.sendall(('POST /api/projects HTTP/1.1\r\nHost: scanner.test\r\nAuthorization: ' + auth +
                                 '\r\nOrigin: ' + env['PUBLIC_ORIGIN'] + '\r\nContent-Length: 8\r\n\r\nx').encode())
                held.append(pending)
            time.sleep(0.3)
            record('per-ip-connection-limit-eight', request('/api/projects')[0] == 429)
        finally:
            for pending in held:
                pending.close()
        payload = json.dumps({'name': 'fixture', 'url': 'https://public.example/api', 'method': 'POST',
                              'contentType': 'application/json', 'requestTemplate': '{"message":"{{PROMPT}}","padding":"' + 'x' * 30000 + '"}',
                              'promptPlaceholder': '{{PROMPT}}', 'responsePath': '$.response', 'headers': {}, 'authType': 'NONE', 'timeoutMs': 5000})
        record('large-legitimate-body-proxy-accepts', request('/api/projects/fixture/target', 'PUT', payload)[0] == 201)
        record('proxy-strips-basic-authorization', count()['authorizationSeen'] is False)
        marker = 'APR_PRIVATE_MARKER_' + token
        request('/api/projects?' + marker, 'POST', marker, extra={'X-Fixture': marker})
        logs = run(['docker', 'logs', proxy]).stdout
        record('access-logs-no-markers-or-paths', marker not in logs and '/api/' not in logs and 'Authorization' not in logs)
        results['status'] = 'PASS'
    finally:
        # Solo contenedores/red de estas fixtures; nunca down -v ni BD compartida.
        run(['docker', 'rm', '-f', proxy, counter], check=False)
        run(['docker', 'network', 'rm', network], check=False)
        run(compose + ['down'], env=env, check=False)
        results.setdefault('status', 'FAIL')
        # Retirar únicamente material sintético creado por esta ejecución, fuera de Git.
        assert work.resolve().parent == Path(tempfile.gettempdir()).resolve() and work.name.startswith('apr-')
        for name in ('admin-password', 'app-password-pg', 'app-password-backend', 'target-aes', 'htpasswd',
                     'tls/active/fullchain.pem', 'tls/active/privkey.pem'):
            (work / name).unlink(missing_ok=True)
        results['fixtureSecretsRemoved'] = True
        (evidence / 'local-validation.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
        print('Evidence: ' + str(evidence / 'local-validation.json'))
        print('Synthetic secrets removed; test database volume retained: ' + volume)


if __name__ == '__main__':
    main()
