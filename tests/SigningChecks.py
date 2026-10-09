"""Check signing configuration without copying private material into the fixture."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parent.parent
names = ("COURSELENS_KEYSTORE_FILE", "COURSELENS_STORE_PASSWORD", "COURSELENS_KEY_ALIAS", "COURSELENS_KEY_PASSWORD")
env = os.environ.copy()
for name in names:
    env.pop(name, None)
with tempfile.TemporaryDirectory() as folder:
    fixture = Path(folder)
    (fixture / "app").mkdir()
    for name in ("settings.gradle", "build.gradle", "signing-certificate.sha256"):
        shutil.copy2(root / name, fixture / name)
    shutil.copy2(root / "app/build.gradle", fixture / "app/build.gradle")
    if (root / "local.properties").exists():
        shutil.copy2(root / "local.properties", fixture / "local.properties")
    wrapper = root / ("gradlew.bat" if os.name == "nt" else "gradlew")

    def check(label, args, expected, environment=env):
        command = [str(wrapper)] if os.name == "nt" else ["sh", str(wrapper)]
        result = subprocess.run([*command, "--no-daemon", "-p", str(fixture), "help", *args], cwd=root, env=environment, capture_output=True, text=True)
        if expected:
            assert result.returncode != 0 and expected in result.stdout + result.stderr, label
        else:
            assert result.returncode == 0, label
        print("PASS:", label)

    check("missing key fails instead of generating a signer", [], "Fixed signing is required")
    check("PR verification works without signing secrets", ["-PqaBuild"], None)
    props = {}
    if (root / "signing.properties").exists():
        props = dict(line.split("=", 1) for line in (root / "signing.properties").read_text().splitlines() if "=" in line and not line.startswith("#"))
    signed = env.copy()
    for name, prop in zip(names, ("storeFile", "storePassword", "keyAlias", "keyPassword")):
        signed[name] = os.environ.get(name) or props[prop]
    check("configured fixed signer accepted", [], None, signed)
    (fixture / "signing-certificate.sha256").write_text("0" * 64)
    check("unexpected signer rejected", [], "Signing certificate mismatch", signed)
