package com.ruege.mobile.ui.exercises

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ruege.mobile.R
import com.ruege.mobile.ui.viewmodel.ContentViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Фрагмент для отображения детальной информации о задании.
 * Временная заглушка, в будущем будет использоваться для прямого доступа к конкретному заданию.
 */
@AndroidEntryPoint
class TaskDetailFragment : Fragment() {

    private val contentViewModel: ContentViewModel by viewModels()
    private var taskId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        taskId = arguments?.getInt("taskId", 0) ?: 0
        Timber.d("TaskDetailFragment создан с ID задания: $taskId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messageView = view.findViewById<TextView>(R.id.tvTaskDetailMessage)
        
        if (taskId > 0) {
            messageView.text = "Загрузка задания с ID: $taskId...\nЭта функциональность находится в разработке."
            
            val taskFragment = TaskFragment.newInstance(taskId.toString())
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, taskFragment)
                .addToBackStack(null)
                .commit()
        } else {
            messageView.text = "Ошибка: ID задания не указан или некорректен."
            Toast.makeText(requireContext(), "Ошибка: не удалось загрузить задание", Toast.LENGTH_SHORT).show()
        }
    }
} 