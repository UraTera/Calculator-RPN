package com.tera.calculator_rpn

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tera.calculator_rpn.databinding.ActivityMainBinding
import com.tera.calculator_rpn.history.HistoryActivity
import com.tera.calculator_rpn.history.ItemModel
import com.tera.calculator_rpn.utils.Calculator
import com.tera.calculator_rpn.utils.MyConst
import com.tera.calculator_rpn.utils.TopIndex
import com.tera.calculator_rpn.utils.Utils

class MainActivity : AppCompatActivity(), SelectTimeDialog.ListenerDialog {

    private lateinit var binding: ActivityMainBinding

    private val calculator = Calculator()
    private val topIndex = TopIndex()
    private val utils = Utils()

    private lateinit var sp: SharedPreferences
    private val gson = Gson()

    private var list = ArrayList<ItemModel>()
    private lateinit var bnNum: Array<TextView>
    private lateinit var bnOperation: Array<TextView>

    private var nScreen = 1 // Номер экрана
    private var nKeyboard = 1 // Номер Клавиатуры
    private var nFont = 1 // Номер шрифта

    private var posCursor = 0
    private var memory = ""
    private var orientation = true // Портрет
    private var keyGroupTr = false
    private var keyMenu = false

    private var keyRem = false // Ключ записи выражения в поле ввода из истории
    private var position = 0
    private var exprOld = ""
    private var timeOld = ""

    private var timer: CountDownTimer? = null
    private var brightSys = 0    // Установленная яркость
    private var brightMin = 0.0f // Минимальная яркость. На эмуляторе не работает
    private var screenTimeOut = 0L

    private var numTimeOut = 0
    private var timeOutStr = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sp = getSharedPreferences("parameters", MODE_PRIVATE)

//        initParams()
        initButtons()

