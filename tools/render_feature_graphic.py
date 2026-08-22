"""F-Droid/Play feature graphic (1024x500): the card motif next to the "Fechtkarte" wordmark set
in the app's own bundled UnifrakturMaguntia.

The wordmark is shaped with HarfBuzz (Pillow + libraqm) and requests the ss01 stylistic set
("Modern forms"), because the font's default fraktur k is hard to read for anyone not used to
blackletter -- and a wordmark is exactly where legibility matters. Proper shaping also brings in
the font's own liga feature, which turns the "ch" of Fechtkarte into a single ch ligature, the
typographically correct fraktur form. Card numerals deliberately keep the default forms; only
the wordmark gets ss01.
"""
import pathlib

from PIL import Image, ImageDraw, ImageFont

# Needs Pillow built with libraqm for HarfBuzz shaping, which the ss01/liga features below
# depend on. Windows Pillow wheels ship without it, so run this in a container that has it:
#
#   podman run --rm -v "$(pwd)":/w debian:bookworm-slim sh -c #     'apt-get update -qq && apt-get install -y -qq python3-pil && python3 /w/tools/render_feature_graphic.py'
REPO = pathlib.Path(__file__).resolve().parent.parent

W, H = 1024, 500
PAPER, WOAD_LIGHT, WOAD_DARK, INK = "#FAF7F0", "#D3E9F9", "#58ABE7", "#1A1A1A"
FONT_PATH = str(REPO / "app/src/main/res/font/unifraktur_maguntia.ttf")
TEXT = "Fechtkarte"

img = Image.new("RGB", (W, H), PAPER)
draw = ImageDraw.Draw(img)

icon = 340
left, top = 90, (H - icon) // 2
right, bottom = left + icon, top + icon
cx, cy, half = (left + right) // 2, (top + bottom) // 2, icon // 2

draw.rectangle([left, top, left + half, top + half], fill=WOAD_LIGHT)
draw.rectangle([left + half, top, right, top + half], fill=WOAD_DARK)
draw.rectangle([left, top + half, left + half, bottom], fill=WOAD_DARK)
draw.rectangle([left + half, top + half, right, bottom], fill=WOAD_LIGHT)
draw.rectangle([left, top, right, bottom], outline=INK, width=5)
for px, py in [(cx, top), (cx, bottom), (right, cy), (left, cy),
               (right, top), (right, bottom), (left, bottom), (left, top)]:
    draw.line([(cx, cy), (px, py)], fill=INK, width=4)

text_x = right + 60
available = W - text_x - 50
size = 150
while size > 10:
    font = ImageFont.truetype(FONT_PATH, size, layout_engine=ImageFont.Layout.RAQM)
    bbox = draw.textbbox((0, 0), TEXT, font=font, features=["ss01"])
    if bbox[2] - bbox[0] <= available:
        break
    size -= 2
draw.text((text_x, cy - (bbox[3] - bbox[1]) // 2 - bbox[1]), TEXT, fill=INK, font=font, features=["ss01"])

img.save(REPO / "fastlane/metadata/android/en-US/images/featureGraphic.png")
print("wrote featureGraphic.png at font size", size)
