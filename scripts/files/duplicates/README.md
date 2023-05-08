1. _Search_ Duplicate files in Total Commander
2. _Feed to listbox_
3. Select all files with `*`
4. _Mark > Copy Names With Path To Clipboard_
5. Paste to text file: `files.txt`
6. `kotlinc -J-Xmx4G -script match_groups.main.kts files.txt > groups.txt`
7. `kotlinc -J-Xmx4G -script render_groups.main.kts groups.txt > groups.graph`
8. Render .graph as visual file:
```
dot.exe groups.graph -Tdot > groups.dot
dot.exe groups.graph -Tpdf > groups.pdf
dot.exe groups.graph -Tpng > groups.png
dot.exe groups.graph -Tps > groups.ps
dot.exe groups.graph -Tsvg > groups.svg
```
