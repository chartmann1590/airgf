#!/usr/bin/env python3
"""Read-only: prints the production track's current releases, one per line,
no nested JSON dump, so log output can't get jumbled with tracebacks.
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

    track = edits.tracks().get(packageName=PACKAGE_NAME, editId=edit_id, track=TRACK).execute()
    releases = track.get("releases", [])
    print(f"RELEASE_COUNT={len(releases)}")
    for r in releases:
        print(f"RELEASE name={r.get('name')!r} status={r.get('status')!r} versionCodes={r.get('versionCodes')!r}")

    edits.delete(packageName=PACKAGE_NAME, editId=edit_id).execute()


if __name__ == "__main__":
    main()
