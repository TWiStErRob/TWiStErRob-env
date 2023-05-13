. $env:PROG_HOME\scripts\utils.ps1

# create repository as a file, so the folder cannot be created
#New-Item -Path "$env:USERPROFILE\.m2" -Name "repository" -ItemType File -Force | Out-Null
# Above doesn't work with IDEA using org.eclipse.aether:
# java.io.FileNotFoundException: C:\Users\TWiStEr\.m2\repository\org\junit\platform\... (The system cannot find the path specified)

New-SymLink "%USERPROFILE%\.m2\settings.xml" ".\settings.xml"
New-SymLink "%USERPROFILE%\.m2\repository" "%PROG_HOME%\caches\.m2"
New-SymLink "%PROG_HOME%\repos\maven\settings.xml" ".\settings.xml"
