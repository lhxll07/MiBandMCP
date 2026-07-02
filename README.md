# MiBandMCP

MiBandMCP is a small Android app that exposes Xiaomi Smart Band data as MCP over LAN.

It does not connect to the band directly. Gadgetbridge handles the band connection and export. MiBandMCP reads the exported SQLite file on the phone and serves the latest data to MCP clients.

## What It Has

- native Android app
- local MCP server
- simple Material 3 UI
- single-band support
- Gadgetbridge-based data source

## MCP

Endpoint:

```text
http://<phone-ip>:8787/mcp
```

Tools:

- `band_get_info`
- `band_refresh_now`

Resources:

- `miband://snapshot`
- `miband://status`
- `miband://device`
- `miband://activity/today`
- `miband://daily-metrics/latest`
- `miband://heart-rate/latest`
- `miband://battery/latest`
- `miband://stress/latest`
- `miband://sleep/latest`

## Flow

```text
Band -> Gadgetbridge -> exported SQLite -> MiBandMCP -> MCP over LAN
```

## Quick Start

1. Pair your band in Gadgetbridge.
2. Export the Gadgetbridge database.
3. Open MiBandMCP and select the exported SQLite file.
4. Start the MCP service.
5. Connect from an MCP client on the same LAN.

## Build

```powershell
.\gradlew.bat assembleDebug
```

## Limits

- Gadgetbridge is required
- single device only
- LAN only
- no direct BLE support
- near-real-time, not streaming
