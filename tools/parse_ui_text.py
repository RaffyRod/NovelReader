import re
import sys

path = sys.argv[1] if len(sys.argv) > 1 else "ui.xml"
text = open(path, encoding="utf-8").read()
print("TEXTS:")
for t in re.findall(r'text="([^"]+)"', text)[:80]:
    print("-", t)
print("DESCS:")
for t in re.findall(r'content-desc="([^"]+)"', text)[:40]:
    print("-", t)
