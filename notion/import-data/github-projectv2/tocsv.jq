# Convert to CSV (https://stackoverflow.com/q/32960857/253468), use `-r` on CLI.
def tocsv:
	if
		length == 0
	then
		empty
	else
		.
		| (
			(.[0] | keys_unsorted) as $firstkeys
			| (map(keys) | add | unique) as $allkeys
			| $firstkeys + ($allkeys - $firstkeys)
		) as $columns
		| (
			map(
				. as $row
				| $columns
				# Do not tostring here, because it'll convert "null", which would be omitted otherwise.
				# https://stackoverflow.com/questions/32960857#comment131985587_44012345
				| map(. as $column | $row[$column])
			)
		) as $rows
		| $columns, $rows[]
		| @csv
	end
;
