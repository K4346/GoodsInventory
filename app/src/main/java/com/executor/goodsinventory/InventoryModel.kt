package com.executor.goodsinventory

import com.executor.goodsinventory.domain.env.Logger

object InventoryModel {
    var classes:Int=0
    var labels = java.util.ArrayList<String>()



    const val CAMERA_REQUEST = 1888
    const val GALLERY_REQUEST = 100

    const val MINIMUM_CONFIDENCE_TF_OD_API = 0.5f

    private const val MAINTAIN_ASPECT = false
    val LOGGER: Logger = Logger()

    const val TF_OD_API_INPUT_SIZE = 416

    const val TF_OD_API_IS_QUANTIZED = false

    const val TF_OD_API_MODEL_FILE = "custom_yolov4.tflite"

    const val TF_OD_API_LABELS_FILE = "file:///android_asset/predefined_classes.txt"

}