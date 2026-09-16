# NexusAI 开发任务主线

> 开发原则：按业务依赖和可验证闭环排序，不按中间件清单堆技术。每个任务依次经过：原项目阅读、原理与八股、方案说明、作者实现、代码审核、自动化验证、改进台账更新。

## 状态说明

- `TODO`：尚未开始
- `DOING`：正在实现或修正
- `VERIFY`：等待审核或验证
- `DONE`：通过代码与测试验证

## 0. 工程基线

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| FND-01 | Maven 父工程、Wrapper、Common 空模块与依赖隔离 | 聚合、继承、dependencyManagement、依赖传递 | DONE |
| FND-02 | 创建最小可运行 AuthService | Starter、自动配置、组件扫描、可执行 JAR、Actuator | DOING |
| FND-03 | Docker Compose 启动 MySQL | 容器、网络、Volume、健康检查、环境变量 | TODO |
| FND-04 | AuthService 接入 Flyway | 数据库迁移、基线、不可变迁移、回滚思路 | TODO |
| FND-05 | Testcontainers 数据库测试基线 | 测试金字塔、真实数据库集成测试、容器生命周期 | TODO |
| FND-06 | 分环境配置与密钥约束 | Profile、配置优先级、环境变量、Secret 管理 | TODO |
| FND-07 | 统一日志、Trace ID 和错误响应 | Filter、MDC、异常边界、错误码 | TODO |

## 1. 账号注册

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| ACC-01 | 设计并迁移 account 表 | 主键、唯一索引、时间、状态、逻辑删除取舍 | TODO |
| ACC-02 | 实现 Account 持久化适配器 | 领域对象与持久化对象、Repository、MyBatis/JPA 取舍 | TODO |
| ACC-03 | 实现注册用例 | 应用服务、事务边界、DTO 校验、错误语义 | TODO |
| ACC-04 | 使用安全密码哈希 | bcrypt/Argon2、盐、成本参数、密码升级 | TODO |
| ACC-05 | 注册接口与集成测试 | HTTP 语义、参数校验、重复邮箱、并发注册 | TODO |

## 2. 登录、Token 与会话

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| AUTH-01 | 账号密码登录 | 认证与授权、统一失败信息、防账号枚举 | TODO |
| AUTH-02 | Access Token | JWT 结构、签名、过期、Claims、对称/非对称密钥 | TODO |
| AUTH-03 | 登录会话与 Refresh Token | Stateful/Stateless、Token 哈希、设备会话 | TODO |
| AUTH-04 | Refresh Token 轮换 | Rotation、Reuse Detection、Token Family | TODO |
| AUTH-05 | 退出、撤销和多设备策略 | 撤销、过期、并发刷新、设备管理 | TODO |
| AUTH-06 | Redis 登录状态与验证码 | TTL、Key 规范、缓存与权威数据、限流 | TODO |
| AUTH-07 | 认证安全测试 | 重放、暴力破解、越权、并发刷新 | TODO |
| AUTH-08 | 邮件验证码与可替换通知通道 | SMTP、异步发送、模板、频控、短信适配器 | TODO |

## 3. API Gateway 与可信身份

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| GW-01 | 创建响应式 Gateway | WebFlux、Reactor、路由、Filter 链 | TODO |
| GW-02 | 配置 AuthService 路由 | 路径匹配、StripPrefix、负载均衡边界 | TODO |
| GW-03 | Gateway Token 验证 | JWT 验证、白名单、失败响应 | TODO |
| GW-04 | 下游可信身份模型 | SecurityContext、Header 伪造、服务间认证 | TODO |
| GW-05 | 越权与绕过测试 | IDOR、伪造 Header、直连下游服务 | TODO |

## 4. 用户资料、好友与群组

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| SOC-01 | 用户资料模型和接口 | 账号与资料分离、字段权限、乐观锁 | TODO |
| SOC-02 | 好友申请状态机 | 状态转换、领域不变量、重复申请 | TODO |
| SOC-03 | 同意/拒绝好友申请 | 事务、唯一索引、并发请求 | TODO |
| SOC-04 | 好友列表、删除与拉黑 | 双向关系、一致性、软删除与审计 | TODO |
| SOC-05 | 群组与成员模型 | 聚合、角色、成员状态、权限矩阵 | TODO |
| SOC-06 | 建群、邀请、踢人和退群 | 事务边界、批处理、部分成功语义 | TODO |
| SOC-07 | 社交领域并发与权限测试 | 所有者校验、重复操作、竞态条件 | TODO |
| SOC-08 | 好友申请过期调度 | 过期状态、扫描任务、ShedLock、失败恢复 | TODO |

