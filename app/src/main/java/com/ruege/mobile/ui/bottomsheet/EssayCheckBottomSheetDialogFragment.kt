package com.ruege.mobile.ui.bottomsheet

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.databinding.BottomSheetEssayCheckBinding
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.ui.viewmodel.EssayViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class EssayCheckBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEssayCheckBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EssayViewModel by activityViewModels()
    private var taskItem: TaskItem? = null
    private var markwon: Markwon? = null
    private var textWatcher: TextWatcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            taskItem = it.getParcelable(ARG_TASK_ITEM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEssayCheckBinding.inflate(inflater, container, false)

        markwon = Markwon.builder(requireContext())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(requireContext()))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated for taskItem.taskId: ${taskItem?.taskId}")

        viewModel.clearCheckState()
        viewModel.clearSavedEssay()
        viewModel.clearAdditionalTextState()

        setupViews()
        setupListeners()
        observeCheckState()
        observeAdditionalTextState()
        observeSavedEssay()

        taskItem?.let {
            viewModel.loadAdditionalText(it.textId ?: 1)
            viewModel.loadSavedEssay(it.taskId)

            val sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
            val checkId = sharedPrefs.getString("check_id_${it.taskId}", null)
            if (checkId != null) {
                viewModel.pollExistingCheck(checkId, it.taskId, it.title, binding.essayInputText.text.toString())
            }
        }
    }

    private fun setupViews() {
        val fullText = taskItem?.content ?: ""
        val delimiter = "»."
        val delimiterIndex = fullText.indexOf(delimiter)

        if (delimiterIndex != -1) {
            val visibleText = fullText.substring(0, delimiterIndex + delimiter.length)
            val hiddenText = fullText.substring(delimiterIndex + delimiter.length).trim()

            if (hiddenText.isNotEmpty()) {
                val moreText = " Ещё..."
                val shortSpannable = SpannableString(visibleText + moreText)
                val moreClickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) { showFullText(true) }
                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                    }
                }
                shortSpannable.setSpan(
                    moreClickableSpan,
                    visibleText.length,
                    shortSpannable.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                shortSpannable.setSpan(
                    ForegroundColorSpan(Color.GRAY),
                    visibleText.length,
                    shortSpannable.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.essayTaskText.text = shortSpannable
                binding.essayTaskText.movementMethod = LinkMovementMethod.getInstance()

                val hideText = " Скрыть"
                val fullSpannable = SpannableString(fullText + hideText)
                val hideClickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) { showFullText(false) }
                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                    }
                }
                fullSpannable.setSpan(hideClickableSpan, fullText.length, fullSpannable.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                fullSpannable.setSpan(
                    ForegroundColorSpan(Color.GRAY),
                    fullText.length,
                    fullSpannable.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.fullTaskText.text = fullSpannable
                binding.fullTaskText.movementMethod = LinkMovementMethod.getInstance()

                showFullText(false)
            } else {
                binding.essayTaskText.text = fullText
            }
        } else {
            binding.essayTaskText.text = fullText
        }
    }

    private fun showFullText(show: Boolean) {
        binding.essayTaskText.visibility = if (show) View.GONE else View.VISIBLE
        binding.fullTaskText.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                taskItem?.let {
                    viewModel.saveEssayContent(it.taskId, s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {
                binding.essaySubmitButton.isEnabled = (s?.length ?: 0) >= 150
            }
        }
        binding.essayInputText.addTextChangedListener(textWatcher)

        binding.essaySubmitButton.setOnClickListener {
            val essayText = binding.essayInputText.text.toString()
            taskItem?.let {
                viewModel.checkEssay(it.taskId, it.textId, essayText, it.title)
            }
        }

        binding.solveAgainButton.setOnClickListener {
            taskItem?.let {
                viewModel.clearEssay(it.taskId)
                val sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    remove("check_id_${it.taskId}")
                    apply()
                }
            }
        }
    }

    private fun observeCheckState() {
        viewModel.essayCheckState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.essayCheckingLayout.visibility = View.VISIBLE
                    binding.essaySubmitButton.isEnabled = false
                    binding.essayResultTextContainer.visibility = View.GONE
                    binding.essayInputText.isEnabled = false
                }
                is Resource.Success -> {
                    binding.essayCheckingLayout.visibility = View.GONE
                    binding.essayResultTextContainer.visibility = View.VISIBLE

                    val resultTextView = TextView(requireContext())
                    markwon?.setMarkdown(resultTextView, resource.data?.result ?: "")
                    binding.essayResultTextContainer.removeAllViews()
                    binding.essayResultTextContainer.addView(resultTextView)

                    binding.essayInputText.isEnabled = false
                    binding.essaySubmitButton.visibility = View.GONE
                    binding.solveAgainButton.visibility = View.VISIBLE

                    taskItem?.let {
                        val sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
                        with(sharedPrefs.edit()) {
                            remove("check_id_${it.taskId}")
                            apply()
                        }
                    }
                }
                is Resource.Error -> {
                    binding.essayCheckingLayout.visibility = View.GONE
                    binding.essaySubmitButton.isEnabled = true
                    binding.essayInputText.isEnabled = true
                    binding.essayResultTextContainer.visibility = View.VISIBLE

                    val errorTextView = TextView(requireContext()).apply {
                        text = "Ошибка: ${resource.data?.detail ?: resource.message}"
                    }
                    binding.essayResultTextContainer.removeAllViews()
                    binding.essayResultTextContainer.addView(errorTextView)
                    binding.solveAgainButton.visibility = View.GONE
                }
                null -> {
                    binding.essayCheckingLayout.visibility = View.GONE
                    binding.essayResultTextContainer.visibility = View.GONE
                    binding.essaySubmitButton.isEnabled = binding.essayInputText.text?.length ?: 0 >= 150
                    binding.essayInputText.isEnabled = true
                    binding.essaySubmitButton.visibility = View.VISIBLE
                    binding.solveAgainButton.visibility = View.GONE
                }
            }
        }
    }

    private fun observeSavedEssay() {
        viewModel.savedEssay.observe(viewLifecycleOwner) { userEssay ->
            Timber.d("observeSavedEssay collected for taskItem.taskId: ${taskItem?.taskId}. Received userEssay with taskId: ${userEssay?.taskId}")
            if (userEssay != null && userEssay.taskId != taskItem?.taskId) {
                return@observe
            }
            textWatcher?.let { binding.essayInputText.removeTextChangedListener(it) }

            if (userEssay == null) {
                binding.essayInputText.setText("")
                binding.essayInputText.isEnabled = true
                binding.essaySubmitButton.visibility = View.VISIBLE
                binding.essaySubmitButton.isEnabled = false
                binding.solveAgainButton.visibility = View.GONE
                binding.essayResultTextContainer.visibility = View.GONE
            } else {
                binding.essayInputText.setText(userEssay.essayContent)
                val essayResult = userEssay.result
                if (essayResult != null) {
                    val resultTextView = TextView(requireContext())
                    markwon?.setMarkdown(resultTextView, essayResult)
                    binding.essayResultTextContainer.removeAllViews()
                    binding.essayResultTextContainer.addView(resultTextView)

                    binding.essayResultTextContainer.visibility = View.VISIBLE
                    binding.essayInputText.isEnabled = false
                    binding.essaySubmitButton.visibility = View.GONE
                    binding.solveAgainButton.visibility = View.VISIBLE
                } else {
                    binding.essayInputText.isEnabled = true
                    binding.essaySubmitButton.visibility = View.VISIBLE
                    binding.solveAgainButton.visibility = View.GONE
                    binding.essayResultTextContainer.visibility = View.GONE
                    binding.essaySubmitButton.isEnabled = (userEssay.essayContent?.length ?: 0) >= 150
                }
            }

            binding.essayInputText.addTextChangedListener(textWatcher)
            binding.essayInputText.setSelection(binding.essayInputText.length())

            if (userEssay?.checkId?.isNotEmpty() == true && userEssay.result == null) {
                viewModel.pollExistingCheck(
                    userEssay.checkId!!,
                    taskItem!!.taskId,
                    taskItem!!.title,
                    userEssay.essayContent ?: ""
                )
            }
        }
    }

    private fun observeAdditionalTextState() {
        viewModel.additionalTextState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.additionalTextProgress.visibility = View.VISIBLE
                    binding.additionalText.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.additionalTextProgress.visibility = View.GONE
                    if (resource.data?.isNotEmpty() == true) {
                        binding.additionalText.visibility = View.VISIBLE
                        binding.additionalText.text = "Текст:\n${resource.data}"
                        expandBottomSheet()
                    }
                }
                is Resource.Error -> {
                    binding.additionalTextProgress.visibility = View.GONE
                }
                null -> {
                    binding.additionalTextProgress.visibility = View.GONE
                    binding.additionalText.visibility = View.GONE
                }
                else -> {
                    
                }
            }
        }
    }

    private fun expandBottomSheet() {
        view?.post {
            val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("onDestroyView for taskItem.taskId: ${taskItem?.taskId}")
        viewModel.clearCheckState()
        viewModel.clearAdditionalTextState()
        viewModel.clearSavedEssay()
        _binding = null
    }

    companion object {
        const val TAG = "EssayCheckBottomSheet"
        private const val ARG_TASK_ITEM = "task_item"

        fun newInstance(taskItem: TaskItem): EssayCheckBottomSheetDialogFragment {
            return EssayCheckBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TASK_ITEM, taskItem)
                }
            }
        }
    }
} 