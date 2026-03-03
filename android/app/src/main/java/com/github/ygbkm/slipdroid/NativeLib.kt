package com.github.ygbkm.slipdroid

object NativeLib {
    init {
        System.loadLibrary("slipstream_client")
    }
    external fun initLogger()
    external fun startClient(args: String): String
    external fun stopClient()
}
