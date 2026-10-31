# NexusAI 开发任务主线

> 开发原则：按业务依赖和可验证闭环排序，不按中间件清单堆技术。每个任务依次经过：原项目阅读、原理与八股、方案说明、作者实现、代码审核、自动化验证、改进台账更新。
>
> 文档同步规则：每个功能提交前必须同步本路线图；任务进入 `VERIFY` 时同步 `docs/architecture-improvement-ledger.md` 的实现证据，并在 `docs/learning-journal.md` 记录实际实现、问题、验证结果和遗留边界。

## 执行策略：业务闭环驱动，逐步演进到最终架构

后续全部开发遵循“先完成小而完整的业务闭环，再由明确的问题推动技术升级”。最终功能目标不删减，已完成的认证设计不推倒重做；缩小的是每阶段范围，不是设计、安全或测试质量。下文编号章节保留为任务清单，**实际执行顺序以本节为准，不机械按章节顺序引入中间件**。

### 每项任务的推进方式

1. 阅读原项目对应实现，说明当前业务链路与限制。
2. 明确本次要解决的问题、验收场景，以及暂时不解决的边界。
3. 解释方案与原理：为什么需要这项技术，不引入时有什么限制，是否有更简单的替代方案。每个阶段先列出基础方案、可深入的机制及其收益/代价，由作者决定是否在当前阶段深入；不默认跳过深入设计，也不未经确认将所有增强列为前置任务。
4. 作者实现，随后集中代码审核、自动化测试和必要的人工验证；安全错误与数据正确性问题不延期。
5. 记录实际问题、修复和验证证据；未完成的能力留在台账，不把规划写成已实现。

### 阶段 A：认证入口闭环

- 收尾已实现注册、登录、Token、会话撤销和限流的关键自动化验证，补齐独立测试配置。
- 推进 GW-01～GW-05：先以固定地址路由 AuthService，跑通“通过 Gateway 注册、登录、携带 Token 查询会话”，覆盖无效 Token、伪造身份、撤销和绕过场景。
- 下游保留认证、Session 状态检查和对象级授权；不因引入网关而直接信任客户端身份 Header。
- 验证码作为独立认证闭环后续完成；Nacos、动态配置、熔断等不作为当前入口闭环的前置条件。

### 阶段 B：最小单节点私聊闭环

按以下依赖推进，不先铺开群组和完整社交功能：

```text
用户 A/B 注册登录
→ 最小用户资料与查找、好友申请和同意（SOC-01～03 的必要范围）
→ 创建私聊会话及成员关系（MSG-01 的私聊范围）
→ HTTP 发送、MySQL 持久化、clientMessageId 幂等、会话 seq 与历史游标
→ 单节点 WebSocket 握手认证、连接管理、心跳与在线投递
→ 基于 afterSeq 的断线重连补拉
→ A 发送、B 收到，B 离线后重连能补齐遗漏消息
```

- MSG-02～05/07 先验证消息权限、顺序、重复请求和事务回滚；回复、撤回等作为后续增强。
- RT-01～06/09 先完成单节点必要能力；提取 SYNC-03 的游标补拉能力提前实现，不等待多节点 Presence、Kafka 或复杂 ACK。
- 消息先持久化、提交后再尽力投递；复用消息应用服务，WebSocket Handler 不复制业务规则。
- 第一版明确为“数据库历史可恢复，实时投递尽力而为”，不宣称可靠实时送达。补拉依赖服务端 seq/客户端同步游标，不依赖离线时间。
- 先用测试客户端或最小交互页验收；完整 React 前端后续按业务闭环逐步补齐，不一次铺开全部页面。

### 阶段 C：用故障推动可靠性升级

1. 复现“消息保存成功，提交后投递前进程崩溃”，先记录单节点版本的投递缺口与补拉恢复边界。
2. 引入 Outbox：在同一事务写入消息和待发布记录，实现可恢复扫描、重试；Outbox 先独立理解，不强制与 Kafka 同时引入。
3. 引入 Kafka：明确异步投递/多消费者解耦需求后，接入事件发布、幂等消费、重试和死信。验证发布后未标记成功导致的重复，以及消费者宕机恢复；不宣称 Kafka 自动解决数据库双写或 exactly-once。
4. 引入分层 ACK、送达水位和已读游标：区分“已存储”“客户端已收到”“用户已读”，验证重试、重复和断线恢复。
5. 引入多设备，再引入 Redis Presence 与多节点定向路由，分别验证设备间状态合并、节点故障和恢复；认证已有多 Session 不等同于消息已支持多设备。

