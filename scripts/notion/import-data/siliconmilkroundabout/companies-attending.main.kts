// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
// TODEL https://youtrack.jetbrains.com/issue/KT-47384 cannot use kotlinx-serialization...
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

@Suppress("SpreadOperator")
main(*args)

@Suppress("LongMethod", "ComplexMethod")
fun main(vararg args: String) {
	check(args.isEmpty()) { "No arguments expected." }
	val jsonMapper = jsonMapper {
		addModule(kotlinModule())
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	}
	val companies: List<Company> = jsonMapper.readValue(File("companies-attending.json"))
	@Suppress("SimplifiableCallChain")
	companies
		.map { c ->
			println("Processing ${c.companyid}: ${c.name}")
			val social: Social.SocialUrls = jsonMapper.readValue<List<Social>>(File("data/social\uF03Fid=${c.companyid}"))
				.map { it.social_urls.clean() }
				.single()
			val socials =
				"${social.twitter ?: ""},${social.facebook ?: ""},${social.linkedin ?: ""},${social.instagram ?: ""}"
			val stands = c.stand_numbers.joinToString(",")
			val jobtags = c.jobtags
				.filter { it.approved }
				.distinct()
				.joinToString(",") { it.name.trim() }
			val attending = c.attending
				.filterValues { it }
				.keys
				.map { Mappings.attending[it] }
				.joinToString(",")
			val jobcats = c.category_filter
				.map { Mappings.roleGroupTag[it] }
				.joinToString(",")
			@Suppress("MaxLineLength")
			"""${c.companyid},${esc(c.name)},${esc(c.description)},${c.web_logo},${c.web_logo},${c.video},"${stands}","${jobtags}","${jobcats}","${attending}",${socials}"""
		}
		.joinToString(
			separator = "\n",
			prefix =
			"ID,Name,Description,Logo,icon,Video,Stands,Tags,Job Categories,Attending,Twitter,Facebook,LinkedIn,Instagram\n"
		)
		.let { File("companies-attending.csv").writeText(it) }
	companies
		.flatMap { c ->
			val jobs: List<Job> = jsonMapper.readValue(File("data/jobs\uF03Fid=${c.companyid}"))
			jobs.map { j ->
				println("Processing ${c.companyid}: ${c.name} - ${j.id}: ${j.title}")
				val cat = Mappings.jobRoleName[j.primary_category_tag_id] ?: "Unknown ${j.primary_category_tag_id}"
				val type = listOfNotNull(
					if (j.bonus) "Bonus" else null,
					if (j.hybrid) "Hybrid" else null,
					if (j.equity) "Equity" else null,
					if (j.visa) "Visa" else null,
					if (j.remote) "Remote" else null,
					if (j.paid_relocation) "Relocation" else null,
					Mappings.jobTypes[j.job_type],
				).joinToString(",")
				val tags = j.tags
					.split("|TAG|")
					.filterNot { it == "{\"name\": \"\", \"approved\": \"\", \"id\":}" }
					.map { jsonMapper.readValue<Company.JobTag>(it) }
					.filter { it.approved }
					.joinToString(",") { it.name }
				val equity = range(j.equity_from, j.equity_to)
				val salary = range(j.salary_from, j.salary_to)
				@Suppress("MaxLineLength")
				"""${esc(j.title)},${esc(c.name)},${cat},"${type}",${j.number_of_vacancies},${esc(j.url)},"${tags}",${esc(j.job_location)},${equity},${salary}"""
			}
		}
		.joinToString(
			separator = "\n",
			prefix = "Title,Company,Category,Type,Vacancies,URL,Tags,Location,Equity,Salary\n"
		)
		.let { File("companies-attending-jobs.csv").writeText(it) }
}

fun esc(value: String): String =
	""""${value.replace("\"", "\"\"")}""""

fun range(from: String, to: String): String =
	if (from.isNotBlank() || to.isNotBlank())
		"${from.replace(",", "")} - ${to.replace(",", "")}"
	else
		""

fun Social.SocialUrls?.clean(): Social.SocialUrls =
	if (this == null) {
		Social.SocialUrls(
			twitter = null,
			facebook = null,
			linkedin = null,
			instagram = null
		)
	} else {
		Social.SocialUrls(
			twitter = twitter?.takeIf { it.isNotBlank() }?.let { "https://twitter.com/${it}" },
			linkedin = linkedin?.takeIf { it.isNotBlank() }?.let { "https://www.linkedin.com/company/${it}" },
			instagram = instagram?.takeIf { it.isNotBlank() }?.let { "https://instagram.com/${it}" },
			facebook = facebook?.takeIf { it.isNotBlank() }?.let { "https://facebook.com/${it}" },
		)
	}

@Suppress("PropertyName", "ConstructorParameterNaming")
data class Company(
	val name: String,
	val slug: String,
	val web_logo: String,
	val companyid: Int,
	val video: String,
	val description: String,
	val attending: Map<Int, Boolean>,
	val stand_numbers: List<Int>,
	val jobcount: Int,
	val jobtitles: List<JobTitle>,
	val category_filter: List<Int>,
	val jobtags: List<JobTag>,
) {
	data class JobTitle(
		val title: String,
		val role_type_id: Int,
	)

	data class JobTag(
		val name: String,
		@JsonDeserialize(converter = BooleanConverter::class)
		val approved: Boolean,
		val id: Int,
	)

	class BooleanConverter : StdConverter<String, Boolean>() {
		override fun convert(value: String): Boolean =
			when (value) {
				"t" -> true
				"f" -> false
				else -> error("Cannot convert $value to Boolean")
			}
	}
}

