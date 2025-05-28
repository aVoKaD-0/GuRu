package com.ruege.mobile.ui.theory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.databinding.FragmentTheoryBinding
import com.ruege.mobile.ui.viewmodel.ContentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TheoryFragment : Fragment(), TheoryAdapter.TheoryItemClickListener {

    companion object {
        private const val TAG = "TheoryFragment"
    }

    private var _binding: FragmentTheoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TheoryAdapter
    private lateinit var viewModel: ContentViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            _binding = FragmentTheoryBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при создании view: ${e.message}", e)
            inflater.inflate(R.layout.fragment_theory_fallback, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            // Инициализируем ViewModel
            viewModel = ViewModelProvider(requireActivity())[ContentViewModel::class.java]
            
            // Настраиваем RecyclerView
            binding.recyclerViewTheory.apply {
                layoutManager = LinearLayoutManager(requireContext())
                
                // Создаем и устанавливаем адаптер
                adapter = TheoryAdapter(this@TheoryFragment).also {
                    this@TheoryFragment.adapter = it
                }
            }
            
            // Наблюдаем за данными из ViewModel
            viewModel.theoryTopicsLiveData.observe(viewLifecycleOwner) { topics ->
                adapter.updateItems(topics)
            }
            
            // Данные теории загружаются при запуске приложения
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при настройке RecyclerView: ${e.message}", e)
            Toast.makeText(context, "Произошла ошибка при загрузке данных", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onTheoryItemClick(item: ContentEntity) {
        try {
            // При клике на элемент теории
            Log.d(TAG, "Теория выбрана: ${item.title}, ID: ${item.contentId}")
            
            // Получаем MainActivity
            val mainActivity = requireActivity() as? com.ruege.mobile.MainActivity
            
            // Если удалось получить MainActivity, вызываем метод для показа BottomSheet
            if (mainActivity != null) {
                mainActivity.showTheoryBottomSheet(item.title, item.description ?: "", item.contentId!!)
            } else {
                // Если не удалось получить MainActivity, показываем Toast
                Toast.makeText(requireContext(), "Загрузка: ${item.title}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при открытии теории: ${e.message}", e)
            Toast.makeText(requireContext(), "Не удалось открыть теорию", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 