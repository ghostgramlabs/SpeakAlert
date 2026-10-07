"""Builds the Play Store phone screenshots and feature graphic from raw app captures.

Raw captures (1080x2340 PNG straight from the device, clean demo status bar) live in
tools/store/raw/. Outputs: store/screenshots/NN_name.png (1080x1920) and
store/feature_graphic_1024x500.png. Run from the repo root:
    python tools/store/make_screenshots.py

Design: a bold headline with one highlighted word, the phone slightly tilted with a deep
shadow, and "floating" close-ups - the key part of the screen lifted out of the phone,
enlarged, with its own shadow - over a gradient with soft glows and a dot texture.
"""
import importlib.util
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

RAW = "tools/store/raw"
OUT = "store/screenshots"
W, H = 1080, 1920
ACCENT = (255, 196, 61)

FONTS = r"C:\Windows\Fonts"


def font(names, size):
    for n in names:
        p = os.path.join(FONTS, n)
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


HEAD = font(["seguibl.ttf", "segoeuib.ttf"], 96)
SUB = font(["segoeui.ttf"], 40)
CHIP = font(["segoeuib.ttf"], 34)


def rgb(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


# ------------------------------------------------------------------------------------- pieces
def background(c1, c2):
    a, b = rgb(c1), rgb(c2)
    small = Image.new("RGB", (54, 96))
    px = small.load()
    for y in range(96):
        for x in range(54):
            t = min(1, max(0, x / 54 * 0.35 + y / 96 * 0.65))
            px[x, y] = tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
    bg = small.resize((W, H), Image.BICUBIC).convert("RGBA")
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.ellipse([-300, -260, 640, 620], fill=(255, 255, 255, 60))
    g.ellipse([560, 1000, 1500, 1980], fill=(255, 255, 255, 34))
    g.ellipse([700, 240, 1200, 740], fill=ACCENT + (40,))
    bg = Image.alpha_composite(bg, glow.filter(ImageFilter.GaussianBlur(110)))
    dots = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(dots)
    for y in range(40, H, 46):
        for x in range(40, W, 46):
            fade = max(0, 1 - math.hypot(x - W * 0.8, y - H * 0.2) / 900)
            if fade > 0:
                d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(255, 255, 255, int(55 * fade)))
    return Image.alpha_composite(bg, dots)


def headline(img, parts, sub, y=110):
    """parts: (text, highlighted) pairs; a ("\\n", False) pair starts a new line."""
    d = ImageDraw.Draw(img)
    lines, cur = [], []
    for text, hl in parts:
        if text == "\n":
            lines.append(cur)
            cur = []
        else:
            cur.append((text, hl))
    lines.append(cur)
    for line in lines:
        total = sum(d.textlength(t, font=HEAD) for t, _ in line)
        x = (W - total) / 2
        for t, hl in line:
            tw = d.textlength(t, font=HEAD)
            if hl:
                d.rounded_rectangle([x - 14, y + 18, x + tw + 14, y + 112], radius=22, fill=ACCENT + (255,))
                d.text((x, y), t, font=HEAD, fill=(30, 27, 75, 255))
            else:
                d.text((x, y), t, font=HEAD, fill=(255, 255, 255, 255))
            x += tw
        y += 112
    y += 16
    row = ""
    for w_ in sub.split() + [None]:
        trial = (row + " " + w_).strip() if w_ else row
        if w_ is None or d.textlength(trial, font=SUB) > 900:
            tw = d.textlength(row, font=SUB)
            d.text(((W - tw) / 2, y), row, font=SUB, fill=(255, 255, 255, 225))
            y += 54
            row = w_ or ""
        else:
            row = trial
    return y


