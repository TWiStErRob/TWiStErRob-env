## Data gathering

1. Open https://www.siliconmilkroundabout.com/companies and search `var app_config`,
   it'll have `event_id:`. Use that to save `companies-attending_event_id=<event_id>.json` from:  
   https://www.siliconmilkroundabout.com/api/companies-attending?event_id=<event_id>
    ```json5
    //...,
    {
    	"name": "Bumble",
    	"other": null,
    	"slug": "Bumble",
    	"web_logo": "615-uByJRaRGD9gw8gAsAVu1CovRvIBWSaQ4OiE_5I8Ugt8.svg",
    	"companyid": 4233,
    	"video": "",
    	"description": "Bumble Inc. is the parent company of Bumble, Badoo, and Fruitz. The Bumble platform enables people to connect and build equitable and healthy relationships.",
    	"attending": { "65": true, "66": true, "67": false },
    	"stand_numbers": "68,68",
    	"jobcount": "14",
    	"jobtitles": "{\"title\": \"Payments Compliance Programme Manager\", \"role_type_id\":54064}|JOB|{\"title\": \"Release Engineer\", \"role_type_id\":54004}|JOB|{\"title\": \"Senior Graphic Designer\", \"role_type_id\":53976}|JOB|{\"title\": \"Site Reliability Engineer (Linux/GCP)\", \"role_type_id\":57094}|JOB|{\"title\": \"Art Director\", \"role_type_id\":53979}|JOB|{\"title\": \"Graphic Designer \", \"role_type_id\":53976}|JOB|{\"title\": \"Lead Mobile DevOps\", \"role_type_id\":54004}|JOB|{\"title\": \"Payments Reporting Specialist\", \"role_type_id\":54064}|JOB|{\"title\": \"Senior iOS Platform Engineer\", \"role_type_id\":54005}|JOB|{\"title\": \"Senior Mobile DevOps Engineer \", \"role_type_id\":54004}|JOB|{\"title\": \"Senior PHP Engineer\", \"role_type_id\":54002}|JOB|{\"title\": \"Senior iOS Engineer\", \"role_type_id\":54005}|JOB|{\"title\": \"Senior Product Manager\", \"role_type_id\":53995}|JOB|{\"title\": \"Product Marketing\", \"role_type_id\":53989}",
    	"category_filter": "53967,53968,53969,53970",
    	"jobtags": "{\"name\": \"Docker\", \"approved\": \"t\", \"id\":23279}|TAG|{\"name\": \"Go\", \"approved\": \"t\", \"id\":184}|TAG|{\"name\": \"Kubernetes\", \"approved\": \"t\", \"id\":36992}|TAG|{\"name\": \"PHP\", \"approved\": \"t\", \"id\":217}|TAG|{\"name\": \"Branding\", \"approved\": \"t\", \"id\":174}|TAG|{\"name\": \"Canva\", \"approved\": \"t\", \"id\":54059}|TAG|{\"name\": \"Design\", \"approved\": \"t\", \"id\":186}|TAG|{\"name\": \"Marketing\", \"approved\": \"t\", \"id\":216}|TAG|{\"name\": \"Linux\", \"approved\": \"t\", \"id\":202}|TAG|{\"name\": \"MySQL\", \"approved\": \"t\", \"id\":209}|TAG|{\"name\": \"Python\", \"approved\": \"t\", \"id\":222}|TAG|{\"name\": \"Terraform \", \"approved\": \"t\", \"id\":34267}|TAG|{\"name\": \"Branding\", \"approved\": \"t\", \"id\":174}|TAG|{\"name\": \"Canva\", \"approved\": \"t\", \"id\":54059}|TAG|{\"name\": \"Design\", \"approved\": \"t\", \"id\":186}|TAG|{\"name\": \"Marketing\", \"approved\": \"t\", \"id\":216}|TAG|{\"name\": \"Branding\", \"approved\": \"t\", \"id\":174}|TAG|{\"name\": \"Canva\", \"approved\": \"t\", \"id\":54059}|TAG|{\"name\": \"Design\", \"approved\": \"t\", \"id\":186}|TAG|{\"name\": \"Marketing\", \"approved\": \"t\", \"id\":216}|TAG|{\"name\": \"Java\", \"approved\": \"t\", \"id\":192}|TAG|{\"name\": \"Kotlin\", \"approved\": \"t\", \"id\":40363}|TAG|{\"name\": \"Kubernetes\", \"approved\": \"t\", \"id\":36992}|TAG|{\"name\": \"Ruby\", \"approved\": \"t\", \"id\":226}|TAG|{\"name\": \"SQL\", \"approved\": \"t\", \"id\":240}|TAG|{\"name\": \"Swift    \", \"approved\": \"t\", \"id\":35131}|TAG|{\"name\": \"Docker\", \"approved\": \"t\", \"id\":23279}|TAG|{\"name\": \"Java\", \"approved\": \"t\", \"id\":192}|TAG|{\"name\": \"Kotlin\", \"approved\": \"t\", \"id\":40363}|TAG|{\"name\": \"PHP\", \"approved\": \"t\", \"id\":217}|TAG|{\"name\": \"Ruby\", \"approved\": \"t\", \"id\":226}|TAG|{\"name\": \"PHP\", \"approved\": \"t\", \"id\":217}|TAG|{\"name\": \"SQL\", \"approved\": \"t\", \"id\":240}|TAG|{\"name\": \"Swift    \", \"approved\": \"t\", \"id\":35131}|TAG|{\"name\": \"Advertising\", \"approved\": \"t\", \"id\":155}|TAG|{\"name\": \"Product marketing\", \"approved\": \"t\", \"id\":250}"
    }
    ```
