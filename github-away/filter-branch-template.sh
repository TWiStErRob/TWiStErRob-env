#!/bin/sh
# Given: a repository you want to rewrite, let's call it <repo>.
# Usage: place this script file in .git folder inside <repo>.
# filter-branch arg: --commit-filter "source $(PWD)/../../.git/commit-filter-foo.sh"
# Working directory (PWD): <repo>/.git-rewrite/t/
# Docs: https://git-scm.com/docs/git-filter-branch

if [ -z "$GIT_COMMIT" ]; then echo "GIT_COMMIT is empty"; exit 1; fi

# Self diagnostics
out_dir=../../.git/filter-branch-diagnostics.~
mkdir -p $out_dir
out=$out_dir/$GIT_COMMIT
rm -f $out
echo Params to script: >> $out
echo "$@" >> $out
echo Env of execution: >> $out
env | sort >> $out

# Actual changes for filter-branch
# Use `echo ... >> $out` to leave a trace.
if [ "$GIT_COMMITTER_EMAIL" = "user@local.host" ];
then
    GIT_COMMITTER_NAME="Róbert Papp (TWiStErRob)";
    GIT_AUTHOR_NAME="Róbert Papp (TWiStErRob)";
    GIT_COMMITTER_EMAIL="papp.robert.s@gmail.com";
    GIT_AUTHOR_EMAIL="papp.robert.s@gmail.com";
    git_commit_non_empty_tree "$@";
else
    git_commit_non_empty_tree "$@";
fi
