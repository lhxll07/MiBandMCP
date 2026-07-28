# 架构说明

MiBandMCP 保持单应用模块，因为前台服务、ContentResolver 权限和 Compose UI 都属于同一个 Android 应用生命周期。代码边界按职责划分，而不是为分层而分层。

## UNIX 原则

| 原则 | 项目中的约束 |
| --- | --- |
| 做好一件事 | 应用只把 Gadgetbridge 导出的最新快照提供给 MCP 客户端 |
| 小而可组合 | Gadgetbridge、快照、协议、服务和 UI 之间使用明确的模型或函数连接 |
| 文本/结构化数据接口 | MCP 边界使用 JSON-RPC 和 `kotlinx.serialization` |
| 避免隐式行为 | 刷新由用户或 `band_refresh_now` 显式触发 |
| 沉默是金 | 后台空闲时不轮询，不创建无意义任务 |

## 职责边界

- `data/gb`：发送 Gadgetbridge Intent、接收结果并读取导出数据库
- `data/prefs`：只保存端口和导出文件 URI
- `data/snapshot`：维护唯一的内存 `AppSnapshot`
- `mcp/McpProtocol`：纯 JSON-RPC 请求处理，可独立单元测试
- `mcp/McpServerManager`：只管理 Ktor 生命周期和 HTTP 路由
- `service`：保持 MCP 服务前台运行并发布通知
- `ui`：将快照映射为用户可见状态，不读取 SQLite 或处理协议

## 数据流

```text
refresh request
      |
      v
GadgetbridgeBridge -> GadgetbridgeExportReader -> AppSnapshot
                                                   |       |
                                                   v       v
                                                  UI   McpProtocol
```

`AppSnapshot` 是 UI 与 MCP 的共同数据源。导入过程不会维护第二套领域模型，服务状态在应用新快照时被保留。

## 发布接口

HTTP 只暴露两个入口：

- `GET /health`：服务发现和版本检查
- `POST /mcp`：无会话 JSON-RPC MCP 请求

MCP 工具只执行动作，MCP 资源只读取状态。1.0 不提供调试路由、根页面、会话 DELETE 或与资源重复的读取工具。

## 运行约束

- 最低 Android API 26，因此不保留更旧平台分支
- 服务绑定 `0.0.0.0`，展示首选局域网 IPv4 地址
- 无自动轮询和云端依赖
- 局域网接口无认证，只适用于可信网络
