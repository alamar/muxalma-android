package pvt.muxalma.android

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object ClientUUID {
    private const val PREFS_NAME = "client_prefs"
    private const val KEY_CLIENT_UUID = "client_uuid"
    
    fun getUUID(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var uuid = prefs.getString(KEY_CLIENT_UUID, null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_CLIENT_UUID, uuid).apply()
        }
        return uuid
    }
}
