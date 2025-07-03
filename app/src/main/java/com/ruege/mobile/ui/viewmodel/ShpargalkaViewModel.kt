package com.ruege.mobile.ui.viewmodel

import timber.log.Timber
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.repository.ShpargalkaRepository
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import androidx.lifecycle.Observer

@HiltViewModel
class ShpargalkaViewModel @Inject constructor(
    private val shpargalkaRepository: ShpargalkaRepository
) : ViewModel() {
    
    private val TAG = "ShpargalkaViewModel"
    
    val errorMessage: LiveData<String> = shpargalkaRepository.errorMessage
    
    private val _shpargalkaItemsState = MutableLiveData<Resource<List<ContentItem>>>()
    val shpargalkaItemsState: LiveData<Resource<List<ContentItem>>> = _shpargalkaItemsState
    
    private val _currentPdfFile = MutableLiveData<File?>()
    val currentPdfFile: LiveData<File?> = _currentPdfFile
    
    private val _isPdfLoading = MutableLiveData<Boolean>().apply { value = false }
    val isPdfLoading: LiveData<Boolean> = _isPdfLoading
    
    private val _pdfLoadError = MutableLiveData<String?>()

    private val dbObserver = Observer<List<ContentItem>> { items ->
        _shpargalkaItemsState.value = Resource.Success(items ?: emptyList())
    }
    private val dbLiveData = shpargalkaRepository.getShpargalkaContents()
    
    fun getPdfLoadingStatus(): LiveData<Boolean> = _isPdfLoading
    fun currentPdfFile(): LiveData<File?> = _currentPdfFile
    fun getPdfLoadError(): LiveData<String?> = _pdfLoadError

    init {
        _shpargalkaItemsState.value = Resource.Loading()
        dbLiveData.observeForever(dbObserver)
        syncShpargalkaData()
    }
    
    /**
     * Запускает фоновое обновление данных с сервера.
     */
    fun syncShpargalkaData() {
        viewModelScope.launch {
            try {
                Timber.d("Запуск фоновой синхронизации шпаргалок...")
                shpargalkaRepository.fetchAndCacheShpargalkaItems()
                Timber.d("Фоновая синхронизация шпаргалок завершена.")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при фоновой синхронизации шпаргалок")
            }
        }
    }
    
    fun loadShpargalkaPdf(pdfId: Int) {
        _isPdfLoading.value = true
        _pdfLoadError.value = null
        
        viewModelScope.launch {
            try {
                Timber.d("Начинаем загрузку PDF с ID: $pdfId")
                var pdfFile = shpargalkaRepository.getLocalPdfFile(pdfId)
                
                if (pdfFile == null) {
                    Timber.d("Локальная копия PDF не найдена, загружаем с сервера (ID: $pdfId)")
                    pdfFile = shpargalkaRepository.downloadShpargalkaPdf(pdfId)
                    
                    if (pdfFile == null) {
                        Timber.d("Не удалось загрузить PDF с сервера (ID: $pdfId)")
                        _pdfLoadError.value = "Не удалось загрузить PDF с сервера"
                        _isPdfLoading.value = false
                        return@launch
                    }
                } else {
                    Timber.d("Используем локальную копию PDF (ID: $pdfId), путь: ${pdfFile.absolutePath}")
                }
                
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    Timber.d("PDF файл не существует или пустой: ${pdfFile.absolutePath}, размер: ${pdfFile.length()} байт")
                    _pdfLoadError.value = "PDF файл поврежден или пустой"
                    _isPdfLoading.value = false
                    return@launch
                }
                
                Timber.d("PDF файл успешно получен, размер: ${pdfFile.length()} байт")
                _currentPdfFile.value = pdfFile
                _isPdfLoading.value = false
            } catch (e: Exception) {
                Timber.d("Ошибка при загрузке PDF файла", e)
                _pdfLoadError.value = "Ошибка загрузки: ${e.message}"
                _isPdfLoading.value = false
            }
        }
    }
    
    fun downloadPdf(pdfId: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val file = shpargalkaRepository.downloadShpargalkaPdf(pdfId)
                callback(file != null)
            } catch (e: Exception) {
                Timber.d("Error downloading PDF", e)
                callback(false)
            }
        }
    }
    
    fun getLocalPdfFile(pdfId: Int): File? {
        return shpargalkaRepository.getLocalPdfFile(pdfId)
    }

    override fun onCleared() {
        super.onCleared()
        dbLiveData.removeObserver(dbObserver)
    }
}