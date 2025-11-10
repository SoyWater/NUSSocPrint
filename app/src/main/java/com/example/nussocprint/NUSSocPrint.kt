package com.example.nussocprint

import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession
import android.util.Log

class NUSSocPrint : PrintService() {
    private val TAG: String = "MyPrinterDiscovery"
    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        Log.d(TAG, "onCreatePrinterDiscoverySession")
        return SoCDiscoverySession(this)
    }

    override fun onPrintJobQueued(printJob: PrintJob?) {
        TODO("Not yet implemented")
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        printJob.cancel()
    }
}