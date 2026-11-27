param(
    [string]$TaczJar = (Join-Path $PSScriptRoot '../../tacz-forge-1.20.1-1.2.0-hotfix.jar'),
    [string]$YsmJar = (Join-Path $PSScriptRoot '../../ysm-2.6.5-forge+mc1.20.1-release.jar')
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$dependencies = @(
    @{ Source = $TaczJar; Hash = '6B26506E73CFBC8AD56C39D4DA09B9463DFA7E23B8B13389B80354E526548F5D'; Targets = @('libs/tacz-forge-1.20.1-1.2.0-hotfix.jar', 'local-maven/local/tacz/tacz-forge-1.20.1/1.2.0-hotfix/tacz-forge-1.20.1-1.2.0-hotfix.jar') },
    @{ Source = $YsmJar; Hash = '25B5E902B96F4C298690208F8B433CBC31737C23F87590354DBD86F00207BC8F'; Targets = @('local-maven/local/ysm/ysm-forge-1.20.1/2.6.5/ysm-forge-1.20.1-2.6.5.jar') }
)
# Validate every input before copying any dependency. Original mod files stay local.
foreach ($dependency in $dependencies) {
    if (!(Test-Path -LiteralPath $dependency.Source -PathType Leaf)) {
        throw "Missing original mod JAR: $($dependency.Source)"
    }
    if ((Get-FileHash -LiteralPath $dependency.Source -Algorithm SHA256).Hash -ne $dependency.Hash) {
        throw "Unexpected mod version or file checksum: $($dependency.Source)"
    }
}
foreach ($dependency in $dependencies) {
    foreach ($relativePath in $dependency.Targets) {
        $targetPath = Join-Path $projectRoot $relativePath
        New-Item -ItemType Directory -Force -Path (Split-Path $targetPath -Parent) | Out-Null
        if ([IO.Path]::GetFullPath($dependency.Source) -ne [IO.Path]::GetFullPath($targetPath)) {
            Copy-Item -LiteralPath $dependency.Source -Destination $targetPath -Force
        }
        Write-Host "Prepared $relativePath"
    }
}
