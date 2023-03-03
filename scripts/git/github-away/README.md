# github-away
How to set up Git and GitHub on a foreign machine.

Go to https://github.com/settings/tokens and generate a new token.
```shell
git clone https://github.com/*/*
# enter username twisterrob and token as password
git config --local --add user.email papp.robert.s@gmail.com
git config --local --add user.name "Róbert Papp (TWiStErRob)"
```

## Fix existing commits
Note: this will require a force push if already published.

### Before setting the `user` config for the repository
```shell
git commit --amend --author="Róbert Papp (TWiStErRob) <papp.robert.s@gmail.com>" --no-edit
```
would fix the author, but the commiter would stay as before.

### After setting the `user` config for the repository
```shell
git commit --amend --reset-author
```
would fix the author and the commiter at the same time.

### Without setting the `user` config
```shell
git -c user.name="Róbert Papp (TWiStErRob)" -c user.email=papp.robert.s@gmail.com commit --amend --reset-author
```

### Fix multiple commits
Given: a repository you want to rewrite, let's call it <repo>.
This contains a commit hash in history which is the first to modify. Let's call this ``<first-commit-hash>``.
We'll have to use its parent (^), otherwise it skips the first commit we want to change.
Can define a range of commits by using `<last-commit-hash>`, which could be `HEAD`

Looking around the internet I found that I can write a shell script inside `--commit-filter`:
```shell
git filter-branch --commit-filter " \
    long \
    multi \
    line \
    script" <first-commit-hash>..<last-commit-hash>
```
but multiline and escaping is clunky and error prone,
and couldn't find a way to run this "one-liner" in a cross-platform way (`\` is not a thing in Windows cmd).

I found a way to use a shell script file instead with a real one-liner:
```
git filter-branch --commit-filter "source $(PWD)/../../.git/commit-filter-foo.sh" <first-commit-hash>^..<last-commit-hash>
```

Notice the script is using `$(PWD)`, which is `<repo>/.git-rewrite/t/` and hence needing the double `..`.
Since the folder of the repo is version tracked, it's recommended to put the script elsewhere.
I choose `.git` folder of `<repo>` because it's inside the same folder, but not tracked.

Note: to speed things up for experimentation `set FILTER_BRANCH_SQUELCH_WARNING=1` before `filter-branch`.