def phone(screen, sw=760):
    sh = int(screen.height * sw / screen.width)
    bezel, radius = 20, 86
    fw, fh = sw + 2 * bezel, sh + 2 * bezel
    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    d = ImageDraw.Draw(frame)
    d.rounded_rectangle([0, 0, fw - 1, fh - 1], radius=radius, fill=(15, 23, 42, 255))
    d.rounded_rectangle([3, 3, fw - 4, fh - 4], radius=radius - 3, outline=(90, 104, 130, 255), width=3)
    scr = screen.convert("RGBA").resize((sw, sh), Image.LANCZOS)
    m = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, sw - 1, sh - 1], radius=radius - bezel, fill=255)
    frame.paste(scr, (bezel, bezel), m)
    cx = fw // 2
    d.ellipse([cx - 12, bezel + 16, cx + 12, bezel + 40], fill=(15, 23, 42, 255))
    return frame


def paste_any(img, layer, pos):
    """alpha_composite that tolerates negative or overflowing positions."""
    x, y = pos
    l, t = max(0, -x), max(0, -y)
    r, b = min(layer.width, img.width - x), min(layer.height, img.height - y)
    if r > l and b > t:
        img.alpha_composite(layer.crop((l, t, r, b)), (x + l, y + t))


def shadowed(img, layer, pos, blur=36, alpha=150, offset=(0, 30)):
    a = layer.split()[-1].point(lambda v: v * alpha // 255)
    sh = Image.new("L", img.size, 0)
    ax, ay = pos[0] + offset[0], pos[1] + offset[1]
    l, t = max(0, -ax), max(0, -ay)
    sh.paste(a.crop((l, t, a.width, a.height)), (ax + l, ay + t))
    shadow = Image.new("RGBA", img.size, (12, 10, 40, 0))
    shadow.putalpha(sh.filter(ImageFilter.GaussianBlur(blur)))
    img.alpha_composite(shadow)
    paste_any(img, layer, pos)


def tilted_phone(img, name, angle, top, width=760, x=None):
    ph = phone(Image.open(os.path.join(RAW, name + ".png")), width)
    ph = ph.rotate(angle, resample=Image.BICUBIC, expand=True)
    x = (W - ph.width) // 2 if x is None else x
    shadowed(img, ph, (x, top), blur=46, alpha=170, offset=(16, 40))
    return ph


def card(name, box, scale, radius=34, border=True, angle=0):
    """A region of a raw capture lifted out as a floating card."""
    src = Image.open(os.path.join(RAW, name + ".png")).convert("RGBA").crop(box)
    src = src.resize((int(src.width * scale), int(src.height * scale)), Image.LANCZOS)
    pad = 10 if border else 0
    c = Image.new("RGBA", (src.width + 2 * pad, src.height + 2 * pad), (0, 0, 0, 0))
    if border:
        ImageDraw.Draw(c).rounded_rectangle([0, 0, c.width - 1, c.height - 1], radius=radius + pad,
                                            fill=(255, 255, 255, 255))
    m = Image.new("L", src.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, src.width - 1, src.height - 1], radius=radius, fill=255)
    c.paste(src, (pad, pad), m)
    return c.rotate(angle, resample=Image.BICUBIC, expand=True) if angle else c


def chip(text, icon=True):
    d = ImageDraw.Draw(Image.new("L", (1, 1)))
    tw = d.textlength(text, font=CHIP)
    w, h = int(tw + 64 + (46 if icon else 0)), 78
    c = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cd = ImageDraw.Draw(c)
    cd.rounded_rectangle([0, 0, w - 1, h - 1], radius=h // 2, fill=(255, 255, 255, 240))
    x = 30
    if icon:
        cd.ellipse([x - 4, 21, x + 32, 57], fill=ACCENT + (255,))
        cd.line([(x + 5, 40), (x + 12, 47), (x + 24, 31)], fill=(30, 27, 75, 255), width=5)
        x += 46
    cd.text((x, 16), text, font=CHIP, fill=(30, 27, 75, 255))
    return c


def sound_waves(img, cx, cy, radii=(60, 95, 130), start=-45, end=45):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for i, r in enumerate(radii):
        d.arc([cx - r, cy - r, cx + r, cy + r], start, end, fill=(255, 255, 255, 235 - i * 60), width=14)
    img.alpha_composite(layer)


def load_watch():
    spec = importlib.util.spec_from_file_location("mv", os.path.join(os.path.dirname(__file__), "make_video.py"))
    mv = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mv)
    return mv.watch_frame(mv.Layout("vertical"), 2.0)


