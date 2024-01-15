import os
import json
from pathlib import Path
from xml.sax.saxutils import escape

source_dir = Path("../stash-server/ui/v2.5/src/locales")
dest_prefix="src/main/res/values"

def escape_value(value):
    return escape(value).replace(r"'", r"\'")

def parse_dict(d, keys=[]):
    for key, value in d.items():
        key = key.replace("-", "_")
        if isinstance(value, dict):
            yield from parse_dict(value, keys + [key])
        else:
            yield keys + [key], value

def convert_file(source_file, dest_file):
    with open(source_file, "r") as f:
        data = json.load(f)
    dest_file.parent.mkdir(parents=True, exist_ok=True)
    with open(dest_file, "w") as f:
        f.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        f.write("<resources>\n")
        for key, value in parse_dict(data, ["stashapp"]):
            key = "_".join(key)
            f.write(f"    <string name=\"{key}\">{escape_value(value)}</string>\n")
        f.write("</resources>\n")

main_file = source_dir / "en-GB.json"
main_dest = Path(dest_prefix) / "stash_strings.xml"

convert_file(main_file, main_dest)

for file in source_dir.glob("*.json"):
    if file.name.startswith("en-GB"):
        continue
    else:
        lang = file.name.replace(".json", "").replace("-", "+").replace("_", "+")
        convert_file(file, Path(dest_prefix+"-b+"+lang) / "stash_strings.xml")
