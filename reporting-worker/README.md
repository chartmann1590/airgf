# AI response reporting worker

This optional Cloudflare Worker receives user-approved AI response reports and
stores them in KV for 90 days. It uses only Worker/KV APIs and is designed to fit
within Cloudflare's free tier for an early-stage app.

1. Install the free Wrangler CLI: `npm install --save-dev wrangler`.
2. Authenticate: `npx wrangler login`.
3. Create storage: `npx wrangler kv namespace create REPORTS`.
4. Put the returned ID in `wrangler.jsonc` and deploy with `npx wrangler deploy`.
5. Build Android with `AIRGF_REPORT_ENDPOINT` set to the deployed Worker URL.

The Android app stores reports locally when this endpoint is omitted or offline.
No chat context is transmitted until the user confirms the in-app report dialog.
