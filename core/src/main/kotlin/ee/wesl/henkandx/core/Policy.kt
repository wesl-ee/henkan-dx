package ee.wesl.henkandx.core

// These are Android's bit values. I've copied them here because the core module is pure Kotlin and cannot import Android.
private const val CLASS_MASK = 0xf
private const val CLASS_TEXT = 0x1
private const val CLASS_NUMBER = 0x2
private const val VARIATION_MASK = 0xff0
private const val NO_PERSONALIZED_LEARNING = 0x1000000
private val passwords = mapOf(
    CLASS_TEXT to setOf(0x80, 0x90, 0xe0),
    CLASS_NUMBER to setOf(0x10),
)
private val latin = setOf(0x10, 0x20, 0xd0)

fun sensitive(inputType: Int, imeOptions: Int): Boolean = inputType and VARIATION_MASK in passwords[inputType and CLASS_MASK].orEmpty() ||
    imeOptions and NO_PERSONALIZED_LEARNING != 0

fun composable(inputType: Int, imeOptions: Int): Boolean = inputType and CLASS_MASK == CLASS_TEXT &&
    inputType and VARIATION_MASK !in latin &&
    !sensitive(inputType, imeOptions)
