This script assumes Windows host. Syntax may vary slightly on Unix/Mac for setting and using environment variables. `.` is the folder where this README.md file is located.

## Build
Create the `svn2git` docker image from sources.

Run on host in `.`:
```bash
git clone https://github.com/svn-all-fast-export/svn2git.git
# or if the repo is already cloned:
git fetch -p
git pull -r

cd svn2git
docker build -t svn2git . # 2-4 minutes, ~200 MB download, 641MB space
```

## Run bash inside Docker
Run on host in `.`:
```bash
set SVN_REPO=P:\repos\svn
docker run --rm -it -v %CD%\conf:/tmp/conf -v %CD%\workdir:/workdir -v %SVN_REPO%:/tmp/svn svn2git bash
```

## Repo content cleanup
See [repo-cleanup.md](repo-cleanup.md).

If there was a change in the repo, set `%SVN_REPO%` to the `repo` folder.

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

## List all authors in an SVN repo

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
The files in `/tmp/conf` are mapped from the conf folder into the Docker container, so they're live editable without rebuild. `workdir` is also mapped so the output is available on the host.
Change the rules file name (`--rules`) as necessary.

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

## Post-process
It is possible to run scripts on each commit with `git filter-branch`.

### Example for editing commit messages
Clean up commit messages in `workdir\git-co\`:
```bash
git filter-branch --msg-filter "$(PWD)/../../.git/script.bat"
```
(`PWD=%CD%\.git-rewrite\t`, hence so many `../`s.)

Example script to change commit messages (`script.bat`):
This example removes the first line of commit message based on a regex:
```bash
perl -p0 -e "s/^^[^^\n]*\n//"
```
This is a standard batch file,
so `^` (regex's line start anchor or negated character class) needs to be escaped with `^^`.

`-p0` read the whole `stdin` (commit message) as one, so multiline regex is possible.

To execute only for the last commit (to test command / script), add `HEAD^..HEAD` at the end.

_Note: Normally `"%CD%\..\script.bat"` would work just fine, but `git filter-branch` on Windows delegates to 
`git/mingw64/libexec/git-core/git-filter-branch` running under MinGW Bash.
This means that running `git` from a Command Prompt will launch Bash, which in turn will launch the Batch file.
Escaping rules are weird, we can actually use MinGW Bash syntax inside quotes
(properly escaped for Command Prompt, so `--msg-filter` sees it correctly.
`%CD%` will be resolved at the time of calling `git`,
but `$PWD` will be resolved when `git-filter-branch` executes `eval "$filter_msg"`.
This is why it's not possible to even use `\` in `--msg-filter`._

_Note: use `"source $(PWD)/../../.git/script.sh"` to run a Unix Shell script (even on Windows).
In that case script.sh could contain: `perl -p0 -e 's/^[^\n]*\n//'` (different escaping rules)_