## 5. 会话与消息核心（先使用 HTTP）

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| MSG-01 | 会话与成员数据模型 | 私聊唯一性、群聊、角色、数据所有权 | TODO |
| MSG-02 | 消息表和会话序号 | 会话内 seq、索引、时间与顺序 | TODO |
| MSG-03 | HTTP 发送文本消息 | 成员权限、事务、服务端身份 | TODO |
| MSG-04 | clientMessageId 幂等 | 唯一约束、重复请求、幂等响应 | TODO |
| MSG-05 | 历史消息游标分页 | Cursor/Offset、稳定排序、索引命中 | TODO |
| MSG-06 | 回复、撤回与消息状态 | 状态机、权限和时间窗口 | TODO |
| MSG-07 | 消息并发与故障测试 | 顺序、重复、回滚、热点会话 | TODO |

## 6. Outbox 与 Kafka

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| EVT-01 | Outbox 表与同事务写入 | 本地事务、双写问题、最终一致性 | TODO |
| EVT-02 | Outbox Publisher | Polling、Claim、并发发布、重试 | TODO |
| EVT-03 | Kafka 基础配置 | Broker、Topic、Partition、Replication、acks | TODO |
| EVT-04 | message.created 事件 | 事件信封、Schema、Version、Partition Key | TODO |
| EVT-05 | 幂等消费者 | 至少一次、去重、Offset、事务边界 | TODO |
| EVT-06 | 重试、DLT 和告警 | 可恢复错误、毒消息、退避 | TODO |
| EVT-07 | 数据库/Kafka 故障注入 | 宕机窗口、重复发布、恢复验证 | TODO |
| EVT-08 | 好友、群组和会话系统通知 | 领域事件、通知模型、在线投递、离线持久化 | TODO |

## 7. Netty WebSocket 实时通信

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| RT-01 | 创建 Realtime Gateway | NIO、Selector、Reactor、Netty EventLoop | TODO |
| RT-02 | WebSocket 握手认证 | HTTP Upgrade、Token、Channel Attribute | TODO |
| RT-03 | 定义版本化协议帧 | Frame、requestId、兼容性、错误响应 | TODO |
| RT-04 | 连接、设备与 Channel 管理 | 多设备、竞态、连接生命周期 | TODO |
| RT-05 | 心跳和空闲连接 | IdleStateHandler、半开连接、超时 | TODO |
| RT-06 | WebSocket 发送消息 | 复用 MessageService，不复制业务规则 | TODO |
| RT-07 | STORED/DELIVERED/READ ACK | 至少一次投递、状态边界、重发 | TODO |
| RT-08 | 背压、限流与慢连接 | WriteBufferWaterMark、EventLoop 阻塞、Rate Limit | TODO |
| RT-09 | 断网、重连和协议测试 | 重复帧、乱序、恶意帧、连接恢复 | TODO |

## 8. 多节点路由、离线同步与缓存

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| DIST-01 | Redis Presence | TTL、心跳、用户/设备/节点映射 | TODO |
| DIST-02 | 节点定向消息投递 | Consumer Group、节点路由、广播与单播 | TODO |
| DIST-03 | 节点退出与连接恢复 | 故障检测、陈旧 Presence、重连 | TODO |
| SYNC-01 | lastDeliveredSeq | 投递水位、重复投递、断线补偿 | TODO |
| SYNC-02 | lastReadSeq 与未读数 | 读游标、多设备合并、已读回执 | TODO |
| SYNC-03 | 增量同步 API | afterSeq、批量拉取、分页上限 | TODO |
| CACHE-01 | Redis 热消息缓存 | Cache-Aside、回源、缓存穿透/击穿/雪崩 | TODO |
| CACHE-02 | 评估 Canal/CDC | Binlog、最终一致性、重建和对账 | TODO |
| DIST-04 | 两节点故障与恢复测试 | Rebalance、节点宕机、消息不重不漏 | TODO |

## 9. 文件与 MinIO

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| FILE-01 | file_object 数据模型 | 对象键、所有权、状态、哈希 | TODO |
| FILE-02 | 预签名上传 | PUT 签名、有效期、Content-Type/Length | TODO |
| FILE-03 | 上传完成校验 | HEAD、幂等、PENDING/READY | TODO |
| FILE-04 | 私有下载与权限 | 临时 URL、引用授权、防盗链 | TODO |
| FILE-05 | 头像和聊天附件 | 文件引用、消息内容模型、缩略图 | TODO |
| FILE-06 | 孤儿对象清理 | 生命周期、定时任务、补偿 | TODO |

