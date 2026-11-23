"""Translate the heading on each Play phone screenshot; everything below the heading stays as is.

    python -X utf8 tools/store/localize_screenshots.py render [--only es-ES,de-DE]
    python -X utf8 tools/store/localize_screenshots.py upload [--only ...] [--commit]

Sources: store/screenshots-play/en-US/NN.png (the live English screenshots).
Headings: store/screenshot-captions.json. A list of strings per language is one heading per
screenshot; a list of [heading, sub line] pairs adds a sub line under the heading.
Output: build/store-screenshots/<Play language>/NN.png (not committed; regenerate any time).

render finds the heading band (above the first unbroken device edge), repaints it without text,
draws the translated heading with Microsoft Edge (proper shaping for every script, Windows fonts),
shrinks it if needed so it never reaches the device, and pastes the band back on the original.
upload replaces each language's phone screenshots on Play (a dry run unless --commit; English is
never touched).
"""
import argparse
import html
import json
import os
import pathlib
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "store" / "screenshots-play" / "en-US"
OUT = ROOT / "build" / "store-screenshots"
CAPTIONS = ROOT / "store" / "screenshot-captions.json"
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
PACKAGE = "com.ghostgramlabs.speakalert"
STYLE = "gradient"  # "gradient" (heading only) or "flat" (heading + sub line on a flat colour)

FONTS = {
    "ja-JP": '"Yu Gothic UI", "Yu Gothic", "Meiryo UI"',
    "ko-KR": '"Malgun Gothic"',
    "zh-CN": '"Microsoft YaHei UI", "Microsoft YaHei"',
    "zh-TW": '"Microsoft JhengHei UI", "Microsoft JhengHei"',
}
FALLBACK = '"Segoe UI", "Nirmala UI", "Leelawadee UI", "Yu Gothic UI", "Malgun Gothic", "Microsoft YaHei UI", sans-serif'


def band_geometry(img):
    """Rows 0..band_bottom are background plus heading; the device starts below it."""
    a = np.asarray(img.convert("RGB"), dtype=np.int16)
    h, w, _ = a.shape
    rows = int(h * 0.45)
    left = a[:rows, 4:24].mean(axis=1)
    right = a[:rows, w - 24:w - 4].mean(axis=1)
    t = np.linspace(0, 1, w)[None, :, None]
    expected = left[:, None, :] + (right - left)[:, None, :] * t
    off = (np.abs(a[:rows] - expected).max(axis=2) > 25)
    device_top = rows
    for y in range(rows):
        r = off[y]
        if r.sum() < w * 0.4:
            continue
        # longest unbroken run in this row
        edges = np.diff(np.concatenate(([0], r.astype(np.int8), [0])))
        starts, ends = np.where(edges == 1)[0], np.where(edges == -1)[0]
        if (ends - starts).max() > w * 0.4:
            device_top = y
            break
    text_rows = np.where(off[:device_top].mean(axis=1) > 0.002)[0]
    return device_top, int(text_rows.min()), int(text_rows.max())


def clean_band(img, bottom):
    """The heading band with the text removed: each row is the line between its two edge colours."""
    a = np.asarray(img.convert("RGB"), dtype=np.float32)
    w = a.shape[1]
    left = a[:bottom, 4:24].mean(axis=1)
    right = a[:bottom, w - 24:w - 4].mean(axis=1)
    t = np.linspace(0, 1, w)[None, :, None]
    band = left[:, None, :] + (right - left)[:, None, :] * t
    return Image.fromarray(np.clip(band + 0.5, 0, 255).astype(np.uint8), "RGB")


