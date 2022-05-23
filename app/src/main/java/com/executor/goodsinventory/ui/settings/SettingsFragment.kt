package com.executor.goodsinventory.ui.settings

import android.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import com.executor.goodsinventory.data.InventoryModel
import com.executor.goodsinventory.databinding.FragmentSettingsBinding
import com.executor.goodsinventory.domain.utils.UtilsObject

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private var firstLoad = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setModelsSpinner()
        checkBoxNotify()
        setListeners()
    }

    private fun checkBoxNotify() {
        binding.cbTiny.isChecked = InventoryModel.isTiny
        binding.cbQuant.isChecked = InventoryModel.is_quantized
    }

    private fun setListeners() {
        binding.cbTiny.setOnClickListener {
            InventoryModel.isTiny = (it as CheckBox).isChecked
        }
        binding.cbQuant.setOnClickListener {
            InventoryModel.is_quantized = (it as CheckBox).isChecked
        }

        binding.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    if (!firstLoad) {
                        UtilsObject.setCurrentModel(requireContext(), p2)
                    }
                    firstLoad = false
                    checkBoxNotify()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        binding.etInputSize.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                InventoryModel.TF_OD_API_INPUT_SIZE = p0.toString().toInt()
            }
        })

        binding.etAccuracy.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0?.startsWith("0.") != true) {
                    binding.etAccuracy.setText("0.")
                    binding.etAccuracy.setSelection(2)
                } else if (p0.length > 2) {
                    InventoryModel.accuracy = p0.toString().toFloat()
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
        binding.etDelay.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.toString().isNotEmpty()){
                    InventoryModel.timeDelay = p0.toString().toLong()
                }
            }
            override fun afterTextChanged(p0: Editable?) {}
        })
    }

    private fun setModelsSpinner() {
        val spinnerAdapter = context?.let {
            ArrayAdapter(
                it,
                R.layout.simple_spinner_item,
                InventoryModel.models
            )
        }
        spinnerAdapter?.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerModel.adapter = spinnerAdapter
    }
}