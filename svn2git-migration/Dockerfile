FROM svn2git
# This Dockerfile assumes a patched svn2git build, which is `FROM debian:buster`.

# Change locale to let svn handle international characters
ENV LC_ALL C.UTF-8

WORKDIR /workdir
CMD /usr/local/svn2git/svn-all-fast-export
