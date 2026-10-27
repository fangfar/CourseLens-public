"""Package the static website and the app's release configuration."""
import shutil
import sys
import json
import html
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]


if __name__ == '__main__':
    config_bytes = (ROOT / 'remote/config.json').read_bytes()
    config = json.loads(config_bytes)
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'preview/site'
    (output / 'remote').mkdir(parents=True, exist_ok=True)
    shutil.copytree(ROOT / 'web/assets', output / 'assets', dirs_exist_ok=True)
    for name in ('index.html', 'styles.css', 'download.js'):
        shutil.copy2(ROOT / 'web' / name, output / name)
    page = (output / 'index.html').read_text(encoding='utf-8')
    for edition in ('full', 'lite'):
        url = config['android'][edition + 'ApkUrl']
        parsed = urlsplit(url)
        if (parsed.scheme != 'https' or parsed.netloc != 'github.com' or parsed.query or parsed.fragment
                or not parsed.path.startswith('/fangfar/Morrow/releases/download/') or not parsed.path.endswith('.apk')):
            raise ValueError(f'Invalid {edition} APK URL')
        old = f'data-download="{edition}" href="https://github.com/fangfar/Morrow/releases/latest"'
        if page.count(old) != 1:
            raise ValueError(f'Missing {edition} download link')
        page = page.replace(old, f'data-download="{edition}" href="{html.escape(url, quote=True)}"')
    (output / 'index.html').write_text(page, encoding='utf-8')
    (output / 'remote/config.json').write_bytes(config_bytes)
    shutil.copy2(ROOT / 'remote/ocr-plugin.json', output / 'remote/ocr-plugin.json')
    (output / '.nojekyll').touch()
