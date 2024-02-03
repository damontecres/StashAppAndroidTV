import sys
import json
from pathlib import Path
from xml.sax.saxutils import escape

DEBUG = "--debug" in sys.argv

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

def convert_file(source_file, dest_file, allowed_keys):
    if DEBUG:
        print(f"Converting {source_file} to {dest_file}")
    collected_keys = []
    with open(source_file, "r") as f:
        data = json.load(f)
    dest_file.parent.mkdir(parents=True, exist_ok=True)
    with open(dest_file, "w") as f:
        f.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        f.write("<resources>\n")
        for key, value in parse_dict(data, ["stashapp"]):
            key = "_".join(key)
            if allowed_keys and key not in allowed_keys:
                if DEBUG:
                    print(f"Skipping {key} in {source_file}")
                continue
            collected_keys.append(key)
            f.write(f"    <string name=\"{key}\" formatted=\"false\">{escape_value(value)}</string>\n")
        f.write("</resources>\n")
    return set(collected_keys)

main_file = source_dir / "en-GB.json"
main_dest = Path(dest_prefix) / "stash_strings.xml"

allowed_keys = convert_file(main_file, main_dest, set())
if DEBUG:
    print(f"Got {len(allowed_keys)} allowed keys")

for file in source_dir.glob("*.json"):
    if file.name.startswith("en-GB"):
        continue
    else:
        lang = file.name.replace(".json", "").replace("-", "+").replace("_", "+")
        convert_file(file, Path(dest_prefix+"-b+"+lang) / "stash_strings.xml", allowed_keys)

if DEBUG:
    print(main_dest.name)
    print(main_dest.read_text())
