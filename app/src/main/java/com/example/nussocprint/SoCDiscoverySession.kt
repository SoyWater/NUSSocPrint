package com.example.nussocprint

import android.print.PrintAttributes
import android.print.PrintAttributes.Margins
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrinterDiscoverySession
import android.util.Log
import com.hierynomus.mssmb2.SMB2Dialect
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.mssrvs.dto.NetShareInfo1
import com.rapid7.client.dcerpc.transport.RPCTransport
import com.rapid7.client.dcerpc.transport.SMBTransportFactories


class SoCDiscoverySession(private val service: NUSSocPrint) : PrinterDiscoverySession() {

    val TAG: String = "MyPrinterDiscovery"

    override fun onStartPrinterDiscovery(priorityList: List<PrinterId?>) {
        Log.d(TAG, "Starting printer discovery...")

        val printers: MutableList<PrinterInfo?> = ArrayList<PrinterInfo?>()

        // Generate a local printer ID
        val printerId = generatePrinterId("local-demo-printer")

        // Define capabilities (A4, color, 600dpi)
        val capabilities = PrinterCapabilitiesInfo.Builder(printerId)
            .addMediaSize(PrintAttributes.MediaSize.ISO_A4, true)
            .addResolution(PrintAttributes.Resolution("600dpi", "600dpi", 600, 600), false)
            .setColorModes(PrintAttributes.COLOR_MODE_COLOR, PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(Margins(0, 0, 0, 0))
            .build()

        val printer = PrinterInfo.Builder(printerId, "Local Demo Printer", PrinterInfo.STATUS_IDLE)
            .setCapabilities(capabilities)
            .build()

        printers.add(printer)
        addPrinters(printers)
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

fun listShares(host: String, domain: String, username: String, password: String): List<NetShareInfo1?>? {
    val client = SMBClient(
        SmbConfig.builder()
            .withDialects(SMB2Dialect.SMB_3_1_1) // force SMB3.1.1
            .withEncryptData(true) // full encryption (AES-128-GCM)
            .build()
    )
    client.use { smbClient ->
        println("Connecting to $host")
        val connection: Connection = smbClient.connect(host);
        val auth = AuthenticationContext(username, password.toCharArray(), domain);
        val session: Session = connection.authenticate(auth);

        val transport: RPCTransport = SMBTransportFactories.SRVSVC.getTransport(session);
        val serverService: ServerService = ServerService(transport);


        return serverService.shares1;
    }

}