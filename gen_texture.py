import os

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

# v3: Enlarged silhouette to better fill the 16x16 slot.
# Footprint grew from ~10x10 (v2) to ~12x12 with near-1px margins.
# Growth is asymmetric — preserving the stone's gesture, not just its footprint:
# - Crown pushes up to row 2, base tapers to row 13 (was row 12)
# - Mid-body (rows 5–9) reaches col 13 on the right for a pronounced right extension
# - Left edge bulges to col 2 at rows 6–8, with an indent at row 5 (col 3) preserving v2's irregularity
# - Single knot focal point at row 7 col 7 (V4 core glow, 1 pixel)
# - Branch diverges lower-left from knot (V2→V1→V1, tapering early to let the knot win as focal)
# - Main vein exit extends 2 pixels past knot (V2 at row 8 col 8, V1 at row 9 col 9) for diagonal-crack flow
# - Two noise pixels preserved: row 6 col 9 (S2 where S3 expected, lighter), row 8 col 5 (S3 where S2 expected, darker)
# - S5 stays sparse at bottom-right edges and tip
pixels = [
#    0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 0
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 1
    [T,  T,  T,  T,  T,  T,  S1, S1, S2, S3, T,  T,  T,  T,  T,  T ],  # 2  <- crown
    [T,  T,  T,  T,  S1, S1, S2, S2, S2, S3, S3, S4, T,  T,  T,  T ],  # 3
    [T,  T,  T,  S1, S1, S2, S2, S2, V1, S3, S3, S4, S4, T,  T,  T ],  # 4  <- vein entry
    [T,  T,  T,  S1, S2, S2, S3, V2, S3, S3, S3, S4, S4, S5, T,  T ],  # 5  <- left indent (col 3), right reaches col 13
    [T,  T,  S1, S2, S2, S3, V2, V3, S3, S2, S4, S4, S5, S5, T,  T ],  # 6  <- left bulge, noise: S2 at col 9
    [T,  T,  S1, S2, S3, V1, V3, V4, V3, S3, S4, S4, S5, S5, T,  T ],  # 7  <- core glow at knot
    [T,  T,  S2, S3, V2, S3, S3, S3, V2, S4, S4, S5, S5, S5, T,  T ],  # 8  <- branch V2 (col 4), main vein exit (col 8), noise: S3 at col 5
    [T,  T,  T,  S3, V1, S4, S4, S4, S4, V1, S5, S5, S5, S5, T,  T ],  # 9  <- branch V1 taper (col 4), main vein V1 exit (col 9), right extended
    [T,  T,  T,  T,  S3, V1, S4, S4, S4, S5, S5, S5, T,  T,  T,  T ],  # 10 <- branch terminal (col 5)
    [T,  T,  T,  T,  S4, S4, S4, S5, S5, S5, S5, T,  T,  T,  T,  T ],  # 11 <- widened base
    [T,  T,  T,  T,  T,  S5, S5, S5, S5, S5, T,  T,  T,  T,  T,  T ],  # 12
    [T,  T,  T,  T,  T,  T,  S5, S5, T,  T,  T,  T,  T,  T,  T,  T ],  # 13 <- tip
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 14
    [T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T,  T ],  # 15
]

for y, row in enumerate(pixels):
    for x, color in enumerate(row):
        img.putpixel((x, y), color)

out = "src/main/resources/assets/stone_of_mending/textures/item/stone_of_mending.png"
os.makedirs(os.path.dirname(out), exist_ok=True)
img.save(out)
print(f"Saved {out}")

# Also save a 16x scaled version for preview
preview = img.resize((256, 256), Image.Resampling.NEAREST)
preview.save("stone_of_mending_preview.png")
print("Saved stone_of_mending_preview.png (16x scale)")
