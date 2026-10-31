"""Select the lightweight gate only for a PR changing remote/config.json alone."""
import os
import subprocess


def config_only(event, base, head, cwd=None):
    if event != "pull_request" or not base or not head:
        return False
    try:
        result = subprocess.run(
            ["git", "diff", "--no-renames", "--name-only", "-z", base, head, "--"],
            cwd=cwd, capture_output=True, check=True,
        )
    except (OSError, subprocess.CalledProcessError):
        return False
    return result.stdout.split(b"\0") == [b"remote/config.json", b""]


if __name__ == "__main__":
    print(str(config_only(os.getenv("EVENT_NAME"), os.getenv("BASE_SHA"), os.getenv("HEAD_SHA"))).lower())
