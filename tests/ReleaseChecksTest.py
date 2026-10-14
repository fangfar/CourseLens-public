"""Checks that release validation rejects mismatched or unsafe artifacts."""
import unittest
from ReleaseChecks import check_apk, check_new_version, version


class ReleaseChecksTest(unittest.TestCase):
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
