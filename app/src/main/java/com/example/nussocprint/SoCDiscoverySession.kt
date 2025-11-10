package com.example.nussocprint

import android.print.PrintAttributes
import android.print.PrintAttributes.Margins
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrinterDiscoverySession
import android.util.Log
import com.example.nussocprint.util.EncryptedDataStore
import com.example.nussocprint.util.SmbjUtils.getPrinterList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel

private const val TAG: String = "SoCPrinterDiscovery"

class SoCDiscoverySession(private val service: NUSSocPrint) : PrinterDiscoverySession() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val discoveredPrinters = mutableMapOf<PrinterId, PrinterInfo>()

    override fun onStartPrinterDiscovery(priorityList: List<PrinterId?>) {
        Log.i(TAG, "onStartPrinterDiscovery() - Starting Printer Discovery")

        scope.launch {
            val credentials = EncryptedDataStore.getCredentials(service.applicationContext)
            if (credentials == null) {
                Log.d(TAG, "No credentials found")
                return@launch
            }

            val printers: MutableList<PrinterInfo?> = ArrayList()

            val socPrinters = getPrinterList(HOST, DOMAIN, credentials.username, credentials.password)
            for (printerName in socPrinters) {
                val printerId: PrinterId = generatePrinterId(printerName)

                // Define capabilities (A4, color, 600dpi)
                val capabilities = PrinterCapabilitiesInfo.Builder(printerId)
                    .addMediaSize(PrintAttributes.MediaSize.ISO_A4, true)
                    .addResolution(PrintAttributes.Resolution("600dpi", "600dpi", 600, 600), true)
                    .setColorModes(PrintAttributes.COLOR_MODE_COLOR, PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(Margins(0, 0, 0, 0))
                    .build()

                val printer =
                    PrinterInfo.Builder(printerId, printerName, PrinterInfo.STATUS_IDLE)
                        .setCapabilities(capabilities)
                        .build()

                printers.add(printer)
                discoveredPrinters[printerId] = printer
            }

            withContext(Dispatchers.Main) {
                addPrinters(printers)
            }
        }
    }

    /**
     * Ignore everything below - Only here to satisfy the interface
     */
    override fun onDestroy() {
        Log.d(TAG, "onDestroy() - Discovery session destroyed")
        scope.cancel()
    }

    override fun onValidatePrinters(printerIds: List<PrinterId?>) {
        Log.d(TAG, "onValidatePrinters() - Validating Printers: $printerIds")
        addPrinters(printerIds.mapNotNull { discoveredPrinters[it] })
    }

    override fun onStartPrinterStateTracking(printerId: PrinterId) {
        Log.d(TAG, "onStartPrinterStateTracking($printerId)- no-op")
    }

    override fun onStopPrinterStateTracking(printerId: PrinterId) {
        Log.d(TAG, "onStopPrinterStateTracking($printerId) - no-op")
    }

    override fun onStopPrinterDiscovery() {
        Log.d(TAG, "onStopPrinterDiscovery() - no-op")
    }

    private fun generatePrinterId(localName: String?): PrinterId {
        return service.generatePrinterId(localName)
    }
}
