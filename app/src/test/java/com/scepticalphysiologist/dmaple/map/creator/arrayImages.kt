package com.scepticalphysiologist.dmaple.map.creator


fun createImage(w: Int, h: Int, background: Float): Array<FloatArray> {
    return Array(h) { FloatArray(w){ background } }
}

fun imageSize(image: Array<FloatArray>): Pair<Int, Int> {
    return Pair(image[0].size, image.size)
}

fun rotateImage(image: Array<FloatArray>): Array<FloatArray>{
    val (w, h) = imageSize(image)
    val rImage = createImage(h, w, 0f)
    for(i in 0 until w) for(j in 0 until h) rImage[i][j] = image[j][i]
    return rImage
}

fun applyImage(image: Array<FloatArray>, foo: (Float) -> Float){
    val (w, h) = imageSize(image)
    for(i in 0 until w) for(j in 0 until h) image[j][i] = foo(image[j][i])
}

fun paintSlice(image: Array<FloatArray>, i: Int, cent: Int, width: Int, value: Float, horizontal: Boolean) {
    val p = cent - width / 2
    val q = p + width - 1
    if(horizontal) for(j in p..q) image[i][j] = value
    else for(j in p..q) image[j][i] = value
}




