# NexusAI 架构改进对照清单

> 本文档是持续维护的学习与工程台账。它记录 InfiniteChat 的真实实现、暴露的问题、NexusAI 的改进方案、验证方式和最终面试表达。  
> 状态说明：`待设计` → `待实现` → `实现中` → `待验证` → `已验证`。

## 1. 项目学习方法

每个模块按照同一流程推进：

1. 阅读 InfiniteChat 对应源码，画出真实调用链。
2. 解释该实现使用的技术、底层原理和常见面试问题。
3. 区分“可以保留的设计”和“需要改进的设计”。
4. 为 NexusAI 定义业务约束、接口、数据模型和验收标准。
5. 由项目作者独立完成主体代码。
6. 进行代码审查、自动化测试、并发测试和故障测试。
7. 将最终实现与 InfiniteChat 对照，记录解决了什么问题。
8. 整理为简历描述、项目介绍和面试追问链。

不是先复制再改名，也不是脱离原项目重新空想；允许“理解与实现同步进行”，但每项改进都必须能回答：

- 原项目具体怎么做？
- 为什么在某些场景会有问题？
- NexusAI 改了什么？
- 改进带来了什么代价？
- 用什么测试证明改进有效？

## 2. 模块与依赖结构

| ID | InfiniteChat 当前实现 | 常见问题 | NexusAI 改进 | 验证方式 | 状态 |
|---|---|---|---|---|---|
| MOD-01 | 父 POM 在 `dependencies` 中统一引入 Web、MySQL、MyBatis、Redis、Nacos、LangChain4j | 所有子模块被强制引入无关依赖，产生自动配置冲突和包体膨胀 | 父 POM 只做聚合、BOM、`dependencyManagement` 和 `pluginManagement`；子模块按需声明依赖 | `dependency:tree`；禁止规则；模块启动测试 | 已验证（父工程无全局业务依赖，Common 依赖树为空） |
| MOD-02 | `Common` 同时包含 Entity、JWT、MyBatis 配置、Web 异常、DTO 和工具 | 公共模块膨胀，所有服务被业务模型和框架耦合 | `nexus-common-kernel` 保持极小；认证、消息、Web、持久化代码回归所属模块 | 依赖方向检查；ArchUnit | 待设计 |
| MOD-03 | Common 与 OfflineDataService 各有一份 `Message` 实体，消息类型注释已出现差异 | 同表多模型漂移，修改时容易遗漏 | 消息持久化实体只属于 MessageService；跨服务使用版本化事件契约 | 契约测试；schema compatibility 测试 | 待设计 |
| MOD-04 | Gateway 同时具有父 POM 继承的 Spring MVC/Tomcat 和主动声明的 WebFlux | Servlet 与 Reactive 技术栈混用，依赖和启动行为不清晰 | Gateway 只使用响应式 Gateway 所需依赖，不访问业务数据库 | 依赖树；启动测试；无 JDBC/MyBatis 依赖 | 待设计 |
| MOD-05 | RealTimeService 显式使用 Netty 4.2，而 Boot BOM 将大量 Netty 模块管理为 4.1 | 同一组件族版本混用，可能出现运行期二进制不兼容 | 通过兼容 BOM 对齐 Netty 全家桶；引入 Maven Enforcer 依赖收敛检查 | `dependency:tree`；Enforcer；连接压测 | 待设计 |

## 3. 服务边界与数据所有权

| ID | InfiniteChat 当前实现 | 常见问题 | NexusAI 改进 | 验证方式 | 状态 |
|---|---|---|---|---|---|
| BND-01 | UserService 同时负责认证、资料、好友、群聊、会话、通知、MinIO 和实时节点选择 | 单服务低内聚，发布、扩容和故障影响范围过大 | 逐步形成 Identity、Social、Message、Realtime、AI 边界；初期按清晰模块实现，按需要物理拆分 | 模块依赖图；变更影响分析 | 待设计 |
| BND-02 | UserService、OfflineDataService、RealTimeService 共享同一 MySQL 数据库 | 物理拆服务但数据不自治，属于分布式单体 | 明确单表单一所有者；其他服务通过 API、事件或查询投影获取数据 | 禁止跨服务表 Mapper；数据库权限测试 | 待设计 |
| BND-03 | UserService 直接通过 `MessageMapper` 读取消息表 | 绕过 MessageService 契约，表结构修改会传染其他服务 | 会话摘要通过 MessageService 查询接口或事件投影获取 | 架构测试；数据库账号权限隔离 | 待设计 |
| BND-04 | OfflineDataService 实际负责全部消息存储、历史和缓存，却命名为“离线数据” | 名称掩盖真实业务所有权，容易误建在线/离线两套模型 | 建立 `nexus-message-service`，统一负责消息正确性和查询 | API 与数据模型审查 | 待设计 |
| BND-05 | RealTimeService 同时管理 Channel、处理消息规则、群发、通知和 AI 编排 | 网络传输层与业务规则耦合 | `nexus-realtime-gateway` 只管理连接和投递；消息规则由 MessageService 负责 | Handler 代码审查；依赖方向检查 | 待设计 |

