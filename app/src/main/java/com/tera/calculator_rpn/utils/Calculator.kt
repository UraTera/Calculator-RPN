package com.tera.calculator_rpn.utils

import android.util.Log
import java.text.DecimalFormat
import java.util.*
import java.util.Locale.getDefault
import kotlin.math.*

class Calculator {

    fun calc(expression: String): String {

        val exprN = indtoNumber(expression) // Получить число из верхнего индекса
        val exprD = dashToSymbol(exprN)     // Получить символ из символа с чертой
        val prepared = preparingExp(exprD)  // Подготовка
        val rpn = expressToRPN(prepared)    // String
        val res = rpnToExpress(rpn)         // Double
        return formatResult(res)            // Форматировать результат
    }

    //--------------------------------------------
    // Форматировать результат
    private fun formatResult(res: Double): String {
        var resFormat = ""
        lateinit var df: DecimalFormat

        val countM = 6 // Число знаков мантиссы
        val max = "1E$countM".toDouble()        // Double 1000000.0 +6
        val min = 1 / max * 100                 // 1.0E-4
        val minS = (max / 100).toInt().toString()  // String 10000
        val exF = minS.replace('0', '#')      // 1####
        val format = exF.replace("1", "#.") // #.####
        val formatE = format + "E0" // #.######E0

        // Большие
        if ((res >= max || res <= -max)) {
            df = DecimalFormat("#.####E0")
            val temp = df.format(res)    // В строку по формату "1.23E8"
            resFormat = getBasisTen(temp) // Получить основание степени 10
        }
        // 0.0000001
        else if (res <= min && res >= -min && res != 0.0) {
            df = DecimalFormat(formatE)
            val temp = df.format(res)
            resFormat = getBasisTen(temp) // Получить основание степени 10
        }
        // Меньше макс
        else {
            // Положение Раздедителя '.'
            val str = res.toString()
            val i = str.indexOf('.')

            when (i) {
                2 -> df = DecimalFormat("#.######")
                3 -> df = DecimalFormat("#.#####")
                4 -> df = DecimalFormat("#,###.####")
                5 -> df = DecimalFormat("#,###.###")
                6 -> df = DecimalFormat("#,###.##")
                7, 8 -> df = DecimalFormat("#,###.#")
                else -> df = DecimalFormat("#,###.########")
            }
            resFormat = df.format(res)
        }
        // Заменить запятую
        var language = getDefault().toString()
        language = language.substring(0, 2)
        if (language == "ru")
            resFormat = resFormat.replace(',', '.')

        return resFormat // String
    }

    // Получить основание степени 10
    private fun getBasisTen(exp: String): String {

        var i = exp.indexOf('E') + 1 // Положение экспоненты 'E' + 1
        val man = exp.substring(0, i - 1) // "1.23" Мантисса. Удалить конец
        var por = ' ' // Символ порядка
        var ind = ""  // Суммарный верхний индекс

        while (i < exp.length) { // Вся строка
            por = exp[i] // Символ порядка
            ind += getSuperChar(por)
            i++
        }
        return man + "×10" + ind
    }

    // Подготовка выражения.
    private fun preparingExp(expr: String): String {

        var exp = expr
        exp = exp.replace('\u2012', '-')
        exp = exp.replace('\u00D7', '*')
        exp = exp.replace('x', '^')

        val pi = Math.PI.toString()
        exp = exp.replace("π", pi)

        var prepExp = ""
        var i = 0;
        var char = ' '

        while (i < exp.length) {
            char = exp[i]

            if (char == '-') {
                if (i == 0) // Минус в начале выражения
                    prepExp += '0'
                else if (exp[i - 1] == '(')
                    prepExp += '0'
            }

            if (char == '√') {
                if (i == 0) // Корень в начале выражения
                    prepExp += '2'
                else if (exp[i - 1].isDigit() == false)
                    prepExp += '2'
            }

            prepExp += char; i++

        }

        // Операторы в конце строки
        val len = exp.length
        char = exp[len - 1]
        if (char == '+' || char == '-' || char == '*' || char == '/')
            prepExp = exp.substring(0, len - 1)

        return prepExp
    }

