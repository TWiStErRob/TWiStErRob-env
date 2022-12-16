## Testing
 * Create structure to test in Notion ([example](https://www.notion.so/twisterrob/7f74579b8dd3408bb9e31f6f417ac5c1?v=6b127b0e38c54bd0b36b8ef4cb34da4c))
 * Add connection to database in Notion
 * Import data from CSV:
   ```shell
   kotlinc -script notion-import-csv.main.kts 7f74579b8dd3408bb9e31f6f417ac5c1 notion-import-csv.test.csv
   ```
