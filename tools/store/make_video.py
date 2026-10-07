"""Builds the SpeakAlert promo video from raw screen recordings.

    python tools/store/make_video.py RAW_DIR [landscape|vertical] [narrated]

RAW_DIR holds the device recordings (1080x2340 mp4 from `adb shell screenrecord`) named as in
SEGMENTS below, plus optional NAME_end.png stills of each scene's settled last screen.
Output: store/video/speakalert_promo_<layout>.mp4 (H.264, AAC, 30 fps).

The edit: dead time in each recording (stretches where the screen does not change) is capped
at STATIC_CAP seconds, so every tap and transition plays at real speed but nothing drags.
Each segment shows the app in a phone frame beside a caption on an animated brand gradient.
Music is a soft pad generated here (no licensing), and the alert scene carries the reminder
spoken aloud, since the emulator's own audio cannot be captured.
"""
import math
import os
import subprocess
import sys
import tempfile
import wave

import imageio_ffmpeg
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

FF = imageio_ffmpeg.get_ffmpeg_exe()
FPS = 30
STATIC_CAP = 0.9          # seconds a still screen may stay on
XFADE = 10                # frames of crossfade between segments
SR = 44100

# file, start, end (seconds; None = to the end), headline, subline[, speed]
# "watch" is not a recording: it is drawn here (see watch_frame).
SEGMENTS = [
    ("01_home", 0.0, None, "All your reminders,\nat a glance", "Today, upcoming, missed and done."),
    ("02_voice", 1.2, None, "Record it in\nyour own voice", "Tap the mic, speak, pick a time. Done."),
    ("03_typed_repeat", 1.0, None, "Or type it, and\nmake it repeat", "Daily, weekly, monthly or a custom schedule."),
    ("04_alert", 0.0, None, "It speaks when\nit's time", "A full-screen alert you can't miss, even on the lock screen."),
    ("05_notification", 0.0, 21.5, "Act right from\nthe notification", "Replay, mark done or snooze, without opening the app."),
    ("09_widgets", 0.0, 10.8, "Home screen\nwidgets", "See what's next and add a reminder in one tap."),
    ("10_missed", 0.0, None, "Nothing slips\nthrough", "Missed reminders wait for you, ready to play or dismiss."),
    ("06_edit", 0.0, None, "Change plans\nin a tap", "Edit the time and date right on the reminder."),
    ("07_pause", 0.0, None, "Need some quiet?", "Pause reminders for an hour, the day, or until you choose."),
    ("watch", 0.0, 4.5, "On your\nsmartwatch too", "Reminders reach your Wear OS watch, with Done and Snooze."),
    ("08_settings", 3.0, 8.0, "Use it in\nyour language", "English, Spanish, Hindi and Arabic."),
    ("08_settings", 29.0, 36.5, "Spoken the way\nyou write it", "Each reminder is read in its own language automatically."),
    ("11_customize", 1.0, None, "Make it\nyours", "Snooze, follow-ups, tones, volume, quiet hours, backup and more.", 3.2),
    ("08_settings", 51.0, 64.0, "Beautiful in\ndark mode", "Easy on the eyes, day or night."),
]
SPEECH = {"04_alert": "Pack the gym bag for the morning."}

BRAND = [("#3B82F6", "#4338CA"), ("#4F46E5", "#7C3AED"), ("#2563EB", "#1E3A8A")]
FONTS = r"C:\Windows\Fonts"


