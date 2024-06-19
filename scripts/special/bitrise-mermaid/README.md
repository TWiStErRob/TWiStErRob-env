# Bitrise to Mermaid

This script converts a Bitrise configuration file to a Mermaid diagram.
It shows the dependencies between workflows and triggers.

```shell
kotlin bitrise-mermaid.main.kts bitrise.yml > bitrise.mermaid
```

Filter only some workflows, e.g. `ci`:

```shell
kotlin bitrise-mermaid.main.kts bitrise.yml ci > bitrise.mermaid
```

Note: multiple is possible.
