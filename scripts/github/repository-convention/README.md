# GitHub Repository Convention diagnostics

Having many repos means that when there's a new setting the adoption is slow, and it can be confusing which repo has what settings. This project is meant to help maintain a healthy setup between repositories' settings.

## Usage
```shell
set GITHUB_USER=<user>
set GITHUB_TOKEN=ghp_...
kotlinc -script validate.main.kts <org or user> > my.output.json
```

## Development

This is a Kotlin Script + GitHub GraphQL with `public_repo`, `admin:org`, `read:user` and `read:discussion` scopes.
See [DEVELOPMENT.md](../../../.github/DEVELOPMENT.md) for more information.
