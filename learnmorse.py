#!/usr/bin/env python3
"""Learn Morse: a tiny, dependency-free browser practice application."""

from __future__ import annotations

import argparse
import json
import os
import tempfile
import webbrowser
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse


ROOT = Path(__file__).resolve().parent
WEB_ROOT = ROOT / "web"
DATA_DIR = ROOT / "data" / "learnmorse"
STATE_FILE = DATA_DIR / "state.json"

DEFAULT_PRACTICE_TEXTS = [
    {"id": "default-calling", "name": "Calling Practice", "text": "CQ CQ DE LEARN MORSE"},
    {"id": "default-alphabet", "name": "The Alphabet", "text": "a b c d e f g h i j k l m n o p q r s t u v w x y z"},
    {"id": "default-beginnings", "name": "Sentence Beginnings", "text": "Several minutes before dawn, Mara reached a frozen junction where a brass weather vane creaked above a quiet stone lodge. Outside, a quick brown fox zigzagged past juniper bushes while violet clouds gathered over the western ridge. Under a cracked window, she found a wax-sealed envelope beside a quartz chip, an old key, and a faded map. Then the distant village bells rang twice, and she noticed a penciled note: “The route is hidden in plain sight.” Her final clue was simpler: “Read where each sentence begins.”"},
    {"id": "default-indexed", "name": "Indexed Objects", "text": "Inside the stationmaster’s desk, Felix found six labeled objects arranged on a velvet tray: brass, crane, ivory, blade, gate, cedar. Beneath them, in the same order, were the numbers 1–2–1–4–1–2. A yellowed card beside a zinc box said, “Use each number to choose one letter from the word directly above it, then read from left to right.” Outside, a quartz clock clicked while jays argued noisily on the platform roof."},
    {"id": "default-endings", "name": "Sentence Endings", "text": "At two in the morning, Vera crossed the empty harbor and saw a pale halo around the moon. Beside an abandoned kiosk, she found a jigsaw piece, a zinc token, and one broken ski. The watchman’s notebook mentioned a quick vessel that had vanished beyond the fog. A jagged X was scratched beneath the words, “Look only at where each sentence finishes,” beside a cold brass torch. A penciled arrow on the final page led toward the sealed underground vault."},
    {"id": "default-fourth", "name": "Every Fourth Word", "text": "Jonah found a narrow strip of paper tucked inside a cracked compass case beside quartz dust and a tiny zinc buckle. The strip contained one carefully written sentence: “Quick foxes weave cautiously while bright jays advance beyond frozen ridges moving toward distant valleys patiently.” Below it, a second line read, “Take every fourth word, then keep only its first letter.” He checked the count twice before leaving the noisy market square."},
    {"id": "default-shorter", "name": "Choose the Shorter Word", "text": "In a disused signal cabin, Priya discovered five word-pairs painted across a wooden panel: lime–quartz, ivy–zebra, gate–jacket, hawk–violin, tin–copper. A brass plaque beneath them said, “In each pair, choose the shorter word and keep its first letter.” Wind rattled the cracked window while a quick fox crossed the snowy yard and a blue jay vanished behind the freight cars."},
    {"id": "default-center", "name": "Center Letters", "text": "Quinn opened a velvet pouch and found five small cards marked berry, spike, civic, spear, torch. Each word had exactly five letters, and a note beside a quartz lens said, “Take the letter at the exact center of every card, in order.” Outside the workshop, a noisy jackdaw hopped across a zinc gutter while fog drifted over the frozen canal. Quinn copied the result into his field journal."},
    {"id": "default-numbered", "name": "Numbered Objects", "text": "At the old observatory, Mei found five objects tagged with numbers: quartz–2, umbrella–4, ivory–6, compass–8, kite–10. A faded instruction beside a box of glass prisms said, “Arrange the objects from the smallest number to the largest, then take their initials.” Beyond the dome, a fox darted through juniper while a bright meteor flashed above the snowy ridge. Mei checked the sequence twice before touching the locked cabinet."},
    {"id": "default-caesar", "name": "One Letter Back", "text": "Victor found a scrap of paper wedged beneath a quartz specimen in the geology lab. Across it, someone had printed the strange six-letter word XJOUFS in thick black ink. A note underneath said, “Move every letter exactly one place backward in the alphabet.” The ventilation fan buzzed, a blue jacket hung from a hook, and frost glazed the window beside a small zinc box."},
    {"id": "default-word-morse", "name": "Word-Length Morse", "text": "Zara discovered a coded line in the radio shack while a quick storm shook the glass windows and a fox barked beyond the jetty. The line read: “fox javelin vintage zephyrs / owl map quickly / jackets vintage / red zephyrs javelin”. Beneath it was the rule: “A three-letter word is a dot; a seven-letter word is a dash. Slashes separate Morse letters.” She copied the groups carefully beside a quartz dial and checked every word length twice."},
    {"id": "default-odd", "name": "Odd Positions", "text": "In the baggage room of an old express train, Luca found a brass key stamped with the serial VXAYUZLQT. Beside it lay a note saying, “Count from the left and keep only letters in odd-numbered positions.” A cracked mirror reflected a violet jacket, a zinc toolbox, and a faded poster of Quebec. Outside, snow blew past the windows as the midnight train jolted forward."},
]

