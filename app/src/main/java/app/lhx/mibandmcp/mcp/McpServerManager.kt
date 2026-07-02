package app.lhx.mibandmcp.mcp

import app.lhx.mibandmcp.BuildConfig
import app.lhx.mibandmcp.data.snapshot.SnapshotRepository
import app.lhx.mibandmcp.model.AppSnapshot
import app.lhx.mibandmcp.model.BandStatus
import app.lhx.mibandmcp.model.EndpointInfo
import app.lhx.mibandmcp.model.ServiceStatus
import app.lhx.mibandmcp.model.SyncStatus
import app.lhx.mibandmcp.util.LanAddressResolver
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

class McpServerManager(
    private val snapshotRepository: SnapshotRepository,
    private val requestRefresh: () -> Unit,
) {
    private var server: EmbeddedServer<*, *>? = null

    fun isRunning(): Boolean = server != null

    fun start(port: Int): EndpointInfo {
        stop()
        val lanAddress = LanAddressResolver.findBestIpv4Address() ?: "127.0.0.1"
        server = embeddedServer(
            factory = CIO,
            host = "0.0.0.0",
            port = port,
            module = { appModule(snapshotProvider = { snapshotRepository.snapshot.value }, requestRefresh = requestRefresh) },
        ).start(wait = false)
        return EndpointInfo(
            host = lanAddress,
            port = port,
            url = "http://$lanAddress:$port/mcp",
        )
    }

    fun stop() {
        server?.stop(500, 1_000)
        server = null
    }
}

private fun Application.appModule(
    snapshotProvider: () -> AppSnapshot,
    requestRefresh: () -> Unit,
) {
    install(ContentNegotiation) {
        json(json)
    }

    routing {
        get("/health") {
            call.respond(
                HealthResponse(
                    ok = true,
                    service = "MiBandMCP",
                    mcpEndpoint = "/mcp",
                ),
            )
        }
        get("/debug/snapshot") {
            call.respond(snapshotProvider())
        }
        get("/mcp") {
            call.respondText(
                text = "MiBandMCP MCP endpoint. Send JSON-RPC POST requests to this path.",
                contentType = ContentType.Text.Plain,
            )
        }
        post("/mcp") {
            val request = call.receive<JsonRpcRequest>()
            call.response.headers.append(McpProtocolVersionHeader, SupportedProtocolVersion)
            val response = handleRpcRequest(
                request = request,
                snapshotProvider = snapshotProvider,
                requestRefresh = requestRefresh,
            )
            if (response == null) {
                call.respond(
                    HttpStatusCode.Accepted,
                    JsonRpcResponse(
                        result = buildJsonObject {},
                    ),
                )
            } else {
                call.respond(response)
            }
        }
        delete("/mcp") {
            call.response.headers.append(McpProtocolVersionHeader, SupportedProtocolVersion)
            call.respond(HttpStatusCode.NoContent)
        }
        get("/") {
            call.respondText(
                text = "MiBandMCP service is running. MCP endpoint: /mcp",
                contentType = ContentType.Text.Plain,
            )
        }
    }
}

private fun handleRpcRequest(
    request: JsonRpcRequest,
    snapshotProvider: () -> AppSnapshot,
    requestRefresh: () -> Unit,
): JsonRpcResponse? {
    return when (request.method) {
        "initialize" -> jsonRpcResult(
            id = request.id,
            result = buildJsonObject {
                put("protocolVersion", JsonPrimitive(SupportedProtocolVersion))
                put("capabilities", buildJsonObject {
                    put("tools", buildJsonObject {
                        put("listChanged", JsonPrimitive(false))
                    })
                    put("resources", buildJsonObject {
                        put("listChanged", JsonPrimitive(false))
                        put("subscribe", JsonPrimitive(false))
                    })
                })
                put("serverInfo", buildJsonObject {
                    put("name", JsonPrimitive("MiBandMCP"))
                    put("version", JsonPrimitive(BuildConfig.VERSION_NAME))
                })
            },
        )

        "notifications/initialized" -> null
        "ping" -> jsonRpcResult(id = request.id, result = buildJsonObject {})
        "tools/list" -> jsonRpcResult(
            id = request.id,
            result = buildJsonObject {
                put("tools", JsonArray(toolDefinitions))
            },
        )

        "tools/call" -> handleToolCall(
            request = request,
            snapshotProvider = snapshotProvider,
            requestRefresh = requestRefresh,
        )

        "resources/list" -> jsonRpcResult(
            id = request.id,
            result = buildJsonObject {
                put("resources", JsonArray(resourceDefinitions))
            },
        )

        "resources/read" -> handleResourceRead(request = request, snapshotProvider = snapshotProvider)
        else -> jsonRpcError(
            id = request.id,
            code = -32601,
            message = "Method not found: ${request.method}",
        )
    }
}

