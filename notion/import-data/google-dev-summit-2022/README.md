## Data

See data sources in [`descript.ion`](descript.ion) file.

### abstract.csv
```javascript
const data = $$('devsite-expandable')
    .map((it) => ({
        // The expander's title is the clickable part.
        "title": $('.exw-control > h4', it).innerText,
        // "Watch now" button on top right (optional, so provide fallback)
        "youtube": ($('.android-watch-now-link > a', it) ?? {href: null})
                .href
                // Unshorten youtu.be links to get consistent style links.
                ?.replace("https://youtu.be/", "https://www.youtube.com/watch?v="),
        // The expander contains the title and the "Watch now", but any <p> after those is the abstract.
        "abstract": $$('.exw-control ~ p:not(.android-watch-now-link)', it)
                .map((it) => it.innerText).join("\n\n"),
}));
const rows = data.map((it) =>
    // Add quotes around fields with spaces and commas inside, and escape quotes, if any.
    `"${it.title.replace('"', '""')}",${it.youtube??""},"${it.abstract.replace('"', '""')}"`
)
rows.unshift(`Name,Online Content,Abstract`)
console.log(rows.join("\n"));
```