# ------------------------------------------------------------------------------------- shots
def s1_hero():
    img = background("#3B82F6", "#4338CA")
    y = headline(img, [("Reminders that", False), ("\n", False), ("speak", True), (" to you", False)],
                 "Hear every reminder out loud, in your own voice or a clear spoken one.")
    tilted_phone(img, "home_today", 4, y + 40, 700, x=300)
    shadowed(img, card("alert", (60, 860, 1020, 1380), 0.74, angle=-4), (20, 1180))
    return img


def s2_voice():
    img = background("#6366F1", "#7C3AED")
    y = headline(img, [("Record it in your", False), ("\n", False), ("own voice", True)],
                 "Tap the mic, say it, pick a time. That's it.")
    tilted_phone(img, "editor_recording", -4, y + 60, 700, x=20)
    shadowed(img, card("editor_recording", (150, 985, 1050, 1945), 0.74, angle=4), (250, 960))
    return img


def s3_alert():
    img = background("#2563EB", "#1E3A8A")
    y = headline(img, [("Impossible to ", False), ("miss", True)],
                 "A full-screen alert that wakes the screen, even when the phone is locked.")
    tilted_phone(img, "alert", 0, y + 50, 720)
    shadowed(img, card("alert", (60, 1460, 1020, 2040), 0.8, angle=-3), (90, 1400))
    return img


def s4_widgets():
    img = background("#0EA5E9", "#4338CA")
    y = headline(img, [("Right on your", False), ("\n", False), ("home screen", True)],
                 "Widgets show what's next and add a reminder in one tap.")
    tilted_phone(img, "widgets", 3, y + 70, 700, x=290)
    shadowed(img, card("widgets", (60, 160, 1020, 1035), 0.72, radius=46, border=False, angle=-3), (20, 1040))
    return img


def s5_missed():
    img = background("#4F46E5", "#9333EA")
    y = headline(img, [("Nothing ", False), ("slips", True), ("\n", False), ("through", False)],
                 "Missed reminders wait for you, ready to play or dismiss.")
    tilted_phone(img, "missed_tab", -3, y + 50, 700, x=20)
    shadowed(img, card("missed_recovery", (0, 1280, 1080, 2250), 0.64, angle=3), (370, 1230))
    return img


def s6_repeat():
    img = background("#3B82F6", "#6D28D9")
    y = headline(img, [("Repeat it ", False), ("your way", True)],
                 "Daily, weekly, monthly or a custom schedule.")
    tilted_phone(img, "home_upcoming", 3, y + 70, 700, x=300)
    shadowed(img, card("repeat_sheet", (0, 1010, 1080, 2010), 0.62, angle=-4), (10, 1040))
    return img


def s7_watch():
    img = background("#312E81", "#1E1B4B")
    y = headline(img, [("On your ", False), ("watch", True), (" too", False)],
                 "Reminders reach your Wear OS smartwatch, with Done and Snooze.")
    tilted_phone(img, "alert", -6, y + 120, 600, x=10)
    w = load_watch()
    w = w.resize((int(w.width * 0.8), int(w.height * 0.8)), Image.LANCZOS)
    shadowed(img, w, (W - w.width + 70, y + 240), blur=40, alpha=160)
    return img