DEFAULT_STATE = {
    "settings": {
        "characterSpeed": 20,
        "wordSpeed": 15,
        "textSpeed": 15,
        "tonePitch": 650,
        "fontSize": 72,
        "symbolSize": 25,
        "foreground": "#f4f7fb",
        "morseColor": "#8fa3bc",
        "background": "#080b10",
        "accent": "#315e8c",
        "cursorColor": "#ffc857",
        "markerOffset": 0,
        "repeat": False,
        "volume": 55,
    },
    "draft": "CQ CQ DE LEARN MORSE",
    "selectedText": "default-calling",
    "practiceTexts": DEFAULT_PRACTICE_TEXTS,
}


def read_state() -> dict:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    if not STATE_FILE.exists():
        write_state(DEFAULT_STATE)
    try:
        state = json.loads(STATE_FILE.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        state = DEFAULT_STATE.copy()
    # Preserve newly introduced settings without discarding user data.
    merged = {**DEFAULT_STATE, **state}
    merged["settings"] = {**DEFAULT_STATE["settings"], **state.get("settings", {})}
    return merged


def write_state(state: dict) -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix="state-", suffix=".json", dir=DATA_DIR)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(state, handle, indent=2, ensure_ascii=False)
            handle.write("\n")
        os.replace(temporary, STATE_FILE)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(WEB_ROOT), **kwargs)

    def log_message(self, fmt: str, *args) -> None:
        print(f"{self.client_address[0]} - {fmt % args}")

    def json_response(self, payload: dict, status: int = 200) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        if urlparse(self.path).path == "/api/state":
            self.json_response(read_state())
            return
        super().do_GET()

    def do_PUT(self) -> None:
        if urlparse(self.path).path != "/api/state":
            self.json_response({"error": "Not found"}, 404)
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length > 1_000_000:
                raise ValueError("Request too large")
            state = json.loads(self.rfile.read(length))
            if not isinstance(state, dict):
                raise ValueError("State must be an object")
            write_state(state)
            self.json_response({"ok": True})
        except (ValueError, json.JSONDecodeError, OSError) as exc:
            self.json_response({"error": str(exc)}, 400)


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the Learn Morse browser app")
    parser.add_argument("--port", "-p", type=int, default=8765, help="port to listen on (default: 8765)")
    parser.add_argument("--host", default="127.0.0.1", help="host/interface to bind (default: 127.0.0.1)")
    parser.add_argument("--no-browser", action="store_true", help="do not open a browser automatically")
    args = parser.parse_args()
    if not 1 <= args.port <= 65535:
        parser.error("port must be between 1 and 65535")
    read_state()
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    url = f"http://{'127.0.0.1' if args.host in ('0.0.0.0', '::') else args.host}:{args.port}"
    print(f"Learn Morse is running at {url}")
    print(f"Persistent data: {DATA_DIR}")
    if not args.no_browser:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping Learn Morse.")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
