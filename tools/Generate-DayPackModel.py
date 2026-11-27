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

# Matte, low-contrast black/olive camouflage with woven webbing and binding tape.
rng = random.Random(8005)
cloth = Image.new("RGB", (32,32), (47,50,43))
draw = ImageDraw.Draw(cloth)
palette = [(32,35,32),(57,60,47),(63,64,52),(39,42,39),(47,49,42)]
for _ in range(70):
    x, y = rng.randrange(32), rng.randrange(32)
    w, h = rng.randrange(2,10), rng.randrange(1,5)
    points = [(x,y),(x+w//2,y-1),(x+w,y),(x+w-2,y+h),(x+1,y+h+1),(x-2,y+1)]
    shade = rng.choice(palette)
    for dx in (-32,0,32):
        for dy in (-32,0,32):
            draw.polygon([(px+dx,py+dy) for px,py in points],fill=shade)
for y in range(32):
    for x in range(32):
        grain = rng.randrange(-3,4) + ((x+y)%2)*2
        cloth.putpixel((x,y),tuple(v+grain for v in cloth.getpixel((x,y))))
cloth.save(assets/"textures/item/day_pack_cloth.png")
trim = Image.new("RGB",(16,16))
for y in range(16):
    for x in range(16):
        v = [37,43,45,39][y%4]+(x%2)*2
        trim.putpixel((x,y),(v,v+1,v-4))
trim.save(assets/"textures/item/day_pack_webbing.png")
fabric="cloth"
def part(x,y,z,w,h,d,mat="cloth"):
    box((x,y,z),(round(x+w,3),round(y+h,3),round(z+d,3)),mat)

# Rounded silhouette built from stepped panels, with two shoulder pads at the back.
part(3.65,.65,5.35,8.7,1.05,5.25)
part(3.0,1.6,5.0,10,9.9,6.0)
part(3.35,11.5,5.15,9.3,1.65,5.65)
part(4.05,13.15,5.45,7.9,.85,5.05)
for x in (4.25,10.25):
    part(x,4.4,4.0,1.45,7.8,1.1,"web")
    part(x+.15,2.4,4.55,1.05,2.1,.55,"web")
for x in (6.15,9.2):
    part(x,13.85,6.7,.65,1.4,.72,"web")
part(6.15,15.0,6.7,3.7,.6,.72,"web")
# Front pocket gussets and top/bottom zip openings.
part(3.65,7.5,10.88,8.7,4.15,1.22)
part(4.05,11.6,10.85,7.9,.43,1.07,"web")
part(3.67,2.38,10.88,8.66,4.74,1.78)
part(4.05,1.92,10.9,7.9,.51,1.45)
for x,y,z,w in [(3.78,10.98,12.1,8.44),(3.84,6.6,12.66,8.32)]:
    part(x,y,z,w,.23,.15,"zip")
    part(x+w-.75,y-.75,z+.1,.26,.98,.19,"pull")
# Identifiable three rows of segmented PALS webbing on the lower pocket.
for y in (2.9,4.06,5.22):
    for x in (4.05,6.1,8.15,10.2):
        part(x,y,12.66,1.78,.40,.15,"web")
for x in (2.45,12.85):
    part(x,2.0,6.05,.7,4.5,3.55)
    for y in (4.8,9.4):
        part(x+.04,y,5.65,.63,.48,4.9,"web")
    part(x,4.58,8.8,.71,.9,.8,"zip")
textures={"particle":"minecrafteft:item/day_pack_cloth","cloth":"minecrafteft:item/day_pack_cloth",
          "web":"minecrafteft:item/day_pack_webbing","zip":"minecraft:block/gray_concrete",
          "pull":"minecraft:block/polished_deepslate"}
item={"textures":textures,"elements":elements,"display":{
    "gui":display([12,-24,-4],[0,-.15,0],1.01),
    "ground":display([0,15,0],[0,1,0],.54),
    "fixed":display([0,180,0],[0,0,0],.84),
    "firstperson_righthand":display([0,-28,3],[0,0,0],.68),
    "firstperson_lefthand":display([0,-28,3],[0,0,0],.68),
    "thirdperson_righthand":display([82,5,0],[0,2,2.5],.62),
    "thirdperson_lefthand":display([82,5,0],[0,2,2.5],.62)}}
(assets/"models/item/lbt_8005a_day_pack.json").write_text(json.dumps(item,indent=2)+"\n")
