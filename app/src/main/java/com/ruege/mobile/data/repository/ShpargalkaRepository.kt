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
    
    // Для отслеживания ошибок
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // Кэш для шпаргалок, чтобы не запрашивать их каждый раз
    private val _shpargalkaItems = MutableLiveData<List<ShpargalkaItem>>()
    val shpargalkaItems: LiveData<List<ShpargalkaItem>> = _shpargalkaItems
    
    // Добавляем MutableLiveData для отслеживания ContentItems
    private val _contentItems = MutableLiveData<List<ContentItem>>()
    
    // Получить список шпаргалок с сервера и сохранить в БД
    suspend fun fetchAndCacheShpargalkaItems() {
        try {
            // Проверяем, есть ли уже данные в LiveData
            if (!_shpargalkaItems.value.isNullOrEmpty()) {
                Log.d(TAG, "Данные шпаргалок уже загружены, пропускаем запрос к API")
                return
            }
            
            // Проверяем, есть ли данные в базе данных
            val existingContent = withContext(Dispatchers.IO) {
                contentDao.getContentsByTypeSync("shpargalka")
            }
            
            if (!existingContent.isNullOrEmpty()) {
                Log.d(TAG, "Данные шпаргалок уже в БД, загружаем из локальной БД")
                val items = existingContent.map { entity ->
                    ShpargalkaItem(
                        id = entity.orderPosition,
                        title = entity.title,
                        description = entity.description ?: "",
                        groupId = entity.parentId ?: "shpargalki",
                        groupTitle = "Шпаргалки",
                        fileName = null,
                        publishTime = null,
                        isDownloaded = entity.isDownloaded
                    )
                }
                _shpargalkaItems.postValue(items)
                return
            }
            
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
                    
                    // Создаем ContentEntity для сохранения в БД
                    contentEntities.add(ContentEntity.createForKotlin(
                        contentId,
                        title,
                        description,
                        "shpargalka",
                        "shpargalki", // все шпаргалки относятся к одному родителю
                        false,
                        false,
                        id // используем id как порядковый номер
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
                
                // Проверяем наличие родительской категории "shpargalki"
                withContext(Dispatchers.IO) {
                    val categoryExists = categoryDao.getCategoryById("shpargalki").value != null
                    
                    if (!categoryExists) {
                        // Создаем категорию "shpargalki", если её нет
                        val shpargalkiCategory = CategoryEntity(
                            "shpargalki",
                            "Шпаргалки",
                            "Полезные материалы для подготовки к ЕГЭ",
                            "", // iconUrl - может быть пустым или указать URL иконки
                            0, // orderPosition
                            true // isVisible
                        )
                        categoryDao.insert(shpargalkiCategory)
                        Log.d(TAG, "Создана категория 'shpargalki' в БД")
                    }
                }
                
                // Проверяем, что данные действительно изменились перед сохранением в БД
                val needUpdate = withContext(Dispatchers.IO) {
                    val existingIds = contentDao.getContentIdsByType("shpargalka")
                    val newIds = contentEntities.map { it.contentId }.toSet()
                    
                    existingIds.size != newIds.size || !existingIds.containsAll(newIds)
                }
                
                if (needUpdate) {
                    // Показываем сообщение о сохранении данных в БД
                    withContext(Dispatchers.IO) {
                        contentDao.insertAll(contentEntities)
                        Log.d(TAG, "Сохранено ${contentEntities.size} шпаргалок в БД")
                        
                        // После сохранения в БД, также обновляем кэш ContentItems
                        val contentItems = contentEntities.map { entity ->
                            ContentItem(
                                entity.contentId,
                                entity.title,
                                entity.description ?: "",
                                entity.type,
                                entity.parentId ?: "",
                                entity.isDownloaded
                            )
                        }
                        
                        // Обновляем LiveData с ContentItem напрямую
                        withContext(Dispatchers.Main) {
                            // Обновляем существующий _contentItems вместо создания нового LiveData
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
    
    // Получение шпаргалок из локальной БД
    fun getShpargalkaContents(): LiveData<List<ContentItem>> {
        // Заполняем кэш при первом обращении
        if (_contentItems.value == null || _contentItems.value?.isEmpty() == true) {
            refreshShpargalkaContentsInternal()
        }
        return _contentItems
    }
    
    // Внутренний метод для обновления содержимого шпаргалок
    private fun refreshShpargalkaContentsInternal() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Используем синхронный метод запроса, а не LiveData
                val contentEntities = contentDao.getContentsByTypeSync("shpargalka")
                
                if (!contentEntities.isNullOrEmpty()) {
                    val items = contentEntities.map { entity ->
                        ContentItem(
                            entity.contentId,
                            entity.title,
                            entity.description ?: "",
                            entity.type,
                            entity.parentId ?: "",
                            entity.isDownloaded
                        )
                    }
                    // Используем postValue вместо value для обновления из фонового потока
                    _contentItems.postValue(items)
                    Log.d(TAG, "Загружено ${items.size} шпаргалок из БД для отображения")
                } else {
                    Log.d(TAG, "Шпаргалки в БД не найдены, запускаем загрузку")
                    // Если данных в БД нет, запускаем загрузку с сервера
                    fetchAndCacheShpargalkaItems()
                    
                    // После загрузки с сервера пробуем еще раз получить данные из БД
                    val updatedEntities = contentDao.getContentsByTypeSync("shpargalka")
                    val updatedItems = updatedEntities.map { entity ->
                        ContentItem(
                            entity.contentId,
                            entity.title,
                            entity.description ?: "",
                            entity.type,
                            entity.parentId ?: "",
                            entity.isDownloaded
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
    
    // Метод для принудительного обновления данных шпаргалок из БД в LiveData
    fun refreshShpargalkaContents(): LiveData<List<ContentItem>> {
        Log.d(TAG, "Принудительное обновление данных шпаргалок из БД")
        
        // Используем внутренний метод для обновления данных
        refreshShpargalkaContentsInternal()
        
        // Возвращаем тот же LiveData, который используется в getShpargalkaContents
        return _contentItems
    }
    
    // Загрузка PDF файла шпаргалки и сохранение его локально
    suspend fun downloadShpargalkaPdf(pdfId: Int): File? {
        try {
            Log.d(TAG, "Начинаем загрузку PDF с ID: $pdfId")
            val response = shpargalkiApiService.getShpargalkaPdf(pdfId)
            
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val contentType = response.headers()["Content-Type"]
                val contentLength = response.headers()["Content-Length"]?.toLongOrNull() ?: 0
                
                Log.d(TAG, "PDF загружен с сервера: Content-Type=$contentType, Content-Length=$contentLength байт")
                
                // Проверяем тип контента
                if (contentType?.contains("application/pdf") != true) {
                    Log.e(TAG, "Неверный тип контента: $contentType, должен быть application/pdf")
                    _errorMessage.postValue("Ошибка: сервер вернул неверный тип файла")
                    return null
                }
                
                // Создаем директорию для шпаргалок, если её нет
                val shpargalkaDir = File(context.filesDir, "shpargalki")
                if (!shpargalkaDir.exists()) {
                    val dirCreated = shpargalkaDir.mkdirs()
                    Log.d(TAG, "Директория для шпаргалок создана: $dirCreated")
                }
                
                // Создаем файл для сохранения PDF
                val pdfFile = File(shpargalkaDir, "shpargalka_$pdfId.pdf")
                
                // Записываем содержимое в файл
                val success = writeResponseBodyToDisk(responseBody, pdfFile)
                
                if (success) {
                    val fileSize = pdfFile.length()
                    Log.d(TAG, "PDF файл успешно сохранен локально: ${pdfFile.absolutePath}, размер: $fileSize байт")
                    
                    // Проверяем целостность PDF
                    if (isPdfValid(pdfFile)) {
                        // Обновляем статус скачивания в БД
                        withContext(Dispatchers.IO) {
                            val contentId = "shpargalka_$pdfId"
                            contentDao.updateDownloadStatus(contentId, true)
                            Log.d(TAG, "Статус скачивания обновлен в БД для $contentId")
                        }
                        return pdfFile
                    } else {
                        // PDF поврежден или невалиден
                        Log.e(TAG, "PDF файл невалиден или поврежден")
                        _errorMessage.postValue("Ошибка: загруженный PDF-файл поврежден")
                        // Удаляем поврежденный файл
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при загрузке PDF", e)
            _errorMessage.postValue("Ошибка при скачивании файла: ${e.message}")
        }
        
        return null
    }
    
    // Проверка валидности PDF-файла
    private fun isPdfValid(file: File): Boolean {
        return try {
            // Проверяем размер файла
            val fileSize = file.length()
            Log.d(TAG, "Проверка PDF файла, размер: $fileSize байт")
            
            if (fileSize < 100) {
                Log.e(TAG, "PDF файл слишком маленький: $fileSize байт")
                return false
            }
            
            // Проверяем заголовок PDF
            val buffer = ByteArray(8)
            val inputStream = FileInputStream(file)
            val read = inputStream.read(buffer)
            inputStream.close()
            
            if (read < 4) {
                Log.e(TAG, "Не удалось прочитать заголовок PDF")
                return false
            }
            
            // Проверяем сигнатуру PDF файла (%PDF-)
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
    
    // Получение ранее загруженного PDF файла
    fun getLocalPdfFile(pdfId: Int): File? {
        val shpargalkaDir = File(context.filesDir, "shpargalki")
        val pdfFile = File(shpargalkaDir, "shpargalka_$pdfId.pdf")
        
        return if (pdfFile.exists()) pdfFile else null
    }
    
    // Проверка, скачан ли PDF файл
    fun isPdfDownloaded(pdfId: Int): Boolean {
        val shpargalkaDir = File(context.filesDir, "shpargalki")
        val pdfFile = File(shpargalkaDir, "shpargalka_$pdfId.pdf")
        
        return pdfFile.exists()
    }
    
    // Запись содержимого ResponseBody в файл
    private fun writeResponseBodyToDisk(body: ResponseBody, file: File): Boolean {
        return try {
            // Если файл уже существует, удаляем его
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Существующий файл удален: $deleted")
            }
            
            // Получаем размер содержимого
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
                    
                    // Логируем прогресс загрузки для больших файлов
                    if (contentLength > 0 && totalBytesRead % (contentLength / 10) < 4096) {
                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                        Log.d(TAG, "Прогресс записи файла: $progress%")
                    }
                }
                
                outputStream.flush()
                
                Log.d(TAG, "Файл успешно записан на диск: ${file.absolutePath}, размер: ${file.length()} байт")
                true
            } finally {
                // Гарантированно закрываем потоки
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
            
            // Удаляем частично записанный файл
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Частично записанный файл удален из-за ошибки")
            }
            
            false
        }
    }
} 