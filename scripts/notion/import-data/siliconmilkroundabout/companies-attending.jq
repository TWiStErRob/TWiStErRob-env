[
	.[] | . * {
		jobtags: (.jobtags // "") | split("|TAG|") | map(fromjson),
		jobtitles: (.jobtitles // "") | split("|JOB|") | map(fromjson),
		video: (.video // "")
			| sub("^.*?\\|video\\|(?<url>https://.*)$"; "\(.url)")
			| sub("^youtube\\|video\\|(?<id>.*?)$"; "https://www.youtube.com/watch?v=\(.id)")
			| sub("^vimeo\\|video\\|(?<id>.*?)$"; "https://vimeo.com/\(.id)"),
		category_filter: (.category_filter // "") | split(",") | map(tonumber),
		stand_numbers: (.stand_numbers // "") | split(",") | map(tonumber),
		jobcount: (.jobcount // "0") | tonumber,
		web_logo: "https://static.siliconmilkroundabout.com/prod/logos/\(.companyid)/\(.web_logo)",
	}
]
