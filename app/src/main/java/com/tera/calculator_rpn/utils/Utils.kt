package com.tera.calculator_rpn.utils

import com.tera.calculator_rpn.history.ItemModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Utils {

    // Проверить точку
    fun checkDot(expr: String, pos: Int): Boolean {
        var i = pos

        if (i == 0) return true
        if (i == 1) return false

        if (i > expr.length) return false

        if (i == expr.length) {
            i--
            return selextLiyeral(expr, i)
        }

        // Цифры или точка
        if (expr[i].isDigit() || expr[i] == '.') {
            return selextLiyeral(expr, i)
        } else {
            i--
            return selextLiyeral(expr, i)
        }
    }

    // Выделить число
    private fun selextLiyeral(expr: String, pos: Int): Boolean {
        var i = pos
        var start = 0
        var end = 0
        var literal = ""

        // Начало
        while (expr[i].isDigit() || expr[i] == '.') {
            i--
            if (i <= 0) break
        }
        start = i// + 1

        // Конец
        i = pos

        while (expr[i].isDigit() || expr[i] == '.') {
            i++
            if (i >= expr.length) break
        }
        end = i

        literal = expr.substring(start, end)

        return literal.contains(".")

    }

    fun deleteItems(list: ArrayList<ItemModel>, pos: Int): ArrayList<ItemModel> {

        val listU = list
        val size = listU.size

        if (size == 1) {
            listU.clear()
            return listU
        }

        val time = list[pos].time
        var time2 = ""
        val pos2 = pos + 1

        if (size > pos2)
            time2 = list[pos2].time

        return listU
    }

    // Время
    fun getDate(): String{
        val pattern = "d MMMM yyyy"
//        val pattern = "d MMMM h:mm"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date())// + " г."
    }


}