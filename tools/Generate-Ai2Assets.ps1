Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '..\src\main\resources\assets\minecrafteft\textures\item'
[IO.Directory]::CreateDirectory($assetRoot) | Out-Null

# Row-major material IDs match Ai2KitModel. The emblem, rim and ribs are part of the mesh.
$palette = @(
    @(215,160,65), @(220,167,84), @(227,174,76), @(83,64,34),
    @(232,222,192), @(173,48,39), @(202,215,218), @(44,39,31),
    @(54,137,134), @(179,120,44), @(247,197,104)
)
$atlas = [Drawing.Bitmap]::new(128,128)
$random = [Random]::new(2002)
try {
    for ($material=0; $material -lt $palette.Count; $material++) {
        $base = $palette[$material]
        $left = ($material % 4) * 32
        $top = [int][Math]::Floor($material / 4) * 32
        for ($y=0; $y -lt 32; $y++) {
            for ($x=0; $x -lt 32; $x++) {
                $grain = $random.Next(-2,3)
                $color = [Drawing.Color]::FromArgb(255, $base[0]+$grain, $base[1]+$grain, $base[2]+$grain)
                $atlas.SetPixel($left+$x,$top+$y,$color)
            }
        }
    }
    $atlas.Save((Join-Path $assetRoot 'ai2_atlas.png'),[Drawing.Imaging.ImageFormat]::Png)
} finally {
    $atlas.Dispose()
}
