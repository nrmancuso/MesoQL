#!/usr/bin/env python3

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import json
import sys
from urllib.parse import urlparse


class FixtureHandler(BaseHTTPRequestHandler):
    payload = b""

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/products":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(self.payload)))
            self.end_headers()
            self.wfile.write(self.payload)
            return

        self.send_response(404)
        self.end_headers()

    def log_message(self, format, *args):
        return


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: serve-forecast-discussion-fixtures.py <port> <json-file>", file=sys.stderr)
        return 2

    port = int(sys.argv[1])
    fixture_path = Path(sys.argv[2])
    FixtureHandler.payload = json.dumps(json.loads(fixture_path.read_text())).encode("utf-8")

    server = ThreadingHTTPServer(("", port), FixtureHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
