package com.scepticalphysiologist.dmaple.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding

abstract class DMapLEPage<bindingType: ViewBinding> (
    private val inflator: (LayoutInflater, ViewGroup?, Boolean) -> bindingType
): Fragment() {

    private var _binding: bindingType? = null
    protected val binding get() = _binding!!
    protected val navigator: NavController get() = binding.root.findNavController()

    /** Create the UI elements of the view. */
    protected open fun createUI() {}

    // ---------------------------------------------------------------------------------------------
    // View creation and destruction.
    // ---------------------------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflator(inflater, container, false)
        createUI()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
