import re
import sys
from pathlib import Path

t = Path(sys.argv[1]).read_text(encoding="utf-8")
for m in re.finditer(
    r'content-desc="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"|bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*content-desc="([^"]+)"',
    t,
):
    if m.group(1):
        print(f"{m.group(1)}|{m.group(2)}|{m.group(3)}|{m.group(4)}|{m.group(5)}")
    else:
        print(f"{m.group(10)}|{m.group(6)}|{m.group(7)}|{m.group(8)}|{m.group(9)}")
