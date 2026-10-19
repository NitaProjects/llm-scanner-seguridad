#!/usr/bin/env python3
"""Host AWS únicamente: nombres de parámetros, nunca valores en argumentos/logs."""
import argparse
import os
from pathlib import Path
import subprocess
import sys


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--directory', default='/run/llm-scanner')
    parser.add_argument('--region', required=True)
    for name in ('admin-password', 'app-password', 'target-aes', 'htpasswd'):
        parser.add_argument('--' + name + '-parameter', required=True)
    args = parser.parse_args()
    directory = Path(args.directory).resolve()
    # No escribir secretos en disco persistente ni en un checkout.
    mount = subprocess.run(['findmnt', '-n', '-o', 'FSTYPE', '-T', str(directory.parent)], capture_output=True, text=True, check=True)
    if os.geteuid() != 0 or mount.stdout.strip() != 'tmpfs' or not str(directory).startswith('/run/'):
        raise ValueError('invalid destination')
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    os.chmod(directory, 0o711)
    targets = {
        'admin-password': [('admin-password', 70)],
        'app-password': [('app-password-pg', 70), ('app-password-backend', 10001)],
        'target-aes': [('target-aes', 10001)],
        'htpasswd': [('htpasswd', 101)],
    }
    for name, copies in targets.items():
        parameter = getattr(args, name.replace('-', '_') + '_parameter')
        result = subprocess.run(['aws', 'ssm', 'get-parameter', '--region', args.region, '--name', parameter,
                                 '--with-decryption', '--query', 'Parameter.Value', '--output', 'text'],
                                capture_output=True, check=True)
        value = result.stdout.rstrip(b'\r\n')
        if not value:
            raise ValueError('empty parameter')
        for filename, owner in copies:
            path = directory / filename
            fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC | os.O_NOFOLLOW, 0o400)
            with os.fdopen(fd, 'wb') as stream:
                stream.write(value)
            os.chown(path, owner, owner)
            os.chmod(path, 0o400)
    print('SECRETS_READY')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        print('SECRETS_BOOTSTRAP_FAILED', file=sys.stderr)
        sys.exit(1)
