package com.tera.calculator_rpn.history

import java.io.Serializable

data class ItemModel(
    var expr: String,
    var res: String,
    var time: String,
    var select: Boolean
)

