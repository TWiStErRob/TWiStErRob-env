. p:\scripts\utils.ps1

New-SymLink "%PROG_HOME%\tools\build\gradle" "%PROG_HOME%\caches\gradle\wrapper\dists"

git config --global credential.helper wincred
