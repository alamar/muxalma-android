import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AssetExtractor {
    
    // Извлекает файл из assets во временную папку (cache)
    fun extractToCache(context: Context, assetPath: String, targetDir: File?): File? {
        val cacheFile = File(targetDir, assetPath)
        
        // Если файл уже существует и не пустой, возвращаем его
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return cacheFile
        }
        
        // Создаём родительские директории если нужно
        cacheFile.parentFile?.mkdirs()
        
        return try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}