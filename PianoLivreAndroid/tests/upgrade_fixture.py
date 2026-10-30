#!/usr/bin/env python3
"""Deterministic synthetic profile/preferences used by Android upgrade tests."""
import html
import json
import struct


def _double_bits(value):
    return struct.unpack(">q", struct.pack(">d", value))[0]


def synthetic_upgrade_preferences():
    """Return the same known non-contiguous profile fixture for debug/release upgrades."""
    profiles=json.dumps([
        dict(id=0,name="Upgrade A",avatar=0),
        dict(id=7,name="Upgrade B",avatar=3)
    ],ensure_ascii=False)
    library=json.dumps(dict(version=1,favorites=["imported/upgrade.mxl"],collections=[]))
    history=json.dumps(dict(version=1,records=[dict(score="imported/upgrade.mxl",title="Upgrade synthetic session",at=1700000000000,elapsedMs=60000,mode=1,hand=2,fromBeat=0,toBeat=.75,startSpeed=.8,endSpeed=.8)],pieces={"imported/upgrade.mxl":dict(title="Upgrade synthetic session",timeMs=60000,periods=1,lastAt=1700000000000,lastBeat=.75,goalMinutes=15)}))
    lines=[
        "<map>",
        '<boolean name="upgrade_test_fixture" value="true"/>',
    ]
    for key,value in {
        "profiles":'["Upgrade A","Upgrade B"]',
        "profiles_v2":profiles,
        "user_7_file":"imported/upgrade.mxl",
        "user_7_library":library,
        "user_7_history":history,
    }.items():
        lines.append('<string name="'+key+'">'+html.escape(value)+'</string>')
    for key,value in {
        "active_profile":7,
        "profiles_next_id":8,
        "user_7_mode":1,
        "user_7_hand":2,
    }.items():
        lines.append(f'<int name="{key}" value="{value}"/>')
    for key,value in {
        "position":.75,
        "tempo":.8,
        "loop_start":0,
        "loop_end":8,
    }.items():
        lines.append(f'<long name="user_7_{key}" value="{_double_bits(value)}"/>')
    lines.extend([
        '<boolean name="user_7_keyboard" value="true"/>',
        "</map>",
    ])
    return "\n".join(lines)
