param(
    [Parameter(Mandatory=$true)][string]$ModJar,
    [string]$InstallRoot = (Join-Path $env:USERPROFILE 'curseforge\minecraft\Install'),
    [string]$ForgeProfile = 'forge-47.4.10',
    [switch]$PrepareOnly
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$gameRoot = Join-Path $projectRoot 'run-ysm'
$libraryRoot = Join-Path $InstallRoot 'libraries'
$java = Join-Path $InstallRoot 'runtime\java-runtime-gamma\windows-x64\java-runtime-gamma\bin\java.exe'
$forgeJson = Join-Path $InstallRoot "versions\$ForgeProfile\$ForgeProfile.json"
if (!(Test-Path -LiteralPath $forgeJson)) { throw "Installed Forge profile not found: $forgeJson" }
if (!(Test-Path -LiteralPath $java)) { throw "Microsoft Java runtime not found: $java" }
$forge = Get-Content -Raw -LiteralPath $forgeJson | ConvertFrom-Json
if ($forge.inheritsFrom -ne '1.20.1') { throw 'This launcher requires an installed Minecraft 1.20.1 Forge profile.' }
$baseRoot = Join-Path $InstallRoot "versions\$($forge.inheritsFrom)"
$base = Get-Content -Raw -LiteralPath (Join-Path $baseRoot "$($forge.inheritsFrom).json") | ConvertFrom-Json
$profileJar = Join-Path $InstallRoot "versions\$ForgeProfile\$ForgeProfile.jar"
if ($forge.jar) {
    $clientJar = Join-Path $InstallRoot "versions\$($forge.jar)\$($forge.jar).jar"
} elseif (Test-Path -LiteralPath $profileJar -PathType Leaf) {
    $clientJar = $profileJar
} else {
    $clientJar = Join-Path $baseRoot "$($forge.inheritsFrom).jar"
}

function Test-LibraryRules($Library) {
    if (!$Library.rules) { return $true }
    $allowed = $false
    foreach ($rule in $Library.rules) {
        $matches = $true
        if ($rule.os) {
            if ($rule.os.name -and $rule.os.name -ne 'windows') { $matches = $false }
            if ($rule.os.arch -and $rule.os.arch -notin @('x86_64','amd64')) { $matches = $false }
            if ($rule.os.version -and [Environment]::OSVersion.Version.ToString() -notmatch $rule.os.version) { $matches = $false }
        }
        if ($rule.features) {
            foreach ($feature in $rule.features.PSObject.Properties) {
                if ($feature.Value -eq $true) { $matches = $false }
            }
        }
        if ($matches) { $allowed = $rule.action -eq 'allow' }
    }
    return $allowed
}

$libraries = [ordered]@{}
foreach ($library in (@($base.libraries) + @($forge.libraries))) {
    if (!(Test-LibraryRules $library)) { continue }
    if (!$library.downloads.artifact) { throw "Missing library metadata: $($library.name)" }
    $parts = $library.name.Split(':')
    # The Microsoft runtime above is x64. Mojang's OS-only rules also allow the x86 and ARM64 DLLs.
    if ($parts.Count -gt 3 -and $parts[3] -in @('natives-windows-x86','natives-windows-arm64')) { continue }
    $key = $parts[0] + ':' + $parts[1]
    if ($parts.Count -gt 3) { $key += ':' + $parts[3] }
    $libraries[$key] = $library
}
$classpath = @()
foreach ($library in $libraries.Values) {
    $path = Join-Path $libraryRoot $library.downloads.artifact.path
    if (!(Test-Path -LiteralPath $path)) { throw "Installed library not found: $path" }
    $classpath += $path
}
if (!(Test-Path -LiteralPath $clientJar)) { throw "Minecraft client not found: $clientJar" }
$classpath += $clientJar

$mods = Join-Path $gameRoot 'mods'
$natives = Join-Path $gameRoot 'natives'
[IO.Directory]::CreateDirectory($mods) | Out-Null
[IO.Directory]::CreateDirectory($natives) | Out-Null
$copies = @(
    @([IO.Path]::GetFullPath($ModJar), 'minecrafteft-test.jar'),
    @((Join-Path $projectRoot 'local-maven\local\ysm\ysm-forge-1.20.1\2.6.5\ysm-forge-1.20.1-2.6.5.jar'), 'ysm-2.6.5-forge+mc1.20.1-release.jar'),
    @((Join-Path $projectRoot 'libs\tacz-forge-1.20.1-1.2.0-hotfix.jar'), 'tacz-forge-1.20.1-1.2.0-hotfix.jar')
)
foreach ($copy in $copies) {
    if (!(Test-Path -LiteralPath $copy[0])) { throw "Required mod not found: $($copy[0])" }
    Copy-Item -LiteralPath $copy[0] -Destination (Join-Path $mods $copy[1]) -Force
}

# Share only the model folder, keeping the original userdev world's data separate.
$models = Join-Path $projectRoot 'run\config\yes_steve_model'
$modelLink = Join-Path $gameRoot 'config\yes_steve_model'
[IO.Directory]::CreateDirectory((Split-Path -Parent $modelLink)) | Out-Null
if (!(Test-Path -LiteralPath $models)) { throw "Model folder not found: $models" }
if (!(Test-Path -LiteralPath $modelLink)) {
    New-Item -ItemType Junction -Path $modelLink -Target $models | Out-Null
} else {
    $existing = Get-Item -LiteralPath $modelLink
    if ($existing.LinkType -ne 'Junction' -or [IO.Path]::GetFullPath([string]$existing.Target) -ne $models) {
        throw "Expected a model-folder junction at $modelLink; existing content was left untouched."
    }
}

foreach ($path in $classpath) {
    if ($path -notmatch 'natives-windows.*\.jar$') { continue }
    $archive = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        foreach ($entry in $archive.Entries) {
            if (!$entry.FullName.EndsWith('.dll')) { continue }
            $target = Join-Path $natives ([IO.Path]::GetFileName($entry.FullName))
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$true)
        }
    } finally { $archive.Dispose() }
}

