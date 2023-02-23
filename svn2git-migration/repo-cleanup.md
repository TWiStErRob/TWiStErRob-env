## Repo content cleanup

There may be some secrets or words you want to hide from the GIT repositories.
Even if a secret was removed in the past, in SVN history it still exists and that'll be in GIT too.
It is possible to a full-text processing on a dump file of SVN described below.

Run on host in .:
```bash
set SVN_REPO=P:\repos\svn
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

---

Warning: Mind the `conf` and `hooks` folders and files in the repository root. They're not part of the dump.
