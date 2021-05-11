package com.blockchain.nabu.datamanagers.analytics

import android.content.Context
import com.blockchain.nabu.filesystem.QueueFile
import info.blockchain.api.NabuAnalyticsEvent
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException

interface AnalyticsLocalPersistence {
    fun size(): Single<Long>
    fun save(item: NabuAnalyticsEvent): Completable
    fun getAllItems(): Single<List<NabuAnalyticsEvent>>
    fun getOldestItems(n: Int): Single<List<NabuAnalyticsEvent>>

    /**
     * Removes the eldest {@code n} elements.
     *
     */
    fun removeOldestItems(n: Int): Completable
    fun clear(): Completable
}

class AnalyticsFileLocalPersistence(context: Context) : AnalyticsLocalPersistence {

    private val queueFile: QueueFile by lazy {
        val folder: File = context.getDir(DIR_NAME, Context.MODE_PRIVATE)
        createQueueFile(folder) ?: throw IllegalStateException("File system failed to initialised")
    }

    override fun size(): Single<Long> = Single.just(queueFile.size())

    override fun save(item: NabuAnalyticsEvent): Completable = Completable.fromAction {
        queueFile.add(Json.encodeToString(item).toByteArray())
    }

    override fun removeOldestItems(n: Int): Completable = Completable.fromAction {
        queueFile.remove(n)
    }

    override fun clear(): Completable = Completable.fromAction {
        queueFile.clear()
    }

    override fun getAllItems(): Single<List<NabuAnalyticsEvent>> {
        return Single.fromCallable {
            queueFile.read(queueFile.size()).map {
                Json.decodeFromString(it)
            }
        }
    }

    override fun getOldestItems(n: Int): Single<List<NabuAnalyticsEvent>> {
        return Single.fromCallable {
            queueFile.read(n.toLong()).map {
                Json.decodeFromString(it)
            }
        }
    }

    private fun createQueueFile(folder: File): QueueFile? {
        createDirectory(folder)
        val file = File(folder, FILE_NAME)
        return try {
            QueueFile(file)
        } catch (e: IOException) {
            if (file.delete()) {
                QueueFile(file)
            } else {
                null
            }
        }
    }

    private fun createDirectory(location: File) {
        if (!(location.exists() || location.mkdirs() || location.isDirectory)) {
            throw IOException("Could not create directory at $location")
        }
    }

    companion object {
        private const val DIR_NAME = "analytics-disk-queue"
        private const val FILE_NAME = "analytics.json"
    }
}