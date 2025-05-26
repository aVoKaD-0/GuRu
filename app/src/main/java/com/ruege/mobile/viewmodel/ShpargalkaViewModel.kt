package com.ruege.mobile.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.repository.ShpargalkaRepository
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.model.ShpargalkaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ShpargalkaViewModel @Inject constructor(
    private val shpargalkaRepository: ShpargalkaRepository
) : ViewModel() {
    
    private val TAG = "ShpargalkaViewModel"
    
    // Ошибки загрузки
    val errorMessage: LiveData<String> = shpargalkaRepository.errorMessage
    
    // LiveData для шпаргалок (контент)
    val shpargalkaContents: LiveData<List<ContentItem>> = shpargalkaRepository.getShpargalkaContents()
    
    // LiveData для элементов шпаргалок
    val shpargalkaItems: LiveData<List<ShpargalkaItem>> = shpargalkaRepository.shpargalkaItems
    
    // Загрузка шпаргалок с сервера
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Текущий выбранный PDF
    private val _currentPdfFile = MutableLiveData<File?>()
    val currentPdfFile: LiveData<File?> = _currentPdfFile
    
    // Статус загрузки PDF
    private val _isPdfLoading = MutableLiveData<Boolean>().apply { value = false }
    val isPdfLoading: LiveData<Boolean> = _isPdfLoading
    
    // Ошибка загрузки PDF
    private val _pdfLoadError = MutableLiveData<String?>()
    
    // Геттеры для LiveData
    fun getPdfLoadingStatus(): LiveData<Boolean> = _isPdfLoading
    fun currentPdfFile(): LiveData<File?> = _currentPdfFile
    fun getPdfLoadError(): LiveData<String?> = _pdfLoadError
    
    // Загружаем список шпаргалок
    fun loadShpargalkaItems() {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Запуск загрузки шпаргалок через репозиторий")
                
                // Проверяем текущее состояние данных
                if (shpargalkaContents.value == null || shpargalkaContents.value?.isEmpty() == true) {
                    Log.d(TAG, "Отсутствуют данные в LiveData, запрашиваем с сервера")
                    // Если данных нет, форсируем загрузку с сервера
                    shpargalkaRepository.fetchAndCacheShpargalkaItems()
                } else {
                    Log.d(TAG, "Данные уже есть в LiveData: ${shpargalkaContents.value?.size} элементов")
                }
                
                // Если данные шпаргалок все еще пусты, попробуем еще раз
                if (shpargalkaContents.value == null || shpargalkaContents.value?.isEmpty() == true) {
                    Log.d(TAG, "Данные все еще пусты, повторная попытка обновления через репозиторий")
                    shpargalkaRepository.fetchAndCacheShpargalkaItems()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading shpargalka items", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Загрузка PDF шпаргалки по ID
    fun loadShpargalkaPdf(pdfId: Int) {
        _isPdfLoading.value = true
        _pdfLoadError.value = null
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Начинаем загрузку PDF с ID: $pdfId")
                // Проверяем, есть ли локальная копия
                var pdfFile = shpargalkaRepository.getLocalPdfFile(pdfId)
                
                // Если локальной копии нет, загружаем с сервера
                if (pdfFile == null) {
                    Log.d(TAG, "Локальная копия PDF не найдена, загружаем с сервера (ID: $pdfId)")
                    pdfFile = shpargalkaRepository.downloadShpargalkaPdf(pdfId)
                    
                    if (pdfFile == null) {
                        Log.e(TAG, "Не удалось загрузить PDF с сервера (ID: $pdfId)")
                        _pdfLoadError.value = "Не удалось загрузить PDF с сервера"
                        _isPdfLoading.value = false
                        return@launch
                    }
                } else {
                    Log.d(TAG, "Используем локальную копию PDF (ID: $pdfId), путь: ${pdfFile.absolutePath}")
                }
                
                // Проверяем, что файл существует и имеет ненулевой размер
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    Log.e(TAG, "PDF файл не существует или пустой: ${pdfFile.absolutePath}, размер: ${pdfFile.length()} байт")
                    _pdfLoadError.value = "PDF файл поврежден или пустой"
                    _isPdfLoading.value = false
                    return@launch
                }
                
                Log.d(TAG, "PDF файл успешно получен, размер: ${pdfFile.length()} байт")
                _currentPdfFile.value = pdfFile
                _isPdfLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке PDF файла", e)
                _pdfLoadError.value = "Ошибка загрузки: ${e.message}"
                _isPdfLoading.value = false
            }
        }
    }
    
    // Проверка, скачан ли
    fun isPdfDownloaded(pdfId: Int): Boolean {
        return shpargalkaRepository.isPdfDownloaded(pdfId)
    }
    
    // Скачать PDF файл (для кнопки "Скачать")
    fun downloadPdf(pdfId: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val file = shpargalkaRepository.downloadShpargalkaPdf(pdfId)
                callback(file != null)
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading PDF", e)
                callback(false)
            }
        }
    }
    
    // Получить локальный файл PDF
    fun getLocalPdfFile(pdfId: Int): File? {
        return shpargalkaRepository.getLocalPdfFile(pdfId)
    }
    
    // Принудительное обновление данных шпаргалок
    fun refreshShpargalkaData() {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Принудительное обновление данных шпаргалок")
                
                // Сначала запрашиваем актуальные данные с сервера
                shpargalkaRepository.fetchAndCacheShpargalkaItems()
                
                // Затем запрашиваем обновленные данные из БД
                val refreshedData = shpargalkaRepository.refreshShpargalkaContents()
                
                // Упрощаем логику: просто логируем результат
                refreshedData.observeForever(object : androidx.lifecycle.Observer<List<ContentItem>> {
                    override fun onChanged(items: List<ContentItem>) {
                        Log.d(TAG, "Получены обновленные данные шпаргалок: ${items.size} элементов")
                        
                        // Сразу удаляем этот одноразовый наблюдатель
                        refreshedData.removeObserver(this)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing shpargalka data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 