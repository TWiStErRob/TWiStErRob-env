. $env:PROG_HOME\scripts\utils.ps1

New-SymLink "%GRADLE_USER_HOME%\gradle.properties" ".\gradle.properties"
New-SymLink "%GRADLE_USER_HOME%\init.gradle.kts" ".\init.gradle.kts"
