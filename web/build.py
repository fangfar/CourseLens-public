"""Package the static website and the app's release configuration."""
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


if __name__ == '__main__':
    config_bytes = (ROOT / 'remote/config.json').read_bytes()
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'preview/site'
    (output / 'remote').mkdir(parents=True, exist_ok=True)
    shutil.copytree(ROOT / 'web/assets', output / 'assets', dirs_exist_ok=True)
    for name in ('index.html', 'styles.css', 'download.js'):
        shutil.copy2(ROOT / 'web' / name, output / name)
    (output / 'remote/config.json').write_bytes(config_bytes)
    (output / '.nojekyll').touch()
