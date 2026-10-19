"""Run the same business regressions locally and in CI (JDK 17 required)."""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parent.parent
models = ("Course", "ClassTimes", "CourseDisplay", "NextClass", "CourseAdjustment", "WidgetSchedule", "ChaoxingHomework")
tests = sorted((root / "tests").glob("*Test.java"))
with tempfile.TemporaryDirectory() as output:
    sources = [root / f"app/src/main/java/cn/courselens/{name}.java" for name in models]
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", output, *map(str, sources + tests)], cwd=root, check=True)
    for test in tests:
        name = "cn.courselens." + test.stem if test.stem == "ChaoxingHomeworkTest" else test.stem
        subprocess.run(["java", "-ea", "-cp", output, name], cwd=root, check=True)
