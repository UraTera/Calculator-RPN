package com.tera.calculator_rpn.utils

import android.util.Log

class TopIndex {

    // Число перед корнем в верхний индекс
    fun getRootIndex(exp: String): String {
        var i = 0
        var temp = exp
        var iN = 0 // Индекс числа
        var c = ' '
        var obr = "" // Обратная строка накопитель
        var normI = ""
        var res = ""

        var str1 = ""

        while (i < exp.length) {
            c = exp[i]

            if (c == '√') {

                if (i > 0) {
                    iN = i - 1

                    if (exp[iN].isDigit() || exp[iN] == '.') { // Число или точка
                        while (exp[iN].isDigit() || exp[iN] == '.') {
                            val char = getSuperChar(exp[iN])
                            obr += char                    // Обратная строка накопитель
                            normI = StringBuffer(obr).reverse().toString()
                            if (iN == 0) break
                            iN--
                        }
                        val len = normI.length

                        str1 = temp.substring(0, i - len)
                        res = str1 + normI + '√'
                        temp = res
                        obr = ""
                        c = exp[i]
                    } else {
                        res += '√'
                        temp = res
                    }
                } else {
                    res += c
                    temp = res
                }
            } else {
                res += c
                temp = res
            }
            i++
            if (i > exp.length) break
        }
        return res
    }

    // Получить надстрочный индекс + точка
    private fun getSuperChar(c: Char): Char {
        return when (c) {
            '0' -> '\u2070'
            '1' -> '\u00b9'
            '2' -> '\u00b2'
            '3' -> '\u00b3'
            '4' -> '\u2074'
            '5' -> '\u2075'
            '6' -> '\u2076'
            '7' -> '\u2077'
            '8' -> '\u2078'
            '9' -> '\u2079'
            '.' -> '\u2027'
            else -> ' '
        }
    }

    // Символ с чертой
    fun dashSymbol(exp: String): String {

        var i = 0
        var res = ""
        var temp = ' '
//        var temp = ""
        var len = 0
        var keyR = false // Ключ левой скобки
        val rangeD = '\u0080'..'\u00ab'
        val rangeO = '\u008c'..'\u0090' // Операции
        val rangeL = '\u0091'..'\u00ac'
        var countBkt = 0 // Число левых скобок

        while (i < exp.length) {
            len = exp.length

            if (exp[i] == '√') {
                res += exp[i]
                if (i >= len - 1) break
                i++
//                Log.d(TAG, "if Корень = ${exp[i]}")

                // Левая скобка               '(' с чертой
                if (exp[i] == '(' || exp[i] == '\u008e') {
                    keyR = true

                    while (exp[i] != ')' && keyR) { // ')' с чертой
                        if (exp[i] in rangeD || exp[i] in rangeO || exp[i] in rangeL) { // с чертой
                            temp = exp[i] // Не изменять
                        } else
                            temp = getDash(exp[i])

                        if (exp[i] == '(' || exp[i] == '\u008e')
                            countBkt++
                        if (exp[i] == ')' || exp[i] == '\u008f')
                            countBkt--
                        if (countBkt == 0) keyR = false
                        res += temp
                        i++
                        if (i >= exp.length) break
                    }
                }
                // Цифры
                else if (exp[i].isDigit() || exp[i] == '.' || exp[i] in rangeD) {
                    while (exp[i].isDigit() || exp[i] == '.' || exp[i] in rangeD) {
                        if (exp[i].isDigit() || exp[i] == '.') {
                            temp = getDash(exp[i]) // Получить символ
                        } else temp = exp[i]

                        res += temp
                        i++
                        if (i >= exp.length) break
                    }
                }
                // Буквы // sin(
                else {
                    while (exp[i].isLetter()

                    ) {
                        val s = exp[i]
                        temp = getDash(exp[i])

                        i++
                        res += temp
                        if (i >= exp.length) break
                    }

                    if (temp != '\u00ac') { // π
                        while (exp[i] != ')') {
                            temp = getDash(exp[i]) // Получить символ
                            i++
                            res += temp
                            if (i >= exp.length) break
                        }
                    }

                }

                if (i >= exp.length) break

                // Правая скобка
                if (exp[i] == ')') {
                    res += '\u008f' // ')'
                    i++
                    if (i >= exp.length) break
                }
            }

            if (i >= exp.length) break
            res += exp[i]
            i++
        }
        return res
    }

    // Получить подчеркнутый сверху символ
    private fun getDash(c: Char): Char {
        return when (c) {
            '0' -> '\u0080'
            '1' -> '\u0081'
            '2' -> '\u0082'
            '3' -> '\u0083'
            '4' -> '\u0084'
            '5' -> '\u0085'
            '6' -> '\u0086'
            '7' -> '\u0087'
            '8' -> '\u0088'
            '9' -> '\u0089'
            '*', '\u00D7' -> '\u008a'
            '/' -> '\u008b'
            '+' -> '\u008c'
            '-', '\u2012' -> '\u008d'
            '(' -> '\u008e'
            ')' -> '\u008f'
            '.' -> '\u0090'

            'a' -> '\u0091'
            'c' -> '\u0093'
            'i' -> '\u0099'
            'n' -> '\u009e'
            'o' -> '\u009f'
            's' -> '\u00a4'
            't' -> '\u00a5'
            'π' -> '\u00ac'

            else -> ' '
        }
    }

}