After the [dropped support for mobile](https://blog.lastpass.com/2021/02/changes-to-lastpass-free/) and
the [incidents](https://arstechnica.com/information-technology/2022/12/lastpass-says-hackers-have-obtained-vault-data-and-a-wealth-of-customer-info/)
it's time to look for a new Password manager. Since I own Google devices and use Chrome, it makes sense to get the
passwords into Google, so they're just one tap away, everywhere.

Google Chrome finally added the [Note field](https://9to5google.com/2023/06/30/add-notes-to-passwords-chrome/), so
there's nothing stopping me from moving.

## Issues found during Lastpass export:

 * When exported from the website:
   * the downloaded CSV file is [truncated at the first `#`](https://community.logmein.com/t5/LastPass-Support-Discussions/CSV-export-bug/m-p/283647)  
     Workaround: copy-paste from browser (because it also outputs visually).
   * Some fields contain `0x10` (16, DLE Data Link Escape) character. They all seem to be from 2019 H1.
     Workaround: replace with empty string manually.
   * Some records will have http URLs, while from the same account the Chrome extension writes https URLs.
     Workaround: synchronize two variants.
   * Some records were missing their name field compared to the Chrome extension export.
     Workaround: synchronize two variants.
 * When exported from the Chrome Extension:
   * the generated CSV file is not escaped, so multiline notes count as separate entries.  
     Workaround: open in text editor and fix the data before using Excel, be careful if the "Note" column contains a quote sign, that needs to be escaped as `""`.
   * the generated CSV file is not escaped, so if a URL or note contains a comma, there are more columns than necessary.  
     Workaround: open in Excel and fix the data.
   * record ordering is totally different from the website, website keeps date order.
 * Excel
   * can't handle numbers well, if anything starts with a 0, or +, or is long number, it'll mess it up.
     Workaround: make a note and fix before import.o

## Issues found during Google import:
 * Google doesn't overwrite existing entries, so duplicates will be skipped.
   This means changing the exported CSV and re-importing won't work.
   Workaround: delete changed entries before re-importing.
 * Google doesn't have a "Delete all" button, so bulk changes are infeasible.
   Workaround: use the [Google Passwords page](https://passwords.google.com/) and delete them one by one.
 * Exported CSV from Google has "random" order (probably hash-backed)
   Workaround: use pass.main.kts to sort the CSV before comparing.
 * Re-importing the same export from Google yields problems: namely App credentials don't get exported, so import sees empty/changed password which leads to conflict: "A password is already saved"
