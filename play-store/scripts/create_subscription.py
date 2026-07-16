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

# "Access to software programs or other electronic items purchased from the
# Internet" - the general default category for an app-feature subscription
# (not streaming media, not e-books/audiobooks/periodicals).
# https://support.google.com/googleplay/android-developer/answer/16408159
TAX_CATEGORY_CODE = "PTC023DIG"

TITLE = "Amoura Premium"
BENEFITS = [
    "Unlock Spicy Mode permanently",
    "Remove all ads",
]
DESCRIPTION = (
    "Unlocks Spicy Mode permanently and removes all ads. Prefer not to "
    "subscribe? Watch rewarded ads in Settings to unlock Spicy Mode free "
    "for up to 4 hours a day."
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
    service = get_service()
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

    existing = None
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
            regionsVersion_version="2022/02",
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

    current_tax_code = (existing or {}).get("taxAndComplianceSettings", {}).get("productTaxCategoryCode")
    if current_tax_code == TAX_CATEGORY_CODE:
        print(f"Tax category already set to '{TAX_CATEGORY_CODE}' - nothing to do.")
    else:
        print(f"Setting tax category to '{TAX_CATEGORY_CODE}' (was: {current_tax_code!r})...")
        subs.patch(
            packageName=PACKAGE_NAME,
            productId=SUBSCRIPTION_ID,
            updateMask="taxAndComplianceSettings.productTaxCategoryCode",
            regionsVersion_version="2022/02",
            body={
                "taxAndComplianceSettings": {
                    "productTaxCategoryCode": TAX_CATEGORY_CODE,
                },
            },
        ).execute()
        print("Tax category set.")


if __name__ == "__main__":
    main()
