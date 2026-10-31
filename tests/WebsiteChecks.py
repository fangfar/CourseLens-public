"""Check the static site's local resources, links and accessibility basics."""
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit
import json
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1] / 'web'


class SiteParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.ids = set()
        self.links = []
        self.downloads = {}
        self.heading_count = 0

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if 'id' in attrs:
            assert attrs['id'] not in self.ids, f"Duplicate id: {attrs['id']}"
            self.ids.add(attrs['id'])
        if 'data-download' in attrs:
            self.downloads[attrs['data-download']] = attrs.get('class', '').split()
        self.heading_count += tag == 'h1'
        if tag == 'img':
            assert 'alt' in attrs, 'Image needs an alt attribute'
            assert int(attrs.get('width', 0)) > 0 and int(attrs.get('height', 0)) > 0, 'Image needs dimensions'
        for key in ('href', 'src'):
            if key in attrs:
                self.links.append(attrs[key])


html = (ROOT / 'index.html').read_text(encoding='utf-8')
assert html.count('data-download="lite"') == 1 and 'data-download="full"' not in html
assert '下载安卓版' in html
assert html.count('href="https://github.com/fangfar/Morrow"') == 2
page = SiteParser()
page.feed(html)
page.close()
assert page.heading_count == 1, 'Site needs one primary heading'
assert 'button' in page.downloads['lite'] and 'secondary' not in page.downloads['lite'], 'Download needs the primary button'
for link in page.links:
    uri = urlsplit(link)
    if uri.scheme or uri.netloc:
        assert uri.scheme == 'https' and uri.hostname and not uri.username and not uri.password, link
    elif uri.path:
        target = (ROOT / unquote(uri.path)).resolve()
        assert target.is_relative_to(ROOT.resolve()) and target.is_file(), f'Missing local resource: {link}'
    elif uri.fragment:
        assert unquote(uri.fragment) in page.ids, f'Missing anchor: {link}'
    else:
        raise AssertionError(f'Empty link: {link}')
print(f'Website checks passed: {len(page.links)} links and resources')

with tempfile.TemporaryDirectory() as directory:
    subprocess.run([sys.executable, str(ROOT / 'build.py'), directory], check=True)
    generated = (Path(directory) / 'index.html').read_text(encoding='utf-8')
    config = json.loads((Path(directory) / 'remote/config.json').read_text(encoding='utf-8'))
    for edition in ('lite',):
        assert f'data-download="{edition}" href="{config["android"][edition + "ApkUrl"]}"' in generated, f'{edition} must link directly to its APK without JavaScript'
