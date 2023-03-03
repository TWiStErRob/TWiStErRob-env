setlocal
set IDEA_PROPERTIES=%~dp0local.properties
set IDEA_VM_OPTIONS=%~dp0local.vmoptions
call %1\bin\idea.bat
