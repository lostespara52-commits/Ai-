package com.example.automation

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SupportedAppInfo(
    val id: String,
    val name: String,
    val packageName: String,
    val fallbackUrl: String,
    val category: String
)

data class WebResearchResult(
    val query: String,
    val summaryPoints: List<String>,
    val sources: List<Pair<String, String>>
)

class DeviceIntegrationManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val coreSupportedApps = listOf(
        SupportedAppInfo("chrome", "Chrome Browser", "com.android.chrome", "https://www.google.com", "Browser & Research"),
        SupportedAppInfo("chatgpt", "ChatGPT", "com.openai.chatgpt", "https://chatgpt.com", "AI Assistant"),
        SupportedAppInfo("gemini", "Google Gemini", "com.google.android.apps.bard", "https://gemini.google.com", "AI Assistant"),
        SupportedAppInfo("flow", "Google Flow Studio", "com.android.chrome", "https://labs.google/fx/tools/flow", "AI Cinema & Image"),
        SupportedAppInfo("youtube", "YouTube", "com.google.android.youtube", "https://m.youtube.com", "Video & Trends"),
        SupportedAppInfo("whatsapp", "WhatsApp", "com.whatsapp", "https://wa.me", "Messaging"),
        SupportedAppInfo("gmail", "Gmail", "com.google.android.gm", "https://mail.google.com", "Email"),
        SupportedAppInfo("drive", "Google Drive", "com.google.android.apps.docs", "https://drive.google.com", "Cloud Files"),
        SupportedAppInfo("photos", "Google Photos", "com.google.android.apps.photos", "https://photos.google.com", "Media Gallery"),
        SupportedAppInfo("calendar", "Calendar", "com.google.android.calendar", "https://calendar.google.com", "Schedule"),
        SupportedAppInfo("maps", "Google Maps", "com.google.android.apps.maps", "https://maps.google.com", "Navigation"),
        SupportedAppInfo("notes", "Google Keep / Notes", "com.google.android.keep", "https://keep.google.com", "Notes"),
        SupportedAppInfo("phone", "Phone Dialer", "com.google.android.dialer", "tel:", "Calls"),
        SupportedAppInfo("contacts", "Contacts", "com.google.android.contacts", "content://contacts/people", "Contacts"),
        SupportedAppInfo("camera", "Camera", "com.android.camera", "", "Camera"),
        SupportedAppInfo("settings", "System Settings", "com.android.settings", "", "System")
    )

    /**
     * Dynamically retrieves ALL launchable apps installed on the user's Android device
     * combined with core web-fallback integrations.
     */
    val supportedApps: List<SupportedAppInfo>
        get() = getAllInstalledAndCoreApps()

    fun getAllInstalledAndCoreApps(): List<SupportedAppInfo> {
        val result = mutableListOf<SupportedAppInfo>()
        val seenPackages = mutableSetOf<String>()

        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            for (ri in resolveInfos) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (pkg == context.packageName || seenPackages.contains(pkg)) continue
                val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg.substringAfterLast('.') }
                seenPackages.add(pkg)
                result.add(
                    SupportedAppInfo(
                        id = label.lowercase().replace(" ", "_"),
                        name = label,
                        packageName = pkg,
                        fallbackUrl = "",
                        category = "Installed on Phone"
                    )
                )
            }
        } catch (_: Exception) {
        }

        for (core in coreSupportedApps) {
            if (!seenPackages.contains(core.packageName)) {
                result.add(core)
            }
        }
        return result.sortedBy { it.name.lowercase() }
    }

    fun isAccessibilityEnabled(): Boolean {
        return AuraAccessibilityService.isConnected.value
    }

    /**
     * Checks whether Android "Display over other apps" (Overlay) permission is granted.
     */
    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Opens the exact Android "Display over other apps" permission screen.
     */
    fun requestDisplayOverOtherAppsPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {
        }
        openAppDetailsSettings()
    }

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    /**
     * Opens Android Keyboard / Input Method settings so the user can enable "Dua Fatima Voice Keyboard".
     */
    fun openVoiceKeyboardSettings() {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    /**
     * Shows the system Keyboard Switcher dialog so the user can select "Dua Fatima Voice Keyboard".
     */
    fun showKeyboardPicker() {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        } catch (_: Exception) {
        }
    }

    fun isDuaKeyboardEnabled(): Boolean {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val enabledList = imm?.enabledInputMethodList.orEmpty()
            enabledList.any { it.packageName == context.packageName }
        } catch (e: Exception) {
            false
        }
    }

    private fun openAppDetailsSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /**
     * Maps phonetic SpeechRecognizer variants (e.g., "chatpati", "chat gpt", "gpt", Urdu/Hindi script names)
     * to canonical app identifiers.
     */
    fun normalizeAppAlias(raw: String): String {
        val cleaned = raw.lowercase().trim()
            .replace(Regex("(?i)(kholo|open|chala do|chalao|start|launch|app|application|karo|please|dua|fatima|کھولو|چلاؤ|خولو|खोलो|चलाओ)"), "")
            .trim()

        return when {
            cleaned.contains("chatgpt") || cleaned.contains("chat gpt") || cleaned.contains("chatpati") ||
                cleaned.contains("chat pati") || cleaned.contains("chtgpt") || cleaned == "gpt" ||
                cleaned.contains("चैट जीपीटी") || cleaned.contains("चटपटी") || cleaned.contains("जीपीटी") ||
                cleaned.contains("چیٹ جی پی ٹی") || cleaned.contains("چٹپٹی") || cleaned.contains("جی پی ٹی") -> "chatgpt"

            cleaned.contains("youtube") || cleaned.contains("you tube") || cleaned.contains("youtub") ||
                cleaned.contains("यूट्यूब") || cleaned.contains("یوٹیوب") -> "youtube"

            cleaned.contains("whatsapp") || cleaned.contains("whats app") || cleaned.contains("watsap") ||
                cleaned.contains("व्हाट्सएप") || cleaned.contains("واٹس ایپ") || cleaned.contains("واٹسپ") -> "whatsapp"

            cleaned.contains("chrome") || cleaned.contains("google") || cleaned.contains("browser") ||
                cleaned.contains("क्रोम") || cleaned.contains("गूगल") || cleaned.contains("کروم") || cleaned.contains("گوگل") -> "chrome"

            cleaned.contains("camera") || cleaned.contains("camra") || cleaned.contains("photo") ||
                cleaned.contains("कैमरा") || cleaned.contains("کیمرہ") || cleaned.contains("کیمرا") -> "camera"

            cleaned.contains("map") || cleaned.contains("maps") || cleaned.contains("location") ||
                cleaned.contains("मैप") || cleaned.contains("लोकेशन") || cleaned.contains("نقشہ") || cleaned.contains("لوکیشن") -> "maps"

            cleaned.contains("setting") || cleaned.contains("सेटिंग") || cleaned.contains("سیٹنگ") -> "settings"

            cleaned.contains("gmail") || cleaned.contains("email") || cleaned.contains("mail") ||
                cleaned.contains("जीमेल") || cleaned.contains("جی میل") -> "gmail"

            cleaned.contains("phone") || cleaned.contains("dialer") ||
                cleaned.contains("फोन") || cleaned.contains("فون") -> "phone"

            else -> cleaned
        }
    }

    /**
     * Launches ANY installed app on the phone by matching its name/id/package, or falls back to Chrome / Web URL.
     * Also queues pending screen typing & clipboard paste if `optionalPromptToCopy` is provided!
     */
    fun launchAppOrFallback(appQueryOrPackage: String, optionalPromptToCopy: String = ""): String {
        val normalized = normalizeAppAlias(appQueryOrPackage)

        if (optionalPromptToCopy.isNotBlank()) {
            copyToClipboard("Dua Fatima Prompt", optionalPromptToCopy)
            AuraAccessibilityService.pendingTask = PendingAppAutomationTask(
                targetAppHint = normalized,
                textToType = optionalPromptToCopy,
                buttonToTapAfter = "Send"
            )
        }

        // Handle special system intents
        if (normalized == "settings" || normalized == "setting") {
            val intent = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            return "Opened Android System Settings."
        }
        if (normalized == "camera") {
            try {
                val intent = Intent("android.media.action.IMAGE_CAPTURE").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
                return "Opened Camera."
            } catch (_: Exception) {
            }
        }
        if (normalized == "maps") {
            if (optionalPromptToCopy.isNotBlank()) {
                return navigateToPlaceInMaps(optionalPromptToCopy)
            }
            return getCurrentLocationSummaryAndOpenMaps(openMaps = true)
        }

        val allApps = getAllInstalledAndCoreApps()
        val matched = allApps.firstOrNull {
            it.id == normalized ||
                it.name.lowercase() == normalized ||
                it.packageName.lowercase() == normalized
        } ?: allApps.firstOrNull {
            it.name.lowercase().contains(normalized) ||
                normalized.contains(it.name.lowercase()) ||
                it.id.contains(normalized)
        }

        val pm = context.packageManager
        if (matched != null) {
            // Special YouTube search handling if a query was passed
            if ((matched.id == "youtube" || matched.packageName.contains("youtube")) && optionalPromptToCopy.isNotBlank()) {
                return searchYouTube(optionalPromptToCopy)
            }

            val launchIntent = pm.getLaunchIntentForPackage(matched.packageName)
            if (launchIntent != null && !matched.fallbackUrl.contains("labs.google")) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "Opened ${matched.name}" +
                    if (optionalPromptToCopy.isNotBlank()) " & queued automatic screen typing: \"${optionalPromptToCopy.take(40)}\"." else "."
            }
            if (matched.fallbackUrl.startsWith("http")) {
                val targetUrl = if (matched.id == "chatgpt" && optionalPromptToCopy.isNotBlank()) {
                    "https://chatgpt.com/?q=${URLEncoder.encode(optionalPromptToCopy, "UTF-8")}"
                } else {
                    matched.fallbackUrl
                }
                openUrlInBrowser(targetUrl)
                return "Opened ${matched.name}" +
                    if (optionalPromptToCopy.isNotBlank()) " with prompt \"${optionalPromptToCopy.take(40)}\" ready." else "."
            }
        }

        val directIntent = pm.getLaunchIntentForPackage(appQueryOrPackage)
        if (directIntent != null) {
            directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(directIntent)
            return "Opened application $appQueryOrPackage."
        }

        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            openUrlInBrowser(appQueryOrPackage)
            return "Opened URL: $appQueryOrPackage"
        }

        val searchQuery = if (optionalPromptToCopy.isNotBlank()) "$normalized $optionalPromptToCopy" else appQueryOrPackage
        val searchUrl = "https://www.google.com/search?q=${URLEncoder.encode(searchQuery, "UTF-8")}"
        openUrlInBrowser(searchUrl)
        return "Opened Chrome search for '$searchQuery'."
    }

    fun openUrlInBrowser(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun searchYouTube(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.youtube.com/results?search_query=$encoded"
        openUrlInBrowser(url)
        return "Opened YouTube search for '$query'."
    }

    /**
     * Real Mobile GPS / Network Location Reader & Google Maps Controller.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocationSummaryAndOpenMaps(openMaps: Boolean = false): String {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        var bestLocation: Location? = null
        if ((hasFine || hasCoarse) && lm != null) {
            try {
                val providers = lm.getProviders(true)
                for (p in providers) {
                    val loc = lm.getLastKnownLocation(p) ?: continue
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (bestLocation != null) {
            val lat = String.format(Locale.US, "%.5f", bestLocation.latitude)
            val lon = String.format(Locale.US, "%.5f", bestLocation.longitude)
            if (openMaps) {
                val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(My+Current+Location)")
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(mapIntent)
                } catch (_: Exception) {
                    openUrlInBrowser("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
                }
            }
            return "Live GPS Location: Lat $lat, Lon $lon" + if (openMaps) " (Opened in Google Maps)" else ""
        }

        if (!isGpsEnabled) {
            if (openMaps) {
                openSystemSettingPage(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            }
            return "Phone GPS Location is currently OFF. Opened Location Settings so you can turn it ON."
        }

        if (openMaps) {
            openUrlInBrowser("https://www.google.com/maps")
            return "Opened Google Maps for live location tracking."
        }

        return "GPS Active (Grant Location permission or tap 'Open Maps' to view live coordinates)."
    }

    /**
     * Opens Google Maps navigation or route search for any destination or place query.
     */
    fun navigateToPlaceInMaps(destinationQuery: String): String {
        val cleanDest = destinationQuery.trim().ifBlank { "Nearby" }
        val encoded = URLEncoder.encode(cleanDest, "UTF-8")
        return try {
            val navUri = Uri.parse("google.navigation:q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                openUrlInBrowser("https://www.google.com/maps/dir/?api=1&destination=$encoded")
            }
            "Opened Google Maps navigation to '$cleanDest'."
        } catch (e: Exception) {
            openUrlInBrowser("https://www.google.com/maps/search/?api=1&query=$encoded")
            "Opened Google Maps for '$cleanDest'."
        }
    }

    fun initiatePhoneDial(nameOrNumber: String): String {
        val resolvedNumber = resolveContactPhoneNumber(nameOrNumber) ?: nameOrNumber.filter { it.isDigit() || it == '+' }
        val dialUri = if (resolvedNumber.isNotBlank()) {
            Uri.parse("tel:$resolvedNumber")
        } else {
            Uri.parse("tel:")
        }
        return try {
            val intent = Intent(Intent.ACTION_DIAL, dialUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            if (resolvedNumber.isNotBlank()) {
                "Opened Phone Dialer ready to call $nameOrNumber ($resolvedNumber)."
            } else {
                "Opened Phone Dialer for '$nameOrNumber'."
            }
        } catch (e: Exception) {
            "Unable to open Phone Dialer: ${e.localizedMessage}"
        }
    }

    fun prepareMessage(recipient: String, messageBody: String, viaWhatsApp: Boolean = false): String {
        val resolvedNumber = resolveContactPhoneNumber(recipient) ?: recipient.filter { it.isDigit() || it == '+' }
        return try {
            if (viaWhatsApp) {
                val url = if (resolvedNumber.isNotBlank()) {
                    "https://wa.me/${resolvedNumber.removePrefix("+")}?text=${URLEncoder.encode(messageBody, "UTF-8")}"
                } else {
                    "https://wa.me/?text=${URLEncoder.encode(messageBody, "UTF-8")}"
                }
                openUrlInBrowser(url)
                "Opened WhatsApp with message for $recipient: \"$messageBody\""
            } else {
                val smsUri = Uri.parse("smsto:$resolvedNumber")
                val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                    putExtra("sms_body", messageBody)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Prepared SMS message to $recipient: \"$messageBody\""
            }
        } catch (e: Exception) {
            copyToClipboard("Dua Fatima Message", messageBody)
            "Copied message for $recipient to clipboard."
        }
    }

    fun resolveContactPhoneNumber(queryName: String): String? {
        val cleanedDigits = queryName.filter { it.isDigit() || it == '+' }
        if (cleanedDigits.length >= 7) return cleanedDigits

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        return try {
            val resolver = context.contentResolver
            val cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$queryName%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIdx >= 0) return it.getString(numberIdx)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    suspend fun performWebResearch(query: String): WebResearchResult = withContext(Dispatchers.IO) {
        val summaryPoints = mutableListOf<String>()
        val sources = mutableListOf<Pair<String, String>>()
        val encoded = URLEncoder.encode(query, "UTF-8")

        try {
            val ddgUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val req = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "DuaFatima-Agent/2.0")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val abstractText = json.optString("AbstractText")
                        val abstractUrl = json.optString("AbstractURL")
                        val heading = json.optString("Heading", query)
                        if (abstractText.isNotBlank()) {
                            summaryPoints.add(abstractText)
                            if (abstractUrl.isNotBlank()) {
                                sources.add(heading to abstractUrl)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        try {
            val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&utf8=&format=json&srlimit=3"
            val req = Request.Builder()
                .url(wikiUrl)
                .header("User-Agent", "DuaFatima-Agent/2.0")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val searchArr = json.optJSONObject("query")?.optJSONArray("search")
                    if (searchArr != null) {
                        for (i in 0 until searchArr.length()) {
                            val item = searchArr.getJSONObject(i)
                            val title = item.optString("title")
                            val snippet = item.optString("snippet")
                                .replace(Regex("<[^>]*>"), "")
                                .replace("&quot;", "\"")
                                .replace("&amp;", "&")
                            if (snippet.isNotBlank()) {
                                summaryPoints.add("$title: $snippet")
                                val pageUrl = "https://en.wikipedia.org/wiki/${URLEncoder.encode(title.replace(' ', '_'), "UTF-8")}"
                                sources.add("Wikipedia: $title" to pageUrl)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        val googleUrl = "https://www.google.com/search?q=$encoded"
        sources.add("Google Search: $query" to googleUrl)

        WebResearchResult(
            query = query,
            summaryPoints = summaryPoints.distinct().take(6),
            sources = sources.distinctBy { it.second }.take(5)
        )
    }

    // Advanced Hardware & System Controls
    private var isTorchOn = false

    fun toggleFlashlight(enable: Boolean? = null): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return "Camera/Flashlight service unavailable on this device."
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return "No flashlight hardware detected on this device."
            val nextState = enable ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, nextState)
            isTorchOn = nextState
            if (nextState) "Flashlight / Torch turned ON" else "Flashlight / Torch turned OFF"
        } catch (e: Exception) {
            "Flashlight toggle note: ${e.localizedMessage ?: "Not supported on virtual camera"}"
        }
    }

    fun adjustVolume(increase: Boolean): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return "Audio service unavailable."
            val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            "Volume adjusted: $current / $max"
        } catch (e: Exception) {
            "Unable to adjust volume: ${e.localizedMessage}"
        }
    }

    fun getBatteryStatusSummary(): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            if (pct >= 0) {
                "Battery: $pct% (${if (charging) "Charging" else "On Battery"})"
            } else {
                "Battery status active"
            }
        } catch (_: Exception) {
            "Battery telemetry active"
        }
    }

    fun getCurrentTimeAndDateSummary(): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a, EEEE, dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun openSystemSettingPage(settingAction: String) {
        try {
            val intent = Intent(settingAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }
}
