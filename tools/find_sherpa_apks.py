import re
from pathlib import Path

t = Path(
    r"C:\Users\Raffy Rodriguez\.cursor\projects\c-Users-Raffy-Rodriguez-Desktop-Desarrollo-IA-QuickNovel\agent-tools\13683e6b-7fc8-4346-9512-ff2250ad201e.txt"
).read_text(encoding="utf-8", errors="ignore")

for pat in ["davefx", "lessac-medium", "es_MX-ald", "amy-medium"]:
    ms = re.findall(rf"sherpa-onnx-[^\s\"']*{pat}[^\s\"']*\.apk", t, flags=re.I)
    arm = [m for m in ms if "arm64" in m]
    print(pat, "count", len(ms), "arm64", arm[:8])
