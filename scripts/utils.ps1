function substWithLabel {
    param(
        [String]$driveLetter,
        [String]$label,
        [String]$targetPath
    )
    psubst $driveLetter $targetPath
    substlabel $driveLetter $label
}

Function New-SymLink {
    [Diagnostics.CodeAnalysis.SuppressMessage("PSUseShouldProcessForStateChangingFunctions", "", Scope = "Function")]
    [Diagnostics.CodeAnalysis.SuppressMessage("PSAvoidUsingInvokeExpression", "")]
    param($link, $target)
    $resolvedLink = [System.Environment]::ExpandEnvironmentVariables($link)
    $resolvedTarget = Resolve-Path -LiteralPath ([System.Environment]::ExpandEnvironmentVariables($target))
    Write-Host "Linking ""$link"" (""$resolvedLink"") => ""$target"" (""$resolvedTarget"")"
    if (Test-Path -PathType Container $resolvedTarget) {
        $command = "cmd /c mklink /d"
    } elseif (Test-Path -PathType Leaf $resolvedTarget) {
        $command = "cmd /c mklink"
    } else {
        throw "Cannot resolve ""$target"" (""$resolvedTarget"")"
    }
    # TODO make sure link's parent created and is a folder
    Invoke-Expression "$command ""$link"" ""$resolvedTarget"""
}

Function Remove-SymLink {
    [Diagnostics.CodeAnalysis.SuppressMessage("PSUseShouldProcessForStateChangingFunctions")]
    [Diagnostics.CodeAnalysis.SuppressMessageAttribute("PSAvoidUsingInvokeExpression", "")]
    param($link)
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
