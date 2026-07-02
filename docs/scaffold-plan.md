# MiBandMCP V1 Scaffold Plan

## 1. Project Identity

- App name: `MiBandMCP`
- Package name: `app.lhx.mibandmcp`
- Min SDK: `26`
- UI languages: Chinese and English

## 2. Project Shape

V1 should start as a single Android app module.

Top-level structure:

```text
MiBandMCP/
  app/
  docs/
  gradle/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
```

App source structure:

```text
app/src/main/java/app/lhx/mibandmcp/
  data/
    gb/
    prefs/
    snapshot/
  mcp/
  model/
  service/
  ui/
    home/
    settings/
    theme/
  util/
```

## 3. V1 Dependencies

### Required

- AndroidX Core KTX
- Lifecycle Runtime KTX
- Activity Compose
- Compose BOM
- Compose UI
- Compose UI Graphics
- Compose UI Tooling Preview
- Material 3
- Lifecycle ViewModel Compose
- DataStore Preferences
- Kotlin Coroutines Android
- Ktor server core
- Ktor server CIO
- Ktor serialization kotlinx-json
- Ktor content negotiation
- Kotlinx Serialization JSON

### Debug / Tooling

- Compose UI Tooling
- Compose UI Test Manifest

### Test

- JUnit
- AndroidX Test Ext JUnit
- Espresso Core
- Compose UI Test JUnit4

## 4. Explicitly Deferred Dependencies

Do not add in V1 scaffold:

- Hilt
- Koin
- Navigation Compose
- Room
- WorkManager
- Retrofit
- Coil
- Accompanist unless a concrete need appears

## 5. Android Manifest Baseline

### Permissions

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.FOREGROUND_SERVICE`

Consider adding later only if required by implementation:

- `android.permission.POST_NOTIFICATIONS`

### Components

- `MainActivity`
- `McpForegroundService`
- `GadgetbridgeBroadcastReceiver`

### App Configuration

- `supportsRtl="true"`
- backup left enabled unless later rejected
- clear app label and icon placeholders

## 6. Resource Baseline

Create:

- app name string
- bilingual strings set
- theme colors
- light and dark theme support
- launcher icon placeholder

Language resources:

- `values/strings.xml`
- `values-zh-rCN/strings.xml`

## 7. UI Scaffold

### MainActivity

Responsibilities:

- host Compose app
- provide top-level app state
- switch between Home and Settings without heavy navigation framework

Implementation direction:

- one top-level `AppScreen` enum
- simple state-based screen switch

### Home Screen

Initial sections:

- app top bar
- service status card
- data source card
- health summary card
- action row

### Settings Screen

Initial sections:

- port setting
- export/data access setting
- auto refresh info
- language/about block

## 8. Service Scaffold

### McpForegroundService

Responsibilities:

- manage embedded HTTP server lifecycle
- expose current bind address and port
- publish foreground notification
- bridge refresh requests into the app data layer

V1 should expose:

- start
- stop
- is running
- current endpoint summary

### Notification

Show at minimum:

- service running state
- current host/port if known
- quick action to stop service

## 9. Data Layer Scaffold

### Settings Store

Store:

- server port
- last granted export URI or equivalent access handle
- auto refresh preference

### Snapshot Cache

Store:

- latest service status snapshot
- latest band data snapshot
- last sync metadata

Implementation:

- in-memory source of truth
- optional file persistence for last successful snapshot

### Gadgetbridge Bridge

Responsibilities:

- send sync/export intents
- receive integration broadcasts
- provide simple events to app code

### Export Reader

Responsibilities:

- open user-approved exported database file
- read required tables/values
- map raw rows into app-facing models

## 10. MCP Scaffold

V1 MCP implementation should include:

- server bootstrap
- tool routing
- resource routing
- JSON serialization
- basic health/status endpoint for internal debugging if helpful

Initial handlers:

- `band_get_status`
- `band_sync_now`
- `band_get_steps_today`
- `band_get_latest_heart_rate`
- `band_get_sleep_summary`

## 11. Domain Models To Create First

- `ServiceStatus`
- `EndpointInfo`
- `BandStatus`
- `ActivitySummary`
- `HeartRateSample`
- `SleepSummary`
- `SyncStatus`
- `AppSnapshot`

## 12. Suggested Build Order

1. Gradle and Android project bootstrap
2. Theme and two-screen Compose shell
3. DataStore and basic settings UI
4. Foreground service lifecycle
5. Ktor server bootstrap
6. Snapshot models and fake data path
7. Gadgetbridge bridge
8. Export reader
9. Real MCP handlers
10. Polish and error states

## 13. First Deliverable

The first code deliverable should compile and provide:

- app launch
- themed home/settings shell
- start/stop service button
- foreground notification
- local endpoint display
- fake snapshot values

This gives a visible product skeleton before real Gadgetbridge integration.
