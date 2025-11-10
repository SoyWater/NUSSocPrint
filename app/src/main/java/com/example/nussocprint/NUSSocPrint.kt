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
import java.io.IOException

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
            try {
                // Start the job (may throw if job can't be started)
                printJob.start()

                // Get credentials
                val credentials = EncryptedDataStore.getCredentials(applicationContext)
                if (credentials == null) {
                    Log.d(TAG, "No credentials found")
                    printJob.fail("No credentials configured for printing")
                    return@launch
                }

                // Get printer name
                val printerName = printJob.info.printerId?.localId
                if (printerName == null) {
                    Log.d(TAG, "No printer found")
                    printJob.fail("Printer information unavailable")
                    return@launch
                }
                Log.d(TAG, "Printing to $printerName")

                // Get document ParcelFileDescriptor
                val pfd: ParcelFileDescriptor? = try {
                    // printJob.document is non-nullable per API here; access directly
                    printJob.document.data
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to get document data", e)
                    printJob.fail("Failed to access print document: ${e.message}")
                    return@launch
                }

                if (pfd == null) {
                    Log.d(TAG, "No document data in print job")
                    printJob.fail("No document data to print")
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
                    printJob.fail("I/O error while printing: ${e.message}")
                    return@launch
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security error while accessing document or network", e)
                    printJob.fail("Security error: ${e.message}")
                    return@launch
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Illegal state during print", e)
                    printJob.fail("Print failed (illegal state): ${e.message}")
                    return@launch
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error while printing", e)
                    printJob.fail("Unexpected error while printing: ${e.message}")
                    return@launch
                } finally {
                    // Close the ParcelFileDescriptor if it's still open
                    try {
                        pfd.close()
                    } catch (_: Exception) {
                    }
                }

                // If we've reached here, printing succeeded
                Log.d(TAG, "Print succeeded for $printerName")
                printJob.complete()

            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to start or manage print job", e)
                // Job may already be finished/cancelled
                try {
                    printJob.fail("Print job error: ${e.message}")
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unhandled exception in print job handling", e)
                try {
                    printJob.fail("Unhandled error: ${e.message}")
                } catch (_: Exception) {
                }
            }
        }

    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        printJob.cancel()
    }
}
