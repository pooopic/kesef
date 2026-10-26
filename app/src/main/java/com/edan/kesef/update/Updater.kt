package com.edan.kesef.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.edan.kesef.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val apkUrl: String,
)

/**
 * עדכונים ישירות מהאפליקציה, בלי להוריד ZIP ידנית.
 *
 * איך זה עובד:
 * 1. כל push ל-GitHub מפעיל את GitHub Actions, שבונה APK ומפרסם אותו כ-Release (למשל v1.12).
 * 2. האפליקציה שואלת את GitHub: "מה ה-Release האחרון?" ומשווה למספר הגרסה שלה.
 * 3. אם יש חדש: מורידה את ה-APK ופותחת את מסך ההתקנה של אנדרואיד. לחיצה על "עדכן" וזהו.
 */
object Updater {

    /** בודק ב-GitHub מה הגרסה האחרונה. null אם אין רשת או שמשהו נכשל. */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            if (conn.responseCode != 200) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })

            val tag = json.getString("tag_name") // "v1.12"
            val code = tag.substringAfterLast('.').toIntOrNull() ?: return@withContext null
            val assets = json.getJSONArray("assets")
            val apkUrl = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") }
                ?.getString("browser_download_url")
                ?: return@withContext null

            ReleaseInfo(code, tag.removePrefix("v"), json.optString("body"), apkUrl)
        } catch (e: Exception) {
            null
        }
    }

    fun isNewer(info: ReleaseInfo) = info.versionCode > BuildConfig.VERSION_CODE

    /** מוריד את ה-APK לתיקייה זמנית. onProgress מקבל ערך בין 0 ל-1. */
    suspend fun download(context: Context, info: ReleaseInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // מוחקים הורדות ישנות
            val file = File(dir, "kesef-${info.versionName}.apk")

            val conn = URL(info.apkUrl).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true // GitHub מפנה לשרת הקבצים שלו
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
            file
        }

    /**
     * אנדרואיד דורש אישור חד-פעמי "התקנת אפליקציות לא ידועות" לאפליקציה שלנו.
     * אם עוד לא אישרת, פותחים את המסך הזה ומחזירים false.
     */
    fun canInstall(context: Context) = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun install(context: Context, apk: File) {
        // FileProvider נותן למתקין של אנדרואיד גישה זמנית לקובץ שלנו בלבד
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