## 10. AI Chat

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| AI-01 | 创建 AI Service 和模型适配器 | LangChain4j、Provider 抽象、配置隔离 | TODO |
| AI-02 | 同步与流式对话 | SSE/WebSocket、Reactive Streams、取消 | TODO |
| AI-03 | AI 会话复用消息模型 | 普通消息与 AI 消息、任务状态 | TODO |
| AI-04 | 上下文窗口与摘要 | Token、Memory、截断、摘要漂移 | TODO |
| AI-05 | 异步 AI 任务 | 幂等、重试、超时、模型限流 | TODO |
| AI-06 | Tool 权限、确认与审计 | Prompt Injection、Human-in-the-loop、副作用 | TODO |
| AI-07 | 成本、指标和安全测试 | Token 成本、高基数指标、内容安全 | TODO |
| AI-08 | AI 会话摘要 | 长上下文、摘要触发、异步任务、隐私 | TODO |
| AI-09 | 时间、邮件和 MCP 搜索工具 | Tool Schema、MCP、超时、权限、用户确认 | TODO |

## 11. RAG 知识库

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| RAG-01 | 知识库、文档和摄取任务模型 | 生命周期、版本、ACL、任务状态 | TODO |
| RAG-02 | MinIO 文档上传和异步解析 | PDF/文本解析、失败隔离 | TODO |
| RAG-03 | Chunk 策略和去重 | Chunk Size、Overlap、Hash、Metadata | TODO |
| RAG-04 | Embedding 与 pgvector | 向量维度、距离函数、HNSW/IVFFlat | TODO |
| RAG-05 | 权限过滤与向量召回 | Tenant/ACL Filter、TopK、阈值 | TODO |
| RAG-06 | 混合检索与 Rerank | BM25、向量召回、重排 | TODO |
| RAG-07 | 引用和回答生成 | Grounding、Citation、幻觉评估 | TODO |
| RAG-08 | 增量更新、删除和重建 | 版本切换、补偿、零破坏重建 | TODO |
| RAG-09 | RAG 评测 | Recall、MRR、Faithfulness、测试集 | TODO |

## 12. React Web 前端

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| WEB-01 | React + TypeScript + Vite 工程 | 组件、路由、环境变量、构建 | TODO |
| WEB-02 | 登录注册与 Token 管理 | Access/Refresh Token、刷新并发、退出 | TODO |
| WEB-03 | 用户资料、好友和群组页面 | 状态管理、表单校验、权限反馈 | TODO |
| WEB-04 | 会话列表与历史消息 | 游标分页、虚拟列表、未读数 | TODO |
| WEB-05 | WebSocket 客户端 | 连接状态、心跳、ACK、重连、补拉 | TODO |
| WEB-06 | 文本、回复、撤回和已读交互 | 乐观 UI、失败回滚、消息状态 | TODO |
| WEB-07 | 头像和聊天附件上传 | 预签名 URL、进度、失败重试 | TODO |
| WEB-08 | AI 流式对话与取消 | SSE/WebSocket、增量渲染、Abort | TODO |
| WEB-09 | 知识库文档管理和引用展示 | 上传状态、摄取进度、引用跳转 | TODO |
| WEB-10 | 前端单元、集成和 E2E 测试 | Mock、真实后端、关键用户路径 | TODO |

## 13. 上线与求职交付

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| OPS-01 | Actuator、Metrics 和结构化日志 | RED/USE、低基数标签、日志关联 | TODO |
| OPS-02 | OpenTelemetry 链路追踪 | Trace/Span、异步上下文传播 | TODO |
| OPS-03 | 限流、超时、熔断和降级 | 隔离、雪崩、Resilience4j | TODO |
| OPS-04 | CI 质量门 | 编译、测试、格式、依赖和 Secret 扫描 | TODO |
| OPS-05 | 容器化和部署 | Image、健康检查、优雅关闭、配置注入 | TODO |
| OPS-06 | 备份、恢复和迁移演练 | RPO/RTO、数据库和对象存储 | TODO |
| OPS-07 | 容量压测和故障演练 | 吞吐、延迟、连接数、瓶颈定位 | TODO |
| JOB-01 | 项目 README 与架构图 | 可复现部署、设计说明、限制 | TODO |
| JOB-02 | 简历项目描述 | 可量化事实、改进证据、避免空话 | TODO |
| JOB-03 | 模拟面试与追问复盘 | Java、数据库、中间件、系统设计、AI/RAG | TODO |

## 当前任务

下一任务为 `FND-02：创建最小可运行 AuthService`。其目的不是提前实现登录，而是验证可执行 Spring Boot 服务与纯 Java Library 的模块边界。通过后，按 `FND-03 → FND-04 → FND-05` 建立数据库和测试基线，再进入第一个业务闭环 `ACC-01`。
