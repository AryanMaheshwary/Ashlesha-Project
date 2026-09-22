"""
Environment ingress shim ONLY.

This preview platform routes external '/api' traffic to port 8001. The real
application backend is written entirely in JAVA (Spring Boot + raw JDBC) and
listens on port 8080 (managed by the 'javabackend' supervisor program).

This tiny FastAPI app just transparently forwards every /api request to the
Java backend. It contains ZERO business logic and is NOT part of the project
deliverable. For local development you run the Java backend directly and point
the React app at it (see README) - no Python involved.
"""
import os
import subprocess
import threading

import httpx
from fastapi import FastAPI, Request, Response

JAVA_BACKEND = os.environ.get("JAVA_BACKEND_URL", "http://127.0.0.1:8090")

app = FastAPI(title="pharmacy-ingress-shim")

_client = httpx.AsyncClient(base_url=JAVA_BACKEND, timeout=30.0)

_HOP_BY_HOP = {
    "content-length", "transfer-encoding", "connection", "keep-alive",
    "proxy-authenticate", "proxy-authorization", "te", "trailers", "upgrade", "host",
}


@app.api_route("/api/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy(path: str, request: Request):
    url = f"/api/{path}"
    body = await request.body()
    fwd_headers = {k: v for k, v in request.headers.items() if k.lower() not in _HOP_BY_HOP}
    try:
        upstream = await _client.request(
            request.method,
            url,
            content=body,
            headers=fwd_headers,
            params=dict(request.query_params),
        )
    except httpx.ConnectError:
        return Response(
            content='{"error":"Java backend is starting up. Please retry in a few seconds."}',
            status_code=503,
            media_type="application/json",
        )
    resp_headers = {k: v for k, v in upstream.headers.items() if k.lower() not in _HOP_BY_HOP}
    return Response(
        content=upstream.content,
        status_code=upstream.status_code,
        headers=resp_headers,
        media_type=upstream.headers.get("content-type"),
    )


@app.on_event("startup")
async def _bootstrap_backend():
    """Ensure MariaDB + the Java backend are running (self-heals across restarts)."""
    def run():
        try:
            subprocess.run(["/bin/bash", "/app/scripts/bootstrap.sh"], timeout=240)
        except Exception as exc:  # noqa: BLE001
            print(f"[shim] bootstrap error: {exc}")
    threading.Thread(target=run, daemon=True).start()


@app.get("/")
async def root():
    return {"status": "ingress-shim", "backend": "java", "target": JAVA_BACKEND}
