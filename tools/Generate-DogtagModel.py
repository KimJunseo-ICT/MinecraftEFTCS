import json, math, random
from pathlib import Path
from PIL import Image, ImageDraw
assets = Path(__file__).resolve().parents[1] / "src/main/resources/assets/minecrafteft"
elements = []

def box(a, b, material, uv=None, face_materials=None):
    faces = {}
    for face in ("north", "south", "west", "east", "up", "down"):
        texture = (face_materials or {}).get(face, material)
        faces[face] = {"texture": "#" + texture}
        if uv is not None:
            faces[face]["uv"] = uv
    elements.append({"from": list(a), "to": list(b), "faces": faces})

def display(rotation, translation, scale):
    return {"rotation": rotation, "translation": translation, "scale": [scale]*3}

# The border is a separate raised rolled edge on both sides of a brushed silver plate.
metal = Image.new("RGB",(32,32))
rng = random.Random(2401)
for y in range(32):
    for x in range(32):
        v = 165 + (y%5)*2 + (x//5) + rng.randrange(-6,7)
        metal.putpixel((x,y),(v,v+1,v+2))
draw=ImageDraw.Draw(metal)
# Debossed identification lines, laid down as individual pixel glyphs.
for row, word in enumerate(("EFT","P M C","0 1 7")):
    px,py=3,5+row*8
    draw.text((px,py+1),word,fill=(219,220,216),font_size=8,stroke_width=0)
    draw.text((px,py),word,fill=(91,96,94),font_size=8,stroke_width=0)
metal.save(assets/"textures/item/dogtag_plate.png")
for x,y,w,h in [(6.12,.35,3.76,.38),(5.52,.72,4.96,.54),(5.13,1.25,5.74,5.03),
                  (5.55,6.27,4.9,.57),(6.12,6.82,3.76,.35)]:
    box((x,y,7.46),(round(x+w,3),round(y+h,3),8.35),"rim")
box((5.52,1.31,7.37),(10.47,6.2,8.44),"rim",[0,0,16,16],{"north":"plate","south":"plate"})
box((6,6.17,7.36),(10,6.54,8.42),"steel")
box((7.62,6.46,8.42),(8.4,6.94,8.47),"shadow")
# Paired strands form a hanging loop. Small highlights distinguish the beads from the connecting cord.
for side in (-1,1):
    for i in range(18):
        t=i/17
        x=8+side*math.sin(math.pi*t)*1.55
        y=6.9+8.73*t
        z=7.99+side*.14
        if i:
            box((round(min(x,last_x)-.075,2),round(last_y,2),z-.07),
                (round(max(x,last_x)+.075,2),round(y,2),z+.07),"shadow")
        if side==1 or 0<i<17:
            box((round(x-.19,2),round(y-.185,2),z-.18),(round(x+.19,2),round(y+.185,2),z+.18),"steel")
        last_x,last_y=x,y
textures={"particle":"minecrafteft:item/dogtag_plate","plate":"minecrafteft:item/dogtag_plate",
          "rim":"minecraft:block/smooth_stone","steel":"minecraft:block/iron_block","shadow":"minecraft:block/gray_concrete"}
item={"textures":textures,"elements":elements,"display":{
    "gui":display([7,-22,-15],[0,0,0],1.02),
    "ground":display([0,15,-18],[0,2,0],.65),
    "fixed":display([0,180,0],[0,0,0],.9),
    "thirdperson_righthand":display([90,0,0],[0,.3,4.5],.66),
    "thirdperson_lefthand":display([90,0,0],[0,.3,4.5],.66),
    "firstperson_righthand":display([0,-14,2],[0,-1.8,0],.78),
    "firstperson_lefthand":display([0,-14,2],[0,-1.8,0],.78)}}
(assets/"models/item/basic_dogtag.json").write_text(json.dumps(item,indent=2)+"\n")
