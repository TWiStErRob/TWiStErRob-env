FROM svn2git

# Update Subversion (svn) command to 1.9 so it's compatible with my repo.
# https://oitibs.com/subversion-1-9-on-debian-jessie/
RUN apt-get update && apt-get install --yes --no-install-recommends wget
# download the GPG key of the repository and add it to apt:
RUN wget http://opensource.wandisco.com/wandisco-debian.gpg -O /tmp/wandisco-debian.gpg
# add downloaded key to apt
RUN apt-key add /tmp/wandisco-debian.gpg
# create apt sources file for wandisco repo
RUN echo "deb http://opensource.wandisco.com/debian/ jessie svn19" > /etc/apt/sources.list.d/wandisco.sources.list
# update apt and upgrade packages
RUN apt-get update && apt-get install --yes --no-install-recommends subversion subversion-tools

# Change locale to let svn handle international characters
ENV LC_ALL C.UTF-8

WORKDIR /workdir
CMD /usr/local/svn2git/svn-all-fast-export
