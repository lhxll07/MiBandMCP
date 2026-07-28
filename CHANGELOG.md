# Changelog

## 1.0.0 - 2026-07-29

首个稳定版本。

### Added

- 可启动和停止的局域网 MCP 前台服务
- Gadgetbridge 同步、导出和 SQLite 快照读取
- 步数、心率、睡眠、电量、压力与每日指标资源
- Material 3 主页和设置界面
- English、简体中文和应用级语言切换

### Changed

- MCP 工具只保留执行刷新动作的 `band_refresh_now`
- Ktor 服务生命周期与 MCP 协议处理分离
- 导入、UI 和 MCP 统一使用一个 `AppSnapshot` 数据源
- Release 构建启用代码和资源压缩

### Removed

- 未实际执行任何行为的自动刷新设置
- 与 `miband://snapshot` 重复的 `band_get_info` 工具
- `/debug/snapshot`、根文本页面和无状态 DELETE 路由
- API 26 以下兼容分支、占位测试和脚手架规划文档
