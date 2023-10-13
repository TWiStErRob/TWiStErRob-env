@echo Are you sure? CTRL+C if not!
@pause

@setlocal
@set REPO_CONTENTS=db
@set REPO_CONFIG=conf hooks locks format
@set REPO_FULL=%REPO_CONTENTS% %REPO_CONFIG%
@set TEMP_DIR=temp
@set BACKUP_DIR=backup

@IF EXIST %BACKUP_DIR% exit /B 1
mkdir %BACKUP_DIR%

@set ROBOCOPY=robocopy /COPY:DAT /DCOPY:T /IA:RASHCNETO /V /BYTES /NJH /NJS
@rem Copy folders (files will fail)
@for %%d IN (%REPO_CONFIG%) do %ROBOCOPY% /S /E %%d %BACKUP_DIR%\%%d
@rem Copy files (folders will be skipped)
%ROBOCOPY% . %BACKUP_DIR% %REPO_CONFIG%

@for %%d IN (%REPO_CONTENTS%) do move %%d %BACKUP_DIR%\%%d
.\svn\svnadmin create %TEMP_DIR%
@for %%d IN (%REPO_CONTENTS%) do move %TEMP_DIR%\%%d .\%%d
rmdir /S /Q %TEMP_DIR%
.\svn\svnadmin dump -r 0:2447 %BACKUP_DIR% | .\svn\svnadmin load .

endlocal