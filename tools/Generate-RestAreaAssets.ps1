$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$dest=Join-Path $PSScriptRoot '../src/main/resources/assets/minecrafteft/textures/block/rest_area_atlas.png'
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($dest))|Out-Null
$bitmap=[Drawing.Bitmap]::new(256,256)
$graphics=[Drawing.Graphics]::FromImage($bitmap)
$colors=@(@(118,88,53),@(40,47,49),@(111,122,88),@(255,222,139))
function Fill([int]$x,[int]$y,[int]$w,[int]$h,[int[]]$rgb) {
    $brush=[Drawing.SolidBrush]::new([Drawing.Color]::FromArgb($rgb[0],$rgb[1],$rgb[2]))
    $graphics.FillRectangle($brush,$x,$y,$w,$h)
    $brush.Dispose()
}
for($material=0;$material -lt 4;$material++) {
    $base=$colors[$material]
    for($tile=0;$tile -lt 4;$tile++) {
        $x=$tile*64; $y=$material*64
        Fill $x $y 64 64 $base
        for($i=2;$i -lt 64;$i+=4) {
            $shade=if($material -eq 0){if($i%8 -eq 2){-16}else{8}}else{3}
            $color=@([Math]::Clamp($base[0]+$shade,0,255),[Math]::Clamp($base[1]+$shade,0,255),[Math]::Clamp($base[2]+$shade,0,255))
            if($material -eq 0){Fill ($x+$i) $y 1 64 $color}else{Fill $x ($y+$i) 64 1 $color}
        }
        if($material -eq 0) {
            foreach($nailX in @(4,57)){foreach($nailY in @(5,57)){Fill ($x+$nailX) ($y+$nailY) 2 2 @(46,45,40)}}
        }
    }
}
$bitmap.Save($dest,[Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose(); $bitmap.Dispose()