        // Регистрация кнопки Back
        regBack()
    }
    //-------------------

    // Задержка
    private fun withDelay(delay: Long, block: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed(Runnable(block), delay)
    }

    private fun initParams() = with(binding) {
        // Отключить блокировку экрана
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Получить ориентацию
        orientation = getScreenOrientation()

        edExpr.text.clear()
        tvRes.text = ""
        // Выражение
        val expression = sp.getString(MyConst.EXPR, "").toString()
        edExpr.setText(expression)
        posCursor = edExpr.text.length
        edExpr.setSelection(posCursor)
        // Установить фокус
        edExpr.requestFocus()
        // Отключить клавиатуру
        edExpr.showSoftInputOnFocus = false
        // Отключить буфер обмена
        edExpr.isLongClickable = false
        edExpr.setTextIsSelectable(false)
        // Отключить проверку орфографии
        edExpr.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        // Скрыть курсор
        edExpr.isCursorVisible = false
        // Многострочный текст
        edExpr.isSingleLine = false
        // Скрыть View блокировки
        view.isVisible = false

        // Текущая яркость (100)
        brightSys = getBright()

        tvTimeOut.text = timeOutStr
    }

    private fun initButtons() = with(binding) {

        // Позиция Курсора
        edExpr.setOnClickListener {
            edExpr.isCursorVisible = true
            posCursor = binding.edExpr.selectionStart
        }

        // Переместить влево
        bnLeft.setOnClickListener {
            if (posCursor > 0)
                posCursor--
            edExpr.setSelection(posCursor)
        }

        // Переместить вправо
        bnRight.setOnClickListener {
            val str = edExpr.text
            val len = str.length
            if (posCursor < len)
                posCursor++
            edExpr.setSelection(posCursor)
        }

        // Включить экран
        view.setOnClickListener {
            setBright(brightSys.toFloat())
            view.isVisible = false
        }

        // Показать меню
        bnMenu.setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }

        // Радиокнопки Экран
        rgScreen.setOnCheckedChangeListener { _, checkedId ->
            if (R.id.rbScreenLight == checkedId) {
                nScreen = 1
                setScreen()
            } else if (R.id.rbScreenDark == checkedId) {
                nScreen = 2
                setScreen()
            }
            // Закрыть драйвер
            drawer.closeDrawer(GravityCompat.START)
        }

        // Клавиатура
        rgKeyboard.setOnCheckedChangeListener { _, checkedId ->
            if (R.id.rbKeyColor == checkedId) {
                nKeyboard = 1
                setGradStyle()
            } else if (R.id.rbKeyLight == checkedId) {
                nKeyboard = 2
                setLightStyle()
            } else if (R.id.rbKeyDark == checkedId) {
                nKeyboard = 3
                setBlackStyle()
            }
            // Закрыть драйвер
            drawer.closeDrawer(GravityCompat.START)
        }

        // Шрифт
        rgFont.setOnCheckedChangeListener { _, checkedId ->
            if (R.id.rbFontNormal == checkedId) {
                nFont = 1
                setFont()
            } else if (R.id.rbFontLed == checkedId) {
                nFont = 2
                setFont()
            }
            // Закрыть драйвер
            drawer.closeDrawer(GravityCompat.START)
        }

        // Открыть историю
        tvHistory.setOnClickListener {
            openHistory()
            drawer.closeDrawer(GravityCompat.START)
        }

        llTime.setOnClickListener {
            openDialog()
        }

        // Массивы Кнопок
        bnNum = arrayOf(bn0, bn1, bn2, bn3, bn4, bn5, bn6, bn7, bn8, bn9, bnDot, bnRes)
        bnOperation = arrayOf(
            bnSin, bnCos, bnTan, bnPi, bnGroup, bnMPlus, bnMr, bnQrt, //Тригонометрия
            bnCk1, bnCk2, bnMul, bnDiv, bnSum, bnMin, // Операции
            bnBack, bnC
        )
    }

    // Открыть диалог
    private fun openDialog() {
        val manager = supportFragmentManager
        val dialog = SelectTimeDialog()
        val bundle = Bundle()
        bundle.putInt(MyConst.NUM_TIME_OUT, numTimeOut)
        dialog.arguments = bundle
        dialog.show(manager, "time fragment")
    }

    // Данные из диалога
    override fun selectNumTime(num: Int, text: String) {
        numTimeOut = num
        timeOutStr = text
        binding.tvTimeOut.text = text
        setScreenTime()
        // Закрыть драйвер
        binding.drawer.closeDrawer(GravityCompat.START)
    }

    private fun setScreenTime() {
        when(numTimeOut){
            0 -> screenTimeOut = 15000L
            1 -> screenTimeOut = 30000L
            2 -> screenTimeOut = 60000L
            3 -> screenTimeOut = 120000L
            4 -> screenTimeOut = 300000L
            5 -> screenTimeOut = 600000L
        }
        startTimer()
    }

    // Получить текущую яркость экрана
    private fun getBright(): Int {
        val br = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
        return br // 100
    }

    // Установить яркость приложения
    private fun setBright(value: Float) {
        val lp = window.attributes
        lp.screenBrightness = value
        window.attributes = lp
    }

    // Активность приложения. Старт таймера
    override fun onUserInteraction() {
        super.onUserInteraction()
        startTimer()
    }

    // Таймер
    private fun startTimer() = with(binding) {
        timer?.cancel()
        var count = screenTimeOut / 1000
        timer = object : CountDownTimer(screenTimeOut, 1000) {
            override fun onTick(timeM: Long) {
                count--
                //Log.d("myLogs", "Timer, count: $count")
            }
            override fun onFinish() {
                setBright(brightMin)
                view.isVisible = true
                edExpr.isCursorVisible = false
                //Log.d("myLogs", "Timer, СТОП")
            }
        }.start()
    }

    // Восстановить данные
    override fun onResume() {
        super.onResume()
        memory = sp.getString(MyConst.MEM, "").toString()
        nScreen = sp.getInt(MyConst.SCREEN, 1)
        nKeyboard = sp.getInt(MyConst.KBRD, 1)
        nFont = sp.getInt(MyConst.FONT, 1)
        keyGroupTr = sp.getBoolean(MyConst.TRIG, false)
        timeOld = sp.getString(MyConst.TIME, "").toString()
        screenTimeOut = sp.getLong(MyConst.SCREEN_OUT, 120000L) // 2 min

        val str = getString(R.string.time_def)
        numTimeOut = sp.getInt(MyConst.NUM_TIME_OUT, MyConst.NUM_TIME_DEF) // 2 min
        timeOutStr = sp.getString(MyConst.TIME_OUT, str).toString()

        val strList = sp.getString(MyConst.LIST, "[]")
        val itemType = object : TypeToken<java.util.ArrayList<ItemModel>>() {}.type
        list = gson.fromJson(strList, itemType)

        initParams()
        setCheckMenu()
        showGroupTrig()
        getHistory()
        getRes() // Вычислить выражение
        startTimer()
    }

    // Сохранить данные
    override fun onStop() {
        super.onStop()
        val expr = binding.edExpr.text.toString()
        val strList = gson.toJson(list)

        val editor = sp.edit()
        editor.putString(MyConst.EXPR, expr)
        editor.putString(MyConst.MEM, memory)
        editor.putInt(MyConst.SCREEN, nScreen)
        editor.putInt(MyConst.KBRD, nKeyboard)
        editor.putInt(MyConst.FONT, nFont)
        editor.putBoolean(MyConst.TRIG, keyGroupTr)
        editor.putString(MyConst.LIST, strList)
        editor.putString(MyConst.TIME, timeOld)

        editor.putInt(MyConst.NUM_TIME_OUT, numTimeOut)
        editor.putString(MyConst.TIME_OUT, timeOutStr)
        editor.putLong(MyConst.SCREEN_OUT, screenTimeOut)
        editor.apply()

        timer?.cancel()
    }

    // Поворот
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Открыт ли драйвер
        keyMenu = binding.drawer.isDrawerOpen(GravityCompat.END)
        outState.putInt(MyConst.CUR, posCursor)
        outState.putBoolean(MyConst.MENU, keyMenu)
    }

    // Восстановить меню
    private fun setCheckMenu() {
        if (!keyMenu) {
            binding.apply {
                when (nScreen) {
                    1 -> {
                        rbScreenLight.isChecked = true
                        setScreen()
                    }

                    2 -> {
                        rbScreenDark.isChecked = true
                        setScreen()
                    }
                }
                when (nKeyboard) {
                    1 -> {
                        rbKeyColor.isChecked = true
                        setGradStyle()
                    }

                    2 -> {
                        rbKeyLight.isChecked = true
                        setLightStyle()
                    }

                    3 -> {
                        rbKeyDark.isChecked = true
                        setBlackStyle()
                    }
                }
                when (nFont) {
                    1 -> {
                        rbFontNormal.isChecked = true
                        setFont()
                    }

                    2 -> {
                        rbFontLed.isChecked = true
                        setFont()
                    }
                }
            }
        }
    }

    // Получить Ориентацию
    private fun getScreenOrientation(): Boolean {
        if (resources.configuration.orientation ==
            Configuration.ORIENTATION_PORTRAIT
        ) return true
        else return resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
    }

    // Точка
    private fun setDot() {
        val expr = binding.edExpr.text.toString()
        val key = utils.checkDot(expr, posCursor)
        if (!key) {
            writeWord(".")
            getRes()
        }
    }

    // Данные из памяти
    private fun getDataFromMemory() {
        var expr = binding.edExpr.text.toString()
        val value = memory.trim()
        expr += value
        binding.edExpr.setText(expr)
        posCursor = expr.length
        binding.edExpr.setSelection(posCursor)
        getRes()
    }

    // Сброс
    private fun clear() {
        setArray()
        binding.edExpr.setText("")
        binding.tvRes.text = ""
        posCursor = 0
    }

    // Равно
    private fun result() {
        binding.apply {
            var res = tvRes.text.toString()
            res = res.trim()
            edExpr.setText(res)
            tvRes.text = ""
            // Курсор
            val len = edExpr.text.length
            posCursor = len
            edExpr.setSelection(posCursor)
        }
    }

    // Запись в строку ввода
    private fun writeWord(str: String) {

        val expr = binding.edExpr.text

        var s1 = expr.substring(0, posCursor)
        val s2 = expr.substring(posCursor)

        s1 += str

        posCursor = s1.length
        val res = (s1 + s2)

        binding.edExpr.setText(res)
        if (res.contains('√'))
            setSuperscript(res) // Установить надстрочный индекс и черту сврху

        binding.edExpr.setSelection(posCursor)
    }

    // Удалить символ
    private fun deleteLastSinbol() {
        binding.apply {
            val str = edExpr.text.toString()
            val len = str.length
            // Не пустая
            if (str.isNotEmpty()) {
                if (str[len - 1].isLetter()) { // Буквы sin, cos, tan
                    edExpr.setText(str.substring(0, len - 3))
                    posCursor -= 3
                    edExpr.setSelection(posCursor)
                } else {

                    if (posCursor > 0) {
                        val s1 = str.substring(0, posCursor - 1)
                        val s2 = str.substring(posCursor)
                        val res: String = s1 + s2
                        edExpr.setText(res)
                        if (!res.contains('√'))
                            setNormalSymbol(res)
                        posCursor--
                    }
                    edExpr.setSelection(posCursor)
                }
            }
            tvRes.text = ("")
        }
    }

    // Получить результат
    private fun getRes() {
        val express = binding.edExpr.text.toString()
        try {
            val exp = calculator.calc(express) + " "
            binding.tvRes.text = exp
        } catch (e: Exception) {
            binding.tvRes.text = ""
        }
    }

    // Установить надстрочный индекс и черту сврху
    private fun setSuperscript(expression: String) {
        try {
            var expr = topIndex.getRootIndex(expression) // Число перед корнем в верхний индекс
            expr = topIndex.dashSymbol(expr)      // Символ с чертой
            binding.edExpr.setText(expr)
        } catch (_: Exception) {
        }
    }

    // Нормальные символы
    private fun setNormalSymbol(expression: String) {
        var expr = calculator.indtoNumber(expression)
        expr = calculator.dashToSymbol(expr)
        expr = expr.replace('*', '\u00D7')
        expr = expr.replace('-', '\u2012')
        binding.edExpr.setText(expr)
    }

    //--------------------------------------
    // Скрыть / Показать кнопки Тригонометрии от кнопки
    private fun showGroupTrigFromButton() {
        if (keyGroupTr) {
            hideGroup()
            keyGroupTr = false
        } else {
            showGroup()
            keyGroupTr = true
        }
    }

    // Скрыть / Показать кнопки Тригонометрии
    private fun showGroupTrig() {
        if (keyGroupTr) {
            showGroup()
        } else {
            hideGroup()
        }
    }

    private fun showGroup() {
        val nameButton = getString(R.string.ht)
//        val animation = AnimationUtils.loadAnimation(this, R.anim.show_bn_alpha)
        binding.apply {
            grTrig.visibility = View.VISIBLE
//            grTrig.startAnimation(animation)
            bnGroup.text = nameButton

            if (orientation)
                bnPi.visibility = View.VISIBLE
            else bnPi.visibility = View.GONE
        }
    }

    private fun hideGroup() {
        val nameButton = getString(R.string.st)
        binding.apply {
            grTrig.visibility = View.GONE
            bnGroup.text = nameButton
        }
    }

    //---------------------------

    // Заполнение массива
    private fun setArray() {
        val maxItem = 30
        val exprNew = binding.edExpr.text.toString() // Новое выражение
        val size = list.size

        if (exprNew.isNotEmpty()) {

            val result = "= ${binding.tvRes.text}"

            val time = utils.getDate() // Системное время
            var keyData = false

            if (time == timeOld) // Времени равны
                keyData = true

            if (keyRem) { // Добавить выражение в массив из Адаптера

                if (exprNew == exprOld) { // Новое и старое выражение равны
                    // Удалить старую запись, Новую вставить в 0 позицию
                    val pos2 = position + 1

                    if (size > pos2) {
                        val time1 = list[position].time
                        val time2 = list[pos2].time

                        if (time1 != "" && time2 == "") {
                            list[pos2].time = time1
                        }
                    }
                    list.removeAt(position)  // Удалить старую строку
                }
                addList(keyData, exprNew, result, time)

            } else {  // Добавить выражение в массив из полей ввода
                addList(keyData, exprNew, result, time)
            }

            if (size > maxItem) {
                list.removeAt(size - 1)
            }
        }
    }

    // Вставмть значения в Лист
    fun addList(key: Boolean, expr: String, res: String, timeF: String) {
        if (key) { // Времена равны
//            Log.d(TAG, "if(key) position: $position")
            list.add(0, ItemModel(expr, res, timeF, false))
            val size = list.size

            if (size > 1) {
                list[1].time = ""
            }
        } else {
            list.add(0, ItemModel(expr, res, timeF, false))
        }
        keyRem = false
        timeOld = timeF
    }

    // Открыть историю
    private fun openHistory() {
        val intent = Intent(this, HistoryActivity::class.java)
        val strList = gson.toJson(list)
        intent.putExtra(MyConst.LIST, strList)

        // Справа налево
        val enterAnim = R.anim.right_in
        val exitAnim = R.anim.left_out
        val options = ActivityOptionsCompat.makeCustomAnimation(this, enterAnim, exitAnim)
        startActivity(intent, options.toBundle())
    }

    // Получить данные из истории
    private fun getHistory() {
        // Данные из истории в поле ввода из Адаптера
        keyRem = intent.getBooleanExtra(MyConst.REM, false)

        if (keyRem) {
            // Получить список
            val strList = intent.getStringExtra(MyConst.LIST)
            val itemType = object : TypeToken<java.util.ArrayList<ItemModel>>() {}.type
            list = gson.fromJson(strList, itemType)

            exprOld = intent.getStringExtra(MyConst.EXPR).toString()
            position = intent.getIntExtra(MyConst.POS, 0)

            binding.edExpr.setText(exprOld)
            posCursor = exprOld.length
            binding.edExpr.setSelection(posCursor)
        }
        // Список из истории
        val keyClear = intent.getBooleanExtra(MyConst.CLEAR, false)
        if (keyClear) {
            val strList = intent.getStringExtra(MyConst.LIST)
            val itemType = object : TypeToken<java.util.ArrayList<ItemModel>>() {}.type
            list = gson.fromJson(strList, itemType)
        }
    }

    //---------------------------------

    // Обработка кнопок
    fun onClick(view: View) {
        when (view.id) {
            R.id.bn0 -> {
                writeWord("0"); getRes()
            }

            R.id.bn1 -> {
                writeWord("1"); getRes()
            }

            R.id.bn2 -> {
                writeWord("2"); getRes()
            }

            R.id.bn3 -> {
                writeWord("3"); getRes()
            }

            R.id.bn4 -> {
                writeWord("4"); getRes()
            }

            R.id.bn5 -> {
                writeWord("5"); getRes()
            }

            R.id.bn6 -> {
                writeWord("6"); getRes()
            }

            R.id.bn7 -> {
                writeWord("7"); getRes()
            }

            R.id.bn8 -> {
                writeWord("8"); getRes()
            }

            R.id.bn9 -> {
                writeWord("9"); getRes()
            }

            R.id.bnDot -> setDot()

            // Скобки
            R.id.bnCk1 -> writeWord("(")
            R.id.bnCk2 -> {
                writeWord(")"); getRes()
            }
            // Удалить последний символ
            R.id.bnBack -> {
                deleteLastSinbol(); getRes()
            }
            // Сброс
            R.id.bnC -> clear()

            // Простые операции
            R.id.bnMul -> {
                writeWord("\u00D7")
            }

            R.id.bnDiv -> {
                writeWord("/")
            }

            R.id.bnSum -> {
                writeWord("+")
            }

            R.id.bnMin -> {
                writeWord("\u2012")
            }

            // Равно
            R.id.bnRes -> result()

            R.id.bnMPlus -> memory = binding.tvRes.text.toString()
            R.id.bnMr -> getDataFromMemory()
            R.id.bnGroup -> showGroupTrigFromButton()

            R.id.bnQrt -> writeWord("√")        // Корень

            // Тригонометрия
            R.id.bnSin -> writeWord("sin(")
            R.id.bnCos -> writeWord("cos(")
            R.id.bnTan -> writeWord("tan(")
            R.id.bnPi -> {
                writeWord("π"); getRes()
            }
        }
    }

    //-------------------------

    // Экран
    private fun setScreen() = with(binding) {
        val colorL = ContextCompat.getColor(this@MainActivity, R.color.bnL)
        val colorD = ContextCompat.getColor(this@MainActivity, R.color.bnD)

        when(nScreen) {
            1 -> { // Светлая
                if (orientation) screen.setBackgroundResource(R.drawable.screen_vk)
                else screen.setBackgroundResource(R.drawable.screen_hk)
                edExpr.setTextColor(Color.BLACK)
                tvRes.setTextColor(Color.BLACK)
                bnMenu.setImageResource(R.drawable.ic_menu_black)
                bnLeft.setImageResource(R.drawable.ic_arrow_left_black)
                bnRight.setImageResource(R.drawable.ic_arrow_right_black)
                // Пульсация
                var ripple = bnLeft.background as? RippleDrawable
                ripple?.setColor(ColorStateList.valueOf(colorL))
                ripple = bnRight.background as? RippleDrawable
                ripple?.setColor(ColorStateList.valueOf(colorL))
            }

            2 -> { // Темная
                screen.setBackgroundResource(R.drawable.screen_grad)
                edExpr.setTextColor(Color.WHITE)
                tvRes.setTextColor(Color.CYAN)
                bnMenu.setImageResource(R.drawable.ic_menu_white)
                bnLeft.setImageResource(R.drawable.ic_arrow_left_white)
                bnRight.setImageResource(R.drawable.ic_arrow_right_white)

                // Пульсация
                var ripple = bnLeft.background as? RippleDrawable
                ripple?.setColor(ColorStateList.valueOf(colorD))
                ripple = bnRight.background as? RippleDrawable
                ripple?.setColor(ColorStateList.valueOf(colorD))
                edExpr.background = null
            }

            else -> {}
        }
    }

    // Стиль Градиент (Цветные)
    private fun setGradStyle() = with(binding) {

        for (i in bnNum.indices) {
            bnNum[i].setBackgroundResource(R.drawable.grad_bn_num)
        }

        for (i in 0..7) {
            bnOperation[i].setBackgroundResource(R.drawable.grad_bn_fun)
        }

        for (i in 8..13) {
            bnOperation[i].setBackgroundResource(R.drawable.grad_bn_oper)
        }

        for (i in 14..15) {
            bnOperation[i].setBackgroundResource(R.drawable.grad_bn_clear)
        }

        content.forEach {
            if (it is TextView) it.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.white
                )
            )
        }

        bnBack.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        bnC.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        content.setBackgroundResource(R.color.black)
        // navigation bar
        window.navigationBarColor = ContextCompat.getColor(this@MainActivity, R.color.black)
    }

    // Стиль светлый
    private fun setLightStyle() = with(binding) {

        for (i in bnNum.indices) {
            bnNum[i].setBackgroundResource(R.drawable.met_num)
        }

        for (i in bnOperation.indices) {
            bnOperation[i].setBackgroundResource(R.drawable.met_oper)
        }

        content.forEach {
            if (it is TextView) it.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.black
                )
            )
        }

        bnBack.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
        bnC.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
        content.setBackgroundResource(R.color.gray)
        // navigation bar
        window.navigationBarColor = ContextCompat.getColor(this@MainActivity, R.color.gray)

    }

    // Стиль черный
    private fun setBlackStyle() = with(binding) {

        for (i in bnNum.indices) {
            bnNum[i].setBackgroundResource(R.drawable.black_num)
        }

        for (i in bnOperation.indices) {
            bnOperation[i].setBackgroundResource(R.drawable.black_fun)
        }

        content.forEach {
            if (it is TextView) it.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.gray_200
                )
            )
        }

        bnBack.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_200))
        bnC.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_200))
        content.setBackgroundResource(R.color.gray_500)
        // navigation bar
        window.navigationBarColor = ContextCompat.getColor(this@MainActivity, R.color.gray_500)

    }

    // Шрифт строки результатс
    private fun setFont() {
        when (nFont) {
            1 -> binding.tvRes.typeface = ResourcesCompat.getFont(this, R.font.my_font)
            2 -> binding.tvRes.typeface = ResourcesCompat.getFont(this, R.font.led_digit_italic)
        }
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
        finishAffinity()
    }
}