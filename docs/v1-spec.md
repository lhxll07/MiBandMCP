# MiBandMCP V1 Spec

## 1. Product Definition

MiBandMCP is a native Android app that exposes data from a single Xiaomi Smart Band device as a LAN-accessible MCP server.

The app does not connect to the band directly in V1. `Gadgetbridge` remains the device integration layer. MiBandMCP acts as a lightweight bridge between local band data on the phone and external MCP clients.

Primary principles:

- Native Android
- Lightweight
- Clear for normal users
- Elegant UI with Material Design guidance
- Local-first, no cloud dependency

## 2. Goals

V1 goals:

- Run as a small Android app with a simple UI
- Expose band data over MCP on the local network
- Support one configured band device
- Show and return near-real-time data
- Depend on Gadgetbridge as the data source
- Keep setup understandable for non-developer users

## 3. Non-Goals

V1 explicitly does not include:

- Direct BLE communication with the band
- Support for Mi Fitness as a data source
- Support for multiple devices
- Historical trends, charts, or analytics
- Account system or cloud sync
- Writing commands to the band
- Replacing Gadgetbridge pairing or sync flows

## 4. Target User

The target user is a normal Android user who can install Gadgetbridge, pair a band, and follow a short setup flow, but should not need to understand BLE internals or Android developer tooling.

## 5. Core User Stories

1. As a user, I can see whether the MCP service is running and reachable on my LAN.
2. As a user, I can see whether Gadgetbridge and the band data source are configured correctly.
3. As a user, I can manually trigger a refresh when I want the latest data.
4. As a user, I can quickly view steps, latest heart rate, sleep summary, and last sync time in the app.
5. As an MCP client, I can request the latest available band data from the phone.

## 6. Functional Requirements

### 6.1 Device Scope

- Support exactly one band device in V1
- The selected device is configured through Gadgetbridge
- The app does not manage pairing directly

### 6.2 Data Source

- Gadgetbridge is required
- The app reads data through Gadgetbridge integration points
- Preferred flow:
  - trigger sync through Gadgetbridge intent
  - trigger or consume export/update output
  - read the latest exported data
  - update in-app cache

### 6.3 Data Shown In App

The home screen must show:

- MCP service status
- Local network address and port
- Gadgetbridge integration status
- Band data availability status
- Today step count
- Latest heart rate
- Sleep summary for last night
- Last successful sync time

### 6.4 User Actions

The app must support:

- Start MCP service
- Stop MCP service
- Trigger manual sync/refresh
- Open settings

### 6.5 MCP Surface

#### Tools

- `band_get_status`
- `band_sync_now`
- `band_get_steps_today`
- `band_get_latest_heart_rate`
- `band_get_sleep_summary`

#### Resources

- `band://status`
- `band://activity/today`
- `band://heart-rate/latest`
- `band://sleep/last-night`

### 6.6 Realtime Behavior

V1 "real-time" means near-real-time, not continuous streaming.

Requirements:

- When the app is open, it should refresh visible data automatically
- When an MCP request arrives and cached data is stale, the app should trigger a background refresh
- Freshness target for normal operation: within 5 to 15 seconds after a successful sync path
- The app should return the best known cached result if a live refresh is still in progress

### 6.7 Network Access

- The MCP service must be reachable over LAN
- V1 must allow the user to see the reachable address and port
- V1 should default to a sensible port
- V1 should prefer simple access over heavy auth flows, but the risk should be documented for implementation

## 7. UX Requirements

### 7.1 App Structure

Keep the app small and easy to understand.

Recommended V1 navigation:

- Home
- Settings

### 7.2 Home Screen

The home screen should present:

- service state card
- device/data source state card
- health summary card
- primary actions for start/stop and sync

The screen should feel like a system utility, not a fitness dashboard.

### 7.3 Settings Screen

The settings screen should allow:

- configuring host/port behavior if needed
- selecting or confirming Gadgetbridge data access
- enabling or disabling auto refresh behavior
- viewing basic diagnostic information

### 7.4 Visual Design

- Follow Material 3 guidance
- Minimal motion
- Clear hierarchy
- Shallow navigation
- No charts in V1
- Prioritize readability over decoration

## 8. Technical Direction

Preferred implementation direction for V1:

- Kotlin
- Jetpack Compose
- Material 3
- Foreground service for MCP serving
- BroadcastReceiver for Gadgetbridge integration
- Minimal local caching
- LAN HTTP transport for MCP

Architecture should remain intentionally small. V1 should avoid heavy abstractions unless required by platform constraints.

## 9. Data Model Expectations

V1 should model at least:

- `ServiceStatus`
- `BandStatus`
- `ActivitySummary`
- `HeartRateSample`
- `SleepSummary`
- `SyncStatus`

These models should be shaped for UI and MCP output, not to mirror Gadgetbridge internals exactly.

## 10. Error Handling

The app must make failure states visible and understandable.

Minimum error states:

- Gadgetbridge not installed
- Gadgetbridge not configured
- No readable data source
- Sync failed
- Export/read failed
- MCP server failed to start
- No LAN address available

The user should see a short, actionable explanation when possible.

## 11. Performance And Quality Constraints

- Fast startup
- Low background overhead
- Small number of screens
- Small dependency surface
- Stable behavior over feature breadth

V1 should optimize for simplicity and reliability over maximum capability.

## 12. Acceptance Criteria

V1 is acceptable when all of the following are true:

1. A user with Gadgetbridge already configured can install the app and complete setup without adb or desktop tooling.
2. The app can start a foreground MCP service and show a reachable LAN address and port.
3. The home screen clearly shows service state, step count, latest heart rate, sleep summary, and last sync time.
4. A manual sync/refresh action works end-to-end under normal conditions.
5. An MCP client on the same LAN can call the defined tools and receive valid responses.
6. If fresh data is not immediately available, the app returns cached data and indicates sync state clearly.
7. Failure states are surfaced in plain language rather than silent failure.

## 13. Open Implementation Questions

These are implementation questions, not product-scope questions:

- Whether Gadgetbridge export polling is sufficient or whether broadcast-driven refresh is required for acceptable latency
- Whether LAN access in V1 needs a lightweight shared secret
- Whether sleep summary parsing is stable enough across Gadgetbridge export formats
- Whether a tiny local database is necessary or whether file-backed/in-memory cache is enough
