#!/usr/bin/env python3
import json
import string
import sys

KEYS = {c: code for c, code in zip(string.ascii_lowercase, [
    30, 48, 46, 32, 18, 33, 34, 35, 23, 36, 37, 38, 50,
    49, 24, 25, 16, 19, 31, 20, 22, 47, 17, 45, 21, 44,
])}
KEYS |= {"space": 57, "enter": 28, "del": 14, "rshift": 54, "alt": 56, "fn": 251, "sym": 253}
LSHIFT = 42
TITAN, PMIC = 1, 2


def register(dev, name, keys):
    return {"id": dev, "command": "register", "name": name, "vid": 0x1, "pid": dev, "bus": "bluetooth",
            "configuration": [{"type": "UI_SET_EVBIT", "data": [1]}, {"type": "UI_SET_KEYBIT", "data": keys}]}


def key(dev, code, value):
    return {"id": dev, "command": "inject", "events": [1, code, value, 0, 0, 0]}


def delay(ms):
    return [{"id": dev, "command": "delay", "duration": ms} for dev in (TITAN, PMIC)]


def script(tokens):
    yield register(TITAN, "TitanKey", sorted(KEYS.values()))
    yield register(PMIC, "henkan-lshift", [LSHIFT])
    for dev in (TITAN, PMIC):
        yield {"id": dev, "command": "inject", "events": [0, 0, 0]}
    yield from delay(1500)
    for tok in tokens:
        if tok.startswith("~"):
            yield from delay(int(tok[1:]))
            continue
        press, name = (tok[0], tok[1:]) if tok[0] in "+-" else (None, tok)
        dev, code = (PMIC, LSHIFT) if name == "lshift" else (TITAN, KEYS[name])
        for value in [1, 0] if press is None else [1 if press == "+" else 0]:
            yield key(dev, code, value)
            yield from delay(40)
    yield from delay(500)


if __name__ == "__main__":
    for event in script(" ".join(sys.argv[1:]).split()):
        print(json.dumps(event))
