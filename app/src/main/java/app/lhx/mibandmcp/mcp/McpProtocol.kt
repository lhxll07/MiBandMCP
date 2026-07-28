package app.lhx.mibandmcp.mcp

import app.lhx.mibandmcp.BuildConfig
import app.lhx.mibandmcp.model.AppSnapshot
import app.lhx.mibandmcp.model.BandStatus
import app.lhx.mibandmcp.model.ServiceStatus
import app.lhx.mibandmcp.model.SyncStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

internal class McpProtocol(
    private val snapshotProvider: () -> AppSnapshot,
    private val requestRefresh: () -> Unit,
) {
    fun handle(request: JsonRpcRequest): JsonRpcResponse? {
        val response = when {
            request.jsonrpc != JsonRpcVersion -> error(request.id, -32600, "Invalid JSON-RPC version")
            request.method == "initialize" -> initialize(request.id)
            request.method == "ping" -> result(request.id, buildJsonObject {})
            request.method == "tools/list" -> result(request.id) {
                put("tools", JsonArray(toolDefinitions))
            }
            request.method == "tools/call" -> callTool(request)
            request.method == "resources/list" -> result(request.id) {
                put("resources", JsonArray(resourceDefinitions))
            }
            request.method == "resources/read" -> readResource(request)
            request.method.startsWith("notifications/") -> null
            else -> error(request.id, -32601, "Method not found: ${request.method}")
        }
        return if (request.id == null) null else response
    }

    private fun initialize(id: JsonElement?): JsonRpcResponse = result(id) {
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
    }

    private fun callTool(request: JsonRpcRequest): JsonRpcResponse {
        val name = (request.params as? JsonObject)?.get("name")
            ?.let { it as? JsonPrimitive }
            ?.content
            ?: return error(request.id, -32602, "Missing tool name")
        if (name != ToolRefreshNow) {
            return toolResult(request.id, "Unknown tool: $name", isError = true)
        }

        requestRefresh()
        val payload = buildJsonObject {
            put("accepted", JsonPrimitive(true))
            put(
                "syncStatus",
                McpJson.encodeToJsonElement(SyncStatus.serializer(), snapshotProvider().syncStatus),
            )
        }
        return toolResult(request.id, payload.toString(), isError = false)
    }

    private fun readResource(request: JsonRpcRequest): JsonRpcResponse {
        val uri = (request.params as? JsonObject)?.get("uri")
            ?.let { it as? JsonPrimitive }
            ?.content
            ?: return error(request.id, -32602, "Missing resource uri")
        val payload = snapshotProvider().resourcePayload(uri)
            ?: return error(request.id, -32602, "Unknown resource: $uri")
        return result(request.id) {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("uri", JsonPrimitive(uri))
                    put("mimeType", JsonPrimitive(JsonMimeType))
                    put("text", JsonPrimitive(payload.toString()))
                })
            })
        }
    }
}

private fun AppSnapshot.resourcePayload(uri: String): JsonElement? {
    val snapshot = McpJson.encodeToJsonElement(AppSnapshot.serializer(), this).jsonObject
    return when (uri) {
        ResourceSnapshot -> snapshot
        ResourceStatus -> buildJsonObject {
            put("serviceStatus", McpJson.encodeToJsonElement(ServiceStatus.serializer(), serviceStatus))
            put("bandStatus", McpJson.encodeToJsonElement(BandStatus.serializer(), bandStatus))
            put("syncStatus", McpJson.encodeToJsonElement(SyncStatus.serializer(), syncStatus))
        }
        ResourceDevice -> snapshot["deviceProfile"]
        ResourceActivityToday -> snapshot["activitySummary"]
        ResourceDailyMetrics -> snapshot["dailyMetrics"]
        ResourceHeartRateLatest -> snapshot["heartRateSample"]
        ResourceBatteryLatest -> snapshot["batteryStatus"]
        ResourceStressLatest -> snapshot["stressSample"]
        ResourceSleepLatest -> snapshot["sleepSummary"]
        else -> null
    }
}

private fun toolResult(id: JsonElement?, text: String, isError: Boolean): JsonRpcResponse = result(id) {
    put("isError", JsonPrimitive(isError))
    put("content", buildJsonArray {
        add(buildJsonObject {
            put("type", JsonPrimitive("text"))
            put("text", JsonPrimitive(text))
        })
    })
}

private fun result(id: JsonElement?, content: JsonObject): JsonRpcResponse =
    JsonRpcResponse(id = id, result = content)

private inline fun result(
    id: JsonElement?,
    content: JsonObjectBuilder.() -> Unit,
): JsonRpcResponse = result(id, buildJsonObject(content))

private fun error(id: JsonElement?, code: Int, message: String): JsonRpcResponse =
    JsonRpcResponse(id = id, error = JsonRpcError(code, message))

@Serializable
internal data class JsonRpcRequest(
    val jsonrpc: String = JsonRpcVersion,
    val id: JsonElement? = null,
    val method: String,
    val params: JsonElement? = null,
)

@Serializable
internal data class JsonRpcResponse(
    val jsonrpc: String = JsonRpcVersion,
    val id: JsonElement? = null,
    val result: JsonObject? = null,
    val error: JsonRpcError? = null,
)

@Serializable
internal data class JsonRpcError(
    val code: Int,
    val message: String,
)

private data class Resource(
    val uri: String,
    val name: String,
    val description: String,
) {
    fun definition(): JsonObject = buildJsonObject {
        put("uri", JsonPrimitive(uri))
        put("name", JsonPrimitive(name))
        put("description", JsonPrimitive(description))
        put("mimeType", JsonPrimitive(JsonMimeType))
    }
}

internal val McpJson = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
}

private val toolDefinitions = listOf(
    buildJsonObject {
        put("name", JsonPrimitive(ToolRefreshNow))
        put("description", JsonPrimitive("Request a fresh Gadgetbridge sync and export."))
        put("inputSchema", buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {})
            put("additionalProperties", JsonPrimitive(false))
        })
    },
)

private val resources = listOf(
    Resource(ResourceSnapshot, "Full Snapshot", "Complete current band snapshot."),
    Resource(ResourceStatus, "Status", "Service, data source, and sync status."),
    Resource(ResourceDevice, "Device", "Band identity and firmware details."),
    Resource(ResourceActivityToday, "Today's Activity", "Today's step count."),
    Resource(ResourceDailyMetrics, "Daily Metrics", "Latest Xiaomi daily summary."),
    Resource(ResourceHeartRateLatest, "Heart Rate", "Most recent heart rate sample."),
    Resource(ResourceBatteryLatest, "Battery", "Most recent battery level."),
    Resource(ResourceStressLatest, "Stress", "Most recent non-zero stress sample."),
    Resource(ResourceSleepLatest, "Sleep", "Latest sleep summary."),
)
private val resourceDefinitions = resources.map(Resource::definition)

private const val JsonRpcVersion = "2.0"
private const val JsonMimeType = "application/json"
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