## 4. 认证与授权

| ID | InfiniteChat 当前实现 | 常见问题 | NexusAI 改进 | 验证方式 | 状态 |
|---|---|---|---|---|---|
| AUTH-01 | Gateway 验证 Token，但下游接口继续信任请求参数中的 `userId` | IDOR/越权：用户 A 可把参数改成用户 B | 从可信 Token/SecurityContext 获得 `actorId`；客户端 userId 只能作为被操作资源，不能代表操作者 | 用户 A/B 越权集成测试 | 待设计 |
| AUTH-02 | WebSocket 认证了 Channel，但消息中的 `senderId` 未与 Channel 身份强绑定 | 已登录用户可以伪造发送者 | 服务端忽略客户端 senderId，使用 Channel 上的认证身份 | 伪造 senderId 协议测试 | 待设计 |
| AUTH-03 | 固定盐加 MD5 保存密码 | 抗暴力破解能力不足 | Argon2id 或 bcrypt；每个密码独立盐；参数可升级 | 密码验证测试；哈希参数审查 | 待设计 |
| AUTH-04 | JWT 密钥和 MinIO 凭据存在源码配置；Token 以 userId 单值存储 | 密钥泄露、无法良好支持多设备和刷新令牌轮换 | Secret 外部注入；设备登录会话；Refresh Token 哈希、轮换、撤销与复用检测 | secret 扫描；多设备与重放测试 | 待设计 |

## 5. 消息可靠性与实时通信

| ID | InfiniteChat 当前实现 | 常见问题 | NexusAI 改进 | 验证方式 | 状态 |
|---|---|---|---|---|---|
| MSG-01 | WebSocket 收到消息后分别发送 `store-topic` 和 `push-topic` | 两次发送无原子性，可能已推未存或已存未推 | 事务内写 `message + outbox_event`，提交后可靠发布 `message.created` | Kafka/数据库故障注入测试 | 待设计 |
| MSG-02 | 没有稳定的客户端消息幂等键 | 网络重试可能产生重复消息 | `(senderId, clientMessageId)` 唯一约束；重复请求返回同一结果 | 同一请求并发提交测试 | 待设计 |
| MSG-03 | 主要依赖时间戳和全局消息 ID 排序 | 同时间消息、跨节点时钟和分页边界不稳定 | 为每个会话分配单调 `seq`，使用 `(conversationId, seq)` 游标 | 高并发序号唯一/单调测试 | 待设计 |
| MSG-04 | 没有明确 STORED、DELIVERED、READ 等 ACK 语义 | 客户端无法判断消息是否可靠保存或已送达 | 定义版本化协议和分层 ACK，明确每个状态的责任边界 | 协议契约测试；断网重发测试 | 待设计 |
| MSG-05 | 使用“上次断线时间”查询离线消息 | 多设备、重复查询、时钟偏差下容易重漏 | 使用 `afterSeq` 增量同步，并保存 `lastDeliveredSeq`、`lastReadSeq` | 多设备、反复重连、重复同步测试 | 待设计 |
| MSG-06 | Kafka 消费组随机把推送事件交给某个 RealTimeService，本机却只认识自己的 Channel | 消费节点不一定是连接节点，多实例时在线消息可能丢失 | Presence 保存 `user/device -> nodeId`；按节点定向投递 | 两节点路由和节点退出测试 | 待设计 |
| MSG-07 | 一个 userId 只映射一个 Channel | 新连接覆盖旧连接，多设备语义不明确 | `user -> device -> connection`；明确共存、互踢和同步规则 | 多设备连接测试 | 待设计 |

## 6. Kafka、Redis 与 Canal

