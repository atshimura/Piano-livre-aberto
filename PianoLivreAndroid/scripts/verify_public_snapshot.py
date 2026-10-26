#!/usr/bin/env python3
"""Safeguard the open-source snapshot and audited test-score allowlist."""
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
ANDROID = ROOT / "PianoLivreAndroid"
ASSETS = ANDROID / "app/src/main/assets"
SCORES = ASSETS / "scores"
EXPECTED_CLASSICS = {
    "ode-to-joy-ludwig-van-beethoven.mxl",
    "fur-elise-easy-ver.mxl",
    "lago-dos-cisnes.mxl",
    "canon-in-d.mxl",
    "minuet-in-g-major-christian-petzold.mxl",
    "minuet-in-g-minor-bwv-anh-115-christian-petzold.mxl",
    "gymnopedie-no-1-satie.mxl",
}
EXPECTED_EXERCISES = {"estudo-livre-01.mxl", "estudo-livre-02.mxl", "estudo-livre-03.mxl"}
EXPECTED = EXPECTED_CLASSICS | EXPECTED_EXERCISES
DENIED = {"aquarela-toquinho.mxl", "asa-branca-luiz-gonzaga-humberto-texeira-arr-andre-melhor.mxl", "bella-ciao-piano.mxl"}

assert (ROOT / "LICENSE").is_file(), "GPL license absent"
assert (ROOT / "AGENTS.md").is_file(), "Public agent continuity instructions absent"
assert not (ROOT / "DO_NOT_PUBLISH_OWNER_SCORES.txt").exists(), "Non-public musical review marker"
assert not (ANDROID / "signing").exists(), "Private signing assets leaked"
assert not (ROOT / ".github/workflows/windows-self-hosted.yml").exists(), "Personal runner workflow not allowed"

scores = {p.name for p in SCORES.glob("*.mxl")}
assert scores == EXPECTED, f"Unreviewed, missing or extra MXL files: {sorted(scores ^ EXPECTED)}"
assert scores.isdisjoint(DENIED), "Composition/arrangement not approved for redistribution"
catalog = json.loads((ASSETS / "catalog.json").read_text(encoding="utf-8"))
rights = json.loads((ASSETS / "score-rights.json").read_text(encoding="utf-8"))
assert len(catalog) == len(rights) == len(EXPECTED)
assert {x["file"] for x in catalog} == EXPECTED, "Catalog must match allowed files"
assert {x["file"] for x in rights} == EXPECTED, "Every score requires rights manifest"
assert len({x["file"] for x in catalog}) == len(EXPECTED)
assert len({x["file"] for x in rights}) == len(EXPECTED)
for x in rights:
    assert x["name"] and x["composition_basis"] and x["notation_basis"] and x["distribution_scope"]
    if x["file"] in EXPECTED_CLASSICS:
        assert x["composition_basis"] == "historical-public-domain-Brazil"
    else:
        assert x["composition_basis"] == "original-generated-exercise"
for p in SCORES.glob("*.mxl"):
    with ZipFile(p) as z:
        members = z.infolist()
        assert len(members) <= 30, f"Unexpected zip entries in {p.name}"
        assert sum(i.file_size for i in members) < 10_000_000, f"Large decompressed MXL {p.name}"
        assert all(not i.filename.startswith("/") and ".." not in Path(i.filename).parts for i in members), f"Unsafe path {p.name}"
        assert any(i.filename.lower().endswith(".xml") and not i.filename.startswith("META-INF/") for i in members), f"Missing score XML: {p.name}"
        assert z.testzip() is None, f"Bad zip checksum in {p.name}"

gradle = (ANDROID / "app/build.gradle").read_text(encoding="utf-8")
assert "applicationIdSuffix '.opensource'" in gradle, "Public package must remain independent"
assert "PIANOLIVRE_PUBLIC_KEYSTORE_FILE" in gradle, "Public signing controls absent"
assert "beta-debug" not in gradle and "storePassword '" not in gradle, "Private signing materials prohibited"
print("PUBLIC_SOURCE_PACKAGE_AUDIT_PASS")
