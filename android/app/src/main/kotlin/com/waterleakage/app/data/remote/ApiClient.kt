package com.waterleakage.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val PREFS_NAME = "water_leakage_prefs"
    private const val KEY_BASE_URL = "base_url"

    // Default endpoints
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8000/"
    const val USB_BASE_URL = "http://127.0.0.1:8000/"
    const val HOTSPOT_BASE_URL = "http://192.168.137.1:8000/"
    const val DEFAULT_API_PORT = 8000
    const val DISCOVERY_PORT = 37020
    private const val DISCOVERY_MAGIC = "WATER_LEAKAGE_DISCOVER"

    private var currentBaseUrl: String = getDefaultBaseUrl()
    private var cachedApi: WaterLeakageApi? = null

    fun isRunningOnEmulator(): Boolean {
        val fingerprint = (Build.FINGERPRINT ?: "").lowercase()
        val model = (Build.MODEL ?: "").lowercase()
        val product = (Build.PRODUCT ?: "").lowercase()
        val hardware = (Build.HARDWARE ?: "").lowercase()
        val brand = (Build.BRAND ?: "").lowercase()
        val device = (Build.DEVICE ?: "").lowercase()
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase()

        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || fingerprint.contains("emulator")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for")
                || model.contains("sdk_gphone")
                || manufacturer.contains("genymotion")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || product.contains("sdk_gphone")
                || product.contains("google_sdk")
                || product.contains("emulator")
                || product.contains("simulator")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
    }

    fun getDefaultBaseUrl(): String {
        return if (isRunningOnEmulator()) EMULATOR_BASE_URL else USB_BASE_URL
    }

    /**
     * Normalizes user-entered hosts/URLs into a valid Retrofit base URL ending with '/'.
     * Examples:
     * - "10.17.4.111"        -> "http://10.17.4.111:8000/"
     * - "10.17.4.111:8000"   -> "http://10.17.4.111:8000/"
     * - "https://demo.app"   -> "https://demo.app/"
     * Returns null if the input cannot form a valid HTTP/HTTPS URL.
     */
    fun normalizeUrl(raw: String): String? {
        var trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val hasScheme = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
        if (!hasScheme) {
            trimmed = "http://$trimmed"
        }

        val parsed = trimmed.toHttpUrlOrNull() ?: return null
        val isHttps = parsed.scheme.equals("https", ignoreCase = true)

        // Check if caller explicitly wrote a port in the authority segment
        val afterScheme = trimmed.substringAfter("://").substringBefore("/")
        val hasExplicitPort = afterScheme.contains(":")
        val isLocalIpOrHost = parsed.host == "localhost" ||
                parsed.host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))

        val rebuilt = if (!isHttps && !hasExplicitPort && isLocalIpOrHost) {
            parsed.newBuilder().port(DEFAULT_API_PORT).build()
        } else {
            parsed
        }

        var urlString = rebuilt.toString()
        if (!urlString.endsWith("/")) {
            urlString += "/"
        }
        return urlString
    }

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_BASE_URL, null)
        return saved?.let { normalizeUrl(it) } ?: getDefaultBaseUrl()
    }

    fun setBaseUrl(context: Context, newUrl: String): String? {
        val normalized = normalizeUrl(newUrl) ?: return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        currentBaseUrl = normalized
        cachedApi = null // Force recreation on next getApi()
        return normalized
    }

    fun getConnectionLabel(url: String): String {
        return when {
            url.contains("127.0.0.1") || url.contains("localhost") -> "USB Cable"
            url.contains("10.0.2.2") -> "Emulator"
            url.contains("192.168.137.1") -> "PC Hotspot"
            url.startsWith("https://", ignoreCase = true) -> "Remote HTTPS"
            url.contains("192.168.") || url.contains("10.") || url.contains("172.") -> "Wi-Fi / LAN"
            else -> "Custom"
        }
    }

    private fun buildHealthClient(timeoutMs: Long): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * Checks strictly the given URL's /api/v1/health endpoint without falling back to USB.
     */
    suspend fun testHealthStrict(url: String, timeoutMs: Long = 3500L): Boolean = withContext(Dispatchers.IO) {
        val normalized = normalizeUrl(url) ?: return@withContext false
        val healthUrl = "${normalized}api/v1/health"
        try {
            val client = buildHealthClient(timeoutMs)
            val request = Request.Builder().url(healthUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body?.string() ?: ""
                body.contains("\"healthy\"")
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Gets default gateway URLs (e.g., when phone is connected to PC Mobile Hotspot or router).
     */
    private fun getGatewayUrls(context: Context): List<String> {
        val urls = mutableListOf<String>()
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val linkProps = cm?.getLinkProperties(network)
            linkProps?.routes?.forEach { route ->
                val gw = route.gateway
                if (gw is Inet4Address && !gw.isLoopbackAddress) {
                    val host = gw.hostAddress
                    if (!host.isNullOrBlank()) {
                        urls.add("http://$host:$DEFAULT_API_PORT/")
                    }
                }
            }
        } catch (_: Exception) {
        }
        return urls.distinct()
    }

    /**
     * Collects active non-loopback IPv4 addresses and broadcast addresses on this Android device.
     */
    private fun getLocalInterfaces(): Pair<List<String>, List<InetAddress>> {
        val localIps = mutableListOf<String>()
        val broadcasts = mutableListOf<InetAddress>()
        try {
            broadcasts.add(InetAddress.getByName("255.255.255.255"))
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                for (ifAddr in iface.interfaceAddresses) {
                    val addr = ifAddr.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        addr.hostAddress?.let { ip ->
                            if (!ip.startsWith("169.254.") && !localIps.contains(ip)) {
                                localIps.add(ip)
                            }
                        }
                        ifAddr.broadcast?.let { bcast ->
                            if (!broadcasts.contains(bcast)) {
                                broadcasts.add(bcast)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return Pair(localIps, broadcasts)
    }

    /**
     * Stage 2: Sends a UDP discovery broadcast on port 37020 so the backend replies with its LAN URLs.
     */
    private suspend fun discoverViaUdp(broadcastAddresses: List<InetAddress>): List<String> = withContext(Dispatchers.IO) {
        val discovered = linkedSetOf<String>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 600
            }
            val requestBytes = DISCOVERY_MAGIC.toByteArray(Charsets.UTF_8)
            for (bcast in broadcastAddresses) {
                try {
                    val packet = DatagramPacket(requestBytes, requestBytes.size, bcast, DISCOVERY_PORT)
                    socket.send(packet)
                } catch (_: Exception) {
                }
            }

            val deadline = System.currentTimeMillis() + 1300L
            val buf = ByteArray(2048)
            while (System.currentTimeMillis() < deadline) {
                try {
                    val reply = DatagramPacket(buf, buf.size)
                    socket.receive(reply)
                    val text = String(reply.data, 0, reply.length, Charsets.UTF_8)
                    val json = JSONObject(text)
                    if (json.optString("service") == "water-leakage-api") {
                        val urlsArray = json.optJSONArray("urls")
                        if (urlsArray != null) {
                            for (i in 0 until urlsArray.length()) {
                                normalizeUrl(urlsArray.optString(i))?.let { discovered.add(it) }
                            }
                        }
                        val senderIp = reply.address?.hostAddress
                        val port = json.optInt("port", DEFAULT_API_PORT)
                        if (!senderIp.isNullOrBlank()) {
                            normalizeUrl("http://$senderIp:$port/")?.let { discovered.add(it) }
                        }
                        if (discovered.isNotEmpty()) break
                    }
                } catch (_: Exception) {
                    // Timeout waiting for more replies
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
        discovered.toList()
    }

    /**
     * Stage 3: Fast parallel TCP port-8000 scan on the phone's current /24 local Wi-Fi/Hotspot subnets.
     */
    private suspend fun scanLocalSubnets(localIps: List<String>): String? = withContext(Dispatchers.IO) {
        val prefixes = localIps
            .filter { !it.startsWith("10.0.2.") } // Skip emulator internal interface
            .mapNotNull { ip ->
                val parts = ip.split(".")
                if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" to ip else null
            }

        for ((prefix, selfIp) in prefixes) {
            val hostsToCheck = (1..254)
                .map { "$prefix.$it" }
                .filter { it != selfIp }

            // Scan in batches of 64 concurrent TCP probes (~400ms each batch -> ~1.6s total max)
            for (batch in hostsToCheck.chunked(64)) {
                val openHosts = coroutineScope {
                    batch.map { hostIp ->
                        async {
                            if (isTcpPortOpen(hostIp, DEFAULT_API_PORT, 420)) hostIp else null
                        }
                    }.awaitAll().filterNotNull()
                }
                for (openHost in openHosts) {
                    val candidateUrl = "http://$openHost:$DEFAULT_API_PORT/"
                    if (testHealthStrict(candidateUrl, timeoutMs = 1500L)) {
                        return@withContext candidateUrl
                    }
                }
            }
        }
        null
    }

    private fun isTcpPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Multi-stage server auto-detection:
     * 1. Parallel check of saved URL, USB bridge, Emulator host, Windows Hotspot gateway, Wi-Fi gateway.
     * 2. UDP LAN Broadcast discovery (port 37020).
     * 3. Parallel /24 Wi-Fi / Hotspot TCP port-8000 scan.
     */
    suspend fun autoDetectWorkingServer(context: Context): String? = withContext(Dispatchers.IO) {
        val savedUrl = getBaseUrl(context)
        val gatewayUrls = getGatewayUrls(context)
        val isEmu = isRunningOnEmulator()

        // Stage 1: Direct high-priority candidates tested in parallel
        val stage1Candidates = linkedSetOf<String>().apply {
            add(savedUrl)
            if (isEmu) {
                add(EMULATOR_BASE_URL)
            } else {
                add(USB_BASE_URL)
                add("http://localhost:8000/")
            }
            add(HOTSPOT_BASE_URL)
            addAll(gatewayUrls)
            if (!isEmu) {
                add(EMULATOR_BASE_URL)
            }
        }.toList()

        val stage1Results = coroutineScope {
            stage1Candidates.map { candidate ->
                async {
                    if (testHealthStrict(candidate, timeoutMs = 1400L)) candidate else null
                }
            }.awaitAll().filterNotNull()
        }

        if (stage1Results.isNotEmpty()) {
            val winner = stage1Results.first()
            setBaseUrl(context, winner)
            return@withContext winner
        }

        // Stage 2: UDP Broadcast Discovery across active interfaces
        val (localIps, broadcasts) = getLocalInterfaces()
        val udpUrls = discoverViaUdp(broadcasts)
        if (udpUrls.isNotEmpty()) {
            val udpWinner = coroutineScope {
                udpUrls.map { candidate ->
                    async {
                        if (testHealthStrict(candidate, timeoutMs = 1500L)) candidate else null
                    }
                }.awaitAll().firstOrNull { it != null }
            }
            if (udpWinner != null) {
                setBaseUrl(context, udpWinner)
                return@withContext udpWinner
            }
        }

        // Stage 3: Fast parallel /24 subnet HTTP health scan (works even if UDP is blocked)
        val subnetWinner = scanLocalSubnets(localIps)
        if (subnetWinner != null) {
            setBaseUrl(context, subnetWinner)
            return@withContext subnetWinner
        }

        null
    }

    fun getApi(context: Context): WaterLeakageApi {
        val configuredUrl = getBaseUrl(context)
        if (cachedApi == null || currentBaseUrl != configuredUrl) {
            currentBaseUrl = configuredUrl

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(12, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            cachedApi = retrofit.create(WaterLeakageApi::class.java)
        }
        return cachedApi!!
    }
}
