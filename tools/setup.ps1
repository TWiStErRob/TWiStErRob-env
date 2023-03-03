. p:\scripts\utils.ps1

New-SymLink "%PROG_HOME%\tools\build\gradle" "%GRADLE_USER_HOME%\wrapper\dists"

git config --global credential.helper wincred