2. Grab all valid company IDs:
    ```shell
    jq ".[].companyid" "companies-attending_event_id=23.json" > data/urls.txt
    ```
3. Search: `\d+` and Replace in `data/urls.txt`:  
    ```
    https://www.siliconmilkroundabout.com/api/get/jobs?id=$0
    https://www.siliconmilkroundabout.com/api/get/company/social?id=$0
    https://www.siliconmilkroundabout.com/api/get/company-child?id=$0
    ```
4. Download all files
    ```shell
    data$ wget -i urls.txt
    ```
    `company-child*.json`
    ```json
    []
    ```
    `jobs*.json`
    ```json
    [
    	{"id":10528,"company_id":4233,"primary_category_tag_id":54004,"number_of_vacancies":2,"title":"Senior Mobile DevOps Engineer ","salary_from":"","salary_to":"","equity_from":"","equity_to":"","experience_level_from_tag_id":30755,"experience_level_to_tag_id":30755,"url":"https://bumble.wd3.myworkdayjobs.com/en-US/Bumble_Careers/job/London/Senior-Mobile-DevOps-Engineer_JR0979?q=devops&utm_source=seniordevops&utm_medium=seniordevops&utm_campaign=seniordevops&utm_id=SMR22","visa":true,"paid_relocation":true,"remote":false,"hybrid":true,"bonus":true,"equity":true,"job_type":"full_time","job_location":"London, Barcelona","hidden_dttm":null,"tags":"{\"name\": \"Docker\", \"approved\": \"t\", \"id\":23279}|TAG| {\"name\": \"Java\", \"approved\": \"t\", \"id\":192}|TAG| {\"name\": \"Kotlin\", \"approved\": \"t\", \"id\":40363}|TAG| {\"name\": \"PHP\", \"approved\": \"t\", \"id\":217}|TAG| {\"name\": \"Ruby\", \"approved\": \"t\", \"id\":226}"},
    	{"id":10529,"company_id":4233,"primary_category_tag_id":54004,"number_of_vacancies":1,"title":"Lead Mobile DevOps","salary_from":"","salary_to":"","equity_from":"","equity_to":"","experience_level_from_tag_id":53964,"experience_level_to_tag_id":53964,"url":"https://bumble.wd3.myworkdayjobs.com/en-US/Bumble_Careers/job/London/Lead-Mobile-DevOps-Engineer_JR1082-1?q=devops&utm_source=leaddevops&utm_medium=leaddevops&utm_campaign=leaddevops&utm_id=SMR22","visa":true,"paid_relocation":true,"remote":false,"hybrid":true,"bonus":true,"equity":true,"job_type":"full_time","job_location":"London, Barcelona","hidden_dttm":null,"tags":"{\"name\": \"Java\", \"approved\": \"t\", \"id\":192}|TAG| {\"name\": \"Kotlin\", \"approved\": \"t\", \"id\":40363}|TAG| {\"name\": \"Kubernetes\", \"approved\": \"t\", \"id\":36992}|TAG| {\"name\": \"Ruby\", \"approved\": \"t\", \"id\":226}"},
    	{"id":10530,"company_id":4233,"primary_category_tag_id":54004,"number_of_vacancies":1,"title":"Release Engineer","salary_from":"","salary_to":"","equity_from":"","equity_to":"","experience_level_from_tag_id":30755,"experience_level_to_tag_id":30755,"url":"https://bumble.wd3.myworkdayjobs.com/en-US/Bumble_Careers/job/London/Release-engineer_JR0323?q=devops&utm_source=releaseeng&utm_medium=releaseeng&utm_campaign=releaseeng&utm_id=SMR22","visa":true,"paid_relocation":true,"remote":false,"hybrid":true,"bonus":true,"equity":true,"job_type":"full_time","job_location":"London, Barcelona","hidden_dttm":null,"tags":"{\"name\": \"Docker\", \"approved\": \"t\", \"id\":23279}|TAG| {\"name\": \"Go\", \"approved\": \"t\", \"id\":184}|TAG| {\"name\": \"Kubernetes\", \"approved\": \"t\", \"id\":36992}|TAG| {\"name\": \"PHP\", \"approved\": \"t\", \"id\":217}"},
    	{"id":10531,"company_id":4233,"primary_category_tag_id":54005,"number_of_vacancies":3,"title":"Senior iOS Platform Engineer","salary_from":"","salary_to":"","equity_from":"","equity_to":"","experience_level_from_tag_id":30755,"experience_level_to_tag_id":30755,"url":"https://bumble.wd3.myworkdayjobs.com/en-US/Bumble_Careers/details/Senior-iOS-Platform-Engineer_JR1058?q=Platform&utm_source=iosplat&utm_medium=iosplat&utm_campaign=iosplat&utm_id=SMR22","visa":true,"paid_relocation":true,"remote":true,"hybrid":true,"bonus":true,"equity":true,"job_type":"full_time","job_location":"London, Barcelona","hidden_dttm":null,"tags":"{\"name\": \"Swift    \", \"approved\": \"t\", \"id\":35131}"},
    	{"id":10533,"company_id":4233,"primary_category_tag_id":54005,"number_of_vacancies":2,"title":"Senior iOS Engineer","salary_from":"","salary_to":"","equity_from":"","equity_to":"","experience_level_from_tag_id":30755,"experience_level_to_tag_id":30755,"url":"https://bumble.wd3.myworkdayjobs.com/en-US/Bumble_Careers/details/Senior-iOS-Engineer_JR0187?q=iOS&utm_source=seniorios&utm_medium=seniorios&utm_campaign=seniorios&utm_id=SMR22","visa":true,"paid_relocation":true,"remote":false,"hybrid":true,"bonus":true,"equity":true,"job_type":"full_time","job_location":"London, Barcelona","hidden_dttm":null,"tags":"{\"name\": \"Swift    \", \"approved\": \"t\", \"id\":35131}"},
    	{"id":10545,"company_id":4233,"primary_category_tag_id":57094,"number_of_vacancies":1,"title":"Site Reliability Engineer (Linux/GCP)","salary_from":"","salary_to":"","equity_from":"","equity_to":"","experience_level_from_tag_id":30755,"experience_level_to_tag_id":30755,"url":"https://bumble.wd3.myworkdayjobs.com/Bumble_Careers/job/London/Site-Reliability-Engineer--Linux-GCP-_JR0166?utm_source=sitereliability&utm_medium=sitereliability&utm_campaign=sitereliability&utm_id=SMR22","visa":true,"paid_relocation":true,"remote":false,"hybrid":true,"bonus":true,"equity":true,"job_type":"full_time","job_location":"London, Barcelona","hidden_dttm":null,"tags":"{\"name\": \"Linux\", \"approved\": \"t\", \"id\":202}|TAG| {\"name\": \"MySQL\", \"approved\": \"t\", \"id\":209}|TAG| {\"name\": \"Python\", \"approved\": \"t\", \"id\":222}|TAG| {\"name\": \"Terraform \", \"approved\": \"t\", \"id\":34267}"}
    ]
    ```
    `social*.json`
    ```json
    [{"social_urls":{"twitter":"BumbleEng","facebook":"","linkedin":"bumble","instagram":""}}]
    ```
5. Pre-process json (clean up contract)
    ```shell
    jq -f companies-attending.jq 'companies-attending_event_id=23.json' > companies-attending.json
    ```
6. Interpretation  
   See [`Mappings` in kts](companies-attending.main.kts).
   Review `val Mappings.attending` based on `companies-attending.json`'s `attending` fields.
7. Convert to table (csv).  
   See [`main` in kts](companies-attending.main.kts).
   ```shell
   kotlinc -script companies-attending.main.kts
   ```
8. Import to Notion:
   Create two databases in Notion with the properties as listed in the CSV files.
   ```shell
   kotlinc -script notion-import-csv.main.kts <database-id1> companies-attending.csv
   kotlinc -script notion-import-csv.main.kts <database-id2> companies-attending-jobs.csv
   ```
