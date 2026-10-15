#!/usr/bin/env python3
"""Public checkout check: no private history, keys or blocked MXL release assets."""
import json
from pathlib import Path
from zipfile import ZipFile
ROOT = Path(__file__).resolve().parents[2]
android = ROOT / "PianoLivreAndroid"
assert (ROOT / "LICENSE").exists(), "Open-source license absent"
assert not (ROOT / "DO_NOT_PUBLISH_OWNER_SCORES.txt").exists(), "Private review cannot be public"
assert not (android / "signing").exists(), "Private signing directory leaked"
scores = sorted((android / "app/src/main/assets/scores").glob("*.mxl"))
assert len(scores) == 10, "The musical test suite expects ten score assets"
catalog = json.loads((android / "app/src/main/assets/catalog.json").read_text())
assert len(catalog) == 10 and {x["file"] for x in catalog} == {p.name for p in scores}, "Catalog differs from music assets"
for p in scores:
    assert p.name.startswith("estudo-livre-"), "Unreviewed MXL in public CI"
    with ZipFile(p) as z:
        assert "score.xml" in z.namelist(), "Invalid synthetic music package"
gradle = (android / "app/build.gradle").read_text()
assert "applicationIdSuffix '.opensource'" in gradle, "Public debug package is not isolated"
assert "PIANOLIVRE_PUBLIC_KEYSTORE_FILE" in gradle, "Public build signing controls absent"
assert "beta-debug" not in gradle, "Historical beta signing material leaked"
assert "storePassword '" not in gradle, "Literal signing password leaked"
print("PUBLIC_SOURCE_PACKAGE_AUDIT_PASS")