def font(name, size):
    for n in name:
        p = os.path.join(FONTS, n)
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def rgb(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


class Layout:
    def __init__(self, kind):
        self.kind = kind
        if kind == "landscape":
            self.W, self.H = 1920, 1080
            self.screen_h = 940
            self.screen_w = int(self.screen_h * 1080 / 2340)
            self.phone_xy = (1140, 70)
            self.text_x, self.text_y, self.text_w = 150, 330, 820
            self.head, self.sub = font(["seguibl.ttf"], 92), font(["segoeui.ttf"], 40)
        else:
            self.W, self.H = 1080, 1920
            # Two-line headline plus a two-line subline ends near y=530; the phone starts below it.
            self.screen_h = 1290
            self.screen_w = int(self.screen_h * 1080 / 2340)
            self.phone_xy = ((1080 - self.screen_w) // 2 - 16, 580)
            self.text_x, self.text_y, self.text_w = 90, 96, 900
            self.head, self.sub = font(["seguibl.ttf"], 84), font(["segoeui.ttf"], 38)


# ---------------------------------------------------------------- source frames
def _ffmpeg_args(path, start, end, w, h, pix):
    args = [FF, "-v", "error"]
    if start:
        args += ["-ss", str(start)]
    args += ["-i", path]
    if end is not None:
        args += ["-t", str(end - start)]
    return args + ["-vf", "fps=%d,scale=%d:%d:flags=lanczos" % (FPS, w, h), "-f", "rawvideo",
                   "-pix_fmt", pix, "-"]


class Clip:
    """A recording, decoded lazily so memory stays flat however long it is.

    A first pass at thumbnail size decides which frames to keep (long still stretches are
    capped at STATIC_CAP, then an optional speed-up); iterating streams the full-size frames
    and yields only those, followed by any still frames appended with hold()."""

    def __init__(self, path, start, end, w, h, speed=1.0):
        self.args = (path, start, end, w, h)
        self.w, self.h = w, h
        sw, sh = 68, 148
        proc = subprocess.Popen(_ffmpeg_args(path, start, end, sw, sh, "gray"), stdout=subprocess.PIPE)
        keep, still, prev, i = [], 0, None, 0
        cap = int(STATIC_CAP * FPS)
        while True:
            buf = proc.stdout.read(sw * sh)
            if len(buf) < sw * sh:
                break
            small = np.frombuffer(buf, np.uint8).astype(np.int16)
            if prev is not None and np.abs(small - prev).mean() < 0.6:
                still += 1
            else:
                still = 0
            if still <= cap:
                keep.append(i)
                prev = small
            i += 1
        proc.wait()
        if speed != 1.0:
            keep = [keep[int(k * speed)] for k in range(int(len(keep) / speed))]
        self.keep = keep
        self.start = start
        self.extra = []          # (frame or None for "repeat the last one", count)
        self.freezes = {}        # kept position -> extra copies of that frame
        self.mask = None

    def _pos(self, src_sec):
        """Position in the kept frames of the first frame at or after src_sec (source time)."""
        target = round((src_sec - (self.start or 0)) * FPS)
        for p, k in enumerate(self.keep):
            if k >= target:
                return p
        return len(self.keep) - 1

    def freeze_at(self, src_sec, seconds):
        p = self._pos(src_sec)
        self.freezes[p] = self.freezes.get(p, 0) + int(seconds * FPS)

    def delay_from(self, src_sec, seconds):
        """Hold the frame just before src_sec, so the action at src_sec happens later."""
        p = self._pos(src_sec)
        q = max(0, p - 1)
        self.freezes[q] = self.freezes.get(q, 0) + int(round(seconds * FPS))

    def out_seconds(self, src_sec):
        """When the frame at src_sec appears in this clip's output, in seconds."""
        p = self._pos(src_sec)
        return (p + sum(c for q, c in self.freezes.items() if q < p)) / FPS

    def hold(self, frame, count):
        self.extra.append((frame, count))

    def __len__(self):
        return len(self.keep) + sum(self.freezes.values()) + sum(c for _, c in self.extra)

    def __iter__(self):
        path, start, end, w, h = self.args
        wanted = set(self.keep)
        pos = {k: p for p, k in enumerate(self.keep)}
        proc = subprocess.Popen(_ffmpeg_args(path, start, end, w, h, "rgb24"), stdout=subprocess.PIPE,
                                stderr=subprocess.DEVNULL)
        size = w * h * 3
        i, last = 0, None
        try:
            while wanted:
                buf = proc.stdout.read(size)
                if len(buf) < size:
                    break
                if i in wanted:
                    wanted.discard(i)
                    last = np.frombuffer(buf, np.uint8).reshape(h, w, 3)
                    out = self.mask(last) if self.mask else last
                    for _ in range(1 + self.freezes.get(pos[i], 0)):
                        yield out
                i += 1
        finally:
            proc.stdout.close()
            proc.kill()
            proc.wait()
        for frame, count in self.extra:
            f = last if frame is None else frame
            last = f
            for _ in range(count):
                yield self.mask(f) if self.mask else f


def mask_system_notifications(frame):
    """In the notification shade, cover everything below the first card (the emulator's own
    'Serial console' notice), using the panel's background colour."""
    h = frame.shape[0]
    top = frame[int(h * 0.02):int(h * 0.04), :].mean()
    if top > 90:                      # shade not open: the app's light top bar
        return frame
    out = frame.copy()
    y = int(h * 0.455)
    colour = frame[int(h * 0.45), int(frame.shape[1] * 0.02)]
    out[y:, :] = colour
    return out


# ---------------------------------------------------------------- static layers
def background(L, t, colours):
    a, b = rgb(colours[0]), rgb(colours[1])
    W, H = L.W, L.H
    small = np.zeros((36, 64, 3), np.float32)
    yy, xx = np.mgrid[0:36, 0:64]
    ang = 0.6 + 0.25 * math.sin(t * 0.35)
    tt = ((xx / 63) * math.cos(ang) + (yy / 35) * math.sin(ang)) / (abs(math.cos(ang)) + abs(math.sin(ang)))
    for i in range(3):
        small[..., i] = a[i] + (b[i] - a[i]) * tt
    img = Image.fromarray(small.astype(np.uint8)).resize((W, H), Image.BICUBIC).convert("RGBA")
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    ox, oy = 120 * math.sin(t * 0.4), 80 * math.cos(t * 0.3)
    g.ellipse([-W * 0.15 + ox, -H * 0.3 + oy, W * 0.45 + ox, H * 0.55 + oy], fill=(255, 255, 255, 44))
    g.ellipse([W * 0.55 - ox, H * 0.45 - oy, W * 1.15 - ox, H * 1.25 - oy], fill=(255, 255, 255, 28))
    return Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(min(W, H) * 0.09)))