@Suppress("PropertyName", "ConstructorParameterNaming")
data class Social(
	val social_urls: SocialUrls?,
) {
	data class SocialUrls(
		val twitter: String?,
		val facebook: String?,
		val linkedin: String?,
		val instagram: String?,
	)
}

@Suppress("PropertyName", "ConstructorParameterNaming")
data class Job(
	val id: Int,
	val company_id: Int,
	val primary_category_tag_id: Int,
	val number_of_vacancies: Int,
	val title: String,
	val salary_from: String,
	val salary_to: String,
	val equity_from: String,
	val equity_to: String,
	val experience_level_from_tag_id: Int,
	val experience_level_to_tag_id: Int,
	val url: String,
	val visa: Boolean,
	val paid_relocation: Boolean,
	val remote: Boolean,
	val hybrid: Boolean,
	val bonus: Boolean,
	val equity: Boolean,
	val job_type: String?,
	val job_location: String,
	val tags: String,
)

// Wrapped in an `object` to prevent initialization order problems from [main].
object Mappings {

	/**
	 * ```
	 * jq -s "[.[][] | [.experience_level_from_tag_id, .experience_level_to_tag_id]] | flatten | unique | sort" jobs*.json
	 * ```
	 */
	val experienceLevels: Map<Int, String> = mapOf(
		30754 to "?",
		30755 to "?",
		30756 to "?",
		30757 to "junior",
		30758 to "grad",
		53603 to "change career",
		53964 to "?",
		53965 to "change career",
	)

	/**
	 * ```
	 * jq -s "[.[][].job_type] | unique | sort" jobs*.json
	 * ```
	 */
	val jobTypes: Map<String?, String> = mapOf(
		null to "Unknown",
		"contract" to "Contract",
		"full_time" to "Full-time",
		"part_time" to "Part-time",
	)

	/**
	 * ```
	 * $$("#RoleGroupTag > option").map((e) => `${e.value} to "${e.innerText}",`).join("\n")
	 * ```
	 */
	val roleGroupTag: Map<Int, String> = mapOf(
		53970 to "Software Engineering / Developer",
		53966 to "Data",
		53969 to "Product & project management",
		53968 to "Marketing",
		53967 to "Design",
	)

	/**
	 * ```
	 * primary_category_tag_id -> getJobRoleName
	 * ```
	 */
	val jobRoleName: Map<Int, String> = mapOf(
		141 to "Back-end development",
		146 to "Data Science / Big Data",
		145 to "DevOps / Sysadmin",
		142 to "Front-end development",
		29496 to "Game Art &amp; Design",
		29497 to "Game Development",
		150 to "Intelligence / Analytics",
		151 to "Marketing",
		143 to "Mobile development",
		24774 to "Other engineering",
		24775 to "Other product/design/marketing",
		147 to "Product Management",
		25518 to "Project Management",
		144 to "QA / Testing",
		149 to "UX Design",
		148 to "Visual Design",
		53971 to "Data Engineer",
		53972 to "Data Scientist",
		53973 to "Machine Learning Engineer",
		53974 to "Research Engineer",
		53975 to "Data Analysis & BI",
		53976 to "Graphic Design (Digital)",
		53977 to "UI Design",
		53978 to "UX Design",
		53979 to "Product Design",
		53980 to "Other Designer (Digital)",
		53981 to "Other Designer (Non-Digital)",
		53982 to "Performance Marketing",
		53983 to "Growth Marketing",
		53984 to "Copy & Content",
		53985 to "Email & CRM",
		53986 to "Marketing Generalist",
		53987 to "Social Media & Community",
		53988 to "PR & Communications",
		53989 to "Product Marketing",
		53990 to "SEO",
		53991 to "CRO & Web Optimisation",
		53992 to "Brand & Creative Marketing",
		53993 to "Lead Generation & Funnels",
		53994 to "Account Management & Customer Success",
		53995 to "Product Manager",
		53996 to "Delivery Manager & Agile Coach",
		53997 to "Product Analyst",
		53998 to "User Research",
		53999 to "Product Designer",
		54000 to "Product Lead",
		54001 to "Front-End",
		54002 to "Back-End",
		54003 to "Full Stack",
		54004 to "DevOps & Infrastructure",
		54005 to "Mobile",
		54006 to "Support",
		54007 to "QA & Testing",
		54008 to "Software Architect",
		54009 to "Sys Admin",
		54010 to "Management",
		54011 to "Security",
		54012 to "Gaming",
		54054 to "Data Architect",
		54055 to "Business Analyst",
		54056 to "Database Administrator",
		54064 to "Business Development",
		54073 to "Scrum Master",
		54078 to "Product Analyst",
		54130 to "Other",
	)

	/**
	 * ```
	 * var app_config = { ... }
	 * ```
	 */
	val attending: Map<Int, String> = mapOf(
		65 to "Saturday", // 2022 November
		66 to "Sunday", // 2022 November
		67 to "Nextgen", // 2022 November
		68 to "Saturday", // 2023 May
		69 to "Sunday", // 2023 May
	)
}
