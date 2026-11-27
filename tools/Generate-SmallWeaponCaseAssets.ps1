$ErrorActionPreference='Stop'
$assets=Join-Path $PSScriptRoot '../src/main/resources/assets/minecrafteft'
$data=Join-Path $PSScriptRoot '../src/main/resources/data'
$utf8=[Text.UTF8Encoding]::new($false)
function WriteJson([string]$path,$value) {
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null
    [IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 30),$utf8)
}
$names=@('weapon_case','weapon_case_ar_team','weapon_case_elmo','weapon_case_hide404')
foreach($name in $names) {
    WriteJson (Join-Path $assets "models/block/$name.json") @{
        textures=@{particle='minecrafteft:block/weapon_case_atlas'};elements=@()
    }
    WriteJson (Join-Path $assets "blockstates/$name.json") @{
        variants=@{''=@{model="minecrafteft:block/$name"}}
    }
    # Custom item renderer reuses the full 3D model, including the tilted lid marking.
    WriteJson (Join-Path $assets "models/item/$name.json") @{
        parent='builtin/entity';textures=@{particle='minecrafteft:block/weapon_case_atlas'}
        display=@{
            gui=@{rotation=@(22,180,0);translation=@(0,0,0);scale=@(.9,.9,.9)}
            ground=@{translation=@(0,3,0);scale=@(.5,.5,.5)}
            fixed=@{rotation=@(0,180,0);translation=@(0,0,0);scale=@(.8,.8,.8)}
            thirdperson_righthand=@{rotation=@(75,45,0);translation=@(0,2.5,0);scale=@(.375,.375,.375)}
            firstperson_righthand=@{rotation=@(0,45,0);translation=@(0,2,0);scale=@(.5,.5,.5)}
        }
    }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::OpenRead((Join-Path $PSScriptRoot '../../resource/GFL2_icon.zip'))
try {
    $markings=@{'ARteam.png'='ar_team.png';'Elmo_CE.png'='elmo.png';'HIDE404.png'='hide404.png'}
    foreach($name in $markings.Keys) {
        $entry=$zip.GetEntry($name)
        if(!$entry) {throw "Missing marking: $name"}
        $dest=Join-Path $assets ('textures/block/markings/'+$markings[$name])
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($dest))|Out-Null
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$dest,$true)
    }
} finally {$zip.Dispose()}
$tagPath=Join-Path $data 'minecraft/tags/blocks/mineable/pickaxe.json'
$tag=Get-Content -LiteralPath $tagPath -Raw|ConvertFrom-Json -AsHashtable
$tag.values=@($tag.values)+@($names|ForEach-Object {"minecrafteft:$_"})|Select-Object -Unique
WriteJson $tagPath $tag
foreach($locale in @('ko_kr','en_us')) {
    $path=Join-Path $assets "lang/$locale.json"
    $lang=Get-Content -LiteralPath $path -Raw|ConvertFrom-Json -AsHashtable
    if($locale -eq 'ko_kr') {
        $lang['block.minecrafteft.weapon_case']='무기 케이스'
        $lang['block.minecrafteft.weapon_case_ar_team']='무기 케이스 (AR Team)'
        $lang['block.minecrafteft.weapon_case_elmo']='무기 케이스 (ELMO)'
        $lang['block.minecrafteft.weapon_case_hide404']='무기 케이스 (H.I.D.E. 404)'
        $lang['tooltip.minecrafteft.case_capacity_rows']='- 내부 수납 크기 : 10x%s칸'
    } else {
        $lang['block.minecrafteft.weapon_case']='Weapon Case'
        $lang['block.minecrafteft.weapon_case_ar_team']='Weapon Case (AR Team)'
        $lang['block.minecrafteft.weapon_case_elmo']='Weapon Case (ELMO)'
        $lang['block.minecrafteft.weapon_case_hide404']='Weapon Case (H.I.D.E. 404)'
        $lang['tooltip.minecrafteft.case_capacity_rows']='- Storage size: 10x%s cells'
    }
    WriteJson $path $lang
}