    // Формулу в обратную польскую запись
    private fun expressToRPN(exp: String): String {

        val stack = Stack<String>() // Стек
        var current = "" // Текущая строка
        var priority = 0 // Приоритет
//    var

        var sym = ' ' // Текущий символ
        var stack_S = ""

        var i = 0
        while (i < exp.length) {
            sym = exp[i]

            priority = getPriority(exp[i].toString())

            if (priority == 0) current += exp[i] // Число и точка
            if (priority == 1) {                 // Левая скобка
//        if ("(".equals(exp[i])) { // Не ловит
                stack.push(exp[i].toString()) // В стек
                stack_S += stack.peek()
            }

//        if (priority == 2 || priority == 3 || priority == 4) { // Простые операции, корень степень
            if (priority > 1) { // Операции
                // Пустой символ, чтобы данные не слипались
                current += " "

                // Пока стек не пустой
                while (!stack.empty()) {
                    // Новай знак имеет приоритет выше или равен,
                    // переносим предыдущий из стека в строку
                    val priorTop = getPriority(stack.peek())
                    if (priorTop >= priority)
                        current += stack.pop()
                    else break
                }

                // Новый знак в стек
                stack.push(exp[i].toString())
                stack_S += stack.peek()
            }


            //---------------------------
            if (exp[i].isLetter()) { // Тригонометрия
//            current += " "
                var oper = ""

                // Собрать слово (sin)
                while (exp[i].isLetter()) {
                    oper += exp[i]
                    i++
                    if (i >= exp.length) break
                }
                i--
                sym = exp[i]

                // Высший приоритет
                // В стек
                stack.push(oper)
                stack_S += stack.peek()
            }
            // --------------------------
            // Правая скобка (закрывающая)
            if (priority == -1) {
                current += ' '

                // Удаляем до Левой скобки
                while (getPriority(stack.peek()) != 1) current += stack.pop()
                // Удаляем из стека последний символ (левая скобка)
                stack.pop()
                // Удалить последний символ
                stack_S = stack_S.substring(0, stack_S.length - 1)
            }

            i++
        }


        // Пока стек не пустой, содержимое переместить в строку
        while (!stack.empty()) current += stack.pop()
        return current
    }

    //----------------------------
    // Получить результат
    private fun rpnToExpress(rpn: String): Double {

        var operand = "" // Хранитель больших чисел
        val stack = Stack<Double>() // Стек

        var i = 0
        var sym = ""
        var stackRes = ""

        while (i < rpn.length) {
            sym = rpn[i].toString()
            // Если пробел - прпустить
            if (rpn[i] == ' ') {
                i++; continue
            }

            // Цифра и точка
            if (getPriority(rpn[i].toString()) == 0) {
                // Собираем в число. До пробела
                while (rpn[i] != ' ' && getPriority(rpn[i].toString()) == 0) {
                    operand += rpn[i]
                    sym = rpn[i].toString()
                    i++
                    if (i >= rpn.length) {
                        i--; break
                    }
                }
                stack.push(operand.toDouble()); operand = ""
                stackRes += stack.peek()
            }

            // Простые операции
            if (getPriority(rpn[i].toString()) == 2 || getPriority(rpn[i].toString()) == 3) {
                // Берем два последних числа и выполняем операцию
                val a = stack.pop()
                val b = stack.pop()
                if (rpn[i] == '*') stack.push(b * a)
//                if (rpn[i] == '×') stack.push(b * a)
                if (rpn[i] == '/') stack.push(b / a)
                if (rpn[i] == '+') stack.push(b + a)
                if (rpn[i] == '-') stack.push(b - a)
//                if (rpn[i] == '\u2012') stack.push(b - a)
            }

            // Корень и возведение в степень
            if (getPriority(rpn[i].toString()) == 4) {
                val a = stack.pop() // Число под корнем. Степень
                val b = stack.pop() // Степень Корня. Основание Число
                if (rpn[i] == '√') stack.push(nRoot(a, b))
                if (rpn[i] == '^') stack.push(b.pow(a))
            }

            // Тригонометрия
            if (rpn[i].isLetter()) { // Буквы
                var oper = ""
                var ind = rpn.length - 1

                // Собираем в слово (sin)
                while (rpn[i].isLetter()) {
                    oper += rpn[i]
                    i++
                    if (i >= rpn.length) break
                    sym = rpn[i].toString()
                }

                i--

                var res = 0.0
                try {
                    var a = stack.pop() // Угол
                    a = Math.toRadians(a)

                    if ("sin".equals(oper)) res = sin(a)
                    if ("cos".equals(oper)) res = cos(a)
                    if ("tan".equals(oper)) res = tan(a)

                    stack.push(res)
                    stackRes += stack.peek()
                } catch (e: Exception) {
                    break
                }

            }
            i++

        }
        return stack.pop()
    }

    // Корень n-ой степени из числа x (-x)
    private fun nRoot(x: Double, n: Double): Double {
        if (x < 0 && n % 2 > 0)
            return -(-x).pow(1 / n)
        else
            return x.pow(1 / n)
    }

    // Получить символ из верхнего индекса для вычисления
    fun indtoNumber(str: String): String {
        var namExp = ""
        var i = 0;
        var char = ' '

        while (i < str.length) {
            char = str[i]

            val cod = "%04x".format(char.code)
            when (cod) {
                "2070" -> char = '0'
                "00b9" -> char = '1'
                "00b2" -> char = '2'
                "00b3" -> char = '3'
                "2074" -> char = '4'
                "2075" -> char = '5'
                "2076" -> char = '6'
                "2077" -> char = '7'
                "2078" -> char = '8'
                "2079" -> char = '9'
                "2027" -> char = '.'
            }
            namExp += char
            i++
        }
        return namExp
    }

    // Получить символ из символа с чертой для вычисления
    fun dashToSymbol(exp: String): String {
        var i = 0;
        var char = ' '
        var temp = ""

        while (i < exp.length) {
            char = exp[i]

            val cod = "%04x".format(char.code)
            when (cod) {
                "0080" -> char = '0'
                "0081" -> char = '1'
                "0082" -> char = '2'
                "0083" -> char = '3'
                "0084" -> char = '4'
                "0085" -> char = '5'
                "0086" -> char = '6'
                "0087" -> char = '7'
                "0088" -> char = '8'
                "0089" -> char = '9'
                "008a" -> char = '*'
                "008b" -> char = '/'
                "008c" -> char = '+'
                "008d" -> char = '-'
                "008e" -> char = '('
                "008f" -> char = ')'
                "0090" -> char = '.'

                "0091" -> char = 'a'
                "0093" -> char = 'c'
                "0099" -> char = 'i'
                "009e" -> char = 'n'
                "009f" -> char = 'o'
                "00a4" -> char = 's'
                "00a5" -> char = 't'
                "00ac" -> char = 'π'
            }
            temp += char
            i++
         }
        return temp
    }

    // Получить надстрочный индекс
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
            '-' -> '\u207B'

            else -> ' '
        }
    }

    // Получить приоритет
    private fun getPriority(c: String): Int {
        when (c) {
            ")" -> return -1
            "(" -> return 1
            "+", "-" -> return 2
            "*", "/" -> return 3
            "√", "^" -> return 4
            "sin", "cos", "tan" -> return 5
            "0", "1", "2", "3", "4", "5", "6",
            "7", "8", "9", "." -> return 0
            else -> return -2
        }
    }

}