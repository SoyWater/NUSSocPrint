package com.example.nussocprint

import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession

class NUSSocPrint : PrintService() {
    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        return SoCDiscoverySession(this)
    }

    override fun onPrintJobQueued(printJob: PrintJob?) {
        TODO("Not yet implemented")
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        printJob.cancel()
    }
}