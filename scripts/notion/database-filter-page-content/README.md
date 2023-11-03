# Notion page content inference

This fills in values of a Checkbox property with the fact that there's content in the page or not.

Strict mode (last parameter) means any content, even if it's just an empty paragraph or a single space.

## Examples

Create/update `Has Content` column with a boolean/checkbox value whether the page has contents.
```shell
kotlinc -script filter-content.main.kts 0123456789abcdef0123456789abcdef "Has Content" true
```
