"""Generate launcher assets from the supplied artwork (requires Pillow)."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "artwork" / "launcher-icon.png"
RES = ROOT / "app" / "src" / "main" / "res"
PREVIEW = ROOT / "build" / "icon-preview"
PREVIEW.mkdir(parents=True, exist_ok=True)
art = Image.open(SOURCE).convert("RGBA")
drawable = RES / "drawable-nodpi"
drawable.mkdir(parents=True, exist_ok=True)
art.resize((168, 168), Image.Resampling.LANCZOS).save(drawable / "gamepad_mascot.webp", lossless=True)

for density, scale in [("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4)]:
    folder = RES / f"mipmap-{density}"
    folder.mkdir(parents=True, exist_ok=True)
    size = round(48 * scale)
    icon = art.resize((size, size), Image.Resampling.LANCZOS)
    icon.save(folder / "ic_launcher.webp", lossless=True)
    mask = Image.new("L", (size, size))
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    round_icon = icon.copy()
    round_icon.putalpha(mask)
    round_icon.save(folder / "ic_launcher_round.webp", lossless=True)

    # Android masks the central 72dp of a 108dp adaptive layer. Fit the
    # complete supplied image into that area so the controller stays visible.
    layer_size = round(108 * scale)
    art_size = round(72 * scale)
    layer = Image.new("RGBA", (layer_size, layer_size))
    offset = (layer_size - art_size) // 2
    layer.alpha_composite(art.resize((art_size, art_size), Image.Resampling.LANCZOS), (offset, offset))
    layer.save(folder / "ic_launcher_foreground.webp", lossless=True)

preview = Image.new("RGB", (720, 272), "#e7ebef")
for index, shape in enumerate(["square", "rounded", "circle"]):
    icon = art.resize((216, 216), Image.Resampling.LANCZOS)
    mask = Image.new("L", (216, 216), 255 if shape == "square" else 0)
    draw = ImageDraw.Draw(mask)
    if shape == "rounded":
        draw.rounded_rectangle((0, 0, 215, 215), radius=48, fill=255)
    elif shape == "circle":
        draw.ellipse((0, 0, 215, 215), fill=255)
    preview.paste(icon, (12 + index * 240, 12), mask)
    ImageDraw.Draw(preview).text((12 + index * 240, 242), shape, fill="#303b45")
preview.save(PREVIEW / "launcher-shapes.png")
print("Generated all five launcher densities and adaptive foreground layers.")
