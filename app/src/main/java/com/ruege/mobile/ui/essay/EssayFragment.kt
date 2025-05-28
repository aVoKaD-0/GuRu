package com.ruege.mobile.ui.essay

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
class EssayFragment : Fragment(), EssayAdapter.EssayItemClickListener {

    companion object {
        private const val TAG = "EssayFragment"
    }

    private var _binding: FragmentTheoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: EssayAdapter
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
            viewModel = ViewModelProvider(requireActivity())[ContentViewModel::class.java]
            
            binding.recyclerViewTheory.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = EssayAdapter(this@EssayFragment).also {
                    this@EssayFragment.adapter = it
                }
                this.adapter = this@EssayFragment.adapter
            }
            
            viewModel.essayTopicsLiveData.observe(viewLifecycleOwner) { topics ->
                adapter.updateItems(topics)
                Log.d(TAG, "Получено тем сочинений: ${topics.size}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при настройке RecyclerView: ${e.message}", e)
            Toast.makeText(context, "Произошла ошибка при загрузке данных", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onEssayItemClick(item: ContentEntity) {
        try {
            Log.d(TAG, "Сочинение выбрано: ${item.title}, ID: ${item.contentId}")
            val mainActivity = requireActivity() as? com.ruege.mobile.MainActivity
            
            if (mainActivity != null) {
                mainActivity.showEssayBottomSheet(item.title, item.description ?: "", item.contentId!!)
            } else {
                Toast.makeText(requireContext(), "Загрузка: ${item.title}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при открытии сочинения: ${e.message}", e)
            Toast.makeText(requireContext(), "Не удалось открыть сочинение", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 