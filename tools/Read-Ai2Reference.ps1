param([string]$Video='D:\Codex\MinecraftModding\resource\2.mp4')
Add-Type -AssemblyName PresentationCore,WindowsBase
$outDir=Join-Path $PSScriptRoot '..\build\ai2-reference'
[IO.Directory]::CreateDirectory($outDir) | Out-Null
$player=[System.Windows.Media.MediaPlayer]::new()
$player.IsMuted=$true
$player.ScrubbingEnabled=$true
$script:opened=$false
$player.add_MediaOpened({ $script:opened=$true })
$player.add_MediaFailed({ Write-Output $_.ErrorException.Message })
$player.Open([Uri]$Video)
$player.Play()
$timer=[Diagnostics.Stopwatch]::StartNew()
while(!$script:opened -and $timer.Elapsed.TotalSeconds -lt 12) {
    [Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Windows.Threading.DispatcherPriority]::Background,[Action]{})
    Start-Sleep -Milliseconds 50
}
if(!$script:opened) { throw 'Video decoder did not open reference' }
$player.Pause()
Write-Output "Duration=$($player.NaturalDuration.TimeSpan.TotalSeconds), Size=$($player.NaturalVideoWidth)x$($player.NaturalVideoHeight)"
foreach($second in @(0,.75,1.5,2.25,3,3.75,4.5,5.25,5.8)) {
    $player.Position=[TimeSpan]::FromSeconds($second)
    $until=[DateTime]::UtcNow.AddMilliseconds(500)
    while([DateTime]::UtcNow -lt $until) {
        [Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Windows.Threading.DispatcherPriority]::Background,[Action]{})
        Start-Sleep -Milliseconds 20
    }
    $visual=[Windows.Media.DrawingVisual]::new()
    $drawing=$visual.RenderOpen()
    $drawing.DrawVideo($player,[Windows.Rect]::new(0,0,960,540))
    $drawing.Close()
    $bitmap=[Windows.Media.Imaging.RenderTargetBitmap]::new(960,540,96,96,[Windows.Media.PixelFormats]::Pbgra32)
    $bitmap.Render($visual)
    $png=[Windows.Media.Imaging.PngBitmapEncoder]::new()
    $png.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($bitmap))
    $stream=[IO.File]::Create((Join-Path $outDir ("frame-{0:0.00}.png" -f $second)))
    $png.Save($stream)
    $stream.Dispose()
}
$player.Close()
