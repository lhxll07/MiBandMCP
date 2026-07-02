package app.lhx.mibandmcp.util

import java.net.Inet4Address
import java.net.NetworkInterface

object LanAddressResolver {
    fun findBestIpv4Address(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        return interfaces
            .asSequence()
            .filter { !it.isLoopback && it.isUp }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }
}
