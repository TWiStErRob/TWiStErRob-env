if (-not ${env:BACKUP_DIR}) {
    Write-Host "BACKUP_DIR environment variable not set."
    Exit 1
}
if (-not (Test-Path ${env:BACKUP_DIR})) {
    Write-Host "Backup directory is invalid (does it exist?): ${env:BACKUP_DIR}."
    Exit 1
}

$sourceDir = 'svn'
$sourceDate = $(svn/svn/svnlook date svn).Substring(0, "0000-00-00 00:00:00 +0000".Length)
$sourceRev = svn\svn\svnlook youngest svn
$targetFile = "${env:BACKUP_DIR}/svn@${sourceRev}-$(Get-Date $sourceDate -Format 'yyyyMMdd-HHmmss').zip"
if (-not (Test-Path $targetFile)) {
    Write-Host "Backing up SVN repository to $targetFile"
    # a: Add, -u-: Don't update anything, -mx: compression level 0-9, -tzip use ZIP format, -ssc: case sensitive, -ssw: add files opened for writing, -r: recursive
    & 7za a -mx0 -tzip -ssc -ssw -r $targetFile $sourceDir
    #Start-Process -NoNewWindow -Wait -FilePath 7za.exe -ArgumentList "a -u- -mx0 -tzip -ssc -ssw -r ${targetFile}" -WorkingDirectory $sourceDir -Verbose
} else {
    Write-Host "SVN repository at ${sourceRev} (${sourceDate}) is already backed up."
}

$sourceDir = 'release/android'
$targetDir = "${env:BACKUP_DIR}/release/android"
if (Test-Path $targetDir) {
    $lastTime = $(
        Get-ChildItem -Path $targetDir -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime |
            Select-Object -last 1
    ).LastWriteTime
} else {
    $lastTime = [DateTime]::MinValue
}
Write-Host "Backing up Releases newer than ${lastTime} to ${targetDir}"
$targetDir = New-Item -ItemType Directory -Force -Path $targetDir
Get-ChildItem $sourceDir -Recurse |
        Where-Object { !($_.PSIsContainer) -AND $_.LastWriteTime -gt $lastTime } |
        ForEach-Object { Copy-Item -Path $_.FullName -Destination $targetDir -Verbose }
