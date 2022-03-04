@rem %1 = rule file to execute from conf, must exist
@rem %2 = workdir suffix
@rem %3 = min revision
@rem %4 = max revision
set SVN_REPO=p:\repos\svn
set SVN_REPO_SLASH=p:/repos/svn
docker run --rm -it -v %CD%\conf:/tmp/conf -v %CD%\workdir%2:/workdir -v %SVN_REPO%:/tmp/svn svn2git-work bash -c "/usr/local/svn2git/svn-all-fast-export --resume-from %3 --max-rev %4 --identity-map /tmp/conf/migrate.authors --rules /tmp/conf/%1 --debug-rules --stats --svn-ignore --propcheck --empty-dirs --add-metadata --add-metadata-notes --msg-filter 'sed -z -r -f /tmp/conf/svn-msg-filter.sed' /tmp/svn >svn2git.log 2>&1"
