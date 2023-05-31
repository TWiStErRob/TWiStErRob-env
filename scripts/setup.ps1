param(
    [switch]$elevated,
    [String]$cwd
)

$ErrorActionPreference = "Stop"

function Test-Admin {
  $currentUser = New-Object Security.Principal.WindowsPrincipal $([Security.Principal.WindowsIdentity]::GetCurrent())
  $currentUser.IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)
}
function Quoted {
    param([String]$arg)
    return ('"' + $arg + '"')
}
if ((Test-Admin) -eq $false)  {
    if ($elevated) {
        Write-Host "Tried to elevate, did not work, aborting!"
    } else {
        Write-Host "Elevating $($MyInvocation.MyCommand.Definition) in $pwd"
        Start-Process -Verb RunAs -FilePath "powershell.exe" -ArgumentList(
            "-NoLogo",
            "-NoProfile",
            "-NoExit",
            "-ExecutionPolicy", "ByPass",
            "-File", (Quoted $MyInvocation.MyCommand.Definition),
            "-elevated",
            "-cwd", (Quoted($pwd -replace "\\", "\\"))
        )
    }
    exit
}

# StartProcess -Verb RunAs -WorkingDirectory $pwd doesn't work together, so manually propagating and setting $pwd
# https://stackoverflow.com/questions/43494863/start-process-workingdirectory-as-administrator-does-not-set-location#comment74045869_43494863
Set-Location $cwd

. .\utils.ps1

# Set autoexec.cmd for "cmd.exe" launch to support Unicode and better color/history handling.
Write-Host "HKLM\SOFTWARE\Microsoft\Command Processor\Autorun = ""p:\tools\misc\autoexec.cmd"""
reg add "HKLM\SOFTWARE\Microsoft\Command Processor" /v "Autorun" /d "p:\tools\misc\autoexec.cmd" /f

# For subst to work from these host drives their label needs to be cleared, but they can be visually re-programmed.
substlabel C "512-3 System"
substlabel P "512-4 Prog"
substlabel X "1000-2 Data"
substlabel Z "1000-1 Caches"

Write-Host
& .\env.ps1

Write-Host
Write-Host "Setting up links..."
# Expected outputs: symbolic link created for dir <<===>> path

New-SymLink "P:\downloads" "%USERPROFILE%\Downloads"

New-SymLink "p:\tools\xampp\htdocs\maven" "p:\repos\maven"

New-SymLink "p:\web\localhost" "p:\tools\xampp\htdocs"
New-SymLink "p:\web\twisterrob.net\root" "p:\web\twisterrob.net\000webhost.net\ftproot\public_html"

# Dead because it's a VHDX file now.
# New-SymLink "p:\data\ubuntu" "c:\Users\TWiStEr\AppData\Local\Packages\CanonicalGroupLimited.UbuntuonWindows_79rhkp1fndgsc\LocalState\rootfs"

New-SymLink "%ProgramFiles%\nodejs" "P:\tools\lang\nvm-1.1.5\v14.17.6"

Write-Host "Done with links."

Write-Host
Write-Host "Setting up config..."
Push-Location $env:PROG_HOME\config
& .\setup.ps1
Pop-Location
Write-Host "Done with config."

Write-Host
Write-Host "Setting up tools..."
Push-Location $env:PROG_HOME\tools
& .\setup.ps1
Pop-Location
Write-Host "Done with tools."

Write-Host
Write-Host "Setting up secrets..."
Push-Location $env:PROG_HOME\secrets
& .\setup.ps1
Pop-Location
Write-Host "Done with secrets."

Write-Host
Write-Host "Setting up projects..."
Push-Location $env:PROG_HOME\projects
& .\setup.ps1
Pop-Location
Write-Host "Done with projects."

Write-Host
Write-Host "Setting up vitual drives..."
#substWithLabel B "TODO" "TODO"
#substWithLabel D "Data" "data"
substWithLabel T "Downloads" "$env:USERPROFILE\Downloads"
subst
Write-Host "Done with virtual drives."

pause
