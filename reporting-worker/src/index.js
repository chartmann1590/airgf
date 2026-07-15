const MAX_BODY_BYTES = 32 * 1024;

export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return new Response("Method not allowed", { status: 405 });
    }

    const body = await request.text();
    if (new TextEncoder().encode(body).byteLength > MAX_BODY_BYTES) {
      return new Response("Report too large", { status: 413 });
    }

    let report;
    try {
      report = JSON.parse(body);
    } catch {
      return new Response("Invalid JSON", { status: 400 });
    }

    if (!report.reason || !report.content || !Number.isFinite(report.createdAt)) {
      return new Response("Missing report fields", { status: 400 });
    }

    const id = `${report.createdAt}-${crypto.randomUUID()}`;
    await env.REPORTS.put(id, JSON.stringify({
      ...report,
      receivedAt: Date.now(),
    }), { expirationTtl: 60 * 60 * 24 * 90 });

    return Response.json({ id }, { status: 202 });
  },
};
