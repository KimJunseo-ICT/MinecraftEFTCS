"""Bake the IFAK's four-by-four material atlas (Pillow)."""
from pathlib import Path
from random import Random

from PIL import Image, ImageDraw

rng = Random(407)
size = 64
atlas = Image.new("RGB", (size * 4, size * 4))
colors = [(88, 86, 68), (79, 79, 62), (61, 60, 51), (48, 48, 39),
          (87, 82, 63), (65, 66, 62), (108, 100, 78), (184, 182, 165),
          (116, 115, 98), (213, 209, 184), (134, 131, 109), (145, 143, 120),
          (173, 179, 163), (61, 60, 51), (87, 82, 63), (65, 66, 62)]
palette = [(82, 81, 63), (100, 96, 74), (62, 63, 52), (113, 106, 83), (76, 72, 58)]


def cloth(tile):
    image = Image.new("RGB", (size, size), colors[tile])
    draw = ImageDraw.Draw(image)
    if tile in (0, 1):
        for _ in range(105):
            x, y = rng.randrange(-8, size), rng.randrange(-6, size)
            w, h = rng.randrange(3, 12), rng.randrange(2, 7)
            color = rng.choice(palette)
            draw.rectangle((x, y, x+w, y+h), fill=color)
            draw.rectangle((x+w//2, y-2, x+w+3, y+h-2), fill=color)
    pixels = image.load()
    for y in range(size):
        for x in range(size):
            weave = ((x//2+y//2) % 2)*4 - 2
            grain = rng.randrange(-6, 7) + weave
            if tile == 4:
                grain += 8 if (y//2) % 3 == 0 else -4
            if tile == 9:
                grain += -10 if x % 4 == 0 or y % 3 == 0 else 2
            pixels[x, y] = tuple(max(0, min(255, c+grain)) for c in pixels[x,y])
    if tile in (0, 5, 6):
        color = (121, 116, 92) if tile == 0 else (144, 137, 112)
        for n in range(5, size-5, 5):
            for edge in (3, size-4):
                draw.line((n,edge,n+2,edge), fill=color)
                draw.line((edge,n,edge,n+2), fill=color)
    if tile == 6:
        # Short woven tape: compensate for the wide, shallow face UV.
        letters = ["111/010/010/010/111", "111/100/110/100/100",
                   "010/101/111/101/101", "101/110/100/110/101"]
        for col, letter in enumerate(letters):
            for row, line in enumerate(letter.split("/")):
                for j, value in enumerate(line):
                    if value == "1":
                        x, y = 9 + 12*col + 3*j, 14 + 7*row
                        draw.rectangle((x,y,x+2,y+6), fill=(48,48,41))
    if tile == 11:
        draw.rectangle((7,15,57,20),fill=(91,95,80))
        for n in (29,34,39):
            draw.line((11,n,46,n),fill=(95,95,82))
    if tile == 12:
        for n in range(7,59,7):
            draw.line((n,20,n,30+(n%2)*8),fill=(81,87,77),width=2)
    return image


for index in range(len(colors)):
    atlas.paste(cloth(index), (index%4*size, index//4*size))
out = Path(__file__).resolve().parents[1] / "src/main/resources/assets/minecrafteft/textures/item/ifak_atlas.png"
atlas.save(out, optimize=True)
