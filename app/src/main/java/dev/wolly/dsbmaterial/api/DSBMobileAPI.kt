package dev.wolly.dsbmaterial.api

import android.util.Base64
import android.util.Log
import dev.wolly.dsbmaterial.data.PlanInfo
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.net.CookieManager
import java.net.CookiePolicy

class DSBMobileAPI(private val username: String, private val password: String) {
    private val TAG = "DSBMobileAPI"
    private val LOGIN_URL = "https://www.dsbmobile.de/Login.aspx"
    private val WEB_API_URL = "https://www.dsbmobile.de/jhw-1fd98248-440c-4283-bef6-dc82fe769b61.ashx/GetData"

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }
    
    private val client = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieManager))
        .build()

    private suspend fun webLogin(): Boolean = withContext(Dispatchers.IO) {
        try {
            val getRequest = Request.Builder().url(LOGIN_URL).build()
            val getResponse = client.newCall(getRequest).execute()
            val html = getResponse.body?.string() ?: return@withContext false
            
            val doc = Jsoup.parse(html)
            val vs = doc.select("input[name=__VIEWSTATE]").first()?.attr("value") ?: ""
            val vsg = doc.select("input[name=__VIEWSTATEGENERATOR]").first()?.attr("value") ?: ""
            val ev = doc.select("input[name=__EVENTVALIDATION]").first()?.attr("value") ?: ""

            if (vs.isEmpty() || ev.isEmpty()) return@withContext false

            val formBody = FormBody.Builder()
                .add("__VIEWSTATE", vs)
                .add("__VIEWSTATEGENERATOR", vsg)
                .add("__EVENTVALIDATION", ev)
                .add("txtUser", username)
                .add("txtPass", password)
                .add("ctl03", "Anmelden")
                .build()

            val postRequest = Request.Builder()
                .url(LOGIN_URL)
                .post(formBody)
                .build()

            val postResponse = client.newCall(postRequest).execute()
            val responseUrl = postResponse.request.url.toString()
            val responseText = postResponse.body?.string() ?: ""

            return@withContext responseUrl.contains("default.aspx") || responseText.contains("<title>DSBmobile</title>")
        } catch (e: Exception) {
            Log.e(TAG, "Web login error", e)
            false
        }
    }

    private suspend fun callWebApi(): JSONObject? = withContext(Dispatchers.IO) {
        if (!webLogin()) return@withContext null

        val now = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
        val payload = JSONObject().apply {
            put("UserId", username)
            put("UserPw", password)
            put("AppVersion", "2.3")
            put("Language", "de")
            put("OsVersion", "Mozilla/5.0")
            put("AppId", UUID.randomUUID().toString())
            put("Device", "WebApp")
            put("BundleId", "de.heinekingmedia.inhouse.dsbmobile.web")
            put("Date", now)
            put("LastUpdate", now)
            put("PushId", "")
        }

        val compressed = compress(payload.toString())
        val encoded = Base64.encodeToString(compressed, Base64.NO_WRAP)
        
        val bodyObj = JSONObject().apply {
            put("req", JSONObject().apply {
                put("Data", encoded)
                put("DataType", 1)
            })
        }

        val request = Request.Builder()
            .url(WEB_API_URL)
            .post(bodyObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Referer", "https://www.dsbmobile.de/default.aspx")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseJson = JSONObject(response.body?.string() ?: return@withContext null)
            val respData = responseJson.optString("d", "")
            if (respData.isEmpty()) return@withContext null

            val decoded = decompress(Base64.decode(respData, Base64.DEFAULT))
            val data = JSONObject(String(decoded))

            if (data.optInt("Resultcode", -1) != 0) {
                Log.e(TAG, "Web API error: ${data.optString("ResultStatusInfo")}")
                return@withContext null
            }

            data
        } catch (e: Exception) {
            Log.e(TAG, "Web API call failed", e)
            null
        }
    }

    suspend fun getPlans(): List<PlanInfo> = withContext(Dispatchers.IO) {
        val data = callWebApi() ?: return@withContext emptyList()
        val plans = mutableListOf<PlanInfo>()

        val menuItems = data.optJSONArray("ResultMenuItems") ?: return@withContext emptyList()
        for (i in 0 until menuItems.length()) {
            val menu = menuItems.getJSONObject(i)
            val childs = menu.optJSONArray("Childs") ?: continue
            for (j in 0 until childs.length()) {
                val section = childs.getJSONObject(j)
                val root = section.optJSONObject("Root") ?: continue
                val rootChilds = root.optJSONArray("Childs") ?: continue
                for (k in 0 until rootChilds.length()) {
                    val item = rootChilds.getJSONObject(k)
                    val title = item.optString("Title", "")
                    val date = item.optString("Date", "")
                    val itemChilds = item.optJSONArray("Childs") ?: continue
                    for (l in 0 until itemChilds.length()) {
                        val child = itemChilds.getJSONObject(l)
                        val detail = child.optString("Detail", "")
                        if (detail.isEmpty()) continue
                        val isHtml = detail.lowercase().endsWith(".htm") || detail.lowercase().endsWith(".html")
                        plans.add(PlanInfo(
                            title = child.optString("Title", title),
                            date = date,
                            url = detail,
                            isHtml = isHtml
                        ))
                    }
                }
            }
        }
        plans
    }

    suspend fun getSubstitutions(classFilter: String = ""): List<SubstitutionEntry> = withContext(Dispatchers.IO) {
        val plans = getPlans()
        val entries = mutableListOf<SubstitutionEntry>()

        for (plan in plans) {
            if (!plan.isHtml) continue
            try {
                val request = Request.Builder().url(plan.url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) continue
                
                val contentType = response.header("Content-Type", "")
                if (contentType?.contains("image") == true) continue

                val bytes = response.body?.bytes() ?: continue
                
                // Try UTF-8 first, then Windows-1252 (often used by school software)
                var html: String? = null
                for (encoding in listOf("utf-8", "cp1252", "iso-8859-1", "latin-1")) {
                    try {
                        val decoder = charset(encoding).newDecoder()
                        html = decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
                        break
                    } catch (e: Exception) { continue }
                }
                
                if (html != null) {
                    entries.addAll(parsePlanHtml(html, classFilter))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch plan ${plan.url}", e)
            }
        }
        return@withContext mergeSubstitutionEntries(entries)
    }

    private fun mergeSubstitutionEntries(entries: List<SubstitutionEntry>): List<SubstitutionEntry> {
        return entries.groupBy { 
            // Group by everything EXCEPT lesson and rawText
            listOf(it.day, it.className, it.subject, it.room, it.art, it.text)
        }.map { (group, groupEntries) ->
            if (groupEntries.size == 1) return@map groupEntries.first()
            
            // Extract all individual lesson numbers
            val lessons = groupEntries.flatMap { it.lesson.split(Regex("[,\\-\\s]+")) }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sortedBy { it.toIntOrNull() ?: 999 }
            
            val mergedLesson = when {
                lessons.isEmpty() -> groupEntries.first().lesson
                lessons.size > 1 && lessons.all { it.all { c -> c.isDigit() } } -> {
                    val intLessons = lessons.map { it.toInt() }
                    val isSequential = intLessons.zipWithNext().all { it.second == it.first + 1 }
                    if (isSequential) "${lessons.first()} - ${lessons.last()}"
                    else lessons.joinToString(", ")
                }
                else -> lessons.joinToString(", ")
            }
            
            groupEntries.first().copy(lesson = mergedLesson)
        }
    }

    private fun parsePlanHtml(html: String, classFilter: String): List<SubstitutionEntry> {
        val doc = Jsoup.parse(html)
        val results = mutableListOf<SubstitutionEntry>()
        var currentDay = ""

        val dayDivs = doc.select("div.mon_title")
        val dayMap = mutableMapOf<Int, String>()
        dayDivs.forEach { dayMap[System.identityHashCode(it)] = it.text() }

        val allElements = doc.select("div, tr")
        for (el in allElements) {
            if (el.tagName() == "div" && dayMap.containsKey(System.identityHashCode(el))) {
                currentDay = dayMap[System.identityHashCode(el)] ?: ""
                continue
            }
            if (el.tagName() != "tr" || el.select("th").isNotEmpty()) continue
            
            val cells = el.select("td")
            if (cells.isEmpty()) continue
            
            val raw = el.text()
            if (raw.isEmpty()) continue
            
            // Apply filter check against raw text to catch matches before sanitization
            if (classFilter.isNotEmpty() && !raw.lowercase().contains(classFilter.lowercase())) continue

            val c = cells.map { cellText(it) }
            
            // Validation: A valid substitution row MUST have a lesson/period (usually in column 1)
            val lessonRaw = c.getOrNull(1) ?: ""
            val cleanLesson = formatLesson(cleanString(lessonRaw))
            if (cleanLesson.isEmpty() || cleanLesson.lowercase().contains("klasse")) continue
            
            // Skip metadata rows (like "Untis2026" or "Betroffene Klassen")
            val classRaw = c.getOrNull(0) ?: ""
            val cleanClass = cleanString(classRaw)
            if (cleanClass.lowercase().contains("untis") || cleanClass.lowercase().contains("betroffene")) continue

            results.add(SubstitutionEntry(
                day = cleanString(currentDay),
                className = cleanClass,
                lesson = cleanLesson,
                subject = cleanString(c.getOrNull(2) ?: ""),
                art = cleanString(c.getOrNull(3) ?: ""),
                room = cleanString(c.getOrNull(4) ?: ""),
                vertrVon = "",
                nach = "",
                text = cleanString(c.getOrNull(5) ?: ""),
                rawText = raw
            ))
        }
        return results
    }

    suspend fun getAvailableClasses(): List<String> = withContext(Dispatchers.IO) {
        val entries = getSubstitutions("")
        entries.map { it.className }
            .flatMap { it.split(",").map { s -> s.trim() } }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    private fun formatLesson(input: String): String {
        val clean = input.replace(Regex("\\s+"), " ").trim()
        if (clean.length == 2 && clean[0].isDigit() && clean[1].isDigit()) {
            return "${clean[0]} - ${clean[1]}"
        }
        return clean.replace(Regex("(\\d+)\\s+(\\d+)"), "$1 - $2")
    }

    private fun cellText(cell: org.jsoup.nodes.Element): String {
        return cleanString(cell.text())
    }

    private fun cleanString(input: String): String {
        if (input.isEmpty()) return ""
        return input
            .replace("\u00a0", " ")
            .replace("\uFFFD", "")
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
            // Whitelist: Letters (inc. Umlauts), Numbers, Spaces, and basic punctuation
            .replace(Regex("[^\\p{L}\\p{N} äöüÄÖÜß.,:;!\\-+/()&]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun compress(data: String): ByteArray {
        val bos = ByteArrayOutputStream(data.length)
        val gzip = GZIPOutputStream(bos)
        gzip.write(data.toByteArray())
        gzip.close()
        val compressed = bos.toByteArray()
        bos.close()
        return compressed
    }

    private fun decompress(compressed: ByteArray): ByteArray {
        val bais = compressed.inputStream()
        val gzis = GZIPInputStream(bais)
        val decompressed = gzis.readBytes()
        gzis.close()
        bais.close()
        return decompressed
    }
}
