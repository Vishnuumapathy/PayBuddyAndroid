package com.paybuddy.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.paybuddy.R
import com.paybuddy.databinding.FragmentLoginBinding

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        binding.btnLogin.setOnClickListener {
            // Simplified for now
            findNavController().navigate(R.id.action_login_to_onboarding)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
