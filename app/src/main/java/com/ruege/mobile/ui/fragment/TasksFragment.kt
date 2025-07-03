package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ruege.mobile.ui.adapter.TasksAdapter
import com.ruege.mobile.databinding.FragmentTasksBinding
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.ui.viewmodel.TasksViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import android.widget.CheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ruege.mobile.ui.bottomsheet.TaskDisplayBottomSheetDialogFragment

@AndroidEntryPoint
class TasksFragment : Fragment(), TasksAdapter.OnContentClickListener, TasksAdapter.OnItemSelectionListener, TasksAdapter.OnItemDeleteListener {

    companion object {
        private const val TAG = "TasksFragment"
    }

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private lateinit var tasksAdapter: TasksAdapter
    private val viewModel: TasksViewModel by activityViewModels()

    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var errorTextView: TextView
    private lateinit var selectAllCheckBox: CheckBox
    private lateinit var fabDownload: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shimmerLayout = binding.shimmerContentTasks
        errorTextView = binding.errorTextViewTasks
        selectAllCheckBox = binding.selectAllCheckboxTasks
        fabDownload = binding.fabDownloadTasks

        setupRecyclerView()
        observeTasksState()
        observeSelectionState()
        observeBatchDownloadResult()
        observeDeleteStatus()

        selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.selectAllTasks(isChecked)
        }

        fabDownload.setOnClickListener {
            viewModel.downloadSelectedTasks()
        }
    }

    private fun setupRecyclerView() {
        if (!isAdded) return

        tasksAdapter = TasksAdapter(this)
        tasksAdapter.setOnItemSelectionListener(this)
        tasksAdapter.setOnItemDeleteListener(this)
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun observeTasksState() {
        viewModel.taskItemsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    shimmerLayout.visibility = View.VISIBLE
                    shimmerLayout.startShimmer()
                    binding.recyclerViewTasks.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                }
                is Resource.Success -> {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    val items = resource.data
                    if (items != null && items.isNotEmpty()) {
                        tasksAdapter.submitList(items)
                        binding.recyclerViewTasks.visibility = View.VISIBLE
                        errorTextView.visibility = View.GONE
                        updateSelectAllCheckboxState(items)
                    } else {
                        errorTextView.text = "Нет данных по заданиям."
                        errorTextView.visibility = View.VISIBLE
                        binding.recyclerViewTasks.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    binding.recyclerViewTasks.visibility = View.GONE

                    val staleData = resource.data
                    if (staleData != null && staleData.isNotEmpty()) {
                        tasksAdapter.submitList(staleData)
                        binding.recyclerViewTasks.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Ошибка обновления: ${resource.message}. Показаны старые данные.", Toast.LENGTH_LONG).show()
                    } else {
                        errorTextView.text = "Ошибка загрузки: ${resource.message}"
                        errorTextView.visibility = View.VISIBLE
                        errorTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    }
                }
                else -> {

                }
            }
        }
    }

    override fun onContentClick(item: ContentItem) {
        if (item.contentId == "task_group_27") {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Задание 27")
                .setMessage("Для выполнения этого задания перейдите в раздел 'Сочинения'.")
                .setPositiveButton("Понятно", null)
                .show()
        } else {
            showTaskBottomSheet(item.contentId, item.title)
        }
    }

    private fun showTaskBottomSheet(contentId: String?, title: String?) {
        if (contentId == null) {
            Toast.makeText(requireContext(), "Невозможно открыть задание: отсутствует ID.", Toast.LENGTH_SHORT).show()
            return
        }
        val taskSheet = TaskDisplayBottomSheetDialogFragment.newInstance(
            contentId,
            title ?: "Задание"
        )
        if (parentFragmentManager.findFragmentByTag(TaskDisplayBottomSheetDialogFragment.TAG) == null) {
            taskSheet.show(parentFragmentManager, TaskDisplayBottomSheetDialogFragment.TAG)
        }
    }

    override fun onItemSelectionChanged(item: ContentItem, isSelected: Boolean) {
        viewModel.selectTask(item, isSelected)
    }

    override fun onItemDelete(item: ContentItem) {
        if (!item.isDownloaded) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление заданий")
            .setMessage("Вы уверены, что хотите удалить скачанные задания для \"${item.title}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                val egeNumber = item.contentId.removePrefix("task_group_")
                viewModel.deleteDownloadedTaskGroup(egeNumber)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun observeSelectionState() {
        viewModel.isAnyTaskSelected.observe(viewLifecycleOwner) { isSelected ->
            fabDownload.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    private fun observeDeleteStatus() {
        viewModel.deleteTaskGroupStatus.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {

                }
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Задания успешно удалены", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), "Ошибка удаления: ${resource.message}", Toast.LENGTH_LONG).show()
                }
                else -> {

                }
            }
        }
    }

    private fun observeBatchDownloadResult() {
        viewModel.batchDownloadTasksResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    fabDownload.isEnabled = false
                    Toast.makeText(requireContext(), "Начинаю скачивание...", Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    fabDownload.isEnabled = true
                    Toast.makeText(requireContext(), resource.data, Toast.LENGTH_LONG).show()
                }
                is Resource.Error -> {
                    fabDownload.isEnabled = true
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                }
                else -> {

                }
            }
        }
    }

    private fun updateSelectAllCheckboxState(items: List<ContentItem>) {
        val allSelected = items.all { it.isSelected }
        selectAllCheckBox.setOnCheckedChangeListener(null)
        selectAllCheckBox.isChecked = allSelected
        selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.selectAllTasks(isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 