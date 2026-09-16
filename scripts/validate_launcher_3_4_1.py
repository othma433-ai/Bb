#!/usr/bin/env python3
import xml.etree.ElementTree as ET
from pathlib import Path
import sys

ANDROID = "{http://schemas.android.com/apk/res/android}"
manifest = Path("app/src/main/AndroidManifest.xml")
root = ET.parse(manifest).getroot()

launchers = []

for activity in root.findall("./application/activity"):
    name = activity.get(ANDROID + "name")
    for intent_filter in activity.findall("intent-filter"):
        actions = {
            x.get(ANDROID + "name")
            for x in intent_filter.findall("action")
        }
        categories = {
            x.get(ANDROID + "name")
            for x in intent_filter.findall("category")
        }
        if (
            "android.intent.action.MAIN" in actions
            and "android.intent.category.LAUNCHER" in categories
        ):
            launchers.append(name)

expected = "com.althmany.extractor.MainActivity"

print("Launchers:", launchers)

if launchers != [expected]:
    print("FAIL: expected launcher:", expected)
    sys.exit(1)

print("PASS: 3.4.1 workspace is the only launcher")
