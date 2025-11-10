package com.example.nussocprint.util

import com.hierynomus.mssmb2.SMB2Dialect
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.transport.RPCTransport
import com.rapid7.client.dcerpc.transport.SMBTransportFactories

class SmbjUtils {
}

fun listShares(host: String, domain: String, username: String, password: String): List<String> {
    val client = SMBClient(
        SmbConfig.builder()
            .withDialects(SMB2Dialect.SMB_3_1_1) // force SMB3.1.1 (soc only accepts this apparently)
            .withEncryptData(true)
            .build()
    )
    client.use { smbClient ->
        println("Connecting to $host")
        val connection: Connection = smbClient.connect(host);
        val auth = AuthenticationContext(username, password.toCharArray(), domain);
        val session: Session = connection.authenticate(auth);

        val transport: RPCTransport = SMBTransportFactories.SRVSVC.getTransport(session);
        val serverService: ServerService = ServerService(transport);

        return serverService.shares1
            .filter({ shareInfo -> shareInfo.type == 1 && shareInfo.netName != "Lexmark Universal v2"})
            .map({ shareInfo -> shareInfo.netName });
    }

}