package com.executor.goodsinventory.domain.entities

data class Goods(
    val name:String,
    val code : Int,
    val color: Int,
    var count:Int = 0,
    var isFew: Boolean = false
)