### 阶段 D：围绕完整业务逐步补齐功能

- 社交/消息增强：好友拒绝、删除与拉黑、群组角色和成员权限、申请过期、系统通知、回复与撤回，逐个形成可验证闭环。
- 文件：先完成一次授权上传、完成校验和私有下载，再接头像、聊天附件及孤儿清理。
- AI：先完成一次模型调用，再完成与现有消息/权限模型结合的流式对话；随后逐步加入记忆、取消、异步任务、Tools/MCP。副作用工具引入时同步完成授权、确认、幂等和审计。
- RAG：先完成文档上传、解析、切块、向量检索、带引用回答的单知识库闭环，再增加增量更新、删除、混合检索、Rerank 和评测。ACL 从资源模型和检索入口开始落实，不作为最后补丁。
- React：随已稳定的接口逐步交付登录、私聊、社交、文件、AI 和知识库页面，保留完整前端与 E2E 目标。
- 工程治理：配置安全、迁移、日志和测试随功能建设；服务发现、缓存/CDC、熔断和完整可观测性按实际需求分步引入；最终保留部署、备份恢复、压测与故障演练验收，不以“以后再做”永久跳过。

所有升级均需要回答：“当前哪条业务链路暴露了什么问题？本次机制解决什么？引入了什么代价？如何验证？”不为展示技术栈引入组件，也不因阶段性简化而提前宣称最终能力已完成。

## 状态说明

- `TODO`：尚未开始
- `DOING`：正在实现或修正
- `VERIFY`：等待审核或验证
- `DONE`：通过代码与测试验证

## 0. 工程基线

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| FND-01 | Maven 父工程、Wrapper、Common 空模块与依赖隔离 | 聚合、继承、dependencyManagement、依赖传递 | DONE |
| FND-02 | 创建最小可运行 AuthService | Starter、自动配置、组件扫描、可执行 JAR、Actuator | DONE |
| FND-03 | Docker Compose 启动 MySQL | 容器、网络、Volume、健康检查、环境变量 | DONE |
| FND-04 | AuthService 接入 Flyway | 数据库迁移、基线、不可变迁移、回滚思路 | DONE |
| FND-05 | Testcontainers 数据库测试基线 | 测试金字塔、真实数据库集成测试、容器生命周期 | DONE |
| FND-06 | 分环境配置与密钥约束 | Profile、配置优先级、环境变量、Secret 管理 | TODO |
| FND-07 | 统一日志、Trace ID 和错误响应 | Filter、MDC、异常边界、错误码 | TODO |
| FND-08 | OpenAPI 与 Swagger UI | 接口契约、Schema、错误响应、Bearer 安全定义 | VERIFY |

## 1. 账号注册

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| ACC-01 | 设计并迁移 account 表 | 主键、唯一索引、时间、状态、逻辑删除取舍 | DONE |
| ACC-02 | 实现 Account 持久化适配器 | 领域对象与持久化对象、Repository、MyBatis/JPA 取舍 | DONE |
| ACC-03 | 实现注册用例 | 应用服务、事务边界、DTO 校验、错误语义 | DONE |
| ACC-04 | 使用安全密码哈希 | bcrypt/Argon2、盐、成本参数、密码升级 | DONE |
| ACC-05 | 注册接口与集成测试 | HTTP 语义、参数校验、重复邮箱、并发注册 | VERIFY |

## 2. 登录、Token 与会话

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| AUTH-01 | 账号密码登录 | 认证与授权、统一失败信息、防账号枚举 | VERIFY |
| AUTH-02 | Access Token | JWT 结构、签名、过期、Claims、对称/非对称密钥 | VERIFY |
| AUTH-03 | 登录会话与 Refresh Token | Stateful/Stateless、Token 哈希、设备会话 | VERIFY |
| AUTH-04 | Refresh Token 轮换 | Rotation、Reuse Detection、Token Family | VERIFY |
| AUTH-05 | 退出、撤销和多设备策略 | 撤销、过期、并发刷新、设备管理 | VERIFY |
| AUTH-06 | Redis 登录状态与验证码 | TTL、Key 规范、缓存与权威数据、限流 | DOING（登录限流已实现并经作者人工验证，专项自动化测试及验证码待补） |
| AUTH-07 | 认证安全测试 | 重放、暴力破解、越权、并发刷新 | DOING（退出撤销、跨账号会话授权及直连防护已有真实 HTTP/MySQL 测试；重放、并发刷新与 Redis 行为待补） |
| AUTH-08 | 邮件验证码与可替换通知通道 | SMTP、异步发送、模板、频控、短信适配器 | TODO |