function Expand-LauncherArgument([string]$Argument) {
    $expanded = $Argument.Replace('${library_directory}',$libraryRoot).
        Replace('${classpath_separator}',[IO.Path]::PathSeparator).
        Replace('${version_name}',$ForgeProfile)
    # Forge supplies the Minecraft module itself. BootstrapLauncher must ignore the
    # raw client under its actual filename, including for profiles that inherit the jar.
    $ignorePrefix = '-DignoreList='
    if ($expanded.StartsWith($ignorePrefix)) {
        $clientName = [IO.Path]::GetFileName($clientJar)
        if ($expanded.Substring($ignorePrefix.Length).Split(',') -notcontains $clientName) {
            $expanded += ",$clientName"
        }
    }
    return $expanded
}
function Quote-JavaArgument([string]$Argument) {
    return '"' + $Argument.Replace('\','\\').Replace('"','\"') + '"'
}
$arguments = @('-Xmx3G',"-Djava.library.path=$natives")
foreach ($argument in $forge.arguments.jvm) {
    if ($argument -isnot [string]) { throw 'Unexpected Forge JVM argument metadata.' }
    $arguments += Expand-LauncherArgument $argument
}
$arguments += @('-cp',($classpath -join [IO.Path]::PathSeparator),$forge.mainClass)
foreach ($argument in $forge.arguments.game) {
    if ($argument -isnot [string]) { throw 'Unexpected Forge game argument metadata.' }
    $arguments += $argument
}
$arguments += @('--gameDir',$gameRoot,'--assetsDir',(Join-Path $InstallRoot 'assets'),
    '--assetIndex',[string]$base.assetIndex.id,'--username','Dev','--accessToken','0',
    '--uuid','8cfa228e5b56392aaef18c85cb0e8afa','--userType','legacy',
    '--version',$ForgeProfile,'--versionType','release')
$argumentsFile = Join-Path $gameRoot 'launch.args'
[IO.File]::WriteAllLines($argumentsFile,@($arguments | ForEach-Object { Quote-JavaArgument $_ }),[Text.UTF8Encoding]::new($false))
Write-Output "Regular Forge profile: $ForgeProfile (development build remains Forge 47.4.28)"
Write-Output "Java: $java"
Write-Output "Game folder: $gameRoot"
Write-Output "Shared models: $models"
Write-Output 'Original YSM, TACZ and the current Minecraft EFT build are prepared.'
if ($PrepareOnly) { return }
$previousJavaHome = $env:JAVA_HOME
$previousJavaPath = $env:PATH
# gradlew17 uses Temurin to build. Keep the game's native-library environment
# consistent with the Microsoft java.exe instead of inheriting the build JDK.
$env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)
$env:PATH = (Split-Path -Parent $java) + [IO.Path]::PathSeparator + $env:PATH
Push-Location -LiteralPath $gameRoot
try {
    & $java ('@' + $argumentsFile)
    exit $LASTEXITCODE
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJavaHome
    $env:PATH = $previousJavaPath
}
