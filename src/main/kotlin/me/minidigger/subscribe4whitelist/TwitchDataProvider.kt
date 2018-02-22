package me.minidigger.subscribe4whitelist

import me.philippheuer.twitch4j.TwitchClient
import me.philippheuer.twitch4j.TwitchClientBuilder
import me.philippheuer.twitch4j.model.Subscription
import org.bukkit.configuration.file.FileConfiguration
import java.util.logging.Logger
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class TwitchDataProvider : SubscriberDataProvider {
    lateinit var twitchClient: TwitchClient
    lateinit var channelName: String
    var enabled = false

    override fun setup(config: FileConfiguration, logger: Logger) {
        val clientId = config.getString("twitch.clientId", "------")
        val clientSecret = config.getString("twitch.clientSecret", "------")
        val credential = config.getString("twitch.credential", "------")
        channelName = config.getString("twitch.channelName", "------")

        if (clientId == "------" || clientSecret == "------" || credential == "------" || channelName == "------") {
            enabled = false
            return
        }

        twitchClient = TwitchClientBuilder.init()
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .withAutoSaveConfiguration(true)
                .withConfigurationDirectory(File("config"))
                .withCredential(credential) // Get your token at: https://twitchapps.com/tmi/
                .connect()
        enabled = true
    }

    override fun fetchCurrentSubscribers(): List<Subscriber> {
        if (!enabled) return ArrayList()
        val subs = ArrayList<Subscription>()
        var page = 0L

        do {
            val temp = twitchClient.getChannelEndpoint(channelName).getSubscriptions(Optional.of(100), Optional.of(page), Optional.of("asc"))
            subs.addAll(temp)
            page += 100
        } while (temp.size == 100)

        return subs.map { Subscriber(it.user.name, "Twitch", 500) }
    }
}