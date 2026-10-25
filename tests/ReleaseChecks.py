"""Validate a release tag and APK; write checksums and source metadata."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent.parent
PACKAGE = "cn.courselens"


def version(build, tag):
    name = re.search(r"versionName\s+'([^']+)'", build).group(1)
    code = int(re.search(r"versionCode\s+(\d+)", build).group(1))
    if not re.fullmatch(r"v\d+\.\d+(?:\.\d+)?", tag) or tag != f"v{name}" or code < 1:
        raise ValueError("Tag must match app/build.gradle versionName and versionCode")
    return name, code


def check_apk(badging, certificates, name, code, fingerprint):
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.M)
    if not package or package.groups() != (PACKAGE, str(code), name) or "application-debuggable" in badging:
        raise ValueError("APK must be the non-debuggable official package and version")
    signers = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]{64})", certificates)
    if len(signers) != 1 or signers[0].lower() != fingerprint:
        raise ValueError("APK signing certificate mismatch")


def check_new_version(code, previous_codes):
    if previous_codes and code <= max(previous_codes):
        raise ValueError("versionCode must be greater than every existing release tag")


def run(*command):
    return subprocess.check_output(command, cwd=ROOT).decode("utf-8", "replace")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tag")
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--prepare", action="store_true", help="Reject existing tags and non-increasing versionCode")
    args = parser.parse_args()
    name, code = version((ROOT / "app/build.gradle").read_text(encoding="utf-8"), args.tag)
    if args.prepare:
        tags = run("git", "tag", "--list", "v*").splitlines()
        if args.tag in tags:
            raise ValueError("Existing release tags must not be replaced")
        previous = []
        for tag in tags:
            if not re.fullmatch(r"v\d+\.\d+(?:\.\d+)?", tag):
                continue
            build = subprocess.run(["git", "show", f"{tag}:app/build.gradle"], cwd=ROOT, capture_output=True)
            # The historical public v2.4 tag contains release information only.
            if build.returncode == 0:
                previous.append(version(build.stdout.decode("utf-8"), tag)[1])
        check_new_version(code, previous)
    if not args.apk:
        print(f"Validated {args.tag} / versionCode {code}")
        return
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ["ANDROID_SDK_ROOT"]) / "build-tools/34.0.0"
    fingerprint = (ROOT / "signing-certificate.sha256").read_text().strip()
    badging = run(str(sdk / ("aapt.exe" if os.name == "nt" else "aapt")), "dump", "badging", str(args.apk.resolve()))
    certificates = run(str(sdk / ("apksigner.bat" if os.name == "nt" else "apksigner")), "verify", "--print-certs", str(args.apk.resolve()))
    check_apk(badging, certificates, name, code, fingerprint)
    output = ROOT / "app/build/release"
    output.mkdir(parents=True, exist_ok=True)
    filename = f"Morrow-{name}-vc{code}.apk"
    shutil.copyfile(args.apk, output / filename)
    digest = hashlib.sha256((output / filename).read_bytes()).hexdigest()
    (output / "SHA256SUMS").write_text(f"{digest}  {filename}\n", encoding="utf-8")
    metadata = {"tag": args.tag, "sourceCommit": run("git", "rev-parse", "HEAD").strip(), "package": PACKAGE,
                "versionName": name, "versionCode": code, "signingCertificateSHA256": fingerprint, "apkSHA256": digest}
    (output / "release-metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(f"Validated signed release APK: {filename} ({digest})")


if __name__ == "__main__":
    main()
