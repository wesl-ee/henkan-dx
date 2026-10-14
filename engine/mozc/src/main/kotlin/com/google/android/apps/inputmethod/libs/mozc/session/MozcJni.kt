package com.google.android.apps.inputmethod.libs.mozc.session

object MozcJni {
    init {
        System.loadLibrary("mozc")
        check(initialize())
    }

    @JvmStatic private external fun initialize(): Boolean

    @JvmStatic external fun evalCommand(command: ByteArray): ByteArray

    @JvmStatic external fun onPostLoad(profileDir: String, dataPath: String): Boolean

    @JvmStatic external fun getDataVersion(): String
}
