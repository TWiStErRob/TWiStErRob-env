import "./tocsv" as tocsv;

[
	.[]
	# Merge .content and .fieldValues together into one object.
	| (
		.content
		* (
			# Transform fieldValues[] to an object keyed by field name.
			[ .fieldValues.nodes[] | { key: .field.name, value: .value } ] | from_entries
		)
	)
	| del(.title, .createdAt, .updatedAt)
	# Transform resulting object to simplify.
	| (
		.
		* {
			# Optional Labels property, fall back to no labels and pluck out names.
			"Labels": [ (.Labels.nodes // [])[].name ] | join(","),
			"Repository": .Repository.nameWithOwner,
			"Linked pull requests": [ (."Linked pull requests".nodes // [])[].url ] | join("\n"),
			"Milestone": .Milestone.title,
			"Assignees": [ (.Assignees.nodes // [])[].login ] | join(","),
			"body": (.body | gsub("\r\n"; "\n")),
		}
	)
	# Remove empty properties.
	| del(.. | nulls)
]
| tocsv::tocsv
