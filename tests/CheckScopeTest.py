import subprocess
import tempfile
import unittest
from pathlib import Path
from CheckScope import config_only


class CheckScopeTest(unittest.TestCase):
    def test_config_only_and_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            repo = Path(directory)
            def git(*args):
                return subprocess.check_output(["git", "-C", directory, *args], stderr=subprocess.DEVNULL).decode().strip()
            git("init", "-q")
            git("config", "user.name", "test")
            git("config", "user.email", "test@example.invalid")
            (repo / "remote").mkdir()
            (repo / "remote/config.json").write_text("{}")
            (repo / "app.java").write_text("old")
            git("add", ".")
            git("commit", "-qm", "base")
            base = git("rev-parse", "HEAD")
            (repo / "remote/config.json").write_text('{"changed":true}')
            git("add", ".")
            git("commit", "-qm", "config")
            config = git("rev-parse", "HEAD")
            self.assertTrue(config_only("pull_request", base, config, directory))
            self.assertTrue(config_only("push", base, config, directory))
            self.assertFalse(config_only("workflow_dispatch", base, config, directory))
            self.assertFalse(config_only("pull_request", base, base, directory))
            self.assertFalse(config_only("pull_request", "missing", config, directory))
            (repo / "app.java").write_text("new")
            git("add", ".")
            git("commit", "-qm", "mixed")
            self.assertFalse(config_only("pull_request", base, git("rev-parse", "HEAD"), directory))
            git("mv", "remote/config.json", "remote/other.json")
            git("commit", "-qm", "rename")
            self.assertFalse(config_only("pull_request", config, git("rev-parse", "HEAD"), directory))


if __name__ == "__main__":
    unittest.main()
