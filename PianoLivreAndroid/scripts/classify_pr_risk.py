#!/usr/bin/env python3
"""Conservative PR risk classification for the Piano Livre Android CI.

Only additions/modifications of raster drawable assets are considered visual V1.
Any other change (including CI, scripts, tests, Java, XML, deletes, renames)
requires the full regression suite. Never trust PR titles or labels.
"""
import argparse
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip())
PREFIX = "PianoLivreAndroid/app/src/main/res/"
EXTENSIONS = {".png", ".webp", ".jpg", ".jpeg"}


def is_safe_visual(path: str, status: str) -> bool:
    if status not in ("A", "M"):
        return False
    p = pathlib.PurePosixPath(path)
    parts = p.parts
    return (
        path.startswith(PREFIX)
        and len(parts) == 7
        and parts[5].startswith("drawable")
        and p.suffix.lower() in EXTENSIONS
        and ".." not in parts
    )


def classify(changes: list[tuple[str, str]]) -> str:
    if not changes:
        return "full"
    return "visual" if all(is_safe_visual(path, status) for status, path in changes) else "full"


def changed_files(base: str, head: str) -> list[tuple[str, str]]:
    output = subprocess.check_output(
        ["git", "diff", "--name-status", "--no-renames", "-z", base, head],
        cwd=ROOT,
    )
    entries = output.decode("utf-8").split("\0")
    if entries and entries[-1] == "":
        entries.pop()
    if len(entries) % 2:
        raise ValueError("Unexpected git diff record count")
    return [(entries[i], entries[i + 1]) for i in range(0, len(entries), 2)]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    try:
        changes = changed_files(args.base, args.head)
        result = classify(changes)
    except (subprocess.CalledProcessError, UnicodeDecodeError, ValueError) as exc:
        print(f"Risk classifier failure: {exc}", file=sys.stderr)
        return 1
    report = {"tier": result, "base": args.base, "head": args.head,
              "changed": [{"status": s, "path": p} for s, p in changes]}
    pathlib.Path(args.output).write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
