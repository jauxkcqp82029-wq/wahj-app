package com.example.data.sync

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

@Serializable
data class LanSyncPacket(
    val type: String, // PAIR_APPROVED, MESSAGE, LOCK_COMMAND, POLICY_UPDATE, TELEMETRY, TIME_REQUEST, TIME_DECISION
    val childId: String,
    val token: String = "",
    val senderRole: String = "",
    val payloadJson: String = "",
    val parentHost: String = "",
    val parentPort: Int = DEFAULT_PORT
) {
    companion object {
        const val DEFAULT_PORT = 8765
    }
}

/**
 * Lightweight local Wi-Fi / LAN TCP sync engine enabling real two-device pairing and
 * messaging/command relay on the same local network without requiring external cloud keys.
 */
class LocalLanSyncEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private var serverSocket: ServerSocket? = null

    private val _incomingPackets = MutableSharedFlow<LanSyncPacket>(extraBufferCapacity = 32)
    val incomingPackets: SharedFlow<LanSyncPacket> = _incomingPackets.asSharedFlow()

    var boundPort: Int = LanSyncPacket.DEFAULT_PORT
        private set

    fun startServer() {
        if (serverSocket != null && serverSocket?.isClosed == false) return
        scope.launch {
            try {
                val server = runCatching { ServerSocket(LanSyncPacket.DEFAULT_PORT) }
                    .getOrElse { ServerSocket(0) }
                serverSocket = server
                boundPort = server.localPort
                while (!server.isClosed) {
                    val client = server.accept()
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.w("LocalLanSync", "LAN sync server stopped: ${e.message}")
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            socket.use { s ->
                s.soTimeout = 4000
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val line = reader.readLine() ?: return
                val packet = json.decodeFromString<LanSyncPacket>(line)
                _incomingPackets.emit(packet)
            }
        } catch (e: Exception) {
            Log.w("LocalLanSync", "Failed reading LAN packet: ${e.message}")
        }
    }

    suspend fun sendPacket(host: String, port: Int, packet: LanSyncPacket): Boolean {
        if (host.isBlank() || port <= 0) return false
        return withContext(Dispatchers.IO) {
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 2500)
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(json.encodeToString(packet))
                    writer.flush()
                }
                true
            }.getOrElse {
                Log.w("LocalLanSync", "Unable to reach peer $host:$port (${it.message})")
                false
            }
        }
    }

    fun getLocalIpAddress(): String {
        return runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress ?: "127.0.0.1"
        }.getOrDefault("127.0.0.1")
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}
