1. _Search_ Duplicate files in Total Commander
2. _Feed to listbox_
3. Select all files with `*`
4. _Mark > Copy Names With Path To Clipboard_
5. Paste to text file: `files.txt`
6. `kotlinc -script match_groups.main.kts files.txt > groups.txt`
7. `kotlinc -script render_groups.main.kts groups.txt > groups.dot`
