#!/usr/bin/env python3
"""Uploads the Amoura store listing text and graphics to Google Play Console
via the Android Publisher API, as its own self-contained edit (separate from
the AAB/release upload, which the upload-google-play GitHub Action owns).

Requires:
  - GOOGLE_PLAY_SERVICE_ACCOUNT_JSON env var: service account JSON, plaintext
  - google-api-python-client, google-auth installed

Usage:
  python upload_listing.py
"""
import json
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build

PACKAGE_NAME = "com.airgf.app"
LANGUAGE = "en-US"
REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))

TITLE = "Amoura - AI Girlfriend App"
SHORT_DESCRIPTION = (
    "AI girlfriend & AI boyfriend chat companion - private, on-device, always yours"
)
FULL_DESCRIPTION = """Amoura is your private AI girlfriend or boyfriend - a virtual companion that lives entirely on your phone. No accounts, no cloud, no data collection. Just you and your AI companion, always available.

Whether you're looking for an AI girlfriend, an AI boyfriend, or just a caring virtual companion to talk to, Amoura adapts to you. Pick their look, personality, voice, and relationship style, and start chatting in minutes.

WHY PEOPLE LOVE AMOURA

On-Device AI - Every conversation runs locally using an on-device AI model (Gemma 4). No internet required to chat, no servers involved, nothing about your conversations ever leaves your phone.

Choose Your Companion - Girlfriend, boyfriend, or partner. Pick their name, appearance, personality traits, and communication style.

3D Avatars - Multiple unique 3D characters with real-time emotional expressions, lip sync, and idle animation that make your companion feel alive.

Photo Sharing - Send photos and your AI companion understands and reacts to them, or generate images together using on-device Stable Diffusion.

Voice & Lip Sync - Multiple voice profiles with natural, word-level lip-synced speech.

Memory & Personality - Your companion remembers what matters to you and adapts their personality and communication style over time.

Proactive Messages - Your AI girlfriend or boyfriend can reach out to you first, on a schedule you control.

Spicy Mode - Romantic and flirty conversation, off by default. Unlock it permanently with a subscription (which also removes all ads), or temporarily by watching a rewarded ad - up to 4 hours a day, free.

100% Private - On-device conversations, no account required, no chat data collection.

Amoura is a free AI companion app, supported by ads, with an optional subscription for an ad-free, unlimited experience.

Amoura is intended for adult users (18+) and includes romantic and relationship-oriented content."""

ICON_PATH = os.path.join(REPO_ROOT, "play-store", "graphics", "icon", "icon-512.png")
FEATURE_GRAPHIC_PATH = os.path.join(
    REPO_ROOT, "play-store", "graphics", "feature-graphic", "feature-graphic-1024x500.png"
)
SCREENSHOTS_DIR = os.path.join(REPO_ROOT, "play-store", "screenshots", "phone", "framed")
SCREENSHOT_ORDER = [
    "01-welcome.png",
    "02-setup-complete.png",
    "03-chat.png",
    "04-character.png",
    "05-settings.png",
    "06-settings-spicy.png",
]

SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def get_service():
    raw = os.environ.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
    if not raw:
        print("::error::GOOGLE_PLAY_SERVICE_ACCOUNT_JSON is not set", file=sys.stderr)
        sys.exit(1)
    info = json.loads(raw)
    creds = service_account.Credentials.from_service_account_info(info, scopes=SCOPES)
    return build("androidpublisher", "v3", credentials=creds)


def upload_image(service, edit_id, image_type, path):
    with open(path, "rb") as f:
        service.edits().images().upload(
            packageName=PACKAGE_NAME,
            editId=edit_id,
            language=LANGUAGE,
            imageType=image_type,
            media_body=path,
        ).execute()
    print(f"  uploaded {image_type}: {os.path.basename(path)}")


def main():
    service = get_service()

    edit = service.edits().insert(packageName=PACKAGE_NAME, body={}).execute()
    edit_id = edit["id"]
    print(f"Created edit {edit_id}")

    service.edits().listings().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        language=LANGUAGE,
        body={
            "language": LANGUAGE,
            "title": TITLE,
            "shortDescription": SHORT_DESCRIPTION,
            "fullDescription": FULL_DESCRIPTION,
        },
    ).execute()
    print(f"Updated store listing text ({LANGUAGE})")

    # Clear existing images of each type first so re-runs don't just append
    # duplicates - safe no-op on a fresh app with nothing uploaded yet.
    for image_type in ["icon", "featureGraphic", "phoneScreenshots"]:
        try:
            service.edits().images().deleteall(
                packageName=PACKAGE_NAME,
                editId=edit_id,
                language=LANGUAGE,
                imageType=image_type,
            ).execute()
        except Exception as exc:  # noqa: BLE001 - best-effort cleanup
            print(f"  (skip clearing {image_type}: {exc})")

    upload_image(service, edit_id, "icon", ICON_PATH)
    upload_image(service, edit_id, "featureGraphic", FEATURE_GRAPHIC_PATH)
    for filename in SCREENSHOT_ORDER:
        upload_image(service, edit_id, "phoneScreenshots", os.path.join(SCREENSHOTS_DIR, filename))

    service.edits().commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print(f"Committed edit {edit_id} - store listing and graphics are live in Play Console")


if __name__ == "__main__":
    main()
