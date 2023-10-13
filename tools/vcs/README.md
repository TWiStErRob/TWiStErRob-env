## Git

Download from https://git-scm.com/download/win
which redirects to https://github.com/git-for-windows/git/releases.

Replace `git` link (2.35 to 2.38):
```
P:\tools\vcs>dir
2022-03-03  21:19    <SYMLINKD>     git [git-2.35.1]
2022-03-03  21:18    <DIR>          git-2.35.1
2022-10-31  10:26    <DIR>          git-2.38.1

P:\tools\vcs>rmdir git

P:\tools\vcs>dir
2022-03-03  21:18    <DIR>          git-2.35.1
2022-10-31  10:26    <DIR>          git-2.38.1

P:\tools\vcs>mklink /D git git-2.38.1
symbolic link created for git <<===>> git-2.38.1

P:\tools\vcs>dir
2022-10-31  10:33    <SYMLINKD>     git [git-2.38.1]
2022-03-03  21:18    <DIR>          git-2.35.1
2022-10-31  10:26    <DIR>          git-2.38.1
```
