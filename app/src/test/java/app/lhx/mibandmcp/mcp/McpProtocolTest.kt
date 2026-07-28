package app.lhx.mibandmcp.mcp

import app.lhx.mibandmcp.model.AppSnapshot
import app.lhx.mibandmcp.model.HeartRateSample
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpProtocolTest {
    @Test
    fun `lists only the refresh action`() {
        val response = protocol().handle(request("tools/list"))
        val names = response?.result
            ?.get("tools")
            ?.jsonArray
            ?.map { it.jsonObject.getValue("name").jsonPrimitive.content }

        assertEquals(listOf("band_refresh_now"), names)
    }

    @Test
    fun `reads a focused resource from the snapshot`() {
        val snapshot = AppSnapshot(heartRateSample = HeartRateSample(bpm = 72))
        val response = protocol(snapshot).handle(
            request(
                method = "resources/read",
                params = buildJsonObject {
                    put("uri", JsonPrimitive("miband://heart-rate/latest"))
                },
            ),
        )
        val text = response?.result
            ?.get("contents")
            ?.jsonArray
            ?.single()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.content

        assertTrue(text.orEmpty().contains("\"bpm\":72"))
    }

    @Test
    fun `refresh tool performs one action`() {
        var refreshCount = 0
        val protocol = McpProtocol(snapshotProvider = { AppSnapshot() }) { refreshCount += 1 }

        protocol.handle(
            request(
                method = "tools/call",
                params = buildJsonObject {
                    put("name", JsonPrimitive("band_refresh_now"))
                },
            ),
        )

        assertEquals(1, refreshCount)
    }

    @Test
    fun `notifications do not produce responses`() {
        val response = protocol().handle(
            JsonRpcRequest(method = "notifications/initialized"),
        )

        assertNull(response)
    }

    @Test
    fun `rejects an unsupported JSON RPC version`() {
        val response = protocol().handle(
            request(method = "ping").copy(jsonrpc = "1.0"),
        )

        assertEquals(-32600, response?.error?.code)
    }

    private fun protocol(snapshot: AppSnapshot = AppSnapshot()) =
        McpProtocol(snapshotProvider = { snapshot }, requestRefresh = {})

    private fun request(
        method: String,
        params: JsonElement? = null,
    ) = JsonRpcRequest(
        id = JsonPrimitive(1),
        method = method,
        params = params,
    )
}
