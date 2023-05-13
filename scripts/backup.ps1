$ErrorActionPreference = "Stop"

Write-Host "Starting repos."
Push-Location "$env:PROG_HOME\repos"
& ./backup.ps1
Pop-Location
Write-Host "Done with repos."

pause
