$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '../src/main/resources/assets/minecrafteft'
$dataRoot = Join-Path $PSScriptRoot '../src/main/resources/data'
New-Item -ItemType Directory -Force -Path (Join-Path $assetRoot 'textures/block') | Out-Null
$bitmap = New-Object Drawing.Bitmap 256,256
$graphics = [Drawing.Graphics]::FromImage($bitmap)
$random = New-Object Random 1701
function Fill([int]$x,[int]$y,[int]$w,[int]$h,[Drawing.Color]$color) {
    $brush = New-Object Drawing.SolidBrush $color
    $graphics.FillRectangle($brush,$x,$y,$w,$h)
    $brush.Dispose()
}
function Shade([Drawing.Color]$color,[int]$amount) {
    return [Drawing.Color]::FromArgb([Math]::Clamp([int]$color.R+$amount,0,255),
        [Math]::Clamp([int]$color.G+$amount,0,255),[Math]::Clamp([int]$color.B+$amount,0,255))
}
$colors = @([Drawing.Color]::FromArgb(83,106,57),[Drawing.Color]::FromArgb(188,164,109),
    [Drawing.Color]::FromArgb(113,125,132),[Drawing.Color]::FromArgb(43,47,49))
for($material=0;$material -lt 4;$material++) {
    $base=$colors[$material]
    $y=$material*64
    for($tile=0;$tile -lt 4;$tile++) {
        $x=$tile*64
        Fill $x $y 64 64 $base
        for($i=3;$i -lt 62;$i+=6) {
            Fill ($x+$i) ($y+3) 2 58 (Shade $base -19)
            Fill ($x+$i+2) ($y+3) 1 58 (Shade $base 17)
        }
        for($i=0;$i -lt 100;$i++) {
            $rx=$x+$random.Next(2,62); $ry=$y+$random.Next(2,62)
            Fill $rx $ry ($random.Next(1,3)) 1 (Shade $base ($random.Next(-15,9)))
        }
        Fill $x $y 64 3 (Shade $base 16)
        Fill $x ($y+61) 64 3 (Shade $base -30)
        Fill $x $y 3 64 (Shade $base -23)
        Fill ($x+61) $y 3 64 (Shade $base -23)
        foreach($rivetX in @(1,61)) { foreach($rivetY in @(1,20,42,61)) {
            Fill ($x+$rivetX) ($y+$rivetY) 2 2 ([Drawing.Color]::FromArgb(159,161,147))
        }}
        if($tile -eq 2) {
            # Two closed leaves, central seam, steel locking bars and handles.
            Fill ($x+31) ($y+3) 2 58 (Shade $base -40)
            foreach($rod in @(12,24,39,51)) {
                Fill ($x+$rod) ($y+4) 2 56 ([Drawing.Color]::FromArgb(171,174,160))
                Fill ($x+$rod+1) ($y+4) 1 56 ([Drawing.Color]::FromArgb(83,87,81))
                Fill ($x+$rod-2) ($y+37) 6 2 ([Drawing.Color]::FromArgb(189,191,178))
            }
            foreach($hingeY in @(11,30,50)) { foreach($hingeX in @(3,57)) {
                Fill ($x+$hingeX) ($y+$hingeY) 4 3 (Shade $base 30)
            }}
            Fill ($x+5) ($y+7) 5 2 ([Drawing.Color]::FromArgb(222,216,182))
            Fill ($x+5) ($y+11) 4 1 ([Drawing.Color]::FromArgb(222,216,182))
        }
        if($tile -eq 3) {
            # An open-door illusion in the same solid silhouette: dark recess and folded leaves.
            Fill ($x+10) ($y+3) 44 58 ([Drawing.Color]::FromArgb(15,19,20))
            Fill ($x+13) ($y+7) 38 49 ([Drawing.Color]::FromArgb(25,30,30))
            Fill ($x+10) ($y+54) 44 7 ([Drawing.Color]::FromArgb(55,57,49))
            foreach($panel in @(3,55)) {
                Fill ($x+$panel) ($y+4) 6 55 (Shade $base 15)
                Fill ($x+$panel+2) ($y+5) 2 53 (Shade $base -25)
                Fill ($x+$panel+1) ($y+36) 4 2 ([Drawing.Color]::FromArgb(183,185,170))
            }
        }
    }
}
$bitmap.Save((Join-Path $assetRoot 'textures/block/container_atlas.png'),[Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose(); $bitmap.Dispose()
function WriteJson([string]$path,$value) {
    New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
    $value | ConvertTo-Json -Depth 20 -Compress | Set-Content -LiteralPath $path -Encoding utf8
}
function Box($from,$to,[int]$material,[string]$door='north') {
    $faces=@{}
    foreach($face in @('north','south','east','west','up','down')) {
        $tile=0
        if($face -eq 'up') {$tile=1}
        if($face -eq $door) {$tile=2}
        $faces[$face]=@{texture='#atlas';uv=@(($tile*4),($material*4),($tile*4+4),($material*4+4))}
    }
    return @{from=$from;to=$to;faces=$faces}
}
for($tier=1;$tier -le 4;$tier++) {
    WriteJson (Join-Path $assetRoot "blockstates/personal_stash_$tier.json") @{
        variants=@{''=@{model="minecrafteft:block/personal_stash_$tier"}}
    }
    WriteJson (Join-Path $assetRoot "models/block/personal_stash_$tier.json") @{
        textures=@{particle='minecrafteft:block/container_atlas'};elements=@()
    }
    $elements=@()
    $unit=1.45
    $containers=[Math]::Min($tier,3)
    $offset=(16-($containers*3*$unit))/2
    if($tier -eq 4){$offset=0.75}
    for($i=0;$i -lt $containers;$i++) {
        $x=$offset+$i*3*$unit
        $elements+=Box @($x,0,4) @(($x+3*$unit),(3*$unit),(4+5*$unit)) $i
    }
    if($tier -eq 4) {
        $elements+=Box @($offset,(3*$unit),(4+$unit)) @(($offset+5*$unit),(6*$unit),(4+4*$unit)) 3 'east'
        for($z=0;$z -lt 3;$z++) {
            $elements+=Box @(($offset+9*$unit),($z*$unit),(4+$z*$unit)) @(
                ($offset+10*$unit),(($z+1)*$unit),(4+($z+1)*$unit)) 2 'none'
        }
    }
    WriteJson (Join-Path $assetRoot "models/item/personal_stash_$tier.json") @{
        parent='minecraft:block/block';gui_light='side';textures=@{atlas='minecrafteft:block/container_atlas';particle='minecrafteft:block/container_atlas'}
        elements=$elements
        display=@{gui=@{rotation=@(25,225,0);translation=@(0,1,0);scale=@(0.85,0.85,0.85)}
            fixed=@{rotation=@(0,0,0);translation=@(0,0,0);scale=@(0.7,0.7,0.7)}
            ground=@{translation=@(0,3,0);scale=@(0.4,0.4,0.4)}}
    }
}
WriteJson (Join-Path $assetRoot 'blockstates/stash_container_part.json') @{variants=@{''=@{model='minecrafteft:block/stash_container_part'}}}
WriteJson (Join-Path $assetRoot 'models/block/stash_container_part.json') @{textures=@{particle='minecrafteft:block/container_atlas'};elements=@()}
WriteJson (Join-Path $dataRoot 'minecrafteft/loot_tables/blocks/stash_container_part.json') @{type='minecraft:block';pools=@()}
WriteJson (Join-Path $dataRoot 'minecraft/tags/blocks/mineable/pickaxe.json') @{
    replace=$false;values=@('minecrafteft:personal_stash_1','minecrafteft:personal_stash_2',
        'minecrafteft:personal_stash_3','minecrafteft:personal_stash_4','minecrafteft:stash_container_part',
        'minecrafteft:large_weapon_case','minecrafteft:weapon_case_part')
}
Write-Output 'Generated container atlas, four tier models and structure resources.'
