# MiBandMCP V1 Tech Stack

## 1. Decision Summary

MiBandMCP V1 will use a small native Android stack centered on Kotlin, Compose, and a foreground LAN MCP service.

The stack is optimized for:

- native Android UX
- small codebase
- low dependency count
- clear UI for normal users
- simple integration with Gadgetbridge

## 2. Final Stack

### Platform

- Android app
- Single app module
- Single-activity architecture
- Minimum supported Android version: API 28
- Target the current stable Android SDK at implementation time

### Language

- Kotlin

### UI

- Jetpack Compose
- Material 3
- No XML layouts
- No Navigation Compose in V1

Reason:

- Compose + Material 3 is the cleanest current native path
- avoiding XML and Fragment stacks keeps the app smaller
- avoiding Navigation Compose reduces dependency and structural overhead for a two-screen app

### State And Lifecycle

- AndroidX ViewModel
- Kotlin Coroutines
- Kotlin Flow / StateFlow

Reason:

- enough structure for UI state without introducing a heavy architecture framework

### Persistence

- DataStore for settings
- In-memory cache for latest parsed band data
- Optional small snapshot file cache in app storage
- No Room in V1

Reason:

- settings are small and fit DataStore well
- latest band data can be represented as a compact snapshot model
- Room is unnecessary for V1 because this app is not the system of record

### Gadgetbridge Integration

- BroadcastReceiver for Gadgetbridge intents
- Storage Access Framework for user-approved access to exported data
- SQLiteDatabase read-only access for exported Gadgetbridge SQLite files

Reason:

- this matches the chosen integration path without coupling to Gadgetbridge internals

### MCP Server

- Foreground Service
- Embedded HTTP server
- Ktor server with a small JSON API surface
- `kotlinx.serialization` for payloads

Reason:

- a foreground service matches the user-facing "service is running" model
- Ktor is a pragmatic choice for implementing a small local HTTP/MCP service with readable Kotlin code
- serialization keeps payload code simple and explicit

### Network And Service Monitoring

- Android network callbacks for LAN address detection
- Notification for service state and quick actions

Reason:

- the user needs to understand whether the service is reachable

## 3. App Structure

V1 should stay within a small package layout:

```text
app/
  data/
  mcp/
  service/
  ui/
  model/
  util/
```

Suggested responsibilities:

- `data/`: Gadgetbridge bridge, settings store, export reader, snapshot cache
- `mcp/`: server bootstrap, tool handlers, resource handlers, response models
- `service/`: foreground service, sync orchestration, notification handling
- `ui/`: Compose screens, theme, state binding
- `model/`: app-facing domain models
- `util/`: small helpers only

## 4. Core Architectural Rules

V1 should follow these rules:

1. Keep business logic out of composables
2. Keep Gadgetbridge-specific logic out of UI code
3. Keep MCP handler code thin and domain-focused
4. Prefer one app-facing snapshot model over exposing raw export schema everywhere
5. Avoid abstractions that do not remove real complexity

## 5. Minimal Dependency Policy

V1 should include only dependencies that directly support:

- Compose UI
- lifecycle/state management
- settings storage
- MCP/HTTP serving
- JSON serialization

Avoid optional ecosystem add-ons unless implementation proves they are necessary.

## 6. Explicitly Rejected For V1

### Architecture

- No Hilt / Dagger
- No Koin
- No multi-module setup
- No Clean Architecture layering beyond what the code naturally needs

Reason:

- these add ceremony without helping a small utility app enough

### UI

- No Fragment-based UI
- No XML layouts
- No charting library
- No animation libraries

Reason:

- these either move away from the chosen native direction or add scope without serving V1 goals

### Data

- No Room
- No SQLDelight
- No local analytics database

Reason:

- V1 only needs the latest useful snapshot and small settings storage

### Background Work

- No WorkManager in V1

Reason:

- V1 prioritizes explicit user-driven sync plus foreground service behavior
- periodic background work adds complexity and policy edge cases

### Networking

- No Retrofit
- No WebSocket transport in V1

Reason:

- the app is serving MCP locally, not acting as a general API client

## 7. Data Flow

The intended V1 data flow is:

1. User starts MCP service
2. App determines LAN address and starts foreground server
3. User or MCP client requests fresh data
4. App triggers Gadgetbridge sync intent
5. App observes completion or export update
6. App reads latest exported SQLite data
7. App parses data into app snapshot models
8. UI and MCP responses use the same latest snapshot

## 8. Caching Strategy

V1 cache policy:

- Keep latest parsed snapshot in memory
- Persist the latest successful snapshot to app storage as a simple file
- On app start, restore last snapshot immediately if present
- Refresh in the background when data is stale

This gives fast startup and simple failure handling without a full local database.

## 9. Security Posture For V1

V1 needs LAN access, but should stay simple.

Initial position:

- bind to LAN-capable interface
- show address and port clearly
- document that the service is intended for trusted local networks
- keep the option open for a lightweight shared secret if needed during implementation

## 10. Quality Targets

V1 implementation should aim for:

- small APK footprint relative to a typical Compose utility app
- low idle CPU usage
- understandable code over maximal generality
- fast path from install to usable MCP endpoint

## 11. Estimated Size

Given the frozen V1 scope, the expected code size is:

- about 1,500 to 2,500 lines of Kotlin for a solid V1
- possibly closer to 3,000 lines after robustness and UI polish

This is still a small app, but no longer a toy, because LAN service, Gadgetbridge integration, and near-real-time refresh all add real complexity.

## 12. Next Step

The next implementation step is to turn this stack into a concrete scaffold:

- package layout
- Gradle dependencies
- app manifest permissions
- screen structure
- service lifecycle
- MCP response schemas
