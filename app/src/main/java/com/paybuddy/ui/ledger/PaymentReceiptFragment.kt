package com.paybuddy.ui.ledger

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.paybuddy.R
import com.paybuddy.databinding.FragmentPaymentReceiptBinding

class PaymentReceiptFragment : Fragment(R.layout.fragment_payment_receipt) {

    private var _binding: FragmentPaymentReceiptBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPaymentReceiptBinding.bind(view)

        // Setup receipt details and share option
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
