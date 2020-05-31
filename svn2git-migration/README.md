## Build
```bash
git clone https://github.com/svn-all-fast-export/svn2git.git
cd svn2git
docker build -t svn2git .
cd ..
docker build -t svn2git-work .
```

## Run bash
```bash
docker run --rm -it -v %CD%\conf:/tmp/conf -v %CD%\workdir:/workdir -v P:\repos\svn:/tmp/svn svn2git-work bash
```

## List all of the authors in SVN repo
`192.168.178.17` is the IP of the "Ethernet adapter vEthernet (Default Switch)" on host (run `ipconfig`).

```bash
svn checkout svn://192.168.178.17:786 .
# https://gist.github.com/amura2406/a610a6a50690ceda96b6858f2435227f#map-authors-optional
svn log --quiet | grep -E "r[0-9]+ \| .+ \|" | cut -d'|' -f2 | sed 's/ //g' | sort | uniq
```
Based on this I created `migrate.authors`.
```
TWiStEr = Róbert Papp <papp.robert.s@gmail.com>
```

## Execute
after running with `bash` above
```bash
/usr/local/svn2git/svn-all-fast-export --identity-map /tmp/conf/migrate.authors --rules /tmp/conf/twister-plugin-gradle.rules --svn-branches --debug-rules --svn-ignore --empty-dirs /tmp/svn
```
`--add-metadata` would add this line to each `svn path=/Path/In/SVN-repo/; revision=1234` to each commit message.

## Verify
```bash
git clone workdir/git-repo workdir/git-co
cd workdir/git-co
git branch -a
gitk
```
