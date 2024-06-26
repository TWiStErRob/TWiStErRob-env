# My Environment setup & scripts
I have a complex environment setup on Windows, this repository shall contain things that make the environment easily reproducible.

## Contents
 * [caches](caches) files and folders that are emphemeral, and if deleted, they'll regenerate.
 * [config](config) configuration files relocated into a central place.
 * [projects](projects) workspaces for different projects I'm working on.
 * [repos](repos) master copies of files, e.g. SVN server, Maven repository, Artifactory, etc.
 * [scripts](scripts) see [below](#scripts).
 * [secrets](secrets) private keys.
 * [tools](tools) programs, SDK, etc. that do things.

### Scripts
 * [Special](scripts/special)
   * [svn2git-migration](scripts/special/svn2git-migration) A script to migrate from an SVN monorepo into git repository(s).
   * [local-intellij](scripts/special/local-intellij) How to start Android Studio (IntelliJ IDEA) without a trace in a specific directory.
   * [pst-maildir-imap](scripts/special/pst-maildir-imap) A process to convert a .pst file to Maildir format and then sync with an IMAP server.
   * [bitrise-mermaid](scripts/special/bitrise-mermaid) Convert Bitrise configuration to Mermaid diagram.
 * [Git](scripts/git)
   * [github-away](scripts/git/github-away) How to set up GitHub and git on a foreign machine.
   * [merged-branches](scripts/git/merged-branches) Find squashed branches from `git log`.
 * [GitHub](scripts/github)
   * [repository-convention](scripts/github/repository-convention) Self-controlled defaults for GitHub user/orgs.
   * [manage-draft-issues](scripts/github/project-manage-draft-issues) Create/update draft issues in a GitHub project.
   * [user-contribs](scripts/github/user-contribs) Interactive GitHub User Contribution Stats for multiple repositories.
 * [Notion](scripts/notion)
   * [import-data](scripts/notion/import-data) Import data from JSON and CSV into Notion Databases.
   * [move-to-property](scripts/notion/page-section-move-to-property) Move a page section to a property of the page.
   * [database-filter-page-content](scripts/notion/database-filter-page-content) Create a "has content" column based on page content.
 * [Google](scripts/google)
   * [gmail-play-reviews](scripts/google/gmail-play-reviews) Load reviews from emails.

## License
Unlicense, see [.github/LICENSE](.github/LICENSE).
