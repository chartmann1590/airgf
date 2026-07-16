#!/usr/bin/env python3
"""Read-only check of the Amoura Premium subscription's current state in
Play Console via the Android Publisher API - tax/compliance settings,
base plan state, and regional pricing. Makes no changes.

Requires:
  - GOOGLE_PLAY_SERVICE_ACCOUNT_JSON env var: service account JSON, plaintext
"""
import json
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build

PACKAGE_NAME = "com.airgf.app"
SUBSCRIPTION_ID = "amoura_premium_monthly"
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

    sub = subs.get(packageName=PACKAGE_NAME, productId=SUBSCRIPTION_ID).execute()
    print("=== Full subscription resource ===")
    print(json.dumps(sub, indent=2))

    print("\n=== Summary ===")
    tax = sub.get("taxAndComplianceSettings")
    print(f"Tax category set: {tax.get('productTaxCategoryCode') if tax else None}")
    print(f"EEA withdrawal right type: {tax.get('eeaWithdrawalRightType') if tax else None}")

    for bp in sub.get("basePlans", []):
        print(f"\nBase plan '{bp.get('basePlanId')}': state={bp.get('state')}")
        art = bp.get("autoRenewingBasePlanType", {})
        print(f"  billingPeriodDuration={art.get('billingPeriodDuration')}")
        for rc in bp.get("regionalConfigs", []):
            price = rc.get("price", {})
            print(f"  region={rc.get('regionCode')} available={rc.get('newSubscriberAvailability')} price={price.get('units')}.{str(price.get('nanos', 0)).zfill(9)[:2]} {price.get('currencyCode')}")
        orc = bp.get("otherRegionsConfig")
        if orc:
            usd = orc.get("usdPrice", {})
            print(f"  other regions available={orc.get('newSubscriberAvailability')} usdPrice={usd.get('units')}.{str(usd.get('nanos', 0)).zfill(9)[:2]}")

    # Try fetching the app's overall content-rating / data-safety declarations
    # if the API surfaces them (best-effort; these are largely Console-only).
    print("\n=== Note ===")
    print("Content rating questionnaire, Data safety form, and target audience")
    print("are not exposed via the Play Developer API - these can only be")
    print("verified manually in Play Console (App content section).")


if __name__ == "__main__":
    main()
