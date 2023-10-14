# Review email scraper.
Grabs reviews from emails stored in GMail.

## Auth
Create a desktop application OAuth credential in any project in the Developer Console to use this.

## Notes

### URL Parsing
It parses the "Reply" button's URL, which has some changing structure over the years.

There's a mapping between old and new reviews:
```
https://play.google.com/apps/publish?account=${accountId}#ReviewDetailsPlace:p=${appName}&reviewid=${reviewid}
```
->
```
https://play.google.com/console/u/0/developers/${accountId}/app/${appId}/user-feedback/review-details?reviewId=${reviewid}&corpus=PUBLIC_REVIEWS
```

### Review ID format
The old review ID format `gp:[A-Za-z0-9_-]+` still works with the new URL structure.
The new review ID format (GUID) has the same URL structure as the new one.
The new review ID format was introduced around 2022 June in the "A user has written a new review for ... on ..." emails.

## Official review downloads

```
gsutil cp -m -r gs://pubsite_prod_rev_.../reviews/ .
```
