package me.minidigger.subscribe4whitelist

import com.patreon.PatreonAPI
import com.patreon.resources.Campaign
import org.bukkit.configuration.file.FileConfiguration
import java.util.logging.Logger


class PatreonDataProvider : SubscriberDataProvider {

    lateinit var client: PatreonAPI
    lateinit var campaign: Campaign

    var enabled = false

    override fun setup(config: FileConfiguration, logger: Logger) {
        logger.info("Setting up PatreonDataProvider")
        val accessToken = config.getString("patreon.accessToken", "------")
        if (accessToken == "------") {
            logger.warning("No access token found in config")
            return
        }

        client = PatreonAPI(accessToken)
        val campaigns = client.fetchCampaigns().get()
        if (campaigns.size == 0) {
            logger.warning("No active patreon campagin found")
        } else {
            campaign = campaigns[0]
            logger.info("Setup PatreonDataProvider with campaign '${campaign.creationName}'")
            enabled = true
        }
    }

    override fun fetchCurrentSubscribers(): List<Subscriber> {
        if (!enabled) return ArrayList()
        return client.fetchAllPledges(campaign.id)
                .map { pledge -> Subscriber(pledge.patron.fullName, "Patreon", pledge.amountCents) }
    }
}