setlocal
set GITHUB_HOST=github.mycompany.com
kotlinc -script get.main.kts my-self-hosted-org %1
