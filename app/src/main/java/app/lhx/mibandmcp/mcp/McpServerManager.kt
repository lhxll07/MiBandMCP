package app.lhx.mibandmcp.mcp

import app.lhx.mibandmcp.BuildConfig
import app.lhx.mibandmcp.data.snapshot.SnapshotRepository
import app.lhx.mibandmcp.model.EndpointInfo
import app.lhx.mibandmcp.util.LanAddressResolver
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

class McpServerManager(
    snapshotRepository: SnapshotRepository,
    requestRefresh: () -> Unit,
) {
    private val protocol = McpProtocol(
        snapshotProvider = { snapshotRepository.snapshot.value },
        requestRefresh = requestRefresh,
    )
    private var server: EmbeddedServer<*, *>? = null

    fun start(port: Int): EndpointInfo {
        stop()
        val address = LanAddressResolver.findBestIpv4Address() ?: "127.0.0.1"
        server = embeddedServer(
            factory = CIO,
            host = "0.0.0.0",
            port = port,
            module = { mcpModule(protocol) },
        ).start(wait = false)
        return EndpointInfo(
            host = address,
            port = port,
            url = "http://$address:$port/mcp",
        )
    }

    fun stop() {
        server?.stop(500, 1_000)
        server = null
    }
}

private fun Application.mcpModule(protocol: McpProtocol) {
    install(ContentNegotiation) {
        json(McpJson)
    }

    routing {
        get("/health") {
            call.respond(
                HealthResponse(
                    service = "MiBandMCP",
                    version = BuildConfig.VERSION_NAME,
                    mcpEndpoint = "/mcp",
                ),
            )
        }
        post("/mcp") {
            call.response.headers.append(McpProtocolVersionHeader, SupportedProtocolVersion)
            val response = protocol.handle(call.receive<JsonRpcRequest>())
            if (response == null) {
                call.respond(HttpStatusCode.Accepted)
            } else {
                call.respond(response)
            }
        }
    }
}

@Serializable
private data class HealthResponse(
    val service: String,
    val version: String,
    val mcpEndpoint: String,
)

internal const val SupportedProtocolVersion = "2025-06-18"
private const val McpProtocolVersionHeader = "MCP-Protocol-Version"
