package com.tera.calculator_rpn.history

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.tera.calculator_rpn.MainActivity
import com.tera.calculator_rpn.R
import com.tera.calculator_rpn.databinding.ItemHistoryBinding
import com.tera.calculator_rpn.utils.MyConst

class HistoryAdapter(private val context: Context, private var arrayList: ArrayList<ItemModel>, val height: Int) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var keySelect = false
    private val gson = Gson()

    var onItemClick: ((pos: ArrayList<Int>) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ItemHistoryBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemList = arrayList[position]

        with(holder.binding) {
            tvExpr.text = itemList.expr
            tvRes.text = itemList.res
            tvTime.text = itemList.time
            chSelect.isChecked = itemList.select

            val textTime = itemList.time
            if (textTime == "")
                tvTime.height = (height / 4).toInt()
            else
                tvTime.height = (height).toInt()

            setColor(holder, chSelect.isChecked)

            item.setOnClickListener {
                if (keySelect) {
                    chSelect.isChecked = !chSelect.isChecked
                    arrayList[position].select = chSelect.isChecked
                    setColor(holder, chSelect.isChecked)
                    setPosition()
                } else {
                    setColor(holder, chSelect.isChecked)
                    transferData(itemList.expr, position)
                }
            }

            chSelect.setOnClickListener {
                arrayList[position].select = chSelect.isChecked
                setColor(holder, chSelect.isChecked)
                setPosition()
            }
        }
    }

    override fun getItemCount(): Int = arrayList.size

    class ViewHolder(
        val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root)

    // Передать данные
    private fun transferData(expr: String, pos: Int) {
        if (!keySelect) {
            val intent = Intent(context, MainActivity::class.java)
            val strList = gson.toJson(arrayList)
            intent.putExtra(MyConst.LIST, strList)
            intent.putExtra(MyConst.EXPR, expr)
            intent.putExtra(MyConst.POS, pos)
            intent.putExtra(MyConst.REM, true) // Ключ записи выражения в поле ввода
            context.startActivity(intent)
        }
    }

    // Выбранные позиции
    private fun setPosition() {
        val selectPos = ArrayList<Int>() // Список выбранных CheckBox
        val count = arrayList.size

        if (count > 0) {
            var check = false
            for (i in 0 until count) { // Весь список
                check = arrayList[i].select

                if (check) {
                    selectPos.add(i)
                }
            }
        }
        keySelect = selectPos.size > 0
        onItemClick?.invoke(selectPos) // оправьте ваши данные
    }

    // Установить цвет
    private fun setColor(holder: ViewHolder, key: Boolean) {
        with(holder.binding) {
            if (key) {
                tvTime.setBackgroundColor(ContextCompat.getColor(context, R.color.gray))
                cl.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_500))
            } else {
                tvTime.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                cl.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_200))
            }
        }
    }

    // Обновление списка
    @SuppressLint("NotifyDataSetChanged")
    fun updateAdapter(listArray: ArrayList<ItemModel>, key: Boolean) {
        arrayList = listArray
        keySelect = key
        notifyDataSetChanged() // Обновить данные
    }
}

