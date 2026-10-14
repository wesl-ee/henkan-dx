#!/usr/bin/env python3
import html
import json
import re
import subprocess
import sys
import time

from titan import script

APP = "ee.wesl.henkandx.debug"
IME = f"{APP}/ee.wesl.henkandx.HenkanService"

CASES = [
    ("email", "h e l l o space lshift t i t a n", "hello Titan"),
    ("email", "lshift ~100 lshift a b lshift c", "ABc"),
    ("email", "alt w alt e", "12"),
    ("email", "+alt w e -alt", "12"),
    ("text", "t o u k y o u space enter", "東京"),
    ("text", "w a t a s h i h a k u r u m a w o k a u space enter", "私は車を買う"),
    ("text", "k a space +fn q q -fn", ""),
    ("text", "rshift a b c rshift k a enter", "abcか"),
    ("text", "sym a sym s", "・〜"),
    ("email", "sym a sym sym b", "&b"),
    ("visible password", "k a n a", "kana"),
]


def adb(*args):
    return subprocess.run(["adb", "shell", *args], capture_output=True, text=True, check=True).stdout


def nodes():
    xml = adb("uiautomator dump /sdcard/u.xml >/dev/null && cat /sdcard/u.xml && rm /sdcard/u.xml")
    return re.findall(r"<node [^>]*>", xml)


def attr(node, name):
    return html.unescape(re.search(f' {name}="([^"]*)"', node).group(1))


def press(keys):
    events = "\n".join(json.dumps(e) for e in script(keys.split()))
    subprocess.run(["adb", "shell", "uinput", "-"], input=events, text=True, check=True)


def main():
    start = ["am", "start", "-W", "--activity-clear-task", "-n", f"{APP}/ee.wesl.henkandx.DebugActivity", "--ez", "incognito"]
    adb(*start, "true")
    try:
        sys.exit(run())
    finally:
        adb(*start, "false")


def run():
    adb("ime", "set", IME)
    time.sleep(3)
    fields = {}
    for node in nodes():
        if attr(node, "class") == "android.widget.EditText":
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", attr(node, "bounds")))
            fields[attr(node, "hint")] = ((x1 + x2) // 2, (y1 + y2) // 2)
    failed = 0
    for field, keys, want in CASES:
        adb("input", "tap", *map(str, fields[field]))
        time.sleep(0.5)
        press("+fn a -fn del ~800 " + keys)
        time.sleep(0.5)
        focused = next(n for n in nodes() if 'focused="true"' in n and "EditText" in n)
        got = "" if attr(focused, "text") == field else attr(focused, "text")
        ok = got == want
        failed += not ok
        print(f"{'ok  ' if ok else 'FAIL'} {field}: {keys!r} -> {got!r}" + ("" if ok else f" (want {want!r})"))
    return failed > 0


if __name__ == "__main__":
    main()
