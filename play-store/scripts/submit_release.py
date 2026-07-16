#!/usr/bin/env python3
"""Submits the existing draft release on the production track for Google's
review, by flipping its status from "draft" to "completed" - without
rebuilding or re-uploading a new AAB. Run via submit-release.yml.

Requires:
  - GOOGLE_PLAY_SERVICE_ACCOUNT_JSON env var: service account JSON, plaintext
"""
import json
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build

PACKAGE_NAME = "com.airgf.app"
TRACK = "production"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def get_service():
    raw = os.environ.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
    if not raw:
        print("::error::GOOGLE_PLAY_SERVICE_ACCOUNT_JSON is not set", file=sys.stderr)
        sys.exit(1)
    info = json.loads(raw)
    creds = service_account.Credentials.from_service_account_info(info, scopes=SCOPES)
    return build("androidpublisher", "v3", credentials=creds)


def main():
    service = get_service()
    edits = service.edits()

    edit = edits.insert(packageName=PACKAGE_NAME, body={}).execute()
    edit_id = edit["id"]
    print(f"Created edit {edit_id}")

    track = edits.tracks().get(packageName=PACKAGE_NAME, editId=edit_id, track=TRACK).execute()
    print(json.dumps(track, indent=2))

    releases = track.get("releases", [])
    draft_releases = [r for r in releases if r.get("status") == "draft"]
    if not draft_releases:
        print("::error::No draft release found on the production track - nothing to submit.")
        sys.exit(1)

    # Play only allows one "completed" release per track. If multiple drafts
    # stacked up (e.g. from separate publish runs), submit only the most
    # recent one (highest version code) and drop the rest - they're
    # superseded anyway.
    def max_version_code(release):
        codes = release.get("versionCodes", ["0"])
        return max(int(c) for c in codes)

    draft_releases.sort(key=max_version_code, reverse=True)
    to_submit, *superseded = draft_releases
    to_submit["status"] = "completed"
    print(f"Marking release {to_submit.get('name')} (versionCodes={to_submit.get('versionCodes')}) as completed")

    other_releases = [r for r in releases if r.get("status") != "draft"]
    if superseded:
        print(f"Dropping {len(superseded)} superseded draft release(s): "
              f"{[r.get('name') for r in superseded]}")
    releases = other_releases + [to_submit]

    edits.tracks().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        track=TRACK,
        body={"track": TRACK, "releases": releases},
    ).execute()

    edits.commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print(f"Committed edit {edit_id} - release submitted for Google review.")


if __name__ == "__main__":
    main()
