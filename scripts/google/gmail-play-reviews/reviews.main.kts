// Based on https://developers.google.com/gmail/api/quickstart/java
// Postman collection https://www.postman.com/api-evangelist/workspace/google/collection/35240-0d073c40-6ad8-43d0-8baf-8ee3606819e0

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.api-client:google-api-client:2.6.0")
@file:DependsOn("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
@file:DependsOn("com.google.apis:google-api-services-gmail:v1-rev20240520-2.0.0")

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import java.io.File
import java.io.PrintWriter

main()

fun main() {
	val service: Gmail = this
		.gmail(
			"client_secret_projectId-identifier.apps.googleusercontent.com.json",
			GmailScopes.GMAIL_READONLY,
		)
		.setApplicationName("Gmail Google Play Review scraper")
		.build()

	val userId = "me"
	val messages = service.loadMessages(userId)
	val reviews = parse(messages).toList()
	File("reviews-from-gmail.csv").printWriter().use { output ->
		save(reviews, output)
	}
}

fun Gmail.loadMessages(userId: String): Sequence<Message> {
	/** 2016-2019 */
	val noReply = "noreply-play-developer-console@google.com"

	/** 2019-2023 */
	val noDashReply = "no-reply-play-developer-console@google.com"
	val subjectPrefix = "A user has written a new review for"

	return this
		.users()
		.messages()
		.list(userId)
		.setQ("from:(${noReply} OR ${noDashReply}) subject:\"${subjectPrefix}\"")
		.setMaxResults(1000)
		.execute()
		.messages
		.also { println("Messages: ${it.size}") }
		.asSequence()
		.map { message ->
			this
				.users()
				.messages()
				.get(userId, message.id)
				.execute()
		}
}

fun parse(messages: Sequence<Message>): Sequence<Pair<ParsedMessage, Review>> =
	messages
		.map { message -> message.parse() }
		.map { message -> message to runCatching { message.asReview() } }
		.onEach { println(it.second) }
		.mapNotNull { (message, result) ->
			result.fold(
				onSuccess = { review ->
					if (review.toString().contains("ERROR")) {
						println("<parsed>: ${message}")
					}
					message to review
				},
				onFailure = { error ->
					println("<parsed>: ${message}")
					error.printStackTrace()
					null
				},
			)
		}

fun save(reviews: List<Pair<ParsedMessage, Review>>, output: PrintWriter) {
	reviews.forEach { (message, review) ->
		output.printf(
			"%s,\"%s\",%s,%s,%d,\"%s\"\n",
			review.app,
			review.date,
			review.id,
			message.id,
			review.stars,
			review.text.replace("\"", "\"\"")
		)
	}
}

data class ParsedMessage(
	val id: String,
	val subject: String,
	val bodyPlain: String,
)

fun Message.parse(): ParsedMessage {
	val subject = this
		.payload
		.headers
		.single { it.name == "Subject" }
		.value
	val plain = this
		.payload
		.parts
		.single { it.mimeType == "text/plain" }
		.body
		.decodeData()
		.decodeToString(throwOnInvalidSequence = true)
	return ParsedMessage(
		id = this.id,
		subject = subject,
		bodyPlain = plain,
	)
}

data class Review(
	val app: String,
	val date: String,
	val id: String,
	val stars: Int,
	val text: String,
)

fun ParsedMessage.asReview(): Review {
	val newReviewId = Regex("""<https://play\.google\.com/console/.*\?reviewId=([0-9a-f-]+)&corpus=PUBLIC_REVIEWS>""")
		.find(bodyPlain)?.groupValues?.get(1)
	@Suppress("MaxLineLength")
	val newOldReviewId = Regex("""<https://play\.google\.com/console/.*\?reviewId=(gp:[a-zA-Z0-9_-]+)&corpus=PUBLIC_REVIEWS(&donotshowonboarding)?>""")
			.find(bodyPlain)?.groupValues?.get(1)
	val oldReviewId = Regex("""<https://play\.google\.com/apps/publish.*&reviewid=(gp:[a-zA-Z0-9_-]+)>""")
		.find(bodyPlain)?.groupValues?.get(1)
	return Review(
		app = subject
			.substringAfter("A user has written a new review for ", "ERROR")
			.substringBeforeLast(" on ", "ERROR"),
		date = subject
			.substringAfterLast(" on "),
		id = newReviewId ?: newOldReviewId ?: oldReviewId ?: "ERROR",
		stars = Regex("★+").find(bodyPlain)!!.value.length,
		text = bodyPlain
			.replace("\r\n", "\n")
			.substringAfter("★\n\n", "ERROR")
			.substringBefore("\n\nReply", "ERROR"),
	)
}

fun gmail(credentials: String, vararg scopes: String): Gmail.Builder {
	val json: JsonFactory = GsonFactory.getDefaultInstance()
	val transport: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
	return Gmail.Builder(
		transport,
		json,
		authenticate(
			transport,
			json,
			scopes.toList(),
			GoogleClientSecrets.load(json, File(credentials).reader()),
		)
	)
}

fun authenticate(
	transport: NetHttpTransport,
	json: JsonFactory,
	scopes: List<String>,
	clientSecrets: GoogleClientSecrets,
): Credential {
	val flow = GoogleAuthorizationCodeFlow
		.Builder(transport, json, clientSecrets, scopes)
		.setDataStoreFactory(FileDataStoreFactory(File("tokens")))
		.setAccessType("offline")
		.build()
	val receiver: LocalServerReceiver = LocalServerReceiver
		.Builder()
		.setPort(8888)
		.build()
	return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}
