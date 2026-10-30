"""Generate Ultimate Blue / Black / Lava Fire launcher PNGs for all mipmap densities."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# (filename_stem, bg_top, orb_hi, orb_mid, orb_lo, ring, glow, kanji_outline, kanji_fill)
THEMES = {
    "ic_launcher": (
        (13, 27, 51),
        (158, 197, 255),
        (26, 108, 255),
        (2, 6, 15),
        (90, 162, 255, 180),
        (77, 163, 255),
        (5, 11, 24),
        (232, 241, 255),
    ),
    "ic_launcher_black_fire": (
        (26, 29, 36),
        (245, 247, 250),
        (154, 163, 178),
        (10, 12, 16),
        (197, 204, 216, 180),
        (255, 255, 255),
        (5, 7, 10),
        (242, 245, 250),
    ),
    "ic_launcher_lava_fire": (
        (42, 10, 6),
        (255, 176, 138),
        (255, 59, 28),
        (26, 5, 2),
        (255, 122, 61, 180),
        (255, 122, 61),
        (18, 4, 2),
        (255, 224, 208),
    ),
}


def draw_kanji(draw: ImageDraw.ImageDraw, cx: float, cy: float, scale: float, color, width: float):
    def p(x, y):
        return cx + (x - 500) * scale, cy + (y - 500) * scale

    draw.line([p(350, 400), p(650, 400)], fill=color, width=max(1, int(width)))
    draw.line([p(500, 400), p(500, 663)], fill=color, width=max(1, int(width)))
    left = [p(494, 463)]
    for t in range(1, 21):
        tt = t / 20
        x = (1 - tt) ** 2 * 494 + 2 * (1 - tt) * tt * 440 + tt ** 2 * 320
        y = (1 - tt) ** 2 * 463 + 2 * (1 - tt) * tt * 558 + tt ** 2 * 623
        left.append(p(x, y))
    draw.line(left, fill=color, width=max(1, int(width)))
    right = [p(506, 463)]
    for t in range(1, 21):
        tt = t / 20
        x = (1 - tt) ** 2 * 506 + 2 * (1 - tt) * tt * 560 + tt ** 2 * 680
        y = (1 - tt) ** 2 * 463 + 2 * (1 - tt) * tt * 558 + tt ** 2 * 623
        right.append(p(x, y))
    draw.line(right, fill=color, width=max(1, int(width)))
    draw.line([p(455, 573), p(545, 573)], fill=color, width=max(1, int(width)))


def lerp(a, b, t):
    return int(a + (b - a) * t)


def render(size: int, round_mask: bool, theme) -> Image.Image:
    bg_top, orb_hi, orb_mid, orb_lo, ring, glow, k_out, k_fill = theme
    master = 1024
    img = Image.new("RGBA", (master, master), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)
    cx = cy = master // 2

    for r in range(master // 2, 0, -2):
        t = 1 - r / (master / 2)
        c = (
            lerp(bg_top[0], 0, t),
            lerp(bg_top[1], 0, t),
            lerp(bg_top[2], 0, t),
            255,
        )
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=c)

    # Fire glow under orb
    glow_layer = Image.new("RGBA", (master, master), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow_layer)
    gw, gh = int(master * 0.44), int(master * 0.28)
    gy = int(cy + master * 0.14)
    for r in range(max(gw, gh), 0, -2):
        u = r / max(gw, gh)
        a = int(170 * (1 - u) ** 1.4)
        gd.ellipse(
            [cx - r * gw / max(gw, gh), gy - r * gh / max(gw, gh),
             cx + r * gw / max(gw, gh), gy + r * gh / max(gw, gh)],
            fill=(glow[0], glow[1], glow[2], a),
        )
    img = Image.alpha_composite(img, glow_layer)

    orb_r = int(master * 0.29)
    orb2 = Image.new("RGBA", (master, master), (0, 0, 0, 0))
    od2 = ImageDraw.Draw(orb2)
    hx, hy = cx - orb_r * 0.25, cy - orb_r * 0.28
    for r in range(orb_r, 0, -1):
        u = r / orb_r
        if u > 0.55:
            f = (u - 0.55) / 0.45
            col = (
                lerp(orb_mid[0], orb_hi[0], f),
                lerp(orb_mid[1], orb_hi[1], f),
                lerp(orb_mid[2], orb_hi[2], f),
                255,
            )
        elif u > 0.25:
            col = (*orb_mid, 255)
        else:
            f = u / 0.25
            col = (
                lerp(orb_lo[0], orb_mid[0], f),
                lerp(orb_lo[1], orb_mid[1], f),
                lerp(orb_lo[2], orb_mid[2], f),
                255,
            )
        sx = int(cx + (hx - cx) * (1 - u) * 0.35)
        sy = int(cy + (hy - cy) * (1 - u) * 0.35)
        od2.ellipse([sx - r, sy - r, sx + r, sy + r], fill=col)
    img = Image.alpha_composite(img, orb2)
    draw = ImageDraw.Draw(img)

    ring_w = max(4, master // 70)
    draw.ellipse(
        [cx - orb_r, cy - orb_r, cx + orb_r, cy + orb_r],
        outline=ring,
        width=ring_w,
    )

    scale = master / 1000
    draw_kanji(draw, cx, cy, scale, (*k_out, 255), master * 0.04)
    draw_kanji(draw, cx, cy, scale, (*k_fill, 255), master * 0.028)

    if round_mask:
        mask = Image.new("L", (master, master), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, master - 1, master - 1], fill=255)
        out = Image.new("RGBA", (master, master), (0, 0, 0, 0))
        out.paste(img, (0, 0), mask)
        img = out

    return img.resize((size, size), Image.Resampling.LANCZOS)


def main():
    for stem, theme in THEMES.items():
        for folder, size in SIZES.items():
            out_dir = ROOT / folder
            out_dir.mkdir(parents=True, exist_ok=True)
            render(size, False, theme).save(out_dir / f"{stem}.png", optimize=True)
            render(size, True, theme).save(out_dir / f"{stem}_round.png", optimize=True)
            print(f"wrote {folder}/{stem} {size}px")


if __name__ == "__main__":
    main()
