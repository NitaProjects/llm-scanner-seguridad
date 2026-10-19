"""Inspección estructural offline: parser YAML de Compose, sin invocar CloudFormation."""
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

root = Path(__file__).resolve().parents[3]
source = (root / 'docker/production/aws.yml').read_text(encoding='utf-8')
# x-* permite reutilizar el parser YAML instalado, sin dependencia nueva. El servicio no se ejecuta.
wrapped = 'services: {fixture: {image: not-run:fixture}}\nx-template:\n'
wrapped += '\n'.join('  ' + line.replace('$', '$$') for line in source.splitlines())
with tempfile.TemporaryDirectory(prefix='apr-template-') as directory:
    empty = Path(directory) / 'empty-vars'
    empty.write_text('')
    parsed = subprocess.run(['docker', 'compose', '--env-file', str(empty), '-p', 'apr-template', '-f', '-',
                             'config', '--format', 'json'], input=wrapped, capture_output=True, text=True, check=True)
    template = json.loads(parsed.stdout)['x-template']
resources = template['Resources']
ingress = resources['SecurityGroup']['Properties']['SecurityGroupIngress']
assert len(ingress) == 1 and ingress[0]['FromPort'] == ingress[0]['ToPort'] == 443
assert template['Parameters']['RestrictIngressIp']['Default'] == 'false'
metadata = resources['Instance']['Properties']['MetadataOptions']
assert metadata['HttpTokens'] == 'required' and metadata['HttpPutResponseHopLimit'] == 1
assert metadata['HttpProtocolIpv6'] == 'disabled'
assert resources['DataVolume']['DeletionPolicy'] == resources['DataVolume']['UpdateReplacePolicy'] == 'Retain'
assert resources['DataVolume']['Properties']['Encrypted'] is True
assert 'UserData' not in resources['Instance']['Properties']
assert 'KeyName' not in resources['Instance']['Properties']
assert 'ManagedPolicyArns' not in resources['Role']['Properties']
statements = resources['Role']['Properties']['Policies'][0]['PolicyDocument']['Statement']
for statement in statements:
    actions = statement['Action'] if isinstance(statement['Action'], list) else [statement['Action']]
    if any(action in ('ssm:GetParameter', 'ssm:GetParameters') for action in actions):
        assert statement['Resource'] != '*'

# Bootstrap desde cero: CloudFormation crea los repositorios antes de conocer imágenes.
assert 'BackendImageDigest' not in template['Parameters']
assert 'IngressImageDigest' not in template['Parameters']
assert 'BackendImageDigest' not in template['Outputs']
assert 'IngressImageDigest' not in template['Outputs']
expected_pull_resources = [{'Fn::GetAtt': [name, 'Arn']}
                           for name in ('BackendRepository', 'IngressRepository')]
pull_statements = []
for statement in statements:
    actions = statement['Action'] if isinstance(statement['Action'], list) else [statement['Action']]
    ecr_actions = [action for action in actions if action.startswith('ecr:')]
    if ecr_actions and ecr_actions != ['ecr:GetAuthorizationToken']:
        assert set(ecr_actions) == {'ecr:BatchGetImage', 'ecr:GetDownloadUrlForLayer', 'ecr:BatchCheckLayerAvailability'}
        assert statement['Resource'] == expected_pull_resources
        pull_statements.append(statement)
assert len(pull_statements) == 1

repositories = {}
for name in ('BackendRepository', 'IngressRepository'):
    assert resources[name]['Type'] == 'AWS::ECR::Repository'
    assert 'RepositoryName' not in resources[name]['Properties']
    assert template['Outputs'][name]['Value'] == {'Fn::GetAtt': [name, 'RepositoryUri']}
    assert not any('Image' in parameter for parameter in template['Parameters'])
    # Outputs sintéticos, sin consultar AWS ni registrar recursos reales.
    repositories[name] = '000000000000.dkr.ecr.us-east-1.amazonaws.com/fixture-' + name.lower()

# Segunda fase: las referencias publicadas se inyectan al proceso Compose, no al stack.
env = {name: os.environ[name] for name in ('PATH', 'SystemRoot', 'TEMP', 'TMP', 'USERPROFILE') if name in os.environ}
with tempfile.TemporaryDirectory(prefix='apr-bootstrap-') as directory:
    empty = Path(directory) / 'empty-vars'
    empty.write_text('')
    env.update(PUBLIC_HOSTNAME='scanner.test', PUBLIC_ORIGIN='https://scanner.test',
               SECRETS_DIR=directory, TLS_DIR=directory, PGDATA_VOLUME='apr-not-created',
               BACKEND_IMAGE=repositories['BackendRepository'] + '@sha256:' + 'a' * 64,
               INGRESS_IMAGE=repositories['IngressRepository'] + '@sha256:' + 'b' * 64)
    command = ['docker', 'compose', '--env-file', str(empty), '-p', 'apr-bootstrap',
               '-f', str(root / 'docker/production/compose.yml'), 'config', '--format', 'json']
    configured = subprocess.run(command, env=env, capture_output=True, text=True, check=True)
    services = json.loads(configured.stdout)['services']
    assert services['backend']['image'] == env['BACKEND_IMAGE']
    assert services['ingress']['image'] == env['INGRESS_IMAGE']
    assert set(services) == {'backend', 'ingress', 'postgres'}
    for missing in ('BACKEND_IMAGE', 'INGRESS_IMAGE'):
        incomplete = dict(env)
        del incomplete[missing]
        rejected = subprocess.run(command, env=incomplete, capture_output=True, text=True)
        assert rejected.returncode != 0

# Auditar instrucciones vigentes, sin ejecutar Maven ni modificar permisos del checkout.
documents = [root / 'Readme.md', root / 'docker/production/README.md',
             root / 'openspec/changes/aws-production-readiness/design.md']
for document in documents:
    text = document.read_text(encoding='utf-8')
    for match in re.finditer(r'(?<![\w/])\./mvnw\b', text):
        assert re.search(r'\bsh\s+$', text[:match.start()]), str(document) + ': wrapper without sh'
for name in ('backend', 'demo-vulnerable'):
    tracked = subprocess.run(['git', 'ls-files', '--stage', '--', name + '/mvnw'],
                             cwd=root, capture_output=True, text=True, check=True)
    assert tracked.stdout.startswith('100644 ')
result = {'status': 'PASS', 'checks': ['YAML parsed', '443 only', 'optional IP restriction off',
          'IMDSv2 hop1', 'EBS encrypted retained', 'no SSH/UserData', 'SSM parameter scope restricted',
          'initial stack needs no image digest', 'generated repository URI outputs preserved',
          'ECR pull restricted to created repositories', 'post-publish digests reach Compose services',
          'Compose rejects either missing image reference', 'POSIX Maven documentation uses sh',
          'wrapper Git modes unchanged 100644'],
          'awsServiceValidation': 'NOT_EXECUTED', 'deployment': 'NOT_EXECUTED'}
(root / 'openspec/changes/aws-production-readiness/evidence/infrastructure.json').write_text(json.dumps(result, indent=2))
print(json.dumps(result))
