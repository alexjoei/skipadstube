$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path $PSScriptRoot -Parent
$source = [System.Drawing.Image]::FromFile((Join-Path $projectRoot 'assets/logo.png'))
function Write-Icon([string]$relativePath, [int]$size, [double]$scale = 1) {
    $target = Join-Path $projectRoot $relativePath
    New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
    $bitmap = [System.Drawing.Bitmap]::new($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $edge = [int]($size * $scale)
        $offset = [int](($size - $edge) / 2)
        $graphics.DrawImage($source, $offset, $offset, $edge, $edge)
        $bitmap.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $bitmap.Dispose() }
}
try {
    foreach ($size in @(16, 32, 48, 128)) { Write-Icon "desktop/icons/icon-$size.png" $size }
    $densities = @{ mdpi = 48; hdpi = 72; xhdpi = 96; xxhdpi = 144; xxxhdpi = 192 }
    foreach ($density in $densities.Keys) {
        Write-Icon "app/src/main/res/mipmap-$density/ic_launcher.png" $densities[$density]
        Write-Icon "app/src/main/res/mipmap-$density/ic_launcher_foreground.png" ([int]($densities[$density] * 2.25)) 0.6
    }
} finally { $source.Dispose() }