private fun handleToolCall(
    request: JsonRpcRequest,
    snapshotProvider: () -> AppSnapshot,
    requestRefresh: () -> Unit,
): JsonRpcResponse {
    val params = request.params?.jsonObject ?: emptyJsonObject()
    val toolName = params["name"]?.let { it as? JsonPrimitive }?.content
    val snapshot = snapshotProvider()
    val snapshotJson = snapshot.toJsonObject()
    val content = when (toolName) {
        ToolGetInfo -> encodeTextContent(snapshotJson)
        ToolRefreshNow -> {
            requestRefresh()
            encodeTextContent(
                buildJsonObject {
                    put("accepted", JsonPrimitive(true))
                    put("message", JsonPrimitive("Refresh requested"))
                    put("syncStatus", json.encodeToJsonElement(SyncStatus.serializer(), snapshot.syncStatus))
                },
            )
        }
        null -> return jsonRpcError(request.id, -32602, "Missing tool name")
        else -> {
            return jsonRpcResult(
                id = request.id,
                result = buildJsonObject {
                    put("isError", JsonPrimitive(true))
                    put("content", JsonArray(listOf(textContent("Unknown tool: $toolName"))))
                },
            )
        }
    }

    return jsonRpcResult(
        id = request.id,
        result = buildJsonObject {
            put("isError", JsonPrimitive(false))
            put("content", content)
        },
    )
}

private fun handleResourceRead(
    request: JsonRpcRequest,
    snapshotProvider: () -> AppSnapshot,
): JsonRpcResponse {
    val params = request.params?.jsonObject ?: emptyJsonObject()
    val uri = params["uri"]?.let { it as? JsonPrimitive }?.content
        ?: return jsonRpcError(request.id, -32602, "Missing resource uri")
    val snapshot = snapshotProvider()
    val resourcePayload = snapshot.resourcePayload(uri)
        ?: return jsonRpcError(request.id, -32602, "Unknown resource: $uri")

    return jsonRpcResult(
        id = request.id,
        result = buildJsonObject {
            put("contents", JsonArray(listOf(
                buildJsonObject {
                    put("uri", JsonPrimitive(uri))
                    put("mimeType", JsonPrimitive("application/json"))
                    put("text", JsonPrimitive(resourcePayload.toString()))
                },
            )))
        },
    )
}

private fun AppSnapshot.resourcePayload(uri: String): JsonElement? {
    val snapshotJson = toJsonObject()
    return when (uri) {
        ResourceSnapshot -> snapshotJson
        ResourceStatus -> statusPayload(this)
        ResourceDevice -> snapshotJson["deviceProfile"]
        ResourceActivityToday -> snapshotJson["activitySummary"]
        ResourceDailyMetrics -> snapshotJson["dailyMetrics"]
        ResourceHeartRateLatest -> snapshotJson["heartRateSample"]
        ResourceBatteryLatest -> snapshotJson["batteryStatus"]
        ResourceStressLatest -> snapshotJson["stressSample"]
        ResourceSleepLatest -> snapshotJson["sleepSummary"]
        else -> null
    }
}

private fun AppSnapshot.toJsonObject(): JsonObject {
    return json.encodeToJsonElement(AppSnapshot.serializer(), this).jsonObject
}

private fun statusPayload(snapshot: AppSnapshot): JsonElement {
    return buildJsonObject {
        put("serviceStatus", json.encodeToJsonElement(ServiceStatus.serializer(), snapshot.serviceStatus))
        put("bandStatus", json.encodeToJsonElement(BandStatus.serializer(), snapshot.bandStatus))
        put("syncStatus", json.encodeToJsonElement(SyncStatus.serializer(), snapshot.syncStatus))
    }
}

private fun toolDefinition(
    name: String,
    description: String,
): JsonObject {
    return buildJsonObject {
        put("name", JsonPrimitive(name))
        put("description", JsonPrimitive(description))
        put("inputSchema", buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {})
            put("required", buildJsonArray {})
            put("additionalProperties", JsonPrimitive(false))
        })
    }
}

