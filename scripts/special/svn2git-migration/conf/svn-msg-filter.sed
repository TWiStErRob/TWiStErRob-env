#!/bin/sed -z -r -f svn-msg-filter.sed
# sed script (of a single replace command) that
# * works on the whole input at once (-z)
# * uses extended regex syntax (-r)
# * is in a file so it can be documented (-f)

# Expected input on stdin:
#```
#Inventory
#[MOD] TWiStErRob/net.twisterrob.inventory#184 AndroidX Migration 28.0.0 -> 1.0.0
#[REF] Use ApplicationProvider instead of InstrumentationRegistry
#[FIX] New ProGuard notes from AndroidX
#
#svn path=/Projects/Cashier/; revision=2610
#```

# The script will combine the first and last lines into
#```
#[SVN] r2610 Inventory in /Projects/Cashier/
#```

# Note: Each line end has to be unterminated with an escape, so sed handles multiline script correctly.
# Note: s/^ and replacement /... has to be together otherwise the first line is skipped while processing sed input.
s/^([^\n]+)\
((\[[A-Z]{3}\] [^\n]+\n)+)\
svn path=(\/[^;]*); revision=([0-9]+)\
/[SVN] r\5 \1 in \4\
\2\
/
