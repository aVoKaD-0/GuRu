package com.ruege.mobile.data.repository

import android.content.Context
import timber.log.Timber
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.ruege.mobile.data.local.dao.ShpargalkaDao
import com.ruege.mobile.data.local.entity.ShpargalkaEntity
import com.ruege.mobile.data.network.api.ShpargalkiApiService
import com.ruege.mobile.model.ContentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShpargalkaRepository @Inject constructor(
    private val shpargalkiApiService: ShpargalkiApiService,
    private val shpargalkaDao: ShpargalkaDao,
    private val context: Context
) {
    private val TAG = "ShpargalkaRepository"
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    suspend fun fetchAndCacheShpargalkaItems() {
        try {
            val response = shpargalkiApiService.getShpargalkaGroups()
            
            if (response.isSuccessful && response.body() != null) {
                val shpargalkaData = response.body()!!
                
                if (shpargalkaData.isEmpty()) {
                    Timber.d("Получен пустой список шпаргалок с сервера, очищаем кэш.")
                    shpargalkaDao.deleteAll()
                    return
                }
                
                val existingCache = shpargalkaDao.getAllShpargalkiSync()
                val existingCacheMap = existingCache.associateBy { it.id }

                val newCacheEntities = shpargalkaData.mapNotNull { itemData ->
                    val id = (itemData["pdf_id"] as? String)?.toIntOrNull()
                    if (id == null) {
                        Timber.d("Элемент шпаргалки не имеет pdf_id: $itemData")
                        return@mapNotNull null
                    }
                    val title = itemData["title"] as? String ?: "Без названия"
                    val description = itemData["description"] as? String ?: ""
                    
                    ShpargalkaEntity(
                        id = id,
                        title = title,
                        description = description,
                        isDownloaded = existingCacheMap[id]?.isDownloaded ?: isPdfDownloaded(id)
                    )
                }

                shpargalkaDao.replaceAll(newCacheEntities)
                Timber.d("Обновлен кэш шпаргалок в БД: ${newCacheEntities.size} записей")

            } else {
                _errorMessage.postValue("Ошибка загрузки шпаргалок: ${response.message()}")
                Timber.d("Error fetching shpargalka items: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            _errorMessage.postValue("Ошибка соединения")
            Timber.d("Exception fetching shpargalka items", e)
        }
    }
    
    fun getShpargalkaContents(): LiveData<List<ContentItem>> {
        Timber.d("Запрошены шпаргалки из локального кэша (БД)")
        return shpargalkaDao.getAllShpargalki().map { entityList ->
            entityList.map { entity ->
                ContentItem(
                    contentId = "shpargalka_${entity.id}",
                    title = entity.title,
                    description = entity.description,
                    type = "shpargalka",
                    parentId = "shpargalki",
                    isDownloaded = entity.isDownloaded,
                    isSelected = false
                )
            }
        }
    }
    
    suspend fun downloadShpargalkaPdf(pdfId: Int): File? {
        try {
            Timber.d("Начинаем загрузку PDF с ID: $pdfId")

            if (shpargalkiApiService == null) {
                Timber.d("shpargalkiApiService IS NULL!")
                _errorMessage.postValue("Ошибка: Сервис API не инициализирован.")
                return null
            }
            Timber.i("shpargalkiApiService инстанс: $shpargalkiApiService")

            Timber.i("Вызов shpargalkiApiService.getShpargalkaPdf($pdfId)")
            val response: Response<ResponseBody>
            try {
                response = shpargalkiApiService.getShpargalkaPdf(pdfId)
                Timber.i("Ответ от shpargalkiApiService.getShpargalkaPdf($pdfId): Успешно=${response.isSuccessful}, Код=${response.code()}, Тело пустое=${response.body() == null}")
            } catch (e: Exception) {
                Timber.d("ИСКЛЮЧЕНИЕ непосредственно при вызове shpargalkiApiService.getShpargalkaPdf($pdfId)", e)
                _errorMessage.postValue("Ошибка сети: ${e.message}")
                return null
            }

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val contentType = response.headers()["Content-Type"]
                val contentLength = response.headers()["Content-Length"]?.toLongOrNull() ?: 0
                
                Timber.d("PDF загружен с сервера: Content-Type=$contentType, Content-Length=$contentLength байт")
                
                if (contentType?.contains("application/pdf") != true) {
                    Timber.d("Неверный тип контента: $contentType, должен быть application/pdf")
                    _errorMessage.postValue("Ошибка: сервер вернул неверный тип файла")
                    return null
                }
                
                val shpargalkaDir = File(context.filesDir, "shpargalki")
                if (!shpargalkaDir.exists()) {
                    val dirCreated = shpargalkaDir.mkdirs()
                    Timber.d("Директория для шпаргалок создана: $dirCreated")
                }
                
                val pdfFile = File(shpargalkaDir, "shpargalka_$pdfId.pdf")
                
                val success = writeResponseBodyToDisk(responseBody, pdfFile)
                
                if (success) {
                    val fileSize = pdfFile.length()
                    Timber.d("PDF файл успешно сохранен локально: ${pdfFile.absolutePath}, размер: $fileSize байт")
                    
                    if (isPdfValid(pdfFile)) {
                        withContext(Dispatchers.IO) {
                            shpargalkaDao.updateDownloadStatus(pdfId, true)
                            Timber.d("Статус скачивания обновлен в кэше для shpargalka_$pdfId")
                        }
                        return pdfFile
                    } else {
                        Timber.d("PDF файл невалиден или поврежден")
                        _errorMessage.postValue("Ошибка: загруженный PDF-файл поврежден")
                        val deleted = pdfFile.delete()
                        Timber.d("Поврежденный файл удален: $deleted")
                        return null
                    }
                } else {
                    Timber.d("Не удалось записать PDF файл на диск")
                    _errorMessage.postValue("Ошибка при сохранении файла")
                    return null
                }
            } else {
                val errorCode = response.code()
                val errorMessage = response.message()
                Timber.d("Ошибка загрузки PDF: $errorCode $errorMessage")
                _errorMessage.postValue("Ошибка загрузки PDF: $errorCode $errorMessage")
                Timber.d("Полный ответ при ошибке: $response")
            }
        } catch (e: Exception) {
            Timber.d("Исключение при загрузке PDF", e)
            _errorMessage.postValue("Ошибка при скачивании файла: ${e.message}")
        }
        
        return null
    }
    
    private fun isPdfValid(file: File): Boolean {
        return try {
            val fileSize = file.length()
            Timber.d("Проверка PDF файла, размер: $fileSize байт")
            
            if (fileSize < 100) {
                Timber.d("PDF файл слишком маленький: $fileSize байт")
                return false
            }
            
            val buffer = ByteArray(8)
            val inputStream = FileInputStream(file)
            val read = inputStream.read(buffer)
            inputStream.close()
            
            if (read < 4) {
                Timber.d("Не удалось прочитать заголовок PDF")
                return false
            }
            
            val header = String(buffer, 0, 5)
            val isValidHeader = header == "%PDF-"
            
            if (!isValidHeader) {
                Timber.d("Некорректный заголовок PDF: $header")
                return false
            }
            
            Timber.d("PDF файл имеет корректный заголовок: $header")
            true
        } catch (e: Exception) {
            Timber.d("Ошибка при проверке PDF файла", e)
            false
        }
    }
    
    fun getLocalPdfFile(pdfId: Int): File? {
        val shpargalkaDir = File(context.filesDir, "shpargalki")
        val pdfFile = File(shpargalkaDir, "shpargalka_$pdfId.pdf")
        
        return if (pdfFile.exists()) pdfFile else null
    }
    
    fun isPdfDownloaded(pdfId: Int): Boolean {
        val shpargalkaDir = File(context.filesDir, "shpargalki")
        val pdfFile = File(shpargalkaDir, "shpargalka_$pdfId.pdf")
        
        return pdfFile.exists()
    }
    
    private fun writeResponseBodyToDisk(body: ResponseBody, file: File): Boolean {
        return try {
            if (file.exists()) {
                val deleted = file.delete()
                Timber.d("Существующий файл удален: $deleted")
            }
            
            val contentLength = body.contentLength()
            Timber.d("Начинаем запись файла. Размер содержимого: $contentLength байт")
            
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            
            try {
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead: Long = 0
                
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    if (contentLength > 0 && totalBytesRead % (contentLength / 10) < 4096) {
                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                        Timber.d("Прогресс записи файла: $progress%")
                    }
                }
                
                outputStream.flush()
                
                Timber.d("Файл успешно записан на диск: ${file.absolutePath}, размер: ${file.length()} байт")
                true
            } finally {
                try {
                    inputStream?.close()
                } catch (e: IOException) {
                    Timber.d("Ошибка при закрытии входного потока", e)
                }
                
                try {
                    outputStream?.close()
                } catch (e: IOException) {
                    Timber.d("Ошибка при закрытии выходного потока", e)
                }
            }
        } catch (e: IOException) {
            Timber.d("Ошибка при записи файла на диск", e)
            
            if (file.exists()) {
                file.delete()
                Timber.d("Частично записанный файл удален из-за ошибки")
            }
            
            false
        }
    }
} 