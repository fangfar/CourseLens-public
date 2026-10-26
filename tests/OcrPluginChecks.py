"""Verify host/plugin separation, package identity and signing; stage official plugin assets."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import urllib.parse
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def inspect(host, plugin, expected_certificate):
    host_package, host_certificate, host_files = host
    package, certificate, files = plugin
    assert host_package in ('cn.courselens', 'cn.courselens.qa'), 'Unexpected host package'
    expected = 'cn.courselens.ocr.qa' if host_package.endswith('.qa') else 'cn.courselens.ocr'
    assert package == expected, 'Unexpected plugin package'
    assert certificate == host_certificate, 'Host/plugin signer mismatch'
    if not host_package.endswith('.qa'):
        assert certificate == expected_certificate, 'Official signer mismatch'
    assert not any('mlkit' in name.lower() or name.startswith('lib/') for name in host_files), 'OCR engine leaked into host'
    assert any(name.startswith('assets/mlkit-google-ocr-models/') for name in files), 'OCR models missing'
    assert any('libmlkit_google_ocr_pipeline.so' in name for name in files), 'OCR engine missing'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host', type=Path, required=True)
    parser.add_argument('--plugin', type=Path, required=True)
    parser.add_argument('--release-base-url', help='HTTPS release directory for staging production assets')
    args = parser.parse_args()
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT']) / 'build-tools/34.0.0'

    def run(tool, *values):
        suffix = ('.bat' if tool == 'apksigner' else '.exe') if os.name == 'nt' else ''
        return subprocess.check_output([str(sdk / (tool + suffix)), *map(str, values)]).decode('utf-8', 'replace')

    def apk(path):
        badging = run('aapt', 'dump', 'badging', path)
        package = re.search(r"^package: name='([^']+)'", badging, re.M).group(1)
        signers = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([0-9a-f]{64})', run('apksigner', 'verify', '--print-certs', path))
        assert len(signers) == 1, 'Expected one signer'
        with zipfile.ZipFile(path) as archive:
            files = archive.namelist()
        return (package, signers[0], files), badging

    host, _ = apk(args.host)
    plugin, badging = apk(args.plugin)
    inspect(host, plugin, (ROOT / 'signing-certificate.sha256').read_text().strip())
    assert "uses-permission: name='android.permission.INTERNET'" not in badging, 'Offline plugin must not request network access'
    tree = run('aapt', 'dump', 'xmltree', args.plugin, 'AndroidManifest.xml')
    assert 'cn.courselens.ocr.PROTOCOL' in tree, 'Protocol metadata missing'
    print(f'PASS: host {args.host.stat().st_size:,} bytes; plugin {args.plugin.stat().st_size:,} bytes; signer/package/OCR isolation verified')
    if args.release_base_url:
        assert host[0] == 'cn.courselens', 'Cannot publish QA plugin'
        assert 'application-debuggable' not in badging, 'Cannot publish debug plugin'
        uri = urllib.parse.urlsplit(args.release_base_url)
        assert uri.scheme == 'https' and uri.hostname and not uri.username and not uri.password and uri.port in (None, 443), 'Invalid HTTPS download URL'
        code, name = re.search(r"versionCode='(\d+)' versionName='([^']+)'", badging).groups()
        filename = f'Morrow-OCR-{name}-vc{code}.apk'
        url = args.release_base_url.rstrip('/') + '/' + filename
        output = ROOT / 'app/build/release'
        output.mkdir(parents=True, exist_ok=True)
        shutil.copy2(args.plugin, output / filename)
        digest = hashlib.sha256(args.plugin.read_bytes()).hexdigest()
        descriptor = dict(schemaVersion=1, protocol=1, enabled=True, packageName=plugin[0], versionCode=int(code), bytes=args.plugin.stat().st_size, sha256=digest, url=url)
        (output / 'ocr-plugin.json').write_text(json.dumps(descriptor, indent=2) + '\n', encoding='utf-8')
        with (output / 'SHA256SUMS').open('a', encoding='utf-8') as checksums:
            checksums.write(f'{digest}  {filename}\n')
        print('Staged plugin APK and ocr-plugin.json; no upload or deployment performed')


if __name__ == '__main__':
    main()
