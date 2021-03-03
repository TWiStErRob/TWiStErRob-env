#!/bin/bash
# It does not work with normal `sh`, so when using as `--msg-filter`, specify `--msg-filter 'bash svn-msg-filter.sh'`.

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

# Store the sed script in a variable.
# Using heredoc means there's no need to escape things, it's done as a post-processing step.
# Note: s/^ and replacement /... has to be together otherwise the first line is skipped while processing sed input.
read -r -d '' SCRIPT <<-'EOF'
	s/^([^\n]+)
	((\[[A-Z]{3}\] [^\n]+\n)+)
	svn path=(\/[^;]+); revision=([0-9]+)
	/\2[SVN] r\5 \1 in \4
	/
EOF

# Remove trailing new line from the variable, / is the end of the sed script.
# Then replace newlines with continuation new lines so sed treats the whole thing as one script.
SCRIPT=$(echo "$SCRIPT" | sed -z 's/.$//' | sed -z 's/\n/\\\n/g')
# If there are pre-escaped lines:
# sed doesn't support negative lookbehind, so use perl in line-reading-printing mode.
# perl -pe 's/(?<!\\)\n/\\\n/g')

# Run the sed regex replacement on the whole input at once (-z).
sed -z -re "$SCRIPT"