def phone_layer(L):
    """Phone body with a transparent screen, plus its drop shadow, on a full-canvas layer."""
    bez, rad = 16, 64
    sw, sh = L.screen_w, L.screen_h
    fw, fh = sw + 2 * bez, sh + 2 * bez
    x, y = L.phone_xy
    # The shadow is its own layer: it goes under the screen, the body goes over it.
    shadow_layer = Image.new("RGBA", (L.W, L.H), (8, 10, 40, 0))
    shadow = Image.new("L", (L.W, L.H), 0)
    ImageDraw.Draw(shadow).rounded_rectangle([x + 10, y + 26, x + fw - 10, y + fh + 10], radius=rad, fill=150)
    shadow_layer.putalpha(shadow.filter(ImageFilter.GaussianBlur(34)))
    layer = Image.new("RGBA", (L.W, L.H), (0, 0, 0, 0))
    body = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    d.rounded_rectangle([0, 0, fw - 1, fh - 1], radius=rad, fill=(15, 23, 42, 255))
    d.rounded_rectangle([2, 2, fw - 3, fh - 3], radius=rad - 2, outline=(80, 92, 115, 255), width=2)
    hole = Image.new("L", (fw, fh), 0)
    ImageDraw.Draw(hole).rounded_rectangle([bez, bez, bez + sw - 1, bez + sh - 1], radius=rad - bez, fill=255)
    a = body.split()[-1]
    body.putalpha(Image.fromarray(np.minimum(np.array(a), 255 - np.array(hole))))
    cam = fw // 2
    d = ImageDraw.Draw(body)
    d.ellipse([cam - 9, bez + 14, cam + 9, bez + 32], fill=(15, 23, 42, 255))
    layer.alpha_composite(body, (x, y))
    screen_mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(screen_mask).rounded_rectangle([0, 0, sw - 1, sh - 1], radius=rad - bez, fill=255)
    return shadow_layer, layer, screen_mask, (x + bez, y + bez)


def wrap(text, fnt, max_w):
    d = ImageDraw.Draw(Image.new("L", (1, 1)))
    out = []
    for para in text.split("\n"):
        cur = ""
        for w in para.split():
            trial = (cur + " " + w).strip()
            if d.textlength(trial, font=fnt) <= max_w:
                cur = trial
            else:
                out.append(cur)
                cur = w
        out.append(cur)
    return out


