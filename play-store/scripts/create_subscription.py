#!/usr/bin/env python3
"""Creates and activates the Amoura Premium subscription in Google Play
Console via the Android Publisher (Monetization) API. Intended to be run
once, manually, via the create-subscription.yml workflow_dispatch workflow.

Requires:
  - GOOGLE_PLAY_SERVICE_ACCOUNT_JSON env var: service account JSON, plaintext
  - google-api-python-client, google-auth installed
"""
import json
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

PACKAGE_NAME = "com.airgf.app"
# Must exactly match SubscriptionRepositoryImpl.SUBSCRIPTION_PRODUCT_ID in the app.
SUBSCRIPTION_ID = "amoura_premium_monthly"
BASE_PLAN_ID = "amoura-premium-monthly"

PRICE_USD_UNITS = 7
PRICE_USD_NANOS = 990000000  # $7.99
PRICE_EUR_UNITS = 7
PRICE_EUR_NANOS = 490000000  # EUR7.49

TITLE = "Amoura Premium"
BENEFITS = [
    "Unlock Spicy Mode permanently",
    "Remove all ads",
]
DESCRIPTION = (
    "Amoura Premium unlocks Spicy Mode permanently and removes all ads. "
    "Prefer not to subscribe? You can also unlock Spicy Mode temporarily, "
    "for free, by watching rewarded ads in Settings (up to 4 hours/day)."
)

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
    service = service_account_service = get_service()
    subs = service.monetization().subscriptions()

    body = {
        "packageName": PACKAGE_NAME,
        "productId": SUBSCRIPTION_ID,
        "listings": [
            {
                "languageCode": "en-US",
                "title": TITLE,
                "benefits": BENEFITS,
                "description": DESCRIPTION,
            }
        ],
        "basePlans": [
            {
                "basePlanId": BASE_PLAN_ID,
                "autoRenewingBasePlanType": {
                    "billingPeriodDuration": "P1M",
                    "gracePeriodDuration": "P3D",
                    "resubscribeState": "RESUBSCRIBE_STATE_ACTIVE",
                    "prorationMode": "SUBSCRIPTION_PRORATION_MODE_CHARGE_ON_NEXT_BILLING_DATE",
                },
                "regionalConfigs": [
                    {
                        "regionCode": "US",
                        "newSubscriberAvailability": True,
                        "price": {
                            "currencyCode": "USD",
                            "units": str(PRICE_USD_UNITS),
                            "nanos": PRICE_USD_NANOS,
                        },
                    }
                ],
                "otherRegionsConfig": {
                    "newSubscriberAvailability": True,
                    "usdPrice": {
                        "currencyCode": "USD",
                        "units": str(PRICE_USD_UNITS),
                        "nanos": PRICE_USD_NANOS,
                    },
                    "eurPrice": {
                        "currencyCode": "EUR",
                        "units": str(PRICE_EUR_UNITS),
                        "nanos": PRICE_EUR_NANOS,
                    },
                },
            }
        ],
    }

    try:
        existing = subs.get(packageName=PACKAGE_NAME, productId=SUBSCRIPTION_ID).execute()
        print(f"Subscription '{SUBSCRIPTION_ID}' already exists - skipping creation.")
        print(json.dumps(existing, indent=2))
    except HttpError as e:
        if e.resp.status != 404:
            raise
        print(f"Creating subscription '{SUBSCRIPTION_ID}'...")
        created = subs.create(
            packageName=PACKAGE_NAME,
            productId=SUBSCRIPTION_ID,
            **{"regionsVersion.version": "2022/02"},
            body=body,
        ).execute()
        print(json.dumps(created, indent=2))
        print("Created (base plan starts in DRAFT).")

    print(f"Activating base plan '{BASE_PLAN_ID}'...")
    try:
        subs.basePlans().activate(
            packageName=PACKAGE_NAME,
            productId=SUBSCRIPTION_ID,
            basePlanId=BASE_PLAN_ID,
        ).execute()
        print("Base plan activated - subscription is now purchasable.")
    except HttpError as e:
        body_text = e.content.decode() if hasattr(e, "content") else str(e)
        if "ACTIVE" in body_text or e.resp.status == 400:
            print(f"(activation call returned {e.resp.status}, likely already active): {body_text}")
        else:
            raise


if __name__ == "__main__":
    main()