## 3. API Gateway 与可信身份

| ID | 任务 | 关键知识点 | 状态 |
|---|---|---|---|
| GW-01 | 创建响应式 Gateway | WebFlux、Reactor、路由、Filter 链 | VERIFY（启动/健康与依赖边界通过，macOS DNS 原生库告警待处理） |
| GW-02 | 配置 AuthService 路由 | 路径匹配、StripPrefix、负载均衡边界 | VERIFY（作者人工联调通过，模拟下游的路径/请求体/Header/状态透传测试通过） |
| GW-03 | Gateway Token 验证 | JWT 验证、白名单、失败响应 | VERIFY（Reactive Resource Server、JWT/白名单/401/403 自动化测试通过，真实撤销链路联调待补） |
| GW-04 | 下游可信身份模型 | SecurityContext、Header 伪造、服务间认证 | VERIFY（单层代理、Header 清理、默认不信任的 IP 解析已实现并测试；服务间认证与部署隔离留待部署阶段） |
| GW-05 | 越权与绕过测试 | IDOR、伪造 Header、直连下游服务 | VERIFY（伪造头、直连认证、真实 Session 撤销和跨账号撤销测试通过；双服务人工联调记录待补） |

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

GW-03～05 已进入 VERIFY：Gateway 使用 Reactive Resource Server 校验 JWT，并在出站头过滤链清理伪造身份/代理信息，仅写入连接对端的 `X-Nexus-Client-IP`。AuthService 默认不信任任何代理，只有显式配置的精确代理 IP 才可提供一个合法的客户端 IP，用于登录限流；账号身份仍来自 JWT/Session。两端关闭自动 Forwarded 解释，网关关闭默认转发头生成。全仓在移除外部 JWT、Redis、HMAC 和代理变量后 `clean verify` 通过，共 69 个测试（Gateway 28，Auth 41）。新增 Auth 真实 HTTP/Tomcat/MySQL 测试证明撤销后未过期 JWT 被拒绝、A 无法撤销 B 的会话，以及默认直连忽略伪造来源头；这些测试仅替换限流端口，不宣称验证 Redis 算法。尚未形成真实 Gateway→真实 Auth 双进程的本轮完整联调记录，部署隔离/mTLS/多级代理仍是明确后续边界。导读和排错见 `docs/learning-journal.md` 的 GW 章节。

AUTH-06 的登录限流子任务已实现并由作者反馈人工测试通过，子任务处于 `VERIFY`。实现包含 Redis 邮箱/IP 失败计数、Lua 原子更新与 TTL 封禁、成功清理邮箱计数、HMAC Key、HTTP 429 和 `Retry-After`。当前 Lua 使用 `count > maxFailures`，默认第 6 次失败触发封禁；Redis 异常时记录日志并放行。验证码与其他 Redis 登录状态能力尚未完成，AUTH-06 整体保持 `DOING`。本次编译通过，未新增限流专项自动化测试；人工测试的逐项输入输出尚未归档，不据此宣称全部边界场景已验证。

`AUTH-05：退出、撤销和多设备策略` 已进入 `VERIFY`；`AUTH-04` 保持 `VERIFY`，后续仍需用专项自动化测试固化正常轮换、旧 Token 重放、并发刷新、撤销提交和跨 Session 隔离证据。AUTH-05 已完成 `POST /api/auth/logout`、`GET /api/auth/sessions`、`DELETE /api/auth/sessions/{sessionId}`、`DELETE /api/auth/sessions/logout-others` 和 `DELETE /api/auth/sessions/logout-all`。AuthService 现通过 Spring Security Resource Server 验证 Bearer JWT，并从验证后的 `sub/sid` 获取可信账号和当前 Session；受保护请求还会查询 LoginSession 状态，因此 Session 撤销提交后，同一 `sid` 下尚未过期的 Access Token 也会在 AuthService 被拒绝。当前退出继续使用 Refresh Token，以便 Access Token 过期时仍可撤销当前 Family；多设备撤销统一先锁 Session，再撤销对应 `ACTIVE` Refresh Token，批量场景按 Session ID 固定顺序加锁，指定 Session 操作执行账号所有权校验。人工接口场景已经验证，`env -u JWT_ACCESS_TOKEN_SECRET ./mvnw clean verify` 通过，Flyway V1/V2/V3 和现有 9 个测试全部成功；本轮 GW-04/05 已补当前退出、指定 Session 撤销和跨账号隔离的部分自动化证据；其他/全部设备撤销、并发撤销和复用等专项覆盖仍待补，状态保持 `VERIFY` 而非 `DONE`。