private fun resourceDefinition(
    uri: String,
    name: String,
    description: String,
): JsonObject {
    return buildJsonObject {
        put("uri", JsonPrimitive(uri))
        put("name", JsonPrimitive(name))
        put("description", JsonPrimitive(description))
        put("mimeType", JsonPrimitive("application/json"))
    }
}

private fun encodeTextContent(payload: JsonElement): JsonArray {
    return buildJsonArray {
        add(
            buildJsonObject {
                put("type", JsonPrimitive("text"))
                put("text", JsonPrimitive(payload.toString()))
            },
        )
    }
}

private fun textContent(text: String): JsonObject {
    return buildJsonObject {
        put("type", JsonPrimitive("text"))
        put("text", JsonPrimitive(text))
    }
}

private fun jsonRpcResult(
    id: JsonElement?,
    result: JsonObject,
): JsonRpcResponse {
    return JsonRpcResponse(
        id = id,
        result = result,
    )
}

private fun jsonRpcError(
    id: JsonElement?,
    code: Int,
    message: String,
): JsonRpcResponse {
    return JsonRpcResponse(
        id = id,
        error = JsonRpcError(
            code = code,
            message = message,
        ),
    )
}

private fun emptyJsonObject(): JsonObject = JsonObject(emptyMap())

@Serializable
private data class HealthResponse(
    val ok: Boolean,
    val service: String,
    val mcpEndpoint: String,
)

@Serializable
private data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val method: String,
    val params: JsonElement? = null,
)

@Serializable
private data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val result: JsonObject? = null,
    val error: JsonRpcError? = null,
)

@Serializable
private data class JsonRpcError(
    val code: Int,
    val message: String,
)

private val json = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
}

private val toolDefinitions = listOf(
    toolDefinition(
        name = ToolGetInfo,
        description = "Return the full current Mi Band snapshot, including status, device profile, activity, battery, heart rate, stress, sleep, and daily metrics.",
    ),
    toolDefinition(
        name = ToolRefreshNow,
        description = "Request a fresh Gadgetbridge sync/export cycle immediately.",
    ),
)

private val resourceDefinitions = listOf(
    resourceDefinition(
        uri = ResourceSnapshot,
        name = "Full Snapshot",
        description = "Complete current Mi Band snapshot.",
    ),
    resourceDefinition(
        uri = ResourceStatus,
        name = "Band Status",
        description = "Current service, connection, and sync status.",
    ),
    resourceDefinition(
        uri = ResourceDevice,
        name = "Device Profile",
        description = "Band identity and firmware details.",
    ),
    resourceDefinition(
        uri = ResourceActivityToday,
        name = "Today's Activity",
        description = "Today's step count summary.",
    ),
    resourceDefinition(
        uri = ResourceDailyMetrics,
        name = "Latest Daily Metrics",
        description = "Latest Xiaomi daily summary metrics.",
    ),
    resourceDefinition(
        uri = ResourceHeartRateLatest,
        name = "Latest Heart Rate",
        description = "Most recent heart rate sample.",
    ),
    resourceDefinition(
        uri = ResourceBatteryLatest,
        name = "Latest Battery",
        description = "Most recent battery level sample.",
    ),
    resourceDefinition(
        uri = ResourceStressLatest,
        name = "Latest Stress",
        description = "Most recent non-zero stress sample.",
    ),
    resourceDefinition(
        uri = ResourceSleepLatest,
        name = "Latest Sleep",
        description = "Latest sleep summary.",
    ),
)

private const val ToolGetInfo = "band_get_info"
private const val ToolRefreshNow = "band_refresh_now"
private const val ResourceSnapshot = "miband://snapshot"
private const val ResourceStatus = "miband://status"
private const val ResourceDevice = "miband://device"
private const val ResourceActivityToday = "miband://activity/today"
private const val ResourceDailyMetrics = "miband://daily-metrics/latest"
private const val ResourceHeartRateLatest = "miband://heart-rate/latest"
private const val ResourceBatteryLatest = "miband://battery/latest"
private const val ResourceStressLatest = "miband://stress/latest"
private const val ResourceSleepLatest = "miband://sleep/latest"
private const val SupportedProtocolVersion = "2025-06-18"
private const val McpProtocolVersionHeader = "MCP-Protocol-Version"
