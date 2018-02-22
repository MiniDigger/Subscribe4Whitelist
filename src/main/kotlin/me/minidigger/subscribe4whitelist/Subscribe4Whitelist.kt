package me.minidigger.subscribe4whitelist

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.Reader
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class Subscribe4Whitelist : JavaPlugin() {

    private val dataProviders = listOf(PatreonDataProvider(), TwitchDataProvider())
    private var subs = listOf<Subscriber>()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val subFile = File(dataFolder, "subs.json")

    override fun onEnable() {
        // create default stuff
        saveDefaultConfig()
        if(!subFile.exists()) {
            FileWriter(subFile).use { gson.toJson(subs, it) }
        }

        // load subs
        FileReader(subFile).use { subs = gson.fromJson<List<Subscriber>>(it) }
        // start fetch task
        server.scheduler.runTaskTimer(this, { fetch() }, 0, config.getLong("fetch-interval", 300))
        // setup providers
        dataProviders.forEach { it.setup(config, logger) }
    }

    private fun fetch() {
        val fetchedSubs = dataProviders.pmap { provider -> provider.fetchCurrentSubscribers() }.flatten().toList()
        val newSubs = fetchedSubs.toMutableList()
        val lostSubs = subs.toMutableList()
        newSubs.removeAll(subs)
        lostSubs.removeAll(fetchedSubs)

        subs = fetchedSubs

        if (newSubs.size > 0) logger.info("Found ${newSubs.size} new subs!")
        if (lostSubs.size > 0) logger.info("Lost ${newSubs.size} subs!")

        newSubs.forEach {
            Bukkit.broadcastMessage(config.getString("new-sub-msg", "%1 just subscribed on %2 for %3!")
                    .replace("%1", it.name)
                    .replace("%2", it.source)
                    .replace("%3", "${(it.amountInCent * 100.0)} bucks"))
        }

        lostSubs.forEach {
            Bukkit.broadcastMessage(config.getString("lost-sub-msg", "%1 just unsubscribed &2!")
                    .replace("%1", it.name)
                    .replace("%2", it.source))
        }

        FileWriter(subFile).use { gson.toJson(subs, it) }
    }

    private fun <T, R> Iterable<T>.pmap(
            numThreads: Int = Runtime.getRuntime().availableProcessors() - 2,
            exec: ExecutorService = Executors.newFixedThreadPool(numThreads),
            transform: (T) -> R): List<R> {

        val defaultSize = if (this is Collection<*>) this.size else 10
        val destination = Collections.synchronizedList(ArrayList<R>(defaultSize))

        this.forEach { item -> exec.submit { destination.add(transform(item)) } }

        exec.shutdown()
        exec.awaitTermination(2, TimeUnit.MINUTES)

        return ArrayList<R>(destination)
    }

    private inline fun <reified T> Gson.fromJson(reader: Reader) = this.fromJson<T>(reader, object : TypeToken<T>() {}.type)
}
