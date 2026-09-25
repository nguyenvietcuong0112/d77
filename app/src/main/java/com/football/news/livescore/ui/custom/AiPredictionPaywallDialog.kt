package com.football.news.livescore.ui.custom

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.football.news.livescore.R
import com.football.news.livescore.data.local.BillingManager
import com.football.news.livescore.data.local.RequestLimitManager
import com.football.news.livescore.databinding.DialogAiPredictionPaywallBinding
import com.football.news.livescore.ui.iap.IAPActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AiPredictionPaywallDialog : DialogFragment() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var billingManager: BillingManager

    private var _binding: DialogAiPredictionPaywallBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAiPredictionPaywallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        isCancelable = true

        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnLater.setOnClickListener {
            dismiss()
        }

        binding.btnGoPremium.setOnClickListener {
            val intent = Intent(requireContext(), IAPActivity::class.java)
            startActivity(intent)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AiPredictionPaywallDialog"
        
        fun newInstance(): AiPredictionPaywallDialog {
            return AiPredictionPaywallDialog()
        }
    }
}
