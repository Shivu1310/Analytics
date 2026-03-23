import os
import random
import time
from datetime import datetime, timezone

import requests

BACKEND_URL = os.getenv("BACKEND_URL", "http://backend:8080/api/events")
EVENTS_PER_SECOND = float(os.getenv("EVENTS_PER_SECOND", "100"))
LOG_EVERY_N = int(os.getenv("LOG_EVERY_N", "100"))

USERS = [f"user-{i}" for i in range(1, 201)]
SESSIONS = [f"session-{i}" for i in range(1, 401)]
PAGES = [
    "/",
    "/product/1",
    "/product/2",
    "/product/3",
    "/cart",
    "/checkout",
    "/search",
    "/offers"
]


def build_event() -> dict:
    return {
        "user_id": random.choice(USERS),
        "event_type": random.choice(["page_view", "click", "add_to_cart", "checkout"]),
        "page_url": random.choice(PAGES),
        "session_id": random.choice(SESSIONS),
        "timestamp": datetime.now(timezone.utc).isoformat()
    }


def main() -> None:
    sleep_seconds = 1.0 / EVENTS_PER_SECOND
    print(f"Starting generator -> {BACKEND_URL} at {EVENTS_PER_SECOND} events/sec")
    sent = 0

    while True:
        event = build_event()
        try:
            response = requests.post(BACKEND_URL, json=event, timeout=1)
            if response.status_code >= 300:
                print(f"POST failed ({response.status_code}): {response.text[:120]}")
            else:
                sent += 1
                if sent % LOG_EVERY_N == 0:
                    print(
                        f"Sent {sent} events. "
                        f"Latest user_id={event['user_id']}, "
                        f"session_id={event['session_id']}, "
                        f"page_url={event['page_url']}"
                    )
        except requests.RequestException as exc:
            print(f"Backend unavailable: {exc}")

        time.sleep(sleep_seconds)


if __name__ == "__main__":
    main()
