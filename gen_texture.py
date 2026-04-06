from PIL import Image

img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

# Palette - 4 stone shades (cool dark gray, slight blue undertone)
T  = (0, 0, 0, 0)           # transparent
S1 = (105, 106, 116, 255)   # highlight (top-left edges)
S2 = (82, 83, 92, 255)      # light
S3 = (60, 61, 70, 255)      # medium
S4 = (42, 43, 51, 255)      # dark shadow (bottom-right)
S5 = (32, 33, 40, 255)      # deepest edge (sparse, anchoring outline)

# Veins - teal mending seam
V1 = (38, 140, 132, 255)    # dim edge
V2 = (60, 195, 184, 255)    # main vein
V3 = (95, 230, 216, 255)    # bright
V4 = (145, 250, 236, 255)   # core glow (1-2 pixels only)

# v2: Fixes from review:
# - Left side indented at row 5 (col 4 instead of 3) for irregularity
# - Right side extended at row 9 (col 12) for asymmetry / heavier bottom-right
# - Row 11 widened, row 12 softened — rounder base, less pointy
# - Branch vein bumped V1 → V2 for readability at 1x
# - Two texture noise pixels (S3 where S2 expected, S2 where S3 expected) in mid-body
pixels = [
#    0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 0
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 1
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 2
    [T,  T,  T,  T,  T,  T,  S1, S1, S2, S2, S3, T,  T,  T,  T,  T ],  # 3
    [T,  T,  T,  T,  S1, S1, S2, S2, S2, V1, S3, S4, T,  T,  T,  T ],  # 4
    [T,  T,  T,  T,  S1, S2, S2, S3, V2, S3, S3, S4, S5, T,  T,  T ],  # 5  <- left indented (col 4)
    [T,  T,  T,  S1, S2, S2, S3, V2, S3, S2, S4, S4, S5, T,  T,  T ],  # 6  <- noise: S2 at col 9
    [T,  T,  T,  S1, S2, S3, V1, V4, V3, S3, S4, S4, S5, T,  T,  T ],  # 7  <- core glow at knot
    [T,  T,  T,  S2, S3, V2, S3, S4, S4, V2, S4, S5, S5, T,  T,  T ],  # 8  <- branch V2 (brighter)
    [T,  T,  T,  T,  S2, S3, V1, S4, S3, S4, S5, S5, S5, T,  T,  T ],  # 9  <- right extended (col 12), noise: S3 at col 8
    [T,  T,  T,  T,  S3, V2, S3, S4, S4, S5, S5, T,  T,  T,  T,  T ],  # 10
    [T,  T,  T,  T,  T,  S4, S4, S5, S5, S5, S5, T,  T,  T,  T,  T ],  # 11 <- widened (col 10)
    [T,  T,  T,  T,  T,  T,  S5, S5, S5, T,  T,  T,  T,  T,  T,  T ],  # 12
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 13
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 14
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 15
]

for y, row in enumerate(pixels):
    for x, color in enumerate(row):
        img.putpixel((x, y), color)

out = "src/main/resources/assets/stone_of_mending/textures/item/stone_of_mending.png"
import os
os.makedirs(os.path.dirname(out), exist_ok=True)
img.save(out)
print(f"Saved {out}")

# Also save a 16x scaled version for preview
preview = img.resize((256, 256), Image.NEAREST)
preview.save("stone_of_mending_preview.png")
print("Saved stone_of_mending_preview.png (16x scale)")
