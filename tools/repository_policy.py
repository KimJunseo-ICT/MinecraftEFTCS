"""Validate repository/version rules only; never start Minecraft or gameplay tests."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


def require(condition, message):
    if not condition:
        raise SystemExit(message)


def version():
    content = Path("gradle.properties").read_text(encoding="utf-8-sig")
    match = re.search(r"^mod_version=(\S+)$", content, re.M)
    require(match, "Missing mod_version")
    value = match.group(1)
    require(re.fullmatch(r"\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?", value), "Use a semantic version")
    return value


def release_notes(value):
    content = Path("CHANGELOG.md").read_text(encoding="utf-8-sig")
    match = re.search(r"^## \[" + re.escape(value) + r"\] - \d{4}-\d{2}-\d{2}\s*$\n(.*?)(?=^## |\Z)", content, re.M | re.S)
    require(match and match.group(1).strip(), f"Missing CHANGELOG entry for {value}")
    return match.group(1).strip()


def check_pr(event):
    pr = event["pull_request"]
    head, base = pr["head"]["ref"], pr["base"]["ref"]
    require(pr["head"]["repo"]["full_name"] == pr["base"]["repo"]["full_name"] or head != "main", "main sync must originate in this repository")
    if base == "main":
        require(re.fullmatch(r"(?:release|hotfix)/[^/]+", head), "main accepts release/* or hotfix/* only")
    elif base == "develop":
        require(head == "main" or re.fullmatch(r"(?:feature|release|hotfix)/.+", head), "develop accepts feature/*, release/*, hotfix/* or main sync")
    else:
        raise SystemExit("Unsupported PR target")
    if head != "main":
        changed = git("diff", "--name-only", f"{pr['base']['sha']}...{pr['head']['sha']}").splitlines()
        require("CHANGELOG.md" in changed, "Update CHANGELOG.md for every upload")
    if head.startswith(("release/", "hotfix/")):
        value = version()
        require(head.split("/", 1)[1] == value, "Branch version must match mod_version")
        release_notes(value)
    print(f"Git-flow valid: {head} -> {base}")


def audit_files():
    tracked = git("ls-files", "-z").split("\0")
    for name in filter(None, tracked):
        root = name.split("/", 1)[0]
        require(root not in {"build", "run", ".gradle", ".gradle-user-home", ".idea", ".codex", ".agents"} and not root.startswith("run-"), f"Local runtime/cache file tracked: {name}")
        require(not name.endswith(".jar") or name == "gradle/wrapper/gradle-wrapper.jar", f"External JAR tracked: {name}")
        require(not name.endswith(".tsrg") or name == "libs/tacz-extra-mappings.tsrg", f"Generated mapping tracked: {name}")
        path = Path(name)
        if path.is_file():
            require(path.stat().st_size < 50 * 1024 * 1024, f"Unexpected large file: {name}")
            if path.suffix.lower() in {".java", ".json", ".properties", ".ps1", ".py", ".md", ".txt", ".yml", ".yaml"}:
                content = path.read_text(encoding="utf-8-sig", errors="replace")
                require(not re.search(r"(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)", content), f"Possible credential in: {name}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag")
    args = parser.parse_args()
    audit_files()
    if args.tag:
        value = version()
        require(args.tag == f"v{value}", "Tag must match mod_version")
        release_notes(value)
        require(subprocess.call(["git", "merge-base", "--is-ancestor", "HEAD", "origin/main"]) == 0, "Release commit must be on main")
    elif os.environ.get("GITHUB_EVENT_NAME") == "pull_request":
        check_pr(json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text(encoding="utf-8")))
    print("Repository policy passed (no gameplay tests run)")


if __name__ == "__main__":
    main()