| ID | InfiniteChat 当前实现 | 常见问题 | NexusAI 改进 | 验证方式 | 状态 |
|---|---|---|---|---|---|
| MQ-01 | 消费者自动提交；部分异常被捕获后不再抛出；无统一重试和 DLT | 暂时性故障可能变成永久丢事件 | 至少一次消费；业务幂等；受控重试、退避、DLT 和告警 | 重复消费、毒消息和宕机恢复测试 | 待设计 |
| MQ-02 | 好友申请过期任务先从 Redis ZSET 删除，再异步发送 Kafka | Kafka 发送失败后任务永久丢失 | 数据库保存权威到期状态，扫描和发布过程可重试 | Kafka 不可用故障测试 | 待设计 |
| REDIS-01 | 原始邮箱作验证码 key；所有服务共享 DB 2；离线标记无 TTL | key 污染、隐私暴露、状态永久陈旧、服务相互影响 | 统一环境/服务/领域前缀；敏感标识哈希；明确 TTL、容量和可重建性 | key 规范测试；TTL 测试 | 待设计 |
| CDC-01 | Canal 写 Redis 失败被吞掉，外层仍 ACK binlog 批次 | 缓存更新永久缺失 | 缓存可回源；失败不误 ACK；重试、全量重建和对账 | Redis 故障及数据对账测试 | 待设计 |
| CDC-02 | Canal 被用于支撑近期历史消息，但读路径缺少可靠回源 | CDC 延迟或缺失直接影响用户可见数据 | MySQL 是权威数据；Cache-Aside 起步，CDC 只构建可重建投影 | 关闭 Canal/Redis 的降级测试 | 待设计 |

## 7. 文件、AI 与 RAG

| ID | InfiniteChat 当前实现 | 常见问题 | NexusAI 改进 | 验证方式 | 状态 |
|---|---|---|---|---|---|
| FILE-01 | 客户端文件名直接参与对象键，数据库只保存 URL | 覆盖、无所有权、无大小/MIME/哈希验证、孤儿文件 | 服务端生成对象键；`file_object` 元数据；上传完成校验；私有下载签名和清理 | 越权下载、伪造类型、同名上传测试 | 待设计 |
| AI-01 | 模型可以直接触发邮件和知识写入工具 | 提示词注入可能造成未授权外部副作用 | 模型只提出操作；服务端授权；用户确认；幂等执行和审计 | 恶意提示词与重复确认测试 | 待设计 |
| AI-02 | ThreadLocal 承载 AI 请求上下文并进入响应式流 | 异步线程切换时上下文可能丢失或泄漏 | 显式上下文或 Reactor Context；统一清理策略 | 并发流式请求隔离测试 | 待设计 |
| RAG-01 | AiService 启动时 `dropTableFirst(true)` | 重启或多实例会删除共享向量数据 | 普通启动永不删表；迁移和重建是显式管理操作 | 重启、多实例和数据保留测试 | 待设计 |
| RAG-02 | 启动时全量加载本地目录，没有文档版本、去重、ACL 和任务状态 | 无法增量更新、恢复失败或保证权限 | MinIO 原文件 + document/chunk/job 元数据 + 异步摄取 + ACL 检索 | 重复文档、失败重试、越权检索测试 | 待设计 |

## 8. 工程化与上线保障

| ID | InfiniteChat 当前实现 | 常见问题 | NexusAI 改进 | 验证方式 | 状态 |
|---|---|---|---|---|---|
| ENG-01 | 依赖全量 `schema.sql`，脚本包含 DROP/CREATE | 无迁移历史，升级可能破坏数据 | 从第一天使用 Flyway，迁移只前进并经过回滚评估 | 空库和旧版本升级测试 | 待设计 |
| ENG-02 | Java 测试主要是 `contextLoads()`；前端 E2E 契约与后端实现漂移 | 构建成功不能证明业务正确 | 单元、集成、契约、权限、并发和故障测试分层建设 | CI 质量门 | 待设计 |
| ENG-03 | 监控主要集中在 AI 服务，且部分指标使用用户/会话等高基数标签 | 其他链路不可观测；指标系统可能被高基数拖垮 | 全链路低基数指标、结构化日志、Trace、SLO 和告警 | 压测观察；指标基数检查 | 待设计 |
| ENG-04 | README、数据库表和集成测试描述不一致 | 文档不能代表真实系统 | API、事件和迁移作为可验证契约；文档纳入评审和 CI | 文档/契约一致性检查 | 待设计 |

## 9. 每阶段完成后需要补充的证据

每个条目达到 `已验证` 时，追加以下内容：

- NexusAI 实现文件和关键代码位置。
- 数据库迁移版本。
- 对应自动化测试名称。
- 故障测试或压测结果。
- 与 InfiniteChat 相比增加的复杂度和成本。
- 一分钟项目表达。
- 高频面试问题与追问。

## 10. 当前结论

InfiniteChat 不是单纯的“错误项目”。它完成了大量组件的端到端串联，适合用来理解系统全貌；其问题也具有代表性，例如万能 Common、共享数据库、客户端身份不可信、异步双写、无幂等、时间戳式离线同步、WebSocket 集群路由错位和测试不足。

NexusAI 的价值不是换一个目录重新实现相同代码，而是针对这些可复现的问题建立明确约束，并用代码和测试证明改进有效。最终项目介绍将围绕这些有证据的改进展开，而不是泛泛声称“高并发、高可用”。
