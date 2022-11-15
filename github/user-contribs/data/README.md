# Get data
## `curl` (fail)

First I started with a batch file `get-com.bat` where I downloaded the contributors for each repo. 
```shell
curl --header "Authorization: Basic BASE64_TOKEN" https://api.github.com/repos/%1/%2/stats/contributors > %1-%2.json
```
`BASE64_TOKEN` is a base64 encoded (`<user>:<pat>`) string of my username and personal access token.

For self-hosted GitHub Enterprise the URL is a bit different. So `get-enterpise.bat` looks like this:
```shell
curl --header "Authorization: Basic BASE64_TOKEN" https://github.mycompany.com/api/v3/repos/%1/%2/stats/contributors > %1-%2.json
```

This solution doesn't work, because sometimes GitHub will respond with a 202 status code. This means that the data is not ready yet.
See https://docs.github.com/en/rest/metrics/statistics#a-word-about-caching.

## `.main.kts`
Because of the error handling and the 202 handling, I opted to go full-Kotlin.
I used Kotlin Scripts so that everything is in one file.
I based the solution on:
 * `.main.kts`: https://kotlinlang.org/docs/custom-script-deps-tutorial.html
 * `HttpClient`: https://ktor.io/docs/http-client-engines.html#java
 * `Jackson`: https://ktor.io/docs/serialization-client.html > Jackson
 * `jackson {}` config: https://www.baeldung.com/jackson-deserialize-json-unknown-properties
 * `Base64`: https://www.baeldung.com/java-base64-encode-and-decode

# Aggregating lot of repos
Then I wrapped these in a get-all.bat file:
```shell
call get-com.bat public-org repo1
call get-com.bat public-org repo2
call get-enterprise.bat on-prem-org repo3
call get-enterprise.bat on-prem-org repo4
```

Of course this list needs to be handwritten if there's a lot of repos, so I used the GitHub API to generate the scripts.
The scripts are targeting Windows Command Line, but easy to change them. `"""` is a single escaped `"` character inside a command line argument.
Note: the limits on the `gh repo list` commands are necessary because the filtering happens on the client side.

## Migration
At the time of writing there was a migration going on from a self-hosted GitHub Enterprise to a public GitHub.com org.
The self-hosted repositories were in multiple orgs, while on GitHub.com there was only one org (`my-cloud-org`).
Each repository had a repository topic for the original self-hosted org, so it can be filtered. 
The migrated repositories in the on-prem were tagged with repository topics (let's call it `my-migrated-topic`).

## Get all non-migrated repos from self-hosted
This will generate a script for all `my-self-hosted-org/my-self-hosted-repo`-style repos.
`all !=` combo is used to simulate an `none ==` function.
```shell
set GH_HOST=github.mycompany.com
gh repo list my-self-hosted-org --json name,repositoryTopics --limit 200 --jq ".[] | select(.repositoryTopics//[] | all(.name != """my-migrated-topic""")) | """call get-enterprise.bat """ + .name" | sort > get-enterprise-all.bat
```

## Get all migrated repos from self-hosted
Note that the script output is targeting GitHub.com because it contains the most up-to-date migrated repo.
```shell
gh repo list my-self-hosted-org --json name,repositoryTopics --limit 200 --jq ".[] | select(.repositoryTopics//[] | any(.name == """my-migrated-topic""")) | """call get-com.bat """ + .name" | sort > get-com-all.bat
```

## Get all migrated repos from GitHub.com
For a specific old self-hosted organization.
```shell
gh repo list my-cloud-org --json name,repositoryTopics --limit 3000 --jq ".[] | select(.repositoryTopics//[] | any(.name == """my-self-hosted-org""")) | """call get-com.bat """ + .name" | sort > get-com-all.bat
```

## How did I get here?
Originally I saw this example somewhere:
```shell
gh repo list my-self-hosted-org --json name,repositoryTopics --limit 200 --jq ".[] | select(.repositoryTopics//[] | all(.name != """my-migrated-topic"""))"
```
So based on this I tried to replace the `--jq` argument with `--template`:
```shell
gh repo list my-self-hosted-org --limit 200 --json name,repositoryTopics --template "{{range .}}{{printf "%s%n%s" .name .name}}{{end}}"
```
which works, but when I tried to add filtering it got way too complex (because of the `contains()` operation):
```shell
gh repo list my-self-hosted-org --limit 300 --json name,repositoryTopics --template "{{range .}}call {{range .repositoryTopics}}{{if eq .name """my-migrated-topic"""}}get-com.bat{{else}}get-enterprise.bat{{break}}{{end}}{{else}}get-enterprise.bat{{end}} {{.name}}{{"""\n"""}}{{end}}" > get.bat
```
I wanted a "simple enough" one-liner, so I went back to the drawing board and used two commands instead of one:
```shell
gh repo list my-self-hosted-org --json name,repositoryTopics --limit 200 --jq ".[] | select(.repositoryTopics//[] | all(.name != """my-migrated-topic""")) | """call get-enterprise.bat """ + .name" | sort > get-enterprise-all.bat
gh repo list my-self-hosted-org --json name,repositoryTopics --limit 200 --jq ".[] | select(.repositoryTopics//[] | any(.name == """my-migrated-topic""")) | """call get-com.bat """ + .name" | sort > get-com-all.bat
```