def render_text(width, height, lang, heading, sub, style, box):
    """Transparent PNG of the heading (and sub line) rendered by Edge."""
    fonts = FONTS.get(lang, "") + ("," if lang in FONTS else "") + FALLBACK
    rtl = lang.startswith("ar")
    if style == "gradient":
        block = f'<div id="h" style="font-size:100px;line-height:1.22;font-weight:400;color:#16121f">{html.escape(heading)}</div>'
        top, maxh, pad, anchor = box
    else:
        block = (f'<div id="h" style="font-size:76px;line-height:92px;font-weight:700;color:#fff">{html.escape(heading)}</div>'
                 f'<div id="s" style="font-size:40px;line-height:54px;margin-top:10px;color:rgb(203,213,225);padding:0 10px">'
                 f'{html.escape(sub)}</div>')
        top, maxh, pad, anchor = box
    page = f"""<!doctype html><html lang="{lang}" dir="{'rtl' if rtl else 'ltr'}"><head><meta charset="utf-8">
<style>html,body{{margin:0;background:transparent;overflow:hidden}}
#b{{position:absolute;left:{pad}px;right:{pad}px;top:{top}px;text-align:center;font-family:{fonts};
-webkit-font-smoothing:antialiased;word-wrap:break-word}}</style></head>
<body><div id="b">{block}</div><script>
const b=document.getElementById('b'), els=[...b.children], base=els.map(e=>[parseFloat(e.style.fontSize),parseFloat(e.style.lineHeight)||0]);
let k=1; const fit=()=>{{els.forEach((e,i)=>{{e.style.fontSize=(base[i][0]*k)+'px'; if(base[i][1]>3) e.style.lineHeight=(base[i][1]*k)+'px';}});}};
fit(); while(b.offsetHeight>{maxh} && k>0.55){{k-=0.03; fit();}}
if('{anchor}'==='center'){{b.style.top=({top}+({maxh}-b.offsetHeight)/2)+'px';}}
</script></body></html>"""
    with tempfile.TemporaryDirectory() as d:
        page_path = pathlib.Path(d, "p.html")
        page_path.write_text(page, encoding="utf-8")
        shot = pathlib.Path(d, "t.png")
        for attempt in range(3):  # Edge occasionally hangs on start-up; a fresh run always works
            try:
                subprocess.run([EDGE, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                                "--force-device-scale-factor=1", "--default-background-color=00000000",
                                f"--window-size={width},{height}", f"--screenshot={shot}", page_path.as_uri()],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=45)
                if shot.exists():
                    break
            except subprocess.TimeoutExpired:
                continue
        else:
            sys.exit(f"Edge could not render the heading for {lang}")
        layer = Image.open(shot).convert("RGBA")
    if layer.size != (width, height):
        layer = layer.crop((0, 0, width, height))
    return layer


def render(only):
    caps = json.loads(CAPTIONS.read_text(encoding="utf-8"))
    sources = sorted(SRC.glob("*.png"))
    geo = {}
    for src in sources:
        img = Image.open(src)
        geo[src.name] = band_geometry(img)
    for lang, items in caps.items():
        if lang.startswith("_") or lang == "en-US" or (only and lang not in only):
            continue
        if len(items) != len(sources):
            sys.exit(f"{lang}: {len(items)} captions for {len(sources)} screenshots")
        out_dir = OUT / lang
        out_dir.mkdir(parents=True, exist_ok=True)
        for src, item in zip(sources, items):
            img = Image.open(src).convert("RGB")
            w, h = img.size
            device_top, text_top, text_bot = geo[src.name]
            bottom = device_top - 26  # keep the device's shadow untouched
            heading, sub = (item, "") if isinstance(item, str) else item
            if STYLE == "gradient":
                centre = (text_top + text_bot) // 2 if text_bot - text_top > 150 else text_top + 110
                maxh = min(2 * (centre - 40), 2 * (bottom - 20 - centre))
                box = (centre - maxh // 2, maxh, 80, "center")
            else:
                box = (110, bottom - 20 - 110, 70, "top")
            band = clean_band(img, bottom).convert("RGBA")
            band.alpha_composite(render_text(w, bottom, lang, heading, sub, STYLE, box))
            img.paste(band.convert("RGB"), (0, 0))
            img.save(out_dir / src.name, optimize=True)
        print(f"{lang}: {len(items)} screenshots")


def upload(only, commit):
    import google.auth
    from googleapiclient.discovery import build
    from googleapiclient.http import MediaFileUpload
    creds, _ = google.auth.default(scopes=["https://www.googleapis.com/auth/androidpublisher"])
    edits = build("androidpublisher", "v3", credentials=creds, cache_discovery=False).edits()
    edit_id = edits.insert(packageName=PACKAGE, body={}).execute()["id"]
    saved = False
    try:
        for lang_dir in sorted(p for p in OUT.iterdir() if p.is_dir()):
            lang = lang_dir.name
            if lang == "en-US" or (only and lang not in only):
                continue
            files = sorted(lang_dir.glob("*.png"))
            edits.images().deleteall(packageName=PACKAGE, editId=edit_id, language=lang,
                                     imageType="phoneScreenshots").execute()
            for f in files:
                edits.images().upload(packageName=PACKAGE, editId=edit_id, language=lang, imageType="phoneScreenshots",
                                      media_body=MediaFileUpload(str(f), mimetype="image/png")).execute()
            print(f"{lang}: {len(files)} uploaded to draft")
        edits.validate(packageName=PACKAGE, editId=edit_id).execute()
        print("Google validated the draft.")
        if commit:
            edits.commit(packageName=PACKAGE, editId=edit_id).execute()
            saved = True
            print("COMMITTED: screenshots are saved and go through Play review.")
    finally:
        if not saved:
            edits.delete(packageName=PACKAGE, editId=edit_id).execute()
            print("Dry run: draft deleted, nothing changed on Play. Add --commit to save.")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("command", choices=["render", "upload"])
    ap.add_argument("--only", help="comma-separated Play language codes")
    ap.add_argument("--commit", action="store_true")
    a = ap.parse_args()
    only = set(a.only.split(",")) if a.only else None
    render(only) if a.command == "render" else upload(only, a.commit)


if __name__ == "__main__":
    main()
