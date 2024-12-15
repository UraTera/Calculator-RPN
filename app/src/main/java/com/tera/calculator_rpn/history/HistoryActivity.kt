package com.tera.calculator_rpn.history

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tera.calculator_rpn.MainActivity
import com.tera.calculator_rpn.R
import com.tera.calculator_rpn.databinding.ActivityHistoryBinding
import com.tera.calculator_rpn.utils.MyConst


class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter

    private var list = ArrayList<ItemModel>()
    private var selectPos = ArrayList<Int>() // Список выбранных CheckBox
    private var keyClear = false
    private var keySelect = false
    private var keyBnDelete = false
    private val gson = Gson()

    var height = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        var keyUpdata = false

        if (savedInstanceState != null) {
            selectPos = savedInstanceState.getIntegerArrayList(MyConst.SEL) as ArrayList<Int>
            keyClear = savedInstanceState.getBoolean(MyConst.CLEAR)
            keyBnDelete = savedInstanceState.getBoolean(MyConst.DELETE)

            val strList = savedInstanceState.getString(MyConst.LIST)
            val itemType = object : TypeToken<java.util.ArrayList<ItemModel>>() {}.type
            list = gson.fromJson(strList, itemType)

            keyUpdata = true
        } else {
            // Получить список
            val strList = intent.getStringExtra(MyConst.LIST)
            val itemType = object : TypeToken<java.util.ArrayList<ItemModel>>() {}.type
            list = gson.fromJson(strList, itemType)
        }

        val layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(this, list, height)
        binding.apply {
            rcHistory.hasFixedSize()
            rcHistory.layoutManager = layoutManager
            rcHistory.adapter = adapter
        }

        // Слушатель CheckBox
        adapter.onItemClick = { pos ->
            //Log.d("myLogs", "History, onItemClick - pos: $pos")
            selectPos(pos)
        }

        if (keyUpdata)
            updateList()

        // Регистрация кнопки Back
        regBack()
    }

    //-----------------------

    private fun init() {

        // Высота полосы tvTime
        val heightTv = 25
        val sp = resources.displayMetrics.density
        height = (heightTv * sp).toInt()

        binding.apply {

            bnHome.setOnClickListener {
                openMainActivity()
            }

            bnDelete.setOnClickListener {
                deleteIyems()
            }

            chSelectAll.setOnCheckedChangeListener { _, isChecked ->
                selectAll(isChecked)
            }
        }
    }

    // Проверка list
    fun checkList(otpavitel: String) {
        val size = list.size
        //Log.d("myLogs", "$otpavitel - size: $size, selectPos: $selectPos")
    }

    // Поворот
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val strList = gson.toJson(list)
        outState.putString(MyConst.LIST, strList)
        outState.putIntegerArrayList(MyConst.SEL, selectPos)
        outState.putBoolean(MyConst.CLEAR, keyClear)
        outState.putBoolean(MyConst.DELETE, keyBnDelete)
    }

    private fun updateList() {
        val len = selectPos.size
        val saze = list.size

        if (len > 0 && saze > 0) {
            for (i in 0 until len) {
                val p = selectPos[i]
                list[p].select = true
            }
            binding.bnDelete.visibility = View.VISIBLE
            adapter.updateAdapter(list, true)
        }

    }

    // Показать / Скрыть кнопку Удалить от адаптера
    private fun selectPos(pos: ArrayList<Int>) {
        selectPos = pos

        if (selectPos.size > 0) {
            showHightButton(true)
            keySelect = true
        } else {
            showHightButton(false)
            keySelect = false
            binding.chSelectAll.isChecked = false
        }
    }

    // Выделить весь список / Снять выделение от chSelectAll
    private fun selectAll(key: Boolean) {
        if (key) { // Выделить весь список
            showHightButton(true)
            keySelect = true
            for (i in 0 until list.size) {
                val item = list[i]
                item.select = true
                selectPos.add(i)
            }
        } else { // Снять выделение
            showHightButton(false)
            for (item in list)
                item.select = false
            selectPos.clear()
            keySelect = false
        }
        adapter.updateAdapter(list, key)
    }

    // Показать / Скрыть кнопку Удалить
    private fun showHightButton(key: Boolean) {
        if (key) {
            if (!keyBnDelete) {
                val animation = AnimationUtils.loadAnimation(this, R.anim.show_bn_scale)
                binding.bnDelete.visibility = View.VISIBLE
                binding.bnDelete.startAnimation(animation)
                keyBnDelete = true
            }
        } else {
            if (keyBnDelete) {
                val animation = AnimationUtils.loadAnimation(this, R.anim.hight_bn_scale)
                binding.bnDelete.visibility = View.GONE
                binding.bnDelete.startAnimation(animation)
                keyBnDelete = false
            }
        }
    }

    // Удалить элементы
    private fun deleteIyems() {

        if (binding.chSelectAll.isChecked) {
            list.clear()
        } else {
            val len = selectPos.size

            if (len > 0) {
                for (i in len - 1 downTo 0) {
                    val p = selectPos[i] // Удаляемая позиция

                    // utils.deleteItems(list, p)

                    val size = list.size
                    val time = list[p].time
                    var time2 = ""

                    //if (p < 0) break

                    if (size > p + 1)
                        time2 = list[p + 1].time //

                    if (size == 1)
                        list.clear()
                    else if ("" != time && "" == time2) { // 1-й не 0, 2-й = 0
                        if (size > p + 1) { // Не последний
                            list[p + 1].time = time
                            list.removeAt(p)
                        } else list.removeAt(p)
                    } else list.removeAt(selectPos[i])
                }
            }
        }
        // Log.d(TAG, "deleteIyems - list: $list")
        keyClear = true
        selectPos.clear()
        binding.bnDelete.visibility = View.GONE
        binding.chSelectAll.isChecked = false
        showHightButton(false)
        adapter.updateAdapter(list, false)

    }

    // Домой
    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java)

        val strList = gson.toJson(list)
        intent.putExtra(MyConst.LIST, strList)
        intent.putExtra(MyConst.CLEAR, keyClear)

        val enterAnim = R.anim.left_in
        val exitAnim = R.anim.right_out
        val options = ActivityOptionsCompat.makeCustomAnimation(this, enterAnim, exitAnim)
        startActivity(intent, options.toBundle())
    }

    // Регистрация кнопки Back
    private fun regBack() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                exitOnBackPressed()
            }
        } else {
            onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        exitOnBackPressed()
                    }
                })
        }
    }

    // Кнопка Back
    fun exitOnBackPressed() {
        openMainActivity()
    }



}