# Git merged branch listing

Helper script for finding which branches have been merged already to master when using squash merging workflow.

It looks for `foo/BAR-123...` branch naming convention.
 * Where `foo` can be the type of the branch (`fix`/`feature`) or the username of the developer,
 * and `BAR-123` is the ticket number;
 * `...` is the name of the branch and is optional.

This script will not delete any branches, just outputs a shell script that will. It's up to the user to run it.

## Development

This is a Kotlin Script. See [DEVELOPMENT.md](../../docs/DEVELOPMENT.md) for more information.
