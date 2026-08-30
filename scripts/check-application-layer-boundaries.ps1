# Optional Windows entry: forwards to the bash script (single rule source).
# Prefer: Git Bash / WSL → bash scripts/check-application-layer-boundaries.sh
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
if (-not $root) { $root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path }
$script = Join-Path $PSScriptRoot "check-application-layer-boundaries.sh"
$bashCandidates = @(
    "C:\Program Files\Git\bin\bash.exe",
    "C:\Program Files\Git\usr\bin\bash.exe",
    "bash"
)
$bash = $null
foreach ($c in $bashCandidates) {
    if ($c -eq "bash") {
        $cmd = Get-Command bash -ErrorAction SilentlyContinue
        if ($cmd) { $bash = $cmd.Source; break }
    } elseif (Test-Path $c) {
        $bash = $c
        break
    }
}
if (-not $bash) {
    Write-Error "bash not found. Install Git for Windows or use WSL, then: bash scripts/check-application-layer-boundaries.sh"
}
Set-Location $root
& $bash $script
exit $LASTEXITCODE
