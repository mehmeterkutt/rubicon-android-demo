#!/usr/bin/env python3
"""
Fake analytics backend for the demo app. Standard library only.

  python3 server.py                 # listens on 0.0.0.0:8080
  From the emulator the Mac is reachable at http://10.0.2.2:8080

Endpoints
  POST /v1/batch                    # what the SDK will call; gzip and plain JSON accepted
  GET  /control?status=429&retry_after=30&invalid=0,2   # change the next responses
  GET  /control                     # show current behaviour
  GET  /events                      # everything received so far (JSON)
"""
import gzip, json, sys, threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

state = {"status": 200, "retry_after": None, "invalid": [], "received": []}
lock = threading.Lock()


class Handler(BaseHTTPRequestHandler):
    def _send(self, code, body, extra=None):
        data = json.dumps(body).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        for k, v in (extra or {}).items():
            self.send_header(k, v)
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        url = urlparse(self.path)
        q = {k: v[0] for k, v in parse_qs(url.query).items()}
        if url.path == "/control":
            with lock:
                if "status" in q:
                    state["status"] = int(q["status"])
                if "retry_after" in q:
                    state["retry_after"] = q["retry_after"] or None
                if "invalid" in q:
                    state["invalid"] = [int(i) for i in q["invalid"].split(",") if i]
                snapshot = {k: v for k, v in state.items() if k != "received"}
            return self._send(200, snapshot)
        if url.path == "/events":
            with lock:
                return self._send(200, state["received"])
        return self._send(404, {"error": "not found"})

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length)
        if self.headers.get("Content-Encoding", "").lower() == "gzip":
            raw = gzip.decompress(raw)
        try:
            body = json.loads(raw.decode("utf-8"))
        except Exception as e:
            print(f"  !! could not parse body: {e}")
            body = {"_raw": raw[:200].decode("utf-8", "replace")}
        with lock:
            status, retry_after, invalid = state["status"], state["retry_after"], state["invalid"]
        events = body.get("batch") or body.get("events") or (body if isinstance(body, list) else [body])
        print(f"POST {self.path}  status->{status}  {len(events)} event(s)  headers: "
              f"{ {k: v for k, v in self.headers.items() if k.lower() in ('authorization','user-agent','content-encoding','x-sdk-version')} }")
        for ev in events[:10]:
            print("   ", json.dumps(ev)[:300])
        if status == 200:
            with lock:
                state["received"].extend(events)
            return self._send(200, {"ok": True, "accepted": len(events)})
        extra = {"Retry-After": retry_after} if retry_after else None
        if status == 400 and invalid:
            return self._send(400, {"error": "invalid events", "invalid_events": invalid}, extra)
        return self._send(status, {"error": f"forced {status}"}, extra)

    def log_message(self, fmt, *args):
        pass  # keep output to our own prints


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    print(f"fake analytics server on http://0.0.0.0:{port}  (emulator: http://10.0.2.2:{port})")
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()
