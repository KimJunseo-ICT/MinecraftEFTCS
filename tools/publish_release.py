"""Publish the matching CHANGELOG section; authentication comes from Actions only."""
import json
import os
import urllib.error
import urllib.request
from repository_policy import release_notes, version

value = version()
tag = f"v{value}"
base = f"https://api.github.com/repos/{os.environ['GITHUB_REPOSITORY']}/releases"
headers = {
    "Authorization": f"Bearer {os.environ['GITHUB_TOKEN']}",
    "Accept": "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "User-Agent": "MinecraftEFTCS-release",
}
body = {"tag_name": tag, "name": tag, "body": release_notes(value), "draft": False, "prerelease": "-" in value}
try:
    with urllib.request.urlopen(urllib.request.Request(f"{base}/tags/{tag}", headers=headers)) as response:
        existing = json.load(response)
except urllib.error.HTTPError as error:
    if error.code != 404:
        raise
    existing = None
if existing:
    # Do not overwrite edits already made to a published release.
    print(f"Release already exists: {existing['html_url']}")
else:
    request = urllib.request.Request(base, data=json.dumps(body).encode(), headers={**headers, "Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(request) as response:
        print(json.load(response)["html_url"])
