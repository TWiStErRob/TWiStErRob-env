## Data

See data sources in [`descript.ion`](descript.ion) file.

Retrieval was based on [Constants.kt][constants] and [DefaultApiDataSource.kt][api].

[api]: https://github.com/touchlab/DroidconKotlin/blob/e2f833a9075c2d92f1aff931b5d651a58ff8bf1b/shared/src/commonMain/kotlin/co/touchlab/droidcon/domain/service/impl/DefaultApiDataSource.kt
[constants]: https://github.com/touchlab/DroidconKotlin/blob/e2f833a9075c2d92f1aff931b5d651a58ff8bf1b/shared/src/commonMain/kotlin/co/touchlab/droidcon/Constants.kt

## Steps

1. Download data based on `descript.ion` + constants.
2. Edit `notion-import-droidcon.main.kts` to point to the right database / pages.
3. Use [notion-import-droidcon](../notion-import-droidcon.main.kts) to import data into Notion.
