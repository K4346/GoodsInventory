package com.executor.goodsinventory.data

object InventoryModel {
    var classes: Int = 0
    var labels = java.util.ArrayList<String>()

    lateinit var models: ArrayList<String>

    const val CAMERA_REQUEST = 1888
    const val GALLERY_REQUEST = 100

    @JvmField
    var accuracy = 0.5f

    @JvmField
    var TF_OD_API_INPUT_SIZE = 416

    @JvmField
    var isTiny = false

    var is_quantized = false

    var isMin:Int = 10

    var timeDelay = 1L

    var TF_OD_API_MODEL_FILE = "custom_yolov4.tflite"

    var TF_OD_API_LABELS_FILE = "file:///android_asset/predefined_classes.txt"

}