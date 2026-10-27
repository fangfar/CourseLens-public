"""Run the same business regressions locally and in CI (JDK 17 required)."""
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml")
android_name = "{http://schemas.android.com/apk/res/android}name"
assert any(action.get(android_name) == "android.provider.Telephony.SMS_DELIVER" for action in manifest.findall("./queries/intent/action")), "default SMS package visibility missing"
models = ("Course", "ClassTimes", "CourseDisplay", "NextClass", "CourseAdjustment", "WidgetSchedule", "ChaoxingHomework", "TaskRefresh", "ParcelRules")
tests = sorted((root / "tests").glob("*Test.java"))
with tempfile.TemporaryDirectory() as output:
    sources = [root / f"app/src/main/java/cn/courselens/{name}.java" for name in models]
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", output, *map(str, sources + tests)], cwd=root, check=True)
    for test in tests:
        name = "cn.courselens." + test.stem if test.stem in ("ChaoxingHomeworkTest", "TaskRefreshTest", "ParcelRulesTest") else test.stem
        subprocess.run(["java", "-ea", "-cp", output, name], cwd=root, check=True)
