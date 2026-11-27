$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assets = Join-Path $PSScriptRoot '../src/main/resources/assets/minecrafteft'
$data = Join-Path $PSScriptRoot '../src/main/resources/data'
$utf8 = [Text.UTF8Encoding]::new($false)
function WriteJson([string]$path, $value) {
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    [IO.File]::WriteAllText($path, ($value | ConvertTo-Json -Depth 40), $utf8)
}
$bitmap = [Drawing.Bitmap]::new(256,256)
$graphics = [Drawing.Graphics]::FromImage($bitmap)
$colors = @(@(231,231,220),@(30,34,38),@(191,151,66),@(43,47,46))
for($row=0; $row -lt 4; $row++) {
    for($column=0; $column -lt 4; $column++) {
        $c=$colors[$row]
        $brush=[Drawing.SolidBrush]::new([Drawing.Color]::FromArgb($c[0],$c[1],$c[2]))
        $graphics.FillRectangle($brush,$column*64,$row*64,64,64)
        $brush.Dispose()
        # Fine moulded grain/foam weave, deliberately quieter than the container corrugations.
        for($y=0; $y -lt 64; $y+=4) {
            for($x=0; $x -lt 64; $x+=4) {
                $shade=if($row -eq 3){-9}elseif($row -eq 2){7}else{3}
                $brush=[Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(
                    [Math]::Clamp($c[0]+$shade,0,255),[Math]::Clamp($c[1]+$shade,0,255),[Math]::Clamp($c[2]+$shade,0,255)))
                $graphics.FillRectangle($brush,$column*64+$x,$row*64+$y,2,1)
                $brush.Dispose()
            }
        }
    }
}
[IO.Directory]::CreateDirectory((Join-Path $assets 'textures/block')) | Out-Null
$bitmap.Save((Join-Path $assets 'textures/block/weapon_case_atlas.png'),[Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$bitmap.Dispose()
WriteJson (Join-Path $assets 'models/block/large_weapon_case.json') @{
    textures=@{particle='minecrafteft:block/weapon_case_atlas'}; elements=@()
}
WriteJson (Join-Path $assets 'blockstates/large_weapon_case.json') @{
    variants=@{''=@{model='minecrafteft:block/large_weapon_case'}}
}
WriteJson (Join-Path $assets 'models/block/weapon_case_part.json') @{
    textures=@{particle='minecrafteft:block/weapon_case_atlas'}; elements=@()
}
WriteJson (Join-Path $assets 'blockstates/weapon_case_part.json') @{
    variants=@{''=@{model='minecrafteft:block/weapon_case_part'}}
}
WriteJson (Join-Path $data 'minecrafteft/loot_tables/blocks/weapon_case_part.json') @{type='minecraft:block';pools=@()}

$elements=[Collections.Generic.List[object]]::new()
function Box($from,$to,[int]$material) {
    $faces=@{}
    foreach($face in @('north','south','east','west','up','down')) {
        $faces[$face]=@{texture='#atlas';uv=@(0,($material*4),4,($material*4+4))}
    }
    $elements.Add(@{from=$from;to=$to;faces=$faces})
}
Box @(0,0,3.25) @(16,6,12.75) 0
Box @(0,5.3,3.25) @(16,5.8,12.75) 1
Box @(0,5.8,3.25) @(16,7.1,12.75) 0
foreach($x in @(0,5,10,15)) { Box @($x,6.9,3.3) @(([Math]::Min(16,$x+1)),7.3,12.7) 0 }
foreach($x in @(1.8,7.6,13.4)) {
    Box @($x,4.5,3.0) @(($x+.8),6.5,3.3) 2
    Box @($x,.2,3.1) @(($x+.8),4.5,3.3) 0
}
Box @(6.5,2.1,2.95) @(9.5,3.9,3.2) 2
Box @(6.8,2.4,2.9) @(9.2,3.6,3.15) 1
WriteJson (Join-Path $assets 'models/item/large_weapon_case.json') @{
    parent='minecraft:block/block';textures=@{atlas='minecrafteft:block/weapon_case_atlas';particle='minecrafteft:block/weapon_case_atlas'}
    elements=$elements.ToArray();display=@{
        gui=@{rotation=@(25,145,0);translation=@(0,4,0);scale=@(.8,.8,.8)}
        ground=@{translation=@(0,3,0);scale=@(.5,.5,.5)}
        fixed=@{rotation=@(0,180,0);translation=@(0,2,0);scale=@(.8,.8,.8)}
        thirdperson_righthand=@{rotation=@(75,45,0);translation=@(0,2.5,0);scale=@(.375,.375,.375)}
        firstperson_righthand=@{rotation=@(0,45,0);translation=@(0,2,0);scale=@(.5,.5,.5)}
    }
}
$pickaxe=Join-Path $data 'minecraft/tags/blocks/mineable/pickaxe.json'
$tag=Get-Content -LiteralPath $pickaxe -Raw | ConvertFrom-Json -AsHashtable
$tag.values=@($tag.values | Where-Object {$_ -notin @('minecrafteft:large_weapon_case','minecrafteft:weapon_case_part')})+
    @('minecrafteft:large_weapon_case','minecrafteft:weapon_case_part')
WriteJson $pickaxe $tag

foreach($locale in @('ko_kr','en_us')) {
    $path=Join-Path $assets "lang/$locale.json"
    $lang=Get-Content -LiteralPath $path -Raw | ConvertFrom-Json -AsHashtable
    if($locale -eq 'ko_kr') {
        $lang['block.minecrafteft.large_weapon_case']='대형 무기 케이스'
        $lang['tooltip.minecrafteft.case_guns_only']='- 총기 수납 전용 케이스'
        $lang['tooltip.minecrafteft.case_capacity']='- 내부 수납 크기 : 10x15칸'
        $lang['tooltip.minecrafteft.case_contents']='- 총기 %s정 보관중'
        $lang['tooltip.minecrafteft.case_open']='- 개인 창고에서 상자에 커서를 대고 O : 케이스 열기'
        $lang['screen.minecrafteft.close_case_first']='케이스를 먼저 닫아 주세요.'
        $lang['message.minecrafteft.stash_claimed']='개인 창고 %s레벨을 확보했습니다. 문에서 우클릭하거나 50블록 이내에서 I 키를 누르세요.'
        $lang['message.minecrafteft.stash_out_of_range']='개인 창고가 범위 안에 없습니다. 같은 차원의 창고에서 50블록 이내로 이동해 주세요.'
    } else {
        $lang['block.minecrafteft.large_weapon_case']='Large Weapon Case'
        $lang['tooltip.minecrafteft.case_guns_only']='- Gun storage case'
        $lang['tooltip.minecrafteft.case_capacity']='- Storage size: 10x15 cells'
        $lang['tooltip.minecrafteft.case_contents']='- %s guns stored'
        $lang['tooltip.minecrafteft.case_open']='- Hover over the case in your stash and press O to open'
        $lang['screen.minecrafteft.close_case_first']='Close the case first.'
        $lang['message.minecrafteft.stash_claimed']='Personal stash Level %s linked. Right-click its door or press I within 50 blocks.'
        $lang['message.minecrafteft.stash_out_of_range']='Your personal stash is out of range. Move within 50 blocks of it in the same dimension.'
    }
    WriteJson $path $lang
}
