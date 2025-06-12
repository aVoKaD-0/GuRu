package com.ruege.mobile.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ruege.mobile.data.local.AppDatabase
import com.ruege.mobile.data.local.dao.CategoryDao
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.entity.CategoryEntity
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.data.network.api.ShpargalkiApiService
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.model.ShpargalkaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val contentDao: ContentDao,
    private val categoryDao: CategoryDao,
    private val appDatabase: AppDatabase,
    private val context: Context
) {
    private val TAG = "ShpargalkaRepository"
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _shpargalkaItems = MutableLiveData<List<ShpargalkaItem>>()
    val shpargalkaItems: LiveData<List<ShpargalkaItem>> = _shpargalkaItems
    
    private val _contentItems = MutableLiveData<List<ContentItem>>()
    
    suspend fun fetchAndCacheShpargalkaItems() {
        try {
            val response = shpargalkiApiService.getShpargalkaGroups()
            
            if (response.isSuccessful && response.body() != null) {
                val shpargalkaData = response.body()!!
                
                if (shpargalkaData.isEmpty()) {
                    Log.d(TAG, "Получен пустой список шпаргалок с сервера")
                    return
                }
                
                val items = mutableListOf<ShpargalkaItem>()
                val contentEntities = mutableListOf<ContentEntity>()
                
                for (itemData in shpargalkaData) {
                    val id = (itemData["pdf_id"] as? String)?.toIntOrNull() ?: 0
                    val title = itemData["title"] as? String ?: ""
                    val description = itemData["description"] as? String ?: ""
                    val fileName = itemData["file_name"] as? String
                    val publishTime = itemData["publication_date"] as? String
                    
                    val contentId = "shpargalka_$id"
                    
                    contentEntities.add(ContentEntity.createForKotlin(
                        contentId,
                        title,
                        description,
                        "shpargalka",
                        "shpargalki",
                        false,
                        false,
                        id
                    ))
                    
                    items.add(ShpargalkaItem(
                        id = id,
                        title = title,
                        description = description,
                        groupId = "shpargalki",
                        groupTitle = "Шпаргалки",
                        fileName = fileName,
                        publishTime = publishTime,
                        isDownloaded = isPdfDownloaded(id)
                    ))
                }
                
                withContext(Dispatchers.IO) {
                    val categoryExists = categoryDao.getCategoryById("shpargalki").value != null
                    
                    if (!categoryExists) {
                        val shpargalkiCategory = CategoryEntity(
                            "shpargalki",
                            "Шпаргалки",
                            "Полезные материалы для подготовки к ЕГЭ",
                            "",
                            0,
                            true
                        )
                        categoryDao.insert(shpargalkiCategory)
                        Log.d(TAG, "Создана категория 'shpargalki' в БД")
                    }
                }
                
                val needUpdate = withContext(Dispatchers.IO) {
                    val existingIds = contentDao.getContentIdsByType("shpargalka")
                    val newIds = contentEntities.map { it.contentId }.toSet()
                    
                    existingIds.size != newIds.size || !existingIds.containsAll(newIds)
                }
                
                if (needUpdate) {
                    withContext(Dispatchers.IO) {
                        contentDao.insertAll(contentEntities)
                        Log.d(TAG, "Сохранено ${contentEntities.size} шпаргалок в БД")
                        
                        val contentItems = contentEntities.map { entity ->
                            ContentItem(
                                entity.contentId,
                                entity.title,
                                entity.description ?: "",
                                entity.type,
                                entity.parentId ?: "",
                                entity.isDownloaded,
                                false
                            )
                        }
                        
                        withContext(Dispatchers.Main) {
                            _contentItems.value = contentItems
                            Log.d(TAG, "Обновлен кэш ContentItems с ${contentItems.size} элементами шпаргалок")
                        }
                    }
                } else {
                    Log.d(TAG, "Данные шпаргалок не изменились, пропускаем обновление БД")
                }
                
                _shpargalkaItems.postValue(items)
                Log.d(TAG, "Обновлены данные шпаргалок в памяти: ${items.size} записей")
            } else {
                _errorMessage.postValue("Ошибка загрузки шпаргалок: ${response.message()}")
                Log.e(TAG, "Error fetching shpargalka items: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            _errorMessage.postValue("Ошибка соединения")
            Log.e(TAG, "Exception fetching shpargalka items", e)
        }
    }
    
    fun getShpargalkaContents(): LiveData<List<ContentItem>> {
        if (_contentItems.value == null || _contentItems.value?.isEmpty() == true) {
            refreshShpargalkaContentsInternal()
        }
        return _contentItems
    }
    
    private fun refreshShpargalkaContentsInternal() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contentEntities = contentDao.getContentsByTypeSync("shpargalka")
                
                if (!contentEntities.isNullOrEmpty()) {
                    val items = contentEntities.map { entity ->
                        ContentItem(
                            entity.contentId,
                            entity.title,
                            entity.description ?: "",
                            entity.type,
                            entity.parentId ?: "",
                            entity.isDownloaded,
                            false
                        )
                    }
                    _contentItems.postValue(items)
                    Log.d(TAG, "Загружено ${items.size} шпаргалок из БД для отображения")
                } else {
                    Log.d(TAG, "Шпаргалки в БД не найдены, запускаем загрузку")
                    fetchAndCacheShpargalkaItems()
                    
                    val updatedEntities = contentDao.getContentsByTypeSync("shpargalka")
                    val updatedItems = updatedEntities.map { entity ->
                        ContentItem(
                            entity.contentId,
                            entity.title,
                            entity.description ?: "",
                            entity.type,
                            entity.parentId ?: "",
                            entity.isDownloaded,
                            false
                        )
                    }
                    _contentItems.postValue(updatedItems)
                    Log.d(TAG, "После загрузки с сервера получено ${updatedItems.size} шпаргалок")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting shpargalka contents", e)
                _contentItems.postValue(emptyList())
                _errorMessage.postValue("Ошибка при получении данных шпаргалок")
            }
        }
    }
    
    fun refreshShpargalkaContents(): LiveData<List<ContentItem>> {
        Log.d(TAG, "Принудительное обновление данных шпаргалок из БД")
        
        refreshShpargalkaContentsInternal()
        
        return _contentItems
    }
    
    suspend fun downloadShpargalkaPdf(pdfId: Int): File? {
        try {
            Log.d(TAG, "Начинаем загрузку PDF с ID: $pdfId")

            if (shpargalkiApiService == null) {
                Log.e(TAG, "shpargalkiApiService IS NULL!")
                _errorMessage.postValue("Ошибка: Сервис API не инициализирован.")
                return null
            }
            Log.i(TAG, "shpargalkiApiService инстанс: $shpargalkiApiService")

            Log.i(TAG, "Вызов shpargalkiApiService.getShpargalkaPdf($pdfId)")
            val response: Response<ResponseBody>
            try {
                response = shpargalkiApiService.getShpargalkaPdf(pdfId)
                Log.i(TAG, "Ответ от shpargalkiApiService.getShpargalkaPdf($pdfId): Успешно=${response.isSuccessful}, Код=${response.code()}, Тело пустое=${response.body() == null}")
            } catch (e: Exception) {
                Log.e(TAG, "ИСКЛЮЧЕНИЕ непосредственно при вызове shpargalkiApiService.getShpargalkaPdf($pdfId)", e)
                _errorMessage.postValue("Ошибка сети: ${e.message}")
                return null
            }

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val contentType = response.headers()["Content-Type"]
                val contentLength = response.headers()["Content-Length"]?.toLongOrNull() ?: 0
                
                Log.d(TAG, "PDF загружен с сервера: Content-Type=$contentType, Content-Length=$contentLength байт")
                
                if (contentType?.contains("application/pdf") != true) {
                    Log.e(TAG, "Неверный тип контента: $contentType, должен быть application/pdf")
                    _errorMessage.postValue("Ошибка: сервер вернул неверный тип файла")
                    return null
                }
                
                val shpargalkaDir = File(context.filesDir, "shpargalki")
                if (!shpargalkaDir.exists()) {
                    val dirCreated = shpargalkaDir.mkdirs()
                    Log.d(TAG, "Директория для шпаргалок создана: $dirCreated")
                }
                
                val pdfFile = File(shpargalkaDir, "shpargalka_$pdfId.pdf")
                
                val success = writeResponseBodyToDisk(responseBody, pdfFile)
                
                if (success) {
                    val fileSize = pdfFile.length()
                    Log.d(TAG, "PDF файл успешно сохранен локально: ${pdfFile.absolutePath}, размер: $fileSize байт")
                    
                    if (isPdfValid(pdfFile)) {
                        withContext(Dispatchers.IO) {
                            val contentId = "shpargalka_$pdfId"
                            contentDao.updateDownloadStatus(contentId, true)
                            Log.d(TAG, "Статус скачивания обновлен в БД для $contentId")
                        }
                        return pdfFile
                    } else {
                        Log.e(TAG, "PDF файл невалиден или поврежден")
                        _errorMessage.postValue("Ошибка: загруженный PDF-файл поврежден")
                        val deleted = pdfFile.delete()
                        Log.d(TAG, "Поврежденный файл удален: $deleted")
                        return null
                    }
                } else {
                    Log.e(TAG, "Не удалось записать PDF файл на диск")
                    _errorMessage.postValue("Ошибка при сохранении файла")
                    return null
                }
            } else {
                val errorCode = response.code()
                val errorMessage = response.message()
                Log.e(TAG, "Ошибка загрузки PDF: $errorCode $errorMessage")
                _errorMessage.postValue("Ошибка загрузки PDF: $errorCode $errorMessage")
                Log.e(TAG, "Полный ответ при ошибке: $response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при загрузке PDF", e)
            _errorMessage.postValue("Ошибка при скачивании файла: ${e.message}")
        }
        
        return null
    }
    
    private fun isPdfValid(file: File): Boolean {
        return try {
            val fileSize = file.length()
            Log.d(TAG, "Проверка PDF файла, размер: $fileSize байт")
            
            if (fileSize < 100) {
                Log.e(TAG, "PDF файл слишком маленький: $fileSize байт")
                return false
            }
            
            val buffer = ByteArray(8)
            val inputStream = FileInputStream(file)
            val read = inputStream.read(buffer)
            inputStream.close()
            
            if (read < 4) {
                Log.e(TAG, "Не удалось прочитать заголовок PDF")
                return false
            }
            
            val header = String(buffer, 0, 5)
            val isValidHeader = header == "%PDF-"
            
            if (!isValidHeader) {
                Log.e(TAG, "Некорректный заголовок PDF: $header")
                return false
            }
            
            Log.d(TAG, "PDF файл имеет корректный заголовок: $header")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке PDF файла", e)
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
                Log.d(TAG, "Существующий файл удален: $deleted")
            }
            
            val contentLength = body.contentLength()
            Log.d(TAG, "Начинаем запись файла. Размер содержимого: $contentLength байт")
            
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
                        Log.d(TAG, "Прогресс записи файла: $progress%")
                    }
                }
                
                outputStream.flush()
                
                Log.d(TAG, "Файл успешно записан на диск: ${file.absolutePath}, размер: ${file.length()} байт")
                true
            } finally {
                try {
                    inputStream?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Ошибка при закрытии входного потока", e)
                }
                
                try {
                    outputStream?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Ошибка при закрытии выходного потока", e)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка при записи файла на диск", e)
            
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Частично записанный файл удален из-за ошибки")
            }
            
            false
        }
    }
} 