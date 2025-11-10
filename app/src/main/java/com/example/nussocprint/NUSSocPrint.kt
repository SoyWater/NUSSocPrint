package com.example.nussocprint

import android.os.ParcelFileDescriptor
import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession
import android.util.Log
import com.example.nussocprint.util.EncryptedDataStore
import com.example.nussocprint.util.SmbjUtils.printStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.FileInputStream

private const val TAG: String = "SoCPrintService"

const val HOST: String = "nts27.comp.nus.edu.sg"
const val DOMAIN: String = "nusstu"

class NUSSocPrint : PrintService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        Log.d(TAG, "onCreatePrinterDiscoverySession() - returning SoCDiscoverySession")
        return SoCDiscoverySession(this)
    }

    override fun onPrintJobQueued(printJob: PrintJob) {
        Log.d(TAG, "onPrintJobQueued() - new print job: $printJob")
        scope.launch {
            printJob.start()

            // Get credentials
            val credentials = EncryptedDataStore.getCredentials(applicationContext)
            if (credentials == null) {
                Log.d(TAG, "No credentials found")
                return@launch
            }

            // Get printer name
            val printerName = printJob.info.printerId?.localId
            if (printerName == null) {
                Log.d(TAG, "No printer found")
                return@launch
            }
            Log.d(TAG, "Printing to $printerName")

            // Parse file into stream and pass to printer
            printJob.document.data.use { pfd ->
                FileInputStream(pfd?.fileDescriptor).use { stream ->
                    printStream(
                        HOST,
                        DOMAIN,
                        credentials.username,
                        credentials.password,
                        printerName,
                        stream
                    )
                }
            }

        }

    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        printJob.cancel()
    }
}

