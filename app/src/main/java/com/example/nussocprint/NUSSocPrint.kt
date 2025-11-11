package com.example.nussocprint

import android.os.ParcelFileDescriptor
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException

private const val TAG: String = "SoCPrintService"

const val HOST: String = "nts27.comp.nus.edu.sg"
const val DOMAIN: String = "nusstu"

// run a function on main thread, reduce boilerplate
private suspend fun PrintJob.runOnMain(block: PrintJob.() -> Unit) {
    withContext(Dispatchers.Main) {
        this@runOnMain.block()
    }
}

private fun PrintJob.postOnMain(block: PrintJob.() -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        this.block()
    } else {
        val job = this
        Handler(Looper.getMainLooper()).post {
            job.block()
        }
    }
}

class NUSSocPrint : PrintService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        Log.d(TAG, "onCreatePrinterDiscoverySession() - returning SoCDiscoverySession")
        return SoCDiscoverySession(this)
    }

    override fun onPrintJobQueued(printJob: PrintJob) {
        Log.d(TAG, "onPrintJobQueued() - new print job: $printJob")
        printJob.postOnMain { start() }
        scope.launch {
            try {
                // Get credentials
                val credentials = EncryptedDataStore.getCredentials(applicationContext)
                if (credentials == null) {
                    Log.d(TAG, "No credentials found")
                    printJob.runOnMain { fail("No credentials configured for printing") }
                    return@launch
                }

                // Get printer name
                val printerName = withContext(Dispatchers.Main) {
                    printJob.info.printerId?.localId
                }
                if (printerName == null) {
                    Log.d(TAG, "No printer found")
                    printJob.runOnMain { fail("Printer information unavailable") }
                    return@launch
                }
                Log.d(TAG, "Printing to $printerName")

                // Get document ParcelFileDescriptor
                val pfd: ParcelFileDescriptor? = withContext(Dispatchers.Main) {
                    try {
                        printJob.document.data
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed to get document data", e)
                        printJob.fail("Failed to access print document: ${e.message}")
                        return@withContext null
                    }
                }


                if (pfd == null) {
                    Log.d(TAG, "No document data in print job")
                    printJob.runOnMain { fail("No document data to print") }
                    return@launch
                }

                // Parse file into stream and pass to printer
                try {
                    FileInputStream(pfd.fileDescriptor).use { stream ->
                        printStream(
                            HOST,
                            DOMAIN,
                            credentials.username,
                            credentials.password,
                            printerName,
                            stream
                        )
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "I/O error while printing", e)
                    printJob.runOnMain { fail("I/O error while printing: ${e.message}") }
                    return@launch
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security error while accessing document or network", e)
                    printJob.runOnMain { fail("Security error: ${e.message}") }
                    return@launch
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Illegal state during print", e)
                    printJob.runOnMain { fail("Print failed (illegal state): ${e.message}") }
                    return@launch
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error while printing", e)
                    printJob.runOnMain { fail("Unexpected error while printing: ${e.message}") }
                    return@launch
                } finally {
                    try {
                        pfd.close()
                    } catch (_: Exception) {
                    }
                }

                // If we've reached here, printing succeeded
                Log.d(TAG, "Print succeeded for $printerName")
                printJob.runOnMain { complete() }


            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to start or manage print job", e)
                try {
                    printJob.runOnMain { fail("Print job error: ${e.message}") }
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unhandled exception in print job handling", e)
                try {
                    printJob.runOnMain { fail("Unhandled error: ${e.message}") }
                } catch (_: Exception) {
                }
            }
        }

    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        printJob.postOnMain { cancel() }
    }
}