def caption_layer(L, head, sub, step, total):
    layer = Image.new("RGBA", (L.W, L.H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x, y = L.text_x, L.text_y
    centre = L.kind == "vertical"
    chip = "%d / %d" % (step, total)
    cf = font(["segoeuib.ttf"], 30)
    cw = d.textlength(chip, font=cf) + 36
    cx = (L.W - cw) / 2 if centre else x
    d.rounded_rectangle([cx, y, cx + cw, y + 50], radius=25, fill=(255, 255, 255, 46))
    d.text((cx + 18, y + 7), chip, font=cf, fill=(255, 255, 255, 235))
    y += 78
    for line in wrap(head, L.head, L.text_w):
        tw = d.textlength(line, font=L.head)
        d.text(((L.W - tw) / 2 if centre else x, y), line, font=L.head, fill=(255, 255, 255, 255))
        y += int(L.head.size * 1.14)
    y += 18
    for line in wrap(sub, L.sub, L.text_w):
        tw = d.textlength(line, font=L.sub)
        d.text(((L.W - tw) / 2 if centre else x, y), line, font=L.sub, fill=(255, 255, 255, 215))
        y += int(L.sub.size * 1.35)
    return layer


def title_card(L, t, lines, icon_size):
    """Intro / outro: app icon, name and lines, centred."""
    img = background(L, t, BRAND[0])
    icon = Image.open("store/play_store_icon.png").convert("RGBA").resize((icon_size, icon_size), Image.LANCZOS)
    m = Image.new("L", (icon_size, icon_size), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, icon_size - 1, icon_size - 1], radius=int(icon_size * 0.23), fill=255)
    cy = L.H // 2 - icon_size // 2 - (110 if L.kind == "landscape" else 200)
    sh = Image.new("L", (L.W, L.H), 0)
    ImageDraw.Draw(sh).rounded_rectangle([(L.W - icon_size) // 2 + 8, cy + 22, (L.W + icon_size) // 2 - 8,
                                          cy + icon_size + 14], radius=int(icon_size * 0.23), fill=140)
    shadow = Image.new("RGBA", (L.W, L.H), (10, 10, 40, 0))
    shadow.putalpha(sh.filter(ImageFilter.GaussianBlur(26)))
    img = Image.alpha_composite(img, shadow)
    img.paste(icon, ((L.W - icon_size) // 2, cy), m)
    d = ImageDraw.Draw(img)
    y = cy + icon_size + 44
    for text, f, alpha in lines:
        tw = d.textlength(text, font=f)
        d.text(((L.W - tw) / 2, y), text, font=f, fill=(255, 255, 255, alpha))
        y += int(f.size * 1.3)
    return img


# ---------------------------------------------------------------- smartwatch
watch_last = [None]


def watch_frame(L, t):
    """A round Wear OS watch showing SpeakAlert's reminder notification; t in seconds."""
    size = 620 if L.kind == "landscape" else 760
    pad = 140
    img = Image.new("RGBA", (size + 2 * pad, size + 2 * pad + 260), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = img.width // 2, pad + 130 + size // 2
    # strap and case
    d.rounded_rectangle([cx - size * 0.3, 0, cx + size * 0.3, img.height], radius=60, fill=(30, 33, 48, 255))
    d.ellipse([cx - size / 2 - 26, cy - size / 2 - 26, cx + size / 2 + 26, cy + size / 2 + 26], fill=(55, 60, 78, 255))
    d.ellipse([cx - size / 2 - 14, cy - size / 2 - 14, cx + size / 2 + 14, cy + size / 2 + 14], fill=(20, 22, 30, 255))
    d.rounded_rectangle([cx + size / 2 + 8, cy - 40, cx + size / 2 + 40, cy + 40], radius=14, fill=(70, 76, 96, 255))
    d.ellipse([cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2], fill=(0, 0, 0, 255))
    # the notification slides up and fades in
    k = min(1.0, t / 0.7)
    ease = 1 - (1 - k) ** 3
    oy = int((1 - ease) * 80)
    a = ease
    s = size / 620
    tf = font(["segoeuib.ttf"], int(30 * s))
    hf = font(["seguibl.ttf"], int(54 * s))
    bf = font(["segoeui.ttf"], int(32 * s))
    btf = font(["segoeuib.ttf"], int(34 * s))
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    clock = "10:15"
    tw = ld.textlength(clock, font=tf)
    ld.text((cx - tw / 2, cy - size * 0.42), clock, font=tf, fill=(200, 205, 220, 255))
    icon = Image.open("store/play_store_icon.png").convert("RGBA").resize((int(84 * s), int(84 * s)), Image.LANCZOS)
    m = Image.new("L", icon.size, 0)
    ImageDraw.Draw(m).ellipse([0, 0, icon.width - 1, icon.height - 1], fill=255)
    pulse = 0.5 + 0.5 * math.sin(t * 4)
    r = icon.width / 2 + 8 + 10 * pulse
    iy = cy - size * 0.28 + oy
    ld.ellipse([cx - r, iy + icon.height / 2 - r, cx + r, iy + icon.height / 2 + r],
               outline=(96, 165, 250, int(120 * (1 - pulse))), width=4)
    layer.paste(icon, (int(cx - icon.width / 2), int(iy)), m)
    y = iy + icon.height + 14 * s
    for text, f, col in (("Take vitamins", hf, (255, 255, 255, 255)),
                         ("Take your vitamins", bf, (190, 195, 210, 255)),
                         ("with breakfast", bf, (190, 195, 210, 255))):
        tw = ld.textlength(text, font=f)
        ld.text((cx - tw / 2, y), text, font=f, fill=col)
        y += f.size * 1.18
    y += 14 * s
    bw, bh = 168 * s, 74 * s
    for i, (label, fill) in enumerate((("Done", (59, 130, 246, 255)), ("Snooze", (55, 60, 78, 255)))):
        x0 = cx - bw - 8 * s if i == 0 else cx + 8 * s
        ld.rounded_rectangle([x0, y, x0 + bw, y + bh], radius=bh / 2, fill=fill)
        tw = ld.textlength(label, font=btf)
        ld.text((x0 + (bw - tw) / 2, y + (bh - btf.size) / 2 - 4 * s), label, font=btf, fill=(255, 255, 255, 255))
    layer.putalpha(Image.fromarray((np.array(layer.split()[-1]) * a).astype(np.uint8)))
    img.alpha_composite(layer)
    return img


def watch_segment(L, emit, now, head, sub, idx, total, last, seconds):
    cap = caption_layer(L, head, sub, idx, total)
    colours = BRAND[idx % len(BRAND)]
    n = int(seconds * FPS)
    canvas = None
    for i in range(n):
        t = now() / FPS
        img = background(L, t, colours)
        w = watch_frame(L, i / FPS)
        if L.kind == "landscape":
            pos = (L.phone_xy[0] + L.screen_w // 2 - w.width // 2 + 10, (L.H - w.height) // 2)
        else:
            pos = ((L.W - w.width) // 2, L.phone_xy[1] - 40)
        sh = Image.new("L", img.size, 0)
        sh.paste(w.split()[-1].point(lambda v: v * 110 // 255), (pos[0] + 14, pos[1] + 30))
        shadow = Image.new("RGBA", img.size, (8, 10, 40, 0))
        shadow.putalpha(sh.filter(ImageFilter.GaussianBlur(30)))
        canvas = Image.alpha_composite(img, shadow)
        canvas.alpha_composite(w, pos)
        a = min(1, i / 9) * min(1, (n - i) / 6)
        capf = cap.copy()
        capf.putalpha(cap.split()[-1].point(lambda v, a=a: int(v * a)))
        canvas.alpha_composite(capf, (0, int((1 - min(1, i / 9)) * 24)))
        if i < XFADE:
            canvas = Image.blend(last.convert("RGBA"), canvas, (i + 1) / (XFADE + 1))
        emit(canvas)
    watch_last[0] = canvas
    print("segment", idx, "watch", n, "frames", flush=True)


# ---------------------------------------------------------------- audio
def music(seconds, cuts=()):
    """An upbeat track generated here (no licensing): 112 BPM, kick / clap / hats, a bassline,
    plucked chords and an arpeggio over C - Am - F - G, with a whoosh at each scene change."""
    bpm = 112.0
    beat = 60.0 / bpm
    n = int(seconds * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    rng = np.random.default_rng(7)

    def add(sig, at, gain):
        s = int(at * SR)
        if s >= n:
            return
        e = min(n, s + len(sig))
        out[s:e] += sig[: e - s] * gain

    def env(length, attack, decay):
        tt = np.arange(int(length * SR)) / SR
        return np.minimum(1, tt / max(attack, 1e-4)) * np.exp(-tt / decay)

    # instruments
    kt = np.arange(int(0.35 * SR)) / SR
    kick = np.sin(2 * np.pi * (48 * kt + 90 * (1 - np.exp(-kt * 30)) / 30 * 1.0)) * np.exp(-kt * 9)
    clap = rng.standard_normal(int(0.22 * SR)) * env(0.22, 0.002, 0.05)
    clap = np.convolve(clap, np.ones(3) / 3, "same")
    hat = rng.standard_normal(int(0.06 * SR))
    hat = (hat - np.convolve(hat, np.ones(8) / 8, "same")) * env(0.06, 0.001, 0.015)

    def pluck(freq, length, bright=1.0):
        tt = np.arange(int(length * SR)) / SR
        saw = 2 * ((freq * tt) % 1) - 1
        sq = np.sign(np.sin(2 * np.pi * freq * 1.003 * tt)) * 0.5
        sig = (saw * 0.6 + sq * 0.4)
        k = max(2, int(SR / (freq * 6 * bright)))
        sig = np.convolve(sig, np.ones(k) / k, "same")
        return sig * env(length, 0.004, length * 0.35)

    def bass(freq, length):
        tt = np.arange(int(length * SR)) / SR
        sig = np.sin(2 * np.pi * freq * tt) + 0.3 * np.sin(2 * np.pi * freq * 2 * tt)
        return sig * env(length, 0.005, length * 0.6)

    chords = [  # root (bass) and chord tones, Hz
        (65.41, [261.63, 329.63, 392.00, 523.25]),   # C
        (55.00, [220.00, 261.63, 329.63, 440.00]),   # Am
        (43.65, [174.61, 220.00, 261.63, 349.23]),   # F
        (49.00, [196.00, 246.94, 293.66, 392.00]),   # G
    ]
    bars = int(seconds / (4 * beat)) + 1
    for bar in range(bars):
        root, tones = chords[bar % 4]
        b0 = bar * 4 * beat
        intro = b0 < 4 * beat * 2          # first two bars: no drums, let it breathe
        for q in range(4):
            at = b0 + q * beat
            if not intro:
                add(kick, at, 0.9 if q in (0, 2) else 0.55)
                if q in (1, 3):
                    add(clap, at, 0.35)
            for h in (0, 0.5):
                if not intro:
                    add(hat, at + h * beat, 0.16 if h else 0.1)
            # bass: root on the beat, octave on the off-beat
            add(bass(root, beat * 0.45), at, 0.5)
            add(bass(root * 2, beat * 0.3), at + 0.5 * beat, 0.25)
        # chord stabs on 1 and the "and" of 2
        for at in (b0, b0 + 1.5 * beat, b0 + 3 * beat):
            for f in tones[:3]:
                add(pluck(f, beat * 0.9, 0.8), at, 0.09)
        # sixteenth arpeggio from bar 5 on
        if bar >= 4:
            for k in range(16):
                f = tones[[0, 1, 2, 3, 2, 1, 2, 3][k % 8]] * 2
                add(pluck(f, beat * 0.25, 1.4), b0 + k * beat / 4, 0.045)
    # gentle sidechain pump from the kick
    pump = 1 - 0.25 * np.exp(-((t % beat) / 0.12))
    out *= pump
    # whoosh into each scene
    for c in cuts:
        w = rng.standard_normal(int(0.5 * SR))
        ww = np.arange(len(w)) / len(w)
        w = np.convolve(w, np.ones(12) / 12, "same") * np.sin(np.pi * ww) ** 2
        add(w, max(0, c - 0.25), 0.18)
    out *= np.minimum(1, t / 1.0) * np.minimum(1, (seconds - t) / 2.0)
    return out / (np.abs(out).max() + 1e-9) * 0.5


def speak(text):
    """Windows speech to a mono float array at SR."""
    tmp = tempfile.mktemp(suffix=".wav")
    ps = ("Add-Type -AssemblyName System.Speech; $s=New-Object System.Speech.Synthesis.SpeechSynthesizer; "
          "try { $s.SelectVoice('Microsoft Zira Desktop') } catch {}; $s.Rate=-1; "
          "$s.SetOutputToWaveFile('%s'); $s.Speak('%s'); $s.Dispose()" % (tmp, text.replace("'", "''")))
    subprocess.run(["powershell", "-NoProfile", "-Command", ps], capture_output=True)
    conv = tmp + ".f32"
    subprocess.run([FF, "-v", "error", "-y", "-i", tmp, "-ac", "1", "-ar", str(SR), "-f", "f32le", conv])
    data = np.fromfile(conv, np.float32)
    os.remove(tmp)
    os.remove(conv)
    return data / (np.abs(data).max() + 1e-9) * 0.9


# ---------------------------------------------------------------- narration
VOICE = "en-US-AriaNeural"
# One line per segment (same order as SEGMENTS), plus the intro and outro cards.
# A scene's narration is either one line (spoken as the scene starts) or timed cues:
# [(recording_seconds, text), ...], each spoken as that moment of the recording appears.
NARRATION = {
    "intro": "Meet SpeakAlert, the free reminder app that speaks to you. No ads, ever.",
    1: [(0.0, "All your reminders, in one place."),
        (11.5, "Today's list, and everything coming up next.")],
    2: [(1.2, "Tap the mic, and record a reminder in your own voice."),
        (17.5, "Add a short label,"),
        (28.5, "pick a time,"),
        (39.0, "and save.")],
    3: [(1.0, "Prefer typing? Just write it out."),
        (16.5, "Give it a short label,"),
        (26.5, "then make it repeat: daily, weekly, monthly, or on your own schedule.")],
    4: "When it's time, a full-screen alert wakes your phone and reads your reminder out loud, "
       "even on the lock screen.",
    5: [(9.5, "When a reminder is due, it plays right away."),
        (16.8, "Replay it, mark it done, or snooze it, right from the notification.")],
    6: [(0.0, "Home screen widgets show what's coming up,"),
        (9.6, "and add a new reminder in one tap.")],
    7: "Missed one? It waits for you. Play it now, or review it later.",
    8: "Plans changed? Tap the time or date on any reminder to move it.",
    9: "Need some quiet? Pause every reminder for an hour, the rest of the day, or until you choose.",
    10: "Reminders reach your Wear OS watch too, so you can tap done or snooze right from your wrist.",
    11: "Use the app in English, Spanish, Hindi, or Arabic.",
    12: "And each reminder is spoken in the language you wrote it in, automatically.",
    13: [(1.0, "Make it yours. Choose the theme, the language, and what the reminder form shows."),
         (37.4, "Decide how reminders play: spoken, tone only, how loud, and for how long."),
         (72.9, "Set snooze times and follow-up checks,"),
         (93.3, "quiet hours, Do Not Disturb, and your watch,"),
         (113.7, "plus backup, and much more.")],
    14: "Beautiful in dark mode, and easy on the eyes, day or night.",
    "outro": "SpeakAlert. Free, with no ads. Get it on Google Play today.",
}
NARRATION_CACHE = "store/video/narration_cache"


def narration_audio(key):
    """Aria reading NARRATION[key] (a single line), as a float array at SR."""
    return tts(NARRATION[key])


def tts(text):
    """Aria reading text, as a float array at SR. Cached, since edge-tts is online."""
    import hashlib
    os.makedirs(NARRATION_CACHE, exist_ok=True)
    mp3 = os.path.join(NARRATION_CACHE, hashlib.sha1((VOICE + text).encode()).hexdigest()[:16] + ".mp3")
    if not os.path.exists(mp3):
        subprocess.run([sys.executable, "-m", "edge_tts", "--voice", VOICE, "--rate", "+4%", "--text", text,
                        "--write-media", mp3], check=True, capture_output=True)
    raw = subprocess.run([FF, "-v", "error", "-i", mp3, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                         capture_output=True).stdout
    v = np.frombuffer(raw, np.float32).astype(np.float64)
    return v / (np.abs(v).max() + 1e-9) * 0.92


# Narrated cut only: hold the right screen while the narrator describes it. Times are in the
# recording's own seconds. "speech_src" is where the app starts reading the reminder aloud.
NARRATED_TIMING = {
    # alert shows at once; hold it through the narration, then "Play again" -> "Playing now"
    "04_alert": {"freeze": [(4.0, 5.6), (9.0, 1.6)], "speech_src": 8.4},
    # skip the launch splash, hold the "You missed 2 reminders" sheet
    "10_missed": {"start": 5.6, "freeze": [(6.8, 2.6)]},
    # hold the Pause options while they are listed
    "07_pause": {"freeze": [(5.6, 3.6)]},
    # start as the reminder fires (the app starts playing it), not 10 s of idle Home screen
    "05_notification": {"start": 9.5},
}
CUE_GAP = 0.25          # seconds of air between timed cues

LEAD_IN = 0.45          # seconds into a scene before the narrator starts
TAIL = 0.7              # seconds of air after the line before the next scene


# ---------------------------------------------------------------- render
def keep_awake():
    """Ask Windows not to sleep while rendering; a sleeping PC silently stalls the pipes."""
    if sys.platform == "win32":
        import ctypes
        ctypes.windll.kernel32.SetThreadExecutionState(0x80000000 | 0x00000001)  # CONTINUOUS | SYSTEM


def build(raw_dir, kind, narrated=False):
    keep_awake()
    L = Layout(kind)
    os.makedirs("store/video", exist_ok=True)
    out_video = OUT_OVERRIDE or "store/video/speakalert_promo_%s%s.mp4" % ("narrated_" if narrated else "", kind)
    silent = out_video + ".video.mp4"
    enc = subprocess.Popen([FF, "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", "%dx%d" % (L.W, L.H),
                            "-r", str(FPS), "-i", "-", "-c:v", "libx264", "-preset", "slow", "-crf", "18",
                            "-pix_fmt", "yuv420p", "-movflags", "+faststart", silent], stdin=subprocess.PIPE)
    frame_no = 0
    voices = []          # (seconds, samples, gain)
    cuts = []

    def emit(img):
        nonlocal frame_no
        enc.stdin.write(np.asarray(img.convert("RGB"), np.uint8).tobytes())
        frame_no += 1

    def line(key):
        """Queue the narrator for this moment; returns the seconds the scene must last."""
        if not narrated:
            return 0.0
        v = narration_audio(key)
        voices.append((frame_no / FPS + LEAD_IN, v, 1.0))
        return LEAD_IN + len(v) / SR + TAIL

    phone_shadow, phone, screen_mask, screen_xy = phone_layer(L)

    # Intro
    name_f = font(["seguibl.ttf"], 110 if kind == "landscape" else 120)
    tag_f = font(["segoeuib.ttf"], 46)
    small_f = font(["segoeuib.ttf"], 34)
    intro = [("SpeakAlert", name_f, 255), ("Reminders that speak to you", tag_f, 235),
             ("Free  ·  No ads", small_f, 200)]
    n_intro = int(max(2.8, line("intro")) * FPS)
    for i in range(n_intro):
        t = frame_no / FPS
        card = title_card(L, t, intro, 230 if kind == "landscape" else 300)
        k = min(1, i / 12)
        if k < 1:
            card = Image.blend(background(L, t, BRAND[0]), card, k)
        emit(card)
    last = card

    total = len(SEGMENTS)
    for idx, seg in enumerate(SEGMENTS, 1):
        name, start, end, head, sub = seg[:5]
        cuts.append(frame_no / FPS)
        speed = seg[5] if len(seg) > 5 else 1.0
        cues = NARRATION.get(idx) if narrated and isinstance(NARRATION.get(idx), list) else None
        need = 0.0 if cues else line(idx)
        timing = NARRATED_TIMING.get(name, {}) if narrated else {}
        start = timing.get("start", start)
        if name == "watch":
            watch_segment(L, emit, lambda: frame_no, head, sub, idx, len(SEGMENTS), last, max(end, need))
            last = watch_last[0]
            continue
        frames = Clip(os.path.join(raw_dir, name + ".mp4"), start, end, L.screen_w, L.screen_h, speed)
        for src, secs in timing.get("freeze", []):
            frames.freeze_at(src, secs)
        if cues:
            free_at = LEAD_IN
            for src, text in cues:
                v = tts(text)
                at = frames.out_seconds(src)
                if at < free_at:
                    # The narrator is still talking: hold the screen until she has finished.
                    frames.delay_from(src, free_at - at)
                    at = frames.out_seconds(src)
                at = max(at, free_at)
                voices.append((frame_no / FPS + at, v, 1.0))
                free_at = at + len(v) / SR + CUE_GAP
            need = free_at - CUE_GAP + TAIL
        if name in SPEECH:
            # The reminder itself, read aloud by the app.
            v = speak(SPEECH[name])
            at = frames.out_seconds(timing["speech_src"]) if "speech_src" in timing else 0.6
            voices.append((frame_no / FPS + at, v, 0.85))
            need = max(need, at + len(v) / SR + TAIL)
        end_still = os.path.join(raw_dir, name + "_end.png")
        # Hold the settled last screen. A trimmed clip normally ends mid-scene, so only the
        # widgets clip (cut just before the app's blank launch frame) borrows its still too.
        if (end is None or name == "09_widgets") and os.path.exists(end_still):
            still = np.asarray(Image.open(end_still).convert("RGB").resize((L.screen_w, L.screen_h), Image.LANCZOS))
            frames.hold(still, int(1.0 * FPS))
        frames.hold(None, int(0.6 * FPS))               # a beat to read the result
        short = int(need * FPS) - len(frames)
        if short > 0:
            frames.hold(None, short)                    # let the narrator finish
        if name == "05_notification":
            frames.mask = mask_system_notifications
        cap = caption_layer(L, head, sub, idx, total)
        colours = BRAND[idx % len(BRAND)]
        n_frames = len(frames)
        for i, f in enumerate(frames):
            t = frame_no / FPS
            img = background(L, t, colours)
            scr = Image.fromarray(f)
            canvas = Image.alpha_composite(img, phone_shadow)
            canvas.paste(scr, screen_xy, screen_mask)
            canvas = Image.alpha_composite(canvas, phone)
            a = min(1, i / 9) * min(1, (n_frames - i) / 6)
            capf = cap.copy()
            capf.putalpha(cap.split()[-1].point(lambda v, a=a: int(v * a)))
            dy = int((1 - min(1, i / 9)) * 24)
            canvas.alpha_composite(capf, (0, dy))
            if i < XFADE:
                canvas = Image.blend(last.convert("RGBA"), canvas, (i + 1) / (XFADE + 1))
            emit(canvas)
        last = canvas
        print("segment", idx, name, n_frames, "frames", flush=True)

    # Outro
    out_lines = [("SpeakAlert", name_f, 255), ("Never miss what matters", tag_f, 235),
                 ("Free  ·  No ads  ·  Get it on Google Play", font(["segoeuib.ttf"], 40), 225),
                 ("Search “SpeakAlert” on Google Play", font(["segoeui.ttf"], 34), 200)]
    outro_s = max(4.2, line("outro") + 0.8)
    n_out = int(outro_s * FPS)
    for i in range(n_out):
        t = frame_no / FPS
        card = title_card(L, t, out_lines, 230 if kind == "landscape" else 300)
        if i < XFADE:
            card = Image.blend(last.convert("RGBA"), card.convert("RGBA"), (i + 1) / (XFADE + 1))
        if i > n_out - 14:
            card = Image.blend(card.convert("RGBA"), Image.new("RGBA", card.size, (0, 0, 0, 255)),
                               (i - (n_out - 14)) / 14)
        emit(card)
    enc.stdin.close()
    enc.wait()

    # Audio: the music bed (quieter under a narrator), with every voice ducking it.
    seconds = frame_no / FPS
    cuts.append(seconds - outro_s)      # into the end card
    mix = music(seconds, cuts) * (0.55 if narrated else 1.0)
    duck = np.ones(len(mix))
    fade = int(0.15 * SR)
    for at, v, _ in voices:
        s, e = int(at * SR), min(len(mix), int(at * SR) + len(v))
        lo = 0.4
        duck[s:e] = np.minimum(duck[s:e], lo)
        ramp_in = np.linspace(1, lo, fade)
        a0 = max(0, s - fade)
        duck[a0:s] = np.minimum(duck[a0:s], ramp_in[-(s - a0):] if s > a0 else ramp_in[:0])
        b1 = min(len(mix), e + fade)
        duck[e:b1] = np.minimum(duck[e:b1], np.linspace(lo, 1, fade)[: b1 - e])
    mix *= duck
    for at, v, gain in voices:
        s = int(at * SR)
        e = min(len(mix), s + len(v))
        mix[s:e] += v[: e - s] * gain
    mix = np.clip(mix, -1, 1)
    wav = out_video + ".wav"
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((mix * 32767).astype(np.int16).tobytes())
    subprocess.run([FF, "-v", "error", "-y", "-i", silent, "-i", wav, "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-ac", "2",
                    "-shortest", "-movflags", "+faststart", out_video])
    os.remove(silent)
    os.remove(wav)
    print("wrote", out_video, "%.1fs" % seconds)


# ---------------------------------------------------------------- YouTube Shorts cut
# Under a minute: a hook, the seven features that sell the app, and the Play Store end card.
OUT_OVERRIDE = None
SHORTS_SEGMENTS = [
    ("02_voice", 1.2, None, "Say it,\nsave it", "Record a reminder in your own voice.", 1.8),
    ("04_alert", 0.0, None, "It speaks\nout loud", "A full-screen alert, even on the lock screen."),
    ("05_notification", 16.5, 21.5, "Act from the\nnotification", "Snooze or mark done in one tap."),
    ("09_widgets", 0.0, 10.8, "Home screen\nwidgets", "Add a reminder in one tap.", 1.6),
    ("10_missed", 5.6, 15.5, "Nothing slips\nthrough", "Missed reminders wait for you."),
    ("watch", 0.0, 3.6, "On your\nwatch too", "Done and Snooze on your wrist."),
    ("08_settings", 51.0, 64.0, "Make it\nyours", "Dark mode, 4 languages, loads of settings.", 1.6),
]
SHORTS_NARRATION = {
    "intro": "This free reminder app actually talks to you.",
    1: [(1.2, "Record a reminder in your own voice,"), (28.5, "pick a time,"), (39.0, "done.")],
    2: "When it's time, it speaks out loud, even on your lock screen.",
    3: "Snooze it, or mark it done, right from the notification.",
    4: [(6.0, "Add reminders from your home screen.")],
    5: "Missed one? It waits for you.",
    6: "Works on your Wear OS watch too.",
    7: "Dark mode, four languages, and loads of settings.",
    "outro": "Get SpeakAlert free on Google Play.",
}
SHORTS_TIMING = {
    "04_alert": {"freeze": [(4.0, 3.2), (9.0, 1.4)], "speech_src": 8.4},
    "10_missed": {"start": 5.6, "freeze": [(6.8, 1.2)]},
}


if __name__ == "__main__":
    if "shorts" in sys.argv:
        SEGMENTS, NARRATION, NARRATED_TIMING = SHORTS_SEGMENTS, SHORTS_NARRATION, SHORTS_TIMING
        OUT_OVERRIDE = "store/video/speakalert_short_vertical.mp4"
        build(sys.argv[1], "vertical", narrated=True)
        sys.exit(0)
    build(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "landscape",
          narrated=len(sys.argv) > 3 and sys.argv[3] == "narrated")