def s8_customize():
    img = background("#4338CA", "#7C3AED")
    y = headline(img, [("Make it ", False), ("yours", True)],
                 "Dozens of settings to fit how you like to be reminded.")
    tilted_phone(img, "settings_playback", -3, y + 90, 600, x=240)
    chips = ["4 app languages", "Auto spoken language", "Snooze 2 to 15 min", "Follow-up checks",
             "Tone-only mode", "Loop duration", "Quiet hours", "Private playback",
             "Keep until done", "Alerts in Do Not Disturb", "Dark mode", "Backup & restore"]
    ys = [y + 60, y + 170, y + 330, y + 440, y + 600, y + 710, y + 870, y + 980, y + 1140, y + 1250,
          y + 1410, y + 1520]
    for i, (text, cy) in enumerate(zip(chips, ys)):
        c = chip(text)
        x = 24 if i % 2 == 0 else W - c.width - 24
        if cy + c.height < H - 10:
            shadowed(img, c, (x, cy), blur=18, alpha=110, offset=(0, 10))
    return img


SHOTS = [s1_hero, s2_voice, s3_alert, s4_widgets, s5_missed, s6_repeat, s7_watch, s8_customize]


def feature_graphic():
    """Play's 1024x500 banner: icon, name and tagline on the left, the app on the right."""
    fw, fh = 1024, 500
    a, b = rgb("#3B82F6"), rgb("#4338CA")
    small = Image.new("RGB", (64, 32))
    px = small.load()
    for y in range(32):
        for x in range(64):
            t = x / 63 * 0.7 + y / 31 * 0.3
            px[x, y] = tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
    img = small.resize((fw, fh), Image.BICUBIC).convert("RGBA")
    glow = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.ellipse([-200, -260, 420, 300], fill=(255, 255, 255, 40))
    g.ellipse([620, 200, 1200, 760], fill=(255, 255, 255, 26))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(70)))
    icon = Image.open("store/play_store_icon.png").convert("RGBA").resize((150, 150), Image.LANCZOS)
    m = Image.new("L", (150, 150), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, 149, 149], radius=36, fill=255)
    sh = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    smk = Image.new("L", (fw, fh), 0)
    ImageDraw.Draw(smk).rounded_rectangle([64, 92, 214, 242], radius=36, fill=120)
    sh.putalpha(smk.filter(ImageFilter.GaussianBlur(16)))
    img = Image.alpha_composite(img, sh)
    img.paste(icon, (60, 80), m)
    d = ImageDraw.Draw(img)
    d.text((60, 258), "SpeakAlert", font=font(["seguibl.ttf", "segoeuib.ttf"], 78), fill=(255, 255, 255, 255))
    d.text((64, 362), "Reminders that speak to you", font=font(["segoeuib.ttf"], 34), fill=(255, 255, 255, 235))
    d.text((64, 410), "Free · No ads · Voice notes · Widgets · Smartwatch", font=font(["segoeui.ttf"], 24),
           fill=(255, 255, 255, 205))
    ph = phone(Image.open(os.path.join(RAW, "home_today.png")))
    ph = ph.resize((470, int(ph.height * 470 / ph.width)), Image.LANCZOS).rotate(-8, resample=Image.BICUBIC,
                                                                                    expand=True)
    px0, py0 = 600, 46
    sh = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    smk = Image.new("L", (fw, fh), 0)
    smk.paste(ph.split()[-1].point(lambda v: v * 120 // 255), (px0 + 18, py0 + 26))
    sh.putalpha(smk.filter(ImageFilter.GaussianBlur(24)))
    img = Image.alpha_composite(img, sh)
    img.paste(ph, (px0, py0), ph)
    return img.convert("RGB")


def main():
    os.makedirs(OUT, exist_ok=True)
    for f in os.listdir(OUT):
        if f.endswith(".png"):
            os.remove(os.path.join(OUT, f))
    feature_graphic().save("store/feature_graphic_1024x500.png", optimize=True)
    for i, fn in enumerate(SHOTS, 1):
        fn().convert("RGB").save(os.path.join(OUT, "%02d_%s.png" % (i, fn.__name__.split("_", 1)[1])),
                                 optimize=True)
    print("made", len(SHOTS), "screenshots in", OUT)


if __name__ == "__main__":
    main()
