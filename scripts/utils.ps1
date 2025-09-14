function substWithLabel() {
    param(
        [String]$driveLetter,
        [String]$label,
        [String]$targetPath
    )
    psubst $driveLetter $targetPath
    substlabel $driveLetter $label
}

Function New-SymLink() {
    [Diagnostics.CodeAnalysis.SuppressMessage("PSUseShouldProcessForStateChangingFunctions", "")]
    [Diagnostics.CodeAnalysis.SuppressMessage("PSAvoidUsingInvokeExpression", "")]
    param(
        [String]$link,
        [String]$target
    )

    $resolvedLink = [System.Environment]::ExpandEnvironmentVariables($link)
    $resolvedTarget = (Resolve-Path -LiteralPath ([System.Environment]::ExpandEnvironmentVariables($target))).ProviderPath

    Write-Host "Linking ""$link"" (""$resolvedLink"") => ""$target"" (""$resolvedTarget"")"

    if (Test-Path $resolvedLink) {
        $existing = Get-Item $resolvedLink -Force
        if ($existing.Attributes -band [IO.FileAttributes]::ReparsePoint) {
            if ($existing.Target -eq $resolvedTarget) {
                return
            } else {
                throw "There's an existing file/folder at $resolvedLink, and it does not point to the right place." `
                + "`nExisting link: $($existing.Target)" `
                + "`nExpected link: $resolvedTarget"
            }
        } else {
            throw "A real file/folder already exists at '$resolvedLink'. Cannot create symlink."
        }
    }

    $parentDir = Split-Path $resolvedLink -Parent
    if (-not (Test-Path $parentDir)) {
        Write-Host "Creating missing parent directory: $parentDir"
        New-Item -ItemType Directory -Path $parentDir
    } elseif (-not (Test-Path $parentDir -PathType Container)) {
        throw "Parent path '$parentDir' exists but is not a directory."
    }

    if (Test-Path -PathType Container $resolvedTarget) {
        $command = "cmd /c mklink /d"
    } elseif (Test-Path -PathType Leaf $resolvedTarget) {
        $command = "cmd /c mklink"
    } else {
        throw "Cannot resolve ""$target"" (""$resolvedTarget"")"
    }

    Invoke-Expression "$command ""$resolvedLink"" ""$resolvedTarget"""
}

Function Remove-SymLink() {
    [Diagnostics.CodeAnalysis.SuppressMessage("PSUseShouldProcessForStateChangingFunctions", "")]
    [Diagnostics.CodeAnalysis.SuppressMessage("PSAvoidUsingInvokeExpression", "")]
    param(
        [String]$link
    )

    $resolvedLink = Resolve-Path -LiteralPath ([System.Environment]::ExpandEnvironmentVariables($link))
    Write-Host "Unlinking ""$link"" (""$resolvedLink"")"
    if (Test-Path -PathType Container $resolvedLink) {
        $command = "cmd /c rmdir"
    } elseif (Test-Path -PathType Leaf $resolvedLink) {
        $command = "cmd /c del"
    } else {
        throw "Cannot resolve ""$link"" (""$resolvedLink"")"
    }
    Invoke-Expression "$command ""$resolvedLink"""
}
