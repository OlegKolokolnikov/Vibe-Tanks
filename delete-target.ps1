# Delete the corrupted target directory using long path support
$targetDir = "\\?\C:\Users\oleg\Documents\projects\vibeTanks\target"

function Remove-LongPathDirectory {
    param([string]$Path)

    if (Test-Path -LiteralPath $Path) {
        # First, delete all files
        Get-ChildItem -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue |
            Where-Object { -not $_.PSIsContainer } |
            ForEach-Object {
                try {
                    Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue
                } catch {}
            }

        # Then delete directories from deepest to shallowest
        Get-ChildItem -LiteralPath $Path -Recurse -Directory -Force -ErrorAction SilentlyContinue |
            Sort-Object { $_.FullName.Length } -Descending |
            ForEach-Object {
                try {
                    Remove-Item -LiteralPath $_.FullName -Force -Recurse -ErrorAction SilentlyContinue
                } catch {}
            }

        # Finally try to remove the root
        try {
            Remove-Item -LiteralPath $Path -Force -Recurse -ErrorAction SilentlyContinue
        } catch {}
    }
}

# Try different approaches
Write-Host "Attempting to delete target directory..."

# Approach 1: Use robocopy to mirror empty directory
$emptyDir = "C:\Users\oleg\Documents\projects\vibeTanks\empty_temp"
$targetDirNormal = "C:\Users\oleg\Documents\projects\vibeTanks\target"

if (-not (Test-Path $emptyDir)) {
    New-Item -ItemType Directory -Path $emptyDir | Out-Null
}

Write-Host "Using robocopy to clean directory..."
$robocopyResult = & robocopy $emptyDir $targetDirNormal /MIR /R:0 /W:0 /NFL /NDL /NJH /NJS /NC /NS /NP 2>&1

Write-Host "Removing directories..."
Remove-Item -Path $emptyDir -Force -ErrorAction SilentlyContinue
Remove-Item -Path $targetDirNormal -Force -Recurse -ErrorAction SilentlyContinue

if (Test-Path $targetDirNormal) {
    Write-Host "Directory still exists, target may have very deep paths"
} else {
    Write-Host "Successfully deleted target directory"
}
