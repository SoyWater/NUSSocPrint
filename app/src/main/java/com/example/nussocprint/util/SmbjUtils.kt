package com.example.nussocprint.util

import com.hierynomus.mssmb2.SMB2Dialect
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.transport.TransportException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.PrinterShare
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.mssrvs.dto.NetShareInfo1
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import java.io.InputStream

const val PRINTER_TYPE = 1
const val EXCLUDE_NAME = "Lexmark Universal v2"

val SoCSmbConfig: SmbConfig = SmbConfig.builder()
    .withDialects(SMB2Dialect.SMB_3_0, SMB2Dialect.SMB_3_0_2, SMB2Dialect.SMB_3_1_1) // force SMB3 (soc only accepts this apparently)
    .withEncryptData(true)
    .build()


object SmbjUtils {
    fun getPrinterList(host: String, domain: String, username: String, password: String): List<String> {
        SMBClient(SoCSmbConfig).use { smbClient ->
            println("Connecting to $host")
            return try {
                smbClient.connect(host).use { connection ->
                    val auth = AuthenticationContext(username, password.toCharArray(), domain)
                    connection.authenticate(auth).use { session ->
                        val transport = SMBTransportFactories.SRVSVC.getTransport(session)
                        val serverService = ServerService(transport)

                        serverService.shares1
                            .filter { shareInfo -> shareInfo.type == PRINTER_TYPE && shareInfo.netName != EXCLUDE_NAME }
                            .map(NetShareInfo1::getNetName)
                    }
                }
            } catch (e: SMBApiException) {
                println("Authentication failed: " + e.message)
                emptyList()
            } catch (e: TransportException) {
                println("Connection failed: " + e.message)
                emptyList()
            } catch (e: Exception) {
                println("Error: " + e.message)
                emptyList()
            }
        }
    }

    fun printStream(
        host: String,
        domain: String,
        username: String,
        password: String,
        printer: String,
        input: InputStream
    ) {
        SMBClient(SoCSmbConfig).use { smbClient ->
            smbClient.connect(host).use { connection ->
                val auth = AuthenticationContext(username, password.toCharArray(), domain)
                connection.authenticate(auth).use { session ->
                    (session.connectShare(printer) as PrinterShare).use { share ->
                        share.print(input)
                    }
                }
            }
        }
    }
}
