"""Checks that release validation rejects mismatched or unsafe artifacts."""
import unittest
from unittest.mock import patch
from pathlib import Path
import subprocess
import tempfile
import ReleaseChecks
from ReleaseChecks import check_apk, check_new_version, version


class ReleaseChecksTest(unittest.TestCase):
    def test_edition_cannot_mislabel_ocr_contents(self):
        files = ['assets/mlkit-google-ocr-models/model', 'lib/arm64-v8a/libmlkit_google_ocr_pipeline.so']
        ReleaseChecks.check_edition(files, 'full')
        ReleaseChecks.check_edition(['classes.dex'], 'lite')
        for contents, edition in [(files, 'lite'), (['classes.dex'], 'full'), (files[:1], 'full')]:
            with self.assertRaises(ValueError):
                ReleaseChecks.check_edition(contents, edition)
    def test_prepare_with_legacy_tag_without_source(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            def git(*args):
                subprocess.run(["git", *args], cwd=root, check=True, capture_output=True)
            git("init")
            (root / "README.md").write_text("Legacy release information")
            git("add", ".")
            git("-c", "user.name=Fixture", "-c", "user.email=fixture@example.org", "commit", "-m", "Legacy tag")
            git("tag", "v2.4")
            (root / "app").mkdir()
            build = root / "app/build.gradle"
            build.write_text("versionCode 8; versionName '2.6'")
            git("add", ".")
            git("-c", "user.name=Fixture", "-c", "user.email=fixture@example.org", "commit", "-m", "Source tag")
            git("tag", "v2.6")
            with patch.object(ReleaseChecks, "ROOT", root), patch("sys.argv", ["ReleaseChecks.py", "v2.6", "--prepare"]):
                with self.assertRaisesRegex(ValueError, "Existing release tags"):
                    ReleaseChecks.main()
            build.write_text("versionCode 8; versionName '2.7'")
            with patch.object(ReleaseChecks, "ROOT", root), patch("sys.argv", ["ReleaseChecks.py", "v2.7", "--prepare"]):
                with self.assertRaisesRegex(ValueError, "versionCode must be greater"):
                    ReleaseChecks.main()
                build.write_text("versionCode 9; versionName '2.7'")
                ReleaseChecks.main()

    def test_version_code_must_increase(self):
        check_new_version(9, [6, 7, 8])
        check_new_version(1, [])
        for code in (7, 8):
            with self.subTest(code=code), self.assertRaises(ValueError):
                check_new_version(code, [6, 7, 8])

    def test_tag_must_match_source(self):
        build = "versionCode 8; versionName '2.6'"
        self.assertEqual(version(build, "v2.6"), ("2.6", 8))
        for tag in ("v2.7", "2.6", "v2.6;echo x", "../v2.6"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                version(build, tag)

    def test_apk_identity_and_signature(self):
        badging = "package: name='cn.courselens' versionCode='8' versionName='2.6'"
        certificate = "Signer #1 certificate SHA-256 digest: " + "a" * 64
        check_apk(badging, certificate, "2.6", 8, "a" * 64)
        for invalid in (badging.replace("cn.courselens", "cn.courselens.qa"), badging.replace("Code='8'", "Code='9'"),
                        badging.replace("Name='2.6'", "Name='2.7'"), badging + "\napplication-debuggable", ""):
            with self.subTest(badging=invalid), self.assertRaises(ValueError):
                check_apk(invalid, certificate, "2.6", 8, "a" * 64)
        for invalid in ("", certificate.replace("a" * 64, "b" * 64), certificate + "\n" + certificate):
            with self.subTest(certificate=invalid), self.assertRaises(ValueError):
                check_apk(badging, invalid, "2.6", 8, "a" * 64)


if __name__ == "__main__":
    unittest.main()
