. $env:PROG_HOME\scripts\utils.ps1

#New-SymLink ".\gradle" "Z:\caches\gradle"
New-SymLink "P:\tools\build\gradle" ".\gradle\wrapper\dists\wrapper"
New-SymLink ".\.android\avd" "%USERPROFILE%\.android\avd"
New-SymLink "%USERPROFILE%Genymotion" ".\genymotion"
New-SymLink "%USERPROFILE%\AppData\Local\Genymobile" ".\genymotion"
