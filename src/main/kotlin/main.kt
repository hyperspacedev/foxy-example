import io.ktor.http.*
import models.Instance
import utils.aliases.Timeline
import utils.requests.FoxyInstanceScope
import utils.requests.FoxyTimelineScope
import utils.responses.MastodonResponse

suspend fun main() {
    authenticateToMastodon()
    verifyInstance()
    val authors = getAuthorsOfCurrentTimeline()
    println(authors.sorted())
}

private suspend fun authenticateToMastodon() {
    if (Foxy.authenticateExistingSession())
        return

    Foxy.startOAuthFlow {
        instance = "koyu.space"

        appName("Foxy Example")
        appWebsite("https://hyperspace.marquiskurt.net")

        scopes {
            add("read")
        }
    }

    Foxy.finishOAuthFlow(Foxy.AuthGrantType.ClientCredentials)
}

private suspend fun verifyInstance() {
    val instanceRes = Foxy.request<Instance> {
        method = HttpMethod.Get
        instance(FoxyInstanceScope.Instance)
    }

    when (instanceRes) {
        is MastodonResponse.Error -> println("An error occurred: ${instanceRes.error.error}")
        is MastodonResponse.Success -> println("Connected to ${instanceRes.entity.title}")
    }
}

private suspend fun getAuthorsOfCurrentTimeline(): Set<String> {
    val authors = mutableSetOf<String>()

    val timelineRes = Foxy.request<Timeline> {
        method = HttpMethod.Get
        timeline(FoxyTimelineScope.Network)

        parameter("local", true)
        parameter("limit", 50)
    }

    when (timelineRes) {
        is MastodonResponse.Success -> {
            timelineRes.entity.forEach {
                authors.add(it.account.acct)
            }
        }
        is MastodonResponse.Error -> println("Something went wrong: ${timelineRes.error.error}")
    }

    return authors.toSet()
}