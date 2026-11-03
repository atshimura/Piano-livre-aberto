#!/usr/bin/env python3
"""Extract the bundled OSMD inline JavaScript for syntax checking; no build/APK."""
import re
from pathlib import Path
source=Path("app/src/main/assets/notation.html").read_text(encoding="utf8")
scripts=re.findall(r"<script(?:\\s[^>]*)?>(.*?)</script>",source,re.I|re.S)
inline=[text for text in scripts if text.strip()]
if len(inline)!=1:
    raise SystemExit("Expected exactly one inline script in notation.html, got "+str(len(inline)))
out=Path("build/impl4-inline.js")
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(inline[0],encoding="utf8")
print("IMPL4_INLINE_JS_EXTRACT_PASS bytes="+str(len(inline[0].encode("utf8"))))
