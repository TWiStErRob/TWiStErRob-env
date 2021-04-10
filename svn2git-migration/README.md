This script assumes Windows host. Syntax may vary slighly on Unix/Mac for setting and using evronment variables.

## Build
Create the svn2git docker image from sources.

Run on host in `.`:
```bash
git clone https://github.com/svn-all-fast-export/svn2git.git
cd svn2git
docker build -t svn2git . # 2 minutes, ~200 MB download, 641MB space
cd ..
docker build -t svn2git-work . # 1 minutes, ~30 MB download, 715MB space
```

## Run bash inside Docker
Run on host in `.`:
```bash
set SVN_REPO=P:\repos\svn
docker run --rm -it -v %CD%\conf:/tmp/conf -v %CD%\workdir:/workdir -v %SVN_REPO%:/tmp/svn svn2git-work bash
```

## Repo content cleanup
There may be some secrets or words you want to hide from the GIT repositories.
Even if a secret was removed in the past, in SVN history it still exists and that'll be in GIT too.
It is possible to a full-text processing on a dump file of SVN described below.

Run on host in .:
```bash
set SVN_REPO=C:\personal\repos\svn
mkdir workdir\dump
%SVN_REPO%\svn\svnadmin dump %SVN_REPO% >workdir\dump\original.dump
```

Beware: this may corrupt the files, so really make sure that the only bytes changed in the dump files are the ones you intended.
Editing with a simple text editor is not an option as the dump file contains binary files
and line endings will be corrupted by even the smartest of text editors, except maybe hex editors.
`sed` scripting is the easiest to be safe.

Run in docker in `workdir\dump`:
```bash
sed -rf secrets.sed <original.dump >fixed.dump
```

The format of `secrets.sed` file:
```sed
# <Line 1 that needs fixing>
s/original pattern 1/replacement pattern 1/g

# <Line 2 that needs fixing>
s/original pattern 2/replacement pattern 2/g

# <Line 3 that needs fixing>
s/original pattern 3/replacement pattern 3/g
```
Make sure the result doesn't change the length of the strings being replaced,
otherwise it's necessary to re-calculate lots of content-length properties.

Even when the contents change the dump file contains MD5 and SHA-1 checksums of each file in the repo.
To fix these one could re-hash the file contents after `sed` replaced it,
but it's way faster to just remove this security check:
```bash
sed -re '/^(Text-content-md5|Text-content-sha1|Text-copy-source-md5|Text-copy-source-sha1): /d' <fixed.dump >fixed_nohash.dump
```

After transforming the dump file we can re-import the commits.
```bash
svnadmin create repo
svnadmin load repo <fixed_nohash.dump
```

After this replace SVN_REPO with `repo` folder.

Warning: Mind the `conf` and `hooks` folders and files in the repository root. They're not part of the dump.


## Checkout SVN repo
There are several ways of checking out the repository, some of them are faster/slower.

Timings for 3200 revs, 220M data:
 * Checkout from SVN_REPO: 10 minutes
 * Checkout from host via svnserve: 10 minutes
 * Checkout on host from SVN_REPO_SLASH: 1 minute

Note:
 * using `svn checkout --depth empty ...` decreases even the slowest of checkouts to 3 minutes.
 * using `svn checkout --depth empty --ignore-externals ...` makes all of them immediate.

### Checkout from SVN_REPO
Simplest, use the mapped SVN repository via `file` protocol.

Run in docker in `workdir`:
```bash
svn checkout file:///tmp/svn .
```

### Checkout from host
To get localhost of the host machine (if the repo is being `svnserve`'d):
 * run `ipconfig`
 * find "Ethernet adapter vEthernet (Default Switch)"
 * note "IPv4 Address"

Run on host in `repo`:
```bash
svn\svnserve -d --listen-port 786 %* -r .
```
Run in docker in `workdir`:
```bash
svn checkout svn://192.168.178.17:786 .
```

Note that this solution requires authentication as defined by `svnserve.conf` via:
```ini
[general]
anon-access = none
auth-access = write
password-db = passwd
authz-db = authz
```
Note: usernames are case sensitive!

In case of mis-typing and storing the password here's how to reset it in docker:
```bash
$ svn auth --remove *
Deleted 1 matching credentials from '/root/.subversion'
```

### Checkout on host
Run on host in `workdir`:
```bash
set SVN_REPO=P:\repos\svn
set SVN_REPO_SLASH=P:/repos/svn
%SVN_REPO%\svn\svn co file:///%SVN_REPO_SLASH% .
```

Run in docker in `workdir`:
```bash
svn relocate file:///tmp/svn . # migrate from host to docker
```

## List all of the authors in an SVN repo

Run in docker in `workdir`:
```bash
# https://gist.github.com/amura2406/a610a6a50690ceda96b6858f2435227f#map-authors-optional
# svn log format: `r2447 | TWiStEr | 2017-12-17 18:04:28 +0000 (Sun, 17 Dec 2017) | 2 lines`
svn log --quiet | grep -E "r[0-9]+ \| .+? \|" | cut -d'|' -f2 | sed 's/^ //' | sed 's/ $//' | sort | uniq
```
Based on this I created `migrate.authors`.
```
TWiStEr = Róbert Papp (TWiStErRob) <papp.robert.s@gmail.com>
```

## Execute
Run in docker in `workdir`:
```bash
/usr/local/svn2git/svn-all-fast-export \
--identity-map /tmp/conf/migrate.authors \
--rules /tmp/conf/monorepo-split.rules \
--debug-rules \
--stats \
--svn-ignore \
--propcheck \
--empty-dirs \
--add-metadata \
--add-metadata-notes \
--msg-filter 'sed -z -r -f /tmp/conf/svn-msg-filter.sed' \
/tmp/svn \
>svn2git.log 2>&1
```

`--debug-rules` outputs each matched file for each rule instead of just summary:
```
Exporting revision 1118 ...... 2 modifications from SVN /Projects/Sun/ to net.twisterrob.sun/master done
```
with debug:
```
Exporting revision 1118

rev 1118 /Projects/Sun/gen/R.java matched rule: "/tmp/conf/repo.rules:4 /Projects/Sun/"    exporting.
.add/change file ( /Projects/Sun/gen/R.java -> "master" "gen/R.java" )

rev 1118 /Projects/Sun/res/values/arrays.xml matched rule: "/tmp/conf/repo.rules:4 /Projects/Sun/"    exporting.
.add/change file ( /Projects/Sun/res/values/arrays.xml -> "master" "res/values/arrays.xml" )

6 modifications from SVN /Projects/Sun/ to net.twisterrob.sun/master done
```

`-add-metadata-notes` and `--add-metadata` adds this line pattern as commit notes or appended commit message:
```
svn path=/Path/In/SVN-repo/; revision=1234
```


## Verify
Run on host in `.`:
```bash
git clone workdir/git-repo workdir/git-co
cd workdir/git-co
git branch -a
gitk
```
