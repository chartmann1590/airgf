#!/usr/bin/env python3
"""Removes stray "draft" releases from the production track, leaving the
"completed"/in-review release untouched. One-off cleanup after a bug in
submit_release.py left an unused duplicate draft on the track.
"""
import json
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build

PACKAGE_NAME = "com.airgf.app"
TRACK = "production"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def main():
    raw = os.environ.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
    if not raw:
        print("::error::GOOGLE_PLAY_SERVICE_ACCOUNT_JSON is not set", file=sys.stderr)
        sys.exit(1)
    info = json.loads(raw)
    creds = service_account.Credentials.from_service_account_info(info, scopes=SCOPES)
    service = build("androidpublisher", "v3", credentials=creds)
    edits = service.edits()

    edit = edits.insert(packageName=PACKAGE_NAME, body={}).execute()
    edit_id = edit["id"]
    print(f"Created edit {edit_id}")

    track = edits.tracks().get(packageName=PACKAGE_NAME, editId=edit_id, track=TRACK).execute()
    releases = track.get("releases", [])
    for r in releases:
        print(f"Before: name={r.get('name')!r} status={r.get('status')!r} versionCodes={r.get('versionCodes')!r}")

    kept = [r for r in releases if r.get("status") != "draft"]
    removed = [r for r in releases if r.get("status") == "draft"]

    if not removed:
        print("No draft releases found - nothing to remove.")
        edits.delete(packageName=PACKAGE_NAME, editId=edit_id).execute()
        return

    for r in removed:
        print(f"Removing draft release: name={r.get('name')!r} versionCodes={r.get('versionCodes')!r}")

    edits.tracks().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        track=TRACK,
        body={"track": TRACK, "releases": kept},
    ).execute()

    edits.commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print(f"Committed edit {edit_id} - draft release(s) removed, in-review release untouched.")


if __name__ == "__main__":
    main()
