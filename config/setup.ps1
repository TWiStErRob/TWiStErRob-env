. $env:PROG_HOME\scripts\utils.ps1

Push-Location powershell; & .\setup.ps1; Pop-Location
Push-Location maven; & .\setup.ps1; Pop-Location
Push-Location gradle; & .\setup.ps1; Pop-Location
Push-Location git; & .\setup.ps1; Pop-Location
