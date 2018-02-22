package me.minidigger.subscribe4whitelist

import org.bukkit.configuration.file.FileConfiguration
import java.util.logging.Logger

interface SubscriberDataProvider {

    fun fetchCurrentSubscribers(): List<Subscriber>

    fun setup(config: FileConfiguration, logger: Logger)
}