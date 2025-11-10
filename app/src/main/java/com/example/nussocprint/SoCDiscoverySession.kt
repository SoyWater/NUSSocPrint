package com.example.nussocprint

import android.print.PrintAttributes
import android.print.PrintAttributes.Margins
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrinterDiscoverySession
import android.util.Log
import com.example.nussocprint.util.EncryptedDataStore
import com.example.nussocprint.util.listShares
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SoCDiscoverySession(private val service: NUSSocPrint) : PrinterDiscoverySession() {

    private val TAG: String = "MyPrinterDiscovery"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val host = "nts27.comp.nus.edu.sg"
    private val domain = "nusstu"

    override fun onStartPrinterDiscovery(priorityList: List<PrinterId?>) {
        Log.i(TAG, "Starting printer discovery...")

        scope.launch {
            val credentials = EncryptedDataStore.getCredentials(service.applicationContext)
            if (credentials == null) {
                Log.d(TAG, "No credentials found")
                return@launch
            }

            val printers: MutableList<PrinterInfo?> = ArrayList<PrinterInfo?>()

            val socPrinters = listShares(host, domain, credentials.username, credentials.password)
            for (printer in socPrinters) {
                // Generate a local printer ID
                val printerId: PrinterId = withContext(Dispatchers.Main) {
                    generatePrinterId(printer)
                }

                // Define capabilities (A4, color, 600dpi)
                val capabilities = PrinterCapabilitiesInfo.Builder(printerId)
                    .addMediaSize(PrintAttributes.MediaSize.ISO_A4, true)
                    .addResolution(PrintAttributes.Resolution("600dpi", "600dpi", 600, 600), true)
                    .setColorModes(PrintAttributes.COLOR_MODE_COLOR, PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(Margins(0, 0, 0, 0))
                    .build()

                val printer =
                    PrinterInfo.Builder(printerId, printer, PrinterInfo.STATUS_IDLE)
                        .setCapabilities(capabilities)
                        .build()

                printers.add(printer)
            }

            withContext(Dispatchers.Main) {
                addPrinters(printers)
            }
        }
    }

    override fun onStopPrinterDiscovery() {
        Log.d(TAG, "Stopping printer discovery...")
    }

    override fun onStopPrinterStateTracking(printerId: PrinterId) {
        TODO("Not yet implemented")
    }

    override fun onValidatePrinters(printerIds: List<PrinterId?>) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        Log.d(TAG, "Discovery session destroyed")
    }

    override fun onStartPrinterStateTracking(printerId: PrinterId) {
        TODO("Not yet implemented")
    }

    private fun generatePrinterId(localName: String?): PrinterId {
        return service.generatePrinterId(localName)
    }
}

