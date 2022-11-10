# Development setup
This file summarizes the steps to setup a development environment for projects in this repository and may apply to other repositories too.

## Kotlin Scripts

1. Open folder in IntelliJ IDEA
2. Open `....main.kts`
3. _Apply Context_ if necessary
4. If code is red
    * _File > Project Structure..._
    * _Project Settings / Project_
      * _SDK_ select _Java 11+_
      * _Language Level_ select _SDK default_

Then _right click_ the `....main.kts` file to run the script.

Usually without any more setup it'll output what else is missing:
```
Usage: kotlinc -script ....main.kts ...
```

Edit the Kotlin script Run Configuration, usually it'll need some of these:
* Environment variables: `X=x;Y=y`
* Program arguments: `-a "b c" -d e`


## GitHub GraphQL

 1. Get auth token from GitHub
    * [Top right corner > Settings > Developer settings > Personal access tokens > Classic](https://github.com/settings/tokens).
    * Generate new token, classic.
    * Enter sudo mode by authenticating (password or 2FA).
    * Select the relevant scopes.
    * Generate token and save it somewhere secure.
    * In the browser URL bar enter `javascript:` +
    ```
    alert(btoa(`${prompt("GitHub Username")}:${prompt("Personal Access Token")}`))
    ```
    or just execute the above code in Chrome Developer Tools Console.  
    (This might sound weird, but this is a safe way to call encode Base64 without getting saved into recents history.)
    * Input data and copy the alert dialog contents and save it somewhere secure.

 2. Download [GraphQL plugin](https://plugins.jetbrains.com/plugin/8097-graphql).
    * File > Settings > Plugins > Marketplace > GraphQL
    * Install (and restart IDE, if asks).
    * This should pick up `.graphqlconfig`.
    * Open View > Tool Windows > GraphQL.
    * GitHub API > Endpoints > GitHub API > double-click > Get GraphQL Schema from Endpoint.

 3. Execute queries
    * Open a query or scratch query with the above `.graphqlconfig`.
    * Press <kbd>Ctrl</kbd>+<kbd>Enter</kbd> to run it.
    * If it asks for `GITHUB_AUTH_TOKEN`, paste the Base64 token from the alert dialog.
    * If it's annoying to do this on every execution, create an environment variable and restart the IDE.
