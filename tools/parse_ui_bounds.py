import re
import sys

path = sys.argv[1]
needle = sys.argv[2] if len(sys.argv) > 2 else ""
text = open(path, encoding="utf-8").read()
for m in re.finditer(r'<node [^>]+>', text):
    node = m.group(0)
    if needle and needle not in node:
        continue
    desc = re.search(r'content-desc="([^"]*)"', node)
    txt = re.search(r'text="([^"]*)"', node)
    bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
    clickable = 'clickable="true"' in node
    if not bounds:
        continue
    x1, y1, x2, y2 = map(int, bounds.groups())
    label = (desc.group(1) if desc else "") or (txt.group(1) if txt else "")
    if not label:
        continue
    print(f"{label!r} clickable={clickable} center=({(x1+x2)//2},{(y1+y2)//2}) bounds=[{x1},{y1}][{x2},{y2}]")
