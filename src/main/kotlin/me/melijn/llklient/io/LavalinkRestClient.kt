package me.melijn.llklient.io

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import com.sedmelluq.lava.common.tools.DaemonThreadFactory
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.future.await
import me.melijn.llklient.utils.LavalinkUtil
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.LoggerFactory
import java.io.EOFException
import java.io.IOException
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LavalinkRestClient(private val socket: LavalinkSocket) {


    companion object {
        private val log = LoggerFactory.getLogger(LavalinkRestClient::class.java)

        var HTTP_CLIENT: HttpClient = HttpClient(OkHttp) {
            this.defaultRequest {
                this.header("Client-Name", "Lavalink-Klient")
            }
        }

        var EXECUTOR_SERVICE: ExecutorService = Executors.newCachedThreadPool(
            DaemonThreadFactory("Lavalink-RestExecutor")
        )

        private val SEARCH_TRANSFORMER: (DataObject) -> List<AudioTrack> = { loadResult: DataObject ->
            val tracks: MutableList<AudioTrack> = ArrayList()
            val trackData = loadResult.getArray("tracks")
            for (index in 0 until trackData.length()) {
                val track = trackData.getObject(index)
                try {
                    val audioTrack: AudioTrack = LavalinkUtil.toAudioTrack(track.getString("track"))
                    tracks.add(audioTrack)
                } catch (ex: IOException) {
                    log.error("Error converting track", ex)
                }
            }
            tracks
        }
    }

    suspend fun getYoutubeSearchResult(query: String): List<AudioTrack> {
        val result = load("ytsearch:$query")
        return SEARCH_TRANSFORMER(result)
    }

    suspend fun getSoundCloudSearchResult(query: String): List<AudioTrack> {
        val result = load("scsearch:$query")
        return SEARCH_TRANSFORMER(result)
    }

    suspend fun loadItem(identifier: String, callback: AudioLoadResultHandler) {
        val dataObject = load(identifier)
        consumeCallback(callback, dataObject)
    }

    fun consumeCallback(callback: AudioLoadResultHandler, loadResult: DataObject?) {
        if (loadResult == null) {
            callback.noMatches()
            return
        }
        try {
            when (val loadType = loadResult.getString("loadType")) {
                "TRACK_LOADED" -> {
                    val trackDataSingle = loadResult.getArray("tracks")
                    val trackObject = trackDataSingle.getObject(0)
                    val singleTrackBase64 = trackObject.getString("track")
                    val singleAudioTrack: AudioTrack = LavalinkUtil.toAudioTrack(singleTrackBase64)
                    callback.trackLoaded(singleAudioTrack)
                }
                "PLAYLIST_LOADED" -> {
                    val trackData = loadResult.getArray("tracks")
                    val tracks: MutableList<AudioTrack> = ArrayList<AudioTrack>()
                    var index = 0
                    while (index < trackData.length()) {
                        val track = trackData.getObject(index)
                        val trackBase64 = track.getString("track")
                        val audioTrack: AudioTrack = LavalinkUtil.toAudioTrack(trackBase64)
                        tracks.add(audioTrack)
                        index++
                    }
                    val playlistInfo = loadResult.getObject("playlistInfo")
                    val selectedTrackId = playlistInfo.getInt("selectedTrack")
                    val selectedTrack: AudioTrack = if (selectedTrackId < tracks.size && selectedTrackId >= 0) {
                        tracks[selectedTrackId]
                    } else {
                        if (tracks.size == 0) {
                            callback.loadFailed(
                                FriendlyException(
                                    "Playlist is empty",
                                    FriendlyException.Severity.SUSPICIOUS,
                                    IllegalStateException("Empty playlist")
                                )
                            )
                            return
                        }
                        tracks[0]
                    }
                    val playlistName = playlistInfo.getString("name")
                    val playlist = BasicAudioPlaylist(playlistName, tracks, selectedTrack, true)
                    callback.playlistLoaded(playlist)
                }
                "NO_MATCHES" -> callback.noMatches()
                "LOAD_FAILED" -> {
                    val exception = loadResult.getObject("exception")
                    val message = exception.getString("message")
                    val severity: FriendlyException.Severity =
                        FriendlyException.Severity.valueOf(exception.getString("severity"))
                    val friendlyException = FriendlyException(message, severity, Throwable())
                    callback.loadFailed(friendlyException)
                }
                else -> throw IllegalArgumentException("Invalid loadType: $loadType")
            }
        } catch (ex: Exception) {
            callback.loadFailed(FriendlyException(ex.message, FriendlyException.Severity.FAULT, ex))
        }
    }

    suspend fun load(identifier: String): DataObject = suspendCoroutine {
        CoroutineScope(EXECUTOR_SERVICE.asCoroutineDispatcher()).launch {
            try {
                val encPart: String = URLEncoder.encode(identifier, "UTF-8")

                val requestUrl = buildBaseAddress(socket) + encPart
                val response = apiGet(requestUrl, socket.password, 0)
                it.resume(response)
            } catch (ex: Throwable) {
                it.resumeWithException(ex)
            }
        }
    }

    private val wsPattern = Regex("[wW][sS]{1,2}[:][/]{2}")
    private fun buildBaseAddress(socket: LavalinkSocket): String {
        // wss:// or ws:// -> http://
        return socket.remoteUri.toString().replaceFirst(wsPattern, "http://") + "/loadtracks?identifier="
    }

    private suspend fun apiGet(url: String, auth: String, attempt: Int): DataObject {
        if (attempt > 0) {
            log.info("Attempt ${attempt + 1} getting fetching: $url")
        }
        return try {
            val response = HTTP_CLIENT.get<HttpResponse>(url) {
                header("Authorization", auth)
            }

            val statusCode: Int = response.status.value
            if (statusCode != 200) throw IOException("Invalid API Request Status Code: $statusCode")

            DataObject.fromJson(response.readBytes())
        } catch (t: EOFException) {
            if (attempt == 2) {
                delay(1000)
            }
            if (attempt < 3)
                apiGet(url, auth, attempt + 1)
            else throw t
        } catch (t: IOException) {
            if (attempt == 2) {
                delay(1000)
            }
            if (attempt < 3)
                apiGet(url, auth, attempt + 1)
            else throw t
        }
    }
}