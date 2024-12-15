package com.tera.calculator_rpn

import android.app.Dialog
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import com.tera.calculator_rpn.utils.MyConst

class SelectTimeDialog : DialogFragment() {

    interface ListenerDialog {
        fun selectNumTime(num: Int, text: String)
    }

    private lateinit var radioGroup: RadioGroup
    private var numTimeOut = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        if (arguments != null) {
            numTimeOut = arguments?.getInt(MyConst.NUM_TIME_OUT,MyConst.NUM_TIME_DEF)!!
        }
        return inflater.inflate(R.layout.fragment_select_time_dialog, container, false)
    }

    // Для скругленных углов
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Ширина диалога
        setWidthPercent()

        radioGroup = view.findViewById(R.id.radioGroup)

        createRadioButtons1(view)
    }

    // Создать RadioButtons
    private fun createRadioButtons1(view: View){
        val array = resources.getStringArray(R.array.time_out)
        for (i in array.indices) {
            val radioButton = RadioButton(requireContext())
            radioButton.height = 150
            radioButton.text = array[i]
            radioButton.id = i
            if (i == numTimeOut) radioButton.isChecked = true
            radioGroup.addView(radioButton)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = view.findViewById<View>(checkedId) as RadioButton
            val id = radioButton.id
            numTimeOut = radioButton.id
            val text = radioButton.text.toString()
            sendData(text)
            dismiss()
        }
    }

    private fun sendData(text: String) {
        val listener = activity as ListenerDialog
        listener.selectNumTime(numTimeOut, text)
    }

    // Ширина в процентах
    private fun setWidthPercent() {
        val percentage = 80
        val percent = percentage.toFloat() / 100
        val dm = Resources.getSystem().displayMetrics
        val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
        val percentWidth = rect.width() * percent
        dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}