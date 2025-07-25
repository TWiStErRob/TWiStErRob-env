@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.opencsv:opencsv:5.9")

import com.opencsv.CSVReader
import com.opencsv.CSVWriterBuilder
import java.io.StringWriter

@Suppress("SpreadOperator")
main(*args)

fun java.io.File.readCSV(): List<Array<String>> =
	reader().use { reader ->
		CSVReader(reader).use { csvReader ->
			csvReader.readAll()
		}
	}

fun List<Array<out String?>>.toCSV(): String =
	StringWriter().use { writer ->
		val rows = this.map { row ->
			row.map { it ?: "" }.toTypedArray()
		}
		CSVWriterBuilder(writer).build().writeAll(rows)
		writer.toString()
	}

fun main(vararg args: String) {
	check(args.size == 1) { "Usage: kotlinc -script planmyfringe-to-fringelog.main.kts <csvFileName>" }
	val csv = java.io.File(args[0]).readCSV()
	// Date,Name,Rating,W alk to Show,Price(£),Start Time,End Time,Duration,Venue
	val headers = csv[0]
	val data = csv.drop(1)
	val rows = data.map { row ->
		headers.zip(row).toMap().mapValues { it.value.ifBlank { null } }
	}
	val mappedHeaders = arrayOf("Name", "Performer", "Price", "Time", "Rating", "Location", "Type", "Festival", "Link")
	val mapped = rows.map { row ->
		val name = row["Name"]
		val splitName = name!!.split(": ").takeIf { it.size == 2 }
		val title = splitName?.last() ?: name
		val performer = splitName?.first()
		val price = row["Price(£)"]
		// Date: "Wed 08-12", Start Time: "9:30" transforms to "2024-08-12 9:30 -> 2024-08-12 15:30"
		val time = row["Date"].let { date ->
			val date = "2024-${date!!.substring(5, 7)}-${date.substring(8)}"
			val startDateTime = "${date} ${row["Start Time"]}"
			val endDateTime = "${date} ${row["End Time"]}"
			"$startDateTime -> $endDateTime"
		}
		val rating = ""
		val location = row["Venue"]
		val type = when(name) {
			"Work" -> "Work"
			else -> "Performance"
		}
		val festival = "Edinburgh Fringe 2024"
		val link = ""
		arrayOf(title, performer, price, time, rating, location, type, festival, link)
	}
	println((listOf(mappedHeaders) + mapped).toCSV())
}
