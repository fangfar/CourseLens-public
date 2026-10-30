import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


class RemoteConfigChecksTest(unittest.TestCase):
    def test_valid_config_and_invalid_inputs(self):
        root = Path(__file__).resolve().parent.parent
        check = root / "tests/RemoteConfigChecks.py"
        original = json.loads((root / "remote/config.json").read_text(encoding="utf-8"))
        with tempfile.TemporaryDirectory() as directory:
            remote = Path(directory) / "remote"
            remote.mkdir()
            def run(config):
                (remote / "config.json").write_text(json.dumps(config, ensure_ascii=False), encoding="utf-8")
                return subprocess.run([sys.executable, str(check)], cwd=directory, capture_output=True).returncode
            self.assertEqual(0, run(original))
            invalid = json.loads(json.dumps(original))
            invalid["android"]["apkUrl"] = "http://example.invalid/app.apk"
            self.assertNotEqual(0, run(invalid))
            invalid = json.loads(json.dumps(original))
            invalid["android"]["versionCode"] = True
            self.assertNotEqual(0, run(invalid))


if __name__ == "__main__":
    unittest.main()
