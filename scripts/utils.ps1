function substWithLabel {
    param(
        [String]$driveLetter,
        [String]$label,
        [String]$targetPath
    )
    psubst $driveLetter $targetPath
    substlabel $driveLetter $label
}

[Diagnostics.CodeAnalysis.SuppressMessage("PSUseShouldProcessForStateChangingFunctions", "", Scope = "Function")]
[Diagnostics.CodeAnalysis.SuppressMessageAttribute("PSAvoidUsingInvokeExpression", "")]
Function New-SymLink($link, $target) {
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

[Diagnostics.CodeAnalysis.SuppressMessage("PSUseShouldProcessForStateChangingFunctions", "")]
[Diagnostics.CodeAnalysis.SuppressMessage("PSAvoidUsingInvokeExpression", "")]
Function Remove-SymLink($link) {
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
