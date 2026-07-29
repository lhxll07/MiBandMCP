# MiBandMCP

MiBandMCP 1.0 是一个本地优先的 Android 工具，将 Gadgetbridge 导出的单个小米手环快照通过局域网 MCP 服务提供给客户端。

它不直接连接手环，不接入云服务，也不保存历史数据库。Gadgetbridge 负责蓝牙通信和数据库导出，MiBandMCP 只负责读取最新快照并提供 MCP 接口。

## 功能

- 启动和停止局域网 MCP 前台服务
- 调用 Gadgetbridge 同步与数据库导出
- 读取步数、心率、睡眠、电量、压力和每日汇总
- 在主页查看服务、数据源和关键健康状态
- 支持 English、简体中文和跟随系统
- 支持 Android 8.0（API 26）及以上版本

## 使用

1. 安装 Gadgetbridge，并完成手环配对。
2. 在 Gadgetbridge 中启用 `设置 -> 开发者选项 -> Intent API`。
3. 在设备开发者设置中启用所需的同步和数据库导出 Intent。
4. 在 MiBandMCP 设置页选择 Gadgetbridge 导出的 SQLite 数据库。
5. 回到主页刷新数据，然后启动 MCP 服务。
6. 使用页面显示的局域网地址连接 MCP 客户端。

默认端点：

```text
http://<phone-ip>:8787/mcp
```

健康检查：

```text
GET http://<phone-ip>:8787/health
```

## MCP 接口

MiBandMCP 使用资源提供标准 MCP 数据接口，同时提供读取工具以兼容尚未支持资源的客户端。

工具：

- `band_get_data`：读取完整快照或指定的 `status`、`device`、`activity`、`daily_metrics`、`heart_rate`、`battery`、`stress`、`sleep` 数据
- `band_refresh_now`：请求一次 Gadgetbridge 同步与数据库导出

资源：

- `miband://snapshot`
- `miband://status`
- `miband://device`
- `miband://activity/today`
- `miband://daily-metrics/latest`
- `miband://heart-rate/latest`
- `miband://battery/latest`
- `miband://stress/latest`
- `miband://sleep/latest`

数据流保持单向：

```text
Band -> Gadgetbridge -> exported SQLite -> AppSnapshot -> UI / MCP
```

## 构建

需要 JDK 17 和 Android SDK 37。

```bash
bash gradlew :app:assembleDebug
bash gradlew :app:assembleRelease
```

完整检查：

```bash
bash gradlew testDebugUnitTest :app:assembleDebug :app:lintDebug
```

## 限制与安全

- 仅支持单个 Gadgetbridge 设备和用户选择的导出文件
- 刷新是显式触发的近实时同步，不是连续数据流
- MCP 服务没有身份验证，只应在可信局域网中运行
- Gadgetbridge 数据表变化可能需要同步更新读取器

实现边界见 [架构说明](docs/architecture.md)，版本变化见 [CHANGELOG](CHANGELOG.md)。
