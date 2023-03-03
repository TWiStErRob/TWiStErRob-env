# Notion refactoring

This refactors a Notion page by moving a page section (between a heading and the next heading/end of the page) to a property of the page.

Note: at the moment all headings are treated equal.

## Examples

Move "Abstract" section contents to property "Abstract.
```shell
kotlinc -script notion-move-to-property.kts 0123456789abcdef0123456789abcdef Abstract Abstract
```

Move "Bio at DroidCon 2022" section contents to property "Bio"
```shell
kotlinc -script notion-move-to-property.kts 0123456789abcdef0123456789abcdef "Bio at DroidCon 2022" Bio
```
