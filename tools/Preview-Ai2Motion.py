"""Render an offline silhouette sheet using the production AI-2 motion sampler."""
from pathlib import Path
import math
import os
import subprocess
from tempfile import TemporaryDirectory
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
times = [0, .80, 1.25, 1.85, 4, 5.52, 5.76, 6.0, 6.17, 6.30]
jdk_home = Path(os.environ.get("JAVA_HOME", Path.home()/".jdks"/"temurin-17.0.21"))
def jdk(name):
    binary = jdk_home / "bin" / (name + (".exe" if os.name == "nt" else ""))
    return str(binary) if binary.exists() else name
caches = [root/".gradle-user-home", Path(os.environ.get("GRADLE_USER_HOME", Path.home()/".gradle"))]
joml = next((jar for cache in caches for jar in cache.glob("caches/modules-2/files-2.1/org.joml/joml/*/*/joml-*.jar")
             if not jar.name.endswith(("-sources.jar", "-javadoc.jar"))), None)
if joml is None:
    raise SystemExit("Build the project once to resolve JOML.")
with TemporaryDirectory() as classes:
    subprocess.run([jdk("javac"), "-cp", str(joml), "-d", classes,
                    str(root/"src/main/java/com/minecrafteft/medical/MedikitType.java"),
                    str(root/"src/main/java/com/minecrafteft/medical/Ai2TreatmentProgress.java"),
                    str(root/"src/main/java/com/minecrafteft/client/Ai2Motion.java"),
                    str(root/"tools/Ai2PreviewPoses.java")], check=True)
    output = subprocess.check_output([jdk("java"), "-cp", os.pathsep.join((str(joml), classes)),
                                      "com.minecrafteft.client.Ai2PreviewPoses", *map(str, times)], text=True)

def add(a,b): return tuple(x+y for x,y in zip(a,b))
def mul(a,k): return tuple(x*k for x in a)
faces=[]
def cube(center,axes,half,color):
    vertices=[]
    for x,y,z in [(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]:
        v=center
        for axis,size,sign in zip(axes,half,(x,y,z)): v=add(v,mul(axis,size*sign))
        vertices.append(v)
    for idx in [(0,1,2,3),(4,7,6,5),(0,4,5,1),(1,5,6,2),(2,6,7,3),(3,7,4,0)]:
        faces.append(([vertices[i] for i in idx],color))
def clip(poly):
    out=[]
    for a,b in zip(poly,poly[1:]+poly[:1]):
        ia,ib=a[2]<-.05,b[2]<-.05
        if ia: out.append(a)
        if ia!=ib:
            t=(-.05-a[2])/(b[2]-a[2]);out.append(tuple(a[j]+(b[j]-a[j])*t for j in range(3)))
    return out

sheet=Image.new('RGB',(2400,600),(22,28,34))
for frame,(time,row) in enumerate(zip(times,output.splitlines())):
    faces.clear()
    data=np.fromstring(row,sep=" ")
    v=data[:-1].reshape(-1,3)
    for wrist,shoulder,axes in [(v[0],v[2],v[4:7]),(v[1],v[3],v[7:10])]:
        center=(wrist+shoulder)/2
        cube(center,axes,(.08125,math.dist(wrist,shoulder)/2,.08125),(110,137,137))
    cube(v[10],v[11:14],(.142,.165,.035),(207,157,29))
    cube(v[14],v[15:18],(.146,.169,.009),(227,176,38))
    if data[-1]:
        axes=v[19:22];center=v[18]
        cube(center,axes,(.058,.012,.012),(161,47,39))
        cube(center-.098*axes[0],axes,(.039,.0018,.0018),(222,225,228))
    pixels=np.zeros((270,480,3),dtype=np.uint8);pixels[:]=(70,78,82)
    depth=np.zeros((270,480),dtype=float)
    for poly,color in faces:
        poly=clip(poly)
        if len(poly)<3: continue
        projected=[(240+x/(-z)*193,135-y/(-z)*193,1/-z) for x,y,z in poly]
        for tri in range(1,len(projected)-1):
            a,b,c=projected[0],projected[tri],projected[tri+1]
            xmin=max(0,int(math.floor(min(a[0],b[0],c[0]))));xmax=min(479,int(math.ceil(max(a[0],b[0],c[0]))))
            ymin=max(0,int(math.floor(min(a[1],b[1],c[1]))));ymax=min(269,int(math.ceil(max(a[1],b[1],c[1]))))
            if xmin>xmax or ymin>ymax: continue
            denom=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
            if abs(denom)<1e-9: continue
            x,y=np.meshgrid(np.arange(xmin,xmax+1)+.5,np.arange(ymin,ymax+1)+.5)
            w0=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/denom
            w1=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/denom;w2=1-w0-w1
            invz=w0*a[2]+w1*b[2]+w2*c[2]
            view=depth[ymin:ymax+1,xmin:xmax+1]
            mask=(w0>=0)&(w1>=0)&(w2>=0)&(invz>view)
            view[mask]=invz[mask];pixels[ymin:ymax+1,xmin:xmax+1][mask]=color
    image=Image.fromarray(pixels);draw=ImageDraw.Draw(image)
    draw.text((7,7),f'{time:.2f}s',fill='white')
    sheet.paste(image,((frame%5)*480,(frame//5)*300))
out=root/'build/ai2-reference/pose-silhouettes.png';out.parent.mkdir(parents=True,exist_ok=True);sheet.save(out)
print(out)
