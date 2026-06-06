"""
Run this script once to generate the block texture PNG.
Requires: pip install Pillow
"""
import os

try:
    from PIL import Image, ImageDraw

    size = 16
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)

    # Background: dark blue
    draw.rectangle([0, 0, 15, 15], fill=(20, 20, 80, 255))
    # Border: gold
    draw.rectangle([0, 0, 15, 15], outline=(220, 180, 30, 255))
    # Inner cross to suggest "goal" symbol
    draw.line([(7, 2), (7, 13)], fill=(220, 180, 30, 255), width=2)
    draw.line([(2, 7), (13, 7)], fill=(220, 180, 30, 255), width=2)

    out = os.path.join(
        os.path.dirname(__file__),
        "src", "main", "resources", "assets", "modbloc", "textures", "block"
    )
    os.makedirs(out, exist_ok=True)
    img.save(os.path.join(out, "community_goal_block.png"))
    print("Texture generated successfully.")
except ImportError:
    print("Pillow not found. Install with: pip install Pillow")
