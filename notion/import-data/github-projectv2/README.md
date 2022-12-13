# GitHub Project export to Notion

I started managing my things in a GitHub project,
but then found Notion and realized it's a much better way, because I can create my own structure more freely.

## Access

GitHub GraphQL queries require the following scopes:
 * `read:project` to read draft issues from a project.
 * `repo` to read issues in private repos.

Notion import needs a Connection with read/write access.

See [DEVELOPMENT.md](../../../docs/DEVELOPMENT.md) for more information.

## Steps
 1. [Query](ProjectV2Items.graphql) a few times to get all items.
    * See [DEVELOPMENT.md](../../../docs/DEVELOPMENT.md) how.
    * Look at `data.user.projectV2.items.hasNextPage` and pass in the `endCursor` to the next query.
    * Saved files as `output<n>.json`.
 2. Merge items:
    ```shell
    jq -s "[ .[].data.user.projectV2.items.nodes[] ]" output1.json output2.json > items.json
    ```
 3. Process items to CSV:
    ```shell
    jq -r -f process.jq items.json > items.csv
    ```
 4. Create Notion Database and schema (properties).
 5. [Import CSV to Notion](../notion-import-csv.main.kts)
