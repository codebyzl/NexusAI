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
| GW-04/GW-05 | Gateway 验证 Token，但下游接口继续信任请求参数中的 `userId` | IDOR/越权：用户 A 可把参数改成用户 B | 从可信 Token/SecurityContext 获得 `actorId`；客户端 userId 只能作为被操作资源，不能代表操作者 | 用户 A/B 越权集成测试 | 待设计 |
| RT-02/RT-06 | WebSocket 认证了 Channel，但消息中的 `senderId` 未与 Channel 身份强绑定 | 已登录用户可以伪造发送者 | 服务端忽略客户端 senderId，使用 Channel 上的认证身份 | 伪造 senderId 协议测试 | 待设计 |
| ACC-04 | 固定盐加 MD5 保存密码 | 抗暴力破解能力不足 | 通过应用层 `PasswordHasher` 端口隔离密码能力，基础设施使用 Spring Security `DelegatingPasswordEncoder`，当前默认生成带算法标识的 bcrypt 哈希并支持后续算法升级 | 注册后数据库无明文密码；密码登录接口手动验证成功；现有构建测试通过 | 已验证（密码哈希与匹配链路已落地） |
| AUTH-02 | JWT 密钥存在源码配置；Access Token 写入 Redis | 密钥泄漏；Access Token 被迫依赖服务端状态；按 userId 保存不适合多设备 | JWT 密钥通过环境变量注入并校验 Base64/256 bit；Access Token 使用包含 `sid` 的 15 分钟无状态 JWT | 无外部密钥执行 `clean verify`；登录接口解析 Claims 与 TTL | 已验证（签发链路已落地，专项自动化测试待补） |
| AUTH-03 | Refresh Token 使用 JWT 原文并按 userId 单值存入 Redis | 原文泄漏后可直接使用；新设备登录覆盖旧设备；无法独立撤销某个会话 | 每次登录创建 UUID Session；Refresh Token 使用 256 bit 随机值，数据库只保存 SHA-256；Access Token 通过 `sid` 关联会话 | 登录接口与 `auth_session` 哈希手动核对；Flyway V2 和现有测试通过 | 已验证（专项自动化测试待补） |
| AUTH-04 | 刷新时直接覆盖 Redis 中的 Refresh Token，没有历史状态和 Token Family | 无法区分普通无效 Token 与旧 Token 重放；并发刷新缺少可靠的一次性消费约束 | 保存 Refresh Token 历史状态，轮换时标记 `USED` 并创建后继 Token；重用旧 Token 时撤销整个 Session；事务内按 Session→Token 顺序使用行锁串行化消费 | 正常轮换、旧 Token 重放、双请求并发刷新和跨 Session 隔离测试 | 待验证（生产链路已实现，专项自动化测试待补） |
| AUTH-05 | 缺少清晰的当前会话退出、指定设备撤销和多设备会话语义 | 只删除客户端 Token 无法阻止 Refresh Token 继续换取新凭据；按 userId 全量覆盖又会误伤其他设备 | 当前退出使用 Refresh Token 定位独立 Session，事务内按 Session→Token 加锁并撤销该 Family；接口幂等返回 204，后续再扩展会话列表和按设备撤销 | 当前 Token 退出、重复退出、USED Token、刷新/退出并发及跨 Session 隔离测试 | 实现中（当前会话退出已实现，多设备管理待补） |

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
| ENG-01 | 依赖全量 `schema.sql`，脚本包含 DROP/CREATE | 无迁移历史，升级可能破坏数据 | 从第一天使用 Flyway，迁移只前进并经过回滚评估 | 空库和旧版本升级测试 | 已验证（V1 account 迁移入库；历史与校验和已记录；重复启动不重复执行） |
| ENG-02 | Java 测试主要是 `contextLoads()`；前端 E2E 契约与后端实现漂移 | 构建成功不能证明业务正确 | 单元、集成、契约、权限、并发和故障测试分层建设 | CI 质量门 | 部分验证（Testcontainers + MySQL 8.4 已建立数据库集成测试基线；Flyway V1/V2/V3 和 9 个测试进入构建验收；注册、登录与 Token 专项自动化测试仍待补充） |
| ENG-03 | 监控主要集中在 AI 服务，且部分指标使用用户/会话等高基数标签 | 其他链路不可观测；指标系统可能被高基数拖垮 | 全链路低基数指标、结构化日志、Trace、SLO 和告警 | 压测观察；指标基数检查 | 待设计 |
| ENG-04 | README、数据库表和集成测试描述不一致 | 文档不能代表真实系统 | 使用 Springdoc OpenAPI 从 Controller、DTO 和校验注解生成当前 HTTP 契约；统一记录成功/错误响应和 Bearer JWT 安全定义，文档纳入评审和 CI | `/v3/api-docs` 路径/Schema 检查；Swagger UI 启动验证；文档/契约一致性检查 | 部分验证（账号与认证接口已生成 OpenAPI，事件契约和 CI 检查待补） |
| ENG-05 | 基础设施需要手工创建 8 个容器，仓库没有 Compose 清单 | 环境难以复现，容器版本、端口、网络和启动条件容易因人而异 | 通过版本化 Compose 分阶段编排基础设施，声明持久卷、健康检查、网络和环境变量契约 | `docker compose config`；容器健康检查；数据持久化重启测试 | 已验证（MySQL 8.4 healthy；业务账号连接成功；容器重建后探针数据保留） |

### FND-08：OpenAPI 与 Swagger UI

实现位置：

- 父 POM 中的 `springdoc-openapi.version` 和依赖管理
- `nexus-auth-service/pom.xml`
- `auth/infrastructure/openapi/OpenApiConfig.java`
- 账号与认证 Controller、Request、Response 和 `ApiErrorResponse`
- `application.yml` 中的 `springdoc` 配置

实现证据：项目使用适配 Spring Boot 3.5 的 Springdoc OpenAPI 2.9.0，通过 Controller 和 Record DTO 自动生成 OpenAPI 3 契约；当前文档覆盖账号注册、密码登录、Token 轮换和当前会话退出，包含参数校验、成功状态码、统一错误响应 Schema，以及为后续受保护接口预定义的 `bearerAuth` JWT 安全方案。OpenAPI 和 Swagger UI 可以分别通过环境变量关闭，生产环境不需要暴露交互式文档。

验证结果：完整 `clean verify` 成功；本地启动后 `/api/v3/api-docs` 和 `/api/swagger-ui.html` 均返回 HTTP 200，生成的路径为 `/accounts`、`/auth/login`、`/auth/token/refresh`、`/auth/logout`，安全定义包含 `bearerAuth`；设置 `OPENAPI_ENABLED=false` 和 `SWAGGER_UI_ENABLED=false` 后两个地址均返回 404。当前仍缺少 CI 中的 OpenAPI 契约漂移检查，因此状态保持 `VERIFY`。

## 9. 每阶段完成后需要补充的证据

每个条目达到 `已验证` 时，追加以下内容：

- NexusAI 实现文件和关键代码位置。
- 数据库迁移版本。
- 对应自动化测试名称。
- 故障测试或压测结果。
- 与 InfiniteChat 相比增加的复杂度和成本。
- 一分钟项目表达。
- 高频面试问题与追问。

## 10. 当前认证阶段证据

### ACC-04：密码安全

实现位置：

- `auth/application/security/password/PasswordHasher.java`
- `auth/infrastructure/security/password/SpringSecurityPasswordHasher.java`
- `auth/infrastructure/security/password/PasswordConfig.java`

改进证据：注册只保存 Spring Security 生成的带算法标识密码哈希；登录通过同一端口匹配密码，应用层不依赖具体哈希算法。与固定盐 MD5 相比增加了计算成本，但显著提高了离线破解成本，并支持后续算法升级。

### AUTH-02：Access Token

实现位置：

- `auth/application/security/jwt/AccessTokenIssuer.java`
- `auth/infrastructure/security/jwt/JwtAccessTokenIssuer.java`
- `auth/infrastructure/security/jwt/JwtConfig.java`
- `auth/infrastructure/security/jwt/JwtProperties.java`

改进证据：Access Token 使用 HS256、15 分钟 TTL，并包含 `iss/sub/sid/iat/exp/jti/token_type`；签名密钥来自环境变量，启动时校验 Base64 和 256 bit 最小长度。代价是当前对称密钥需要由签发方和未来验证方安全共享，后续可以根据服务边界评估 RSA/EC 非对称签名。

### AUTH-03：登录会话与 Refresh Token

实现位置：

- `db/migration/V2__create_auth_session_table.sql`
- `auth/authentication/domain/LoginSession.java`
- `auth/authentication/infrastructure/persistence/MyBatisLoginSessionRepository.java`
- `auth/infrastructure/security/token/SecureRandomRefreshTokenIssuer.java`
- `auth/authentication/application/LoginWithPasswordService.java`

改进证据：每次登录创建独立 UUID Session；Refresh Token 使用 32 字节 `SecureRandom`，数据库只保存 SHA-256；Access Token 通过 `sid` 关联 Session。登录接口已手动验证，数据库哈希与客户端原文重新计算结果一致；Flyway V1/V2 和现有 9 个测试通过。

当前残余风险：还没有自动化覆盖 Token Claims、Session 写入和并发登录。原先由 `auth_session` 保存单个 Refresh Token Hash 的设计已经在 AUTH-04 迁移为独立历史表，旧结构不再保留。

### AUTH-04：Refresh Token 轮换与复用检测

实现位置：

- `db/migration/V3__create_refresh_token_table.sql`
- `auth/authentication/domain/RefreshToken.java`
- `auth/authentication/domain/RefreshTokenRepository.java`
- `auth/authentication/application/RotateRefreshTokenService.java`
- `auth/authentication/infrastructure/persistence/MyBatisRefreshTokenRepository.java`
- `auth/authentication/infrastructure/persistence/RefreshTokenMapper.java`
- `auth/authentication/interfaces/rest/AuthController.java`

实现证据：V3 将 Refresh Token 从 Session 单值字段迁移为完整历史表，记录 `ACTIVE/USED/REVOKED`、`used_at/revoked_at` 和 `replaced_by_token_id`。轮换时先通过普通 Hash 查询定位 Family，再按 Session→Token 的统一顺序执行 `SELECT ... FOR UPDATE`；同一个 Token 最多产生一个后继 Token，不同 Token 上的刷新、重放检测和会话撤销也通过 Session 锁串行化。后继 Refresh Token 继承 Session 的绝对过期时间，不因轮换形成无限滑动续期。

重用检测采用严格模式：已经标记为 `USED` 的 Token 再次出现时，撤销所属 Session 和该 Family 当前所有 `ACTIVE` Token。应用服务使用 `@Transactional(noRollbackFor = RefreshTokenReuseDetectedException.class)`，确保对外返回 401 时撤销记录仍然提交。普通无效 Token 与已使用 Token 对外都返回 `INVALID_REFRESH_TOKEN`，避免泄露 Token 历史状态。

当前验证：`env -u JWT_ACCESS_TOKEN_SECRET ./mvnw clean verify` 成功，Flyway V1/V2/V3 在全新 MySQL 8.4 容器执行成功，现有 9 个测试全部通过。残余验证缺口是正常 R1→R2 轮换、R1 重放后的撤销提交、同一 R1 并发刷新和不同 Session 隔离测试，因此状态为“待验证”而非“已验证”。

代价与边界：数据库需要保存 Token 历史并定期清理；严格复用检测要求客户端实现 single-flight，否则同一 R1 的正常并发刷新也可能撤销会话；Access Token 当前固定 15 分钟，若在 Session 即将过期时签发，最多可能比 Session 多存活约 15 分钟，是否将 Access Token `exp` 截断到 Session 绝对过期时间留待后续安全策略确认。

### AUTH-05：当前会话退出

实现位置：

- `auth/authentication/application/LogoutCommand.java`
- `auth/authentication/application/LogoutService.java`
- `auth/authentication/interfaces/rest/LogoutRequest.java`
- `auth/authentication/interfaces/rest/AuthController.java`

当前实现：`POST /api/auth/logout` 接收当前客户端持有的 Refresh Token。应用服务只用第一次普通 Hash 查询定位 `sessionId`，随后按 AUTH-04 已确定的 Session→Token 顺序执行锁定查询并重新确认关联关系，再把 LoginSession 改为 `REVOKED`，批量撤销该 Family 当前所有 `ACTIVE` Token。`USED` 历史保持不变，便于保留完整轮换和重放证据。

接口选择 Refresh Token 而不是强制 Access Token，是因为 AuthService 当前尚未建立 Bearer Token Filter/SecurityContext，且用户退出时 Access Token 可能已经过期。Refresh Token 本身是只用于认证服务的长期凭据，可以直接定位当前设备 Session；以后查询或撤销其他设备 Session 时，仍必须依赖经过验证的 Access Token 获取可信 `accountId`，不能相信客户端提交的 accountId。

退出采用幂等语义：未知 Token、记录已清理或会话已经撤销时不暴露内部状态，统一返回 `204 No Content`。在严格复用策略下，R1 轮换为 R2 后，R1 是 `USED` 而不是时间过期；再次提交 R1 仍会结束整个有效 Family，这是有意的重放响应。所有 Token 与 Session 当前共享同一绝对过期时间，因此真正超过 `expires_at` 的 Token 所属 Session 也已经自然失效。

边界与代价：撤销 Session 会立即禁止 Refresh Token 继续轮换，但已经签发的无状态 Access Token 不会被数据库更新主动失效，最多继续存活约 15 分钟。若要求即时退出，需要未来由 Gateway 查询 Session/撤销状态或引入短期 denylist，这会增加状态查询和可用性成本。当前会话退出链路已通过完整构建，但尚未增加退出、重复退出、USED Token、刷新/退出并发和多 Session 隔离专项测试，多设备会话列表与指定设备撤销也尚未实现。

### 当前认证阶段一分钟项目表达

> InfiniteChat 把 Access Token 和 Refresh Token 都做成 JWT 原文，并按 userId 存入 Redis，这会让 Access Token 失去无状态优势，也会造成多设备覆盖和 Refresh Token 原文泄漏风险。NexusAI 将两类 Token 分开：Access Token 是 15 分钟无状态 JWT，Refresh Token 是 256 bit 高熵随机凭据；每次登录建立独立 Session，数据库只保存 Refresh Token 的 SHA-256，JWT 通过 `sid` 关联会话。Refresh Token 使用独立历史表形成 Token Family，轮换后旧 Token 标记为 `USED` 并关联后继 Token；旧 Token 再次出现时撤销整个 Session。并发控制采用 Session 与 Token 行锁，轮换后的 Token 继承会话绝对过期时间。当前生产链路和构建已经走通，下一步用正常轮换、重放、并发和多 Session 隔离测试补齐验证证据。

高频追问：

- 为什么密码使用 bcrypt，而高熵 Refresh Token 可以使用 SHA-256？
- 为什么 Access Token 无状态，Refresh Token 有状态？
- 为什么 JWT 里保存 `sid`，但不保存邮箱和账号状态？
- Refresh Token 原文泄漏与数据库 Hash 泄漏分别有什么风险？
- 并发使用同一个 Refresh Token 时如何保证最多一个请求成功？
- HS256 和 RSA/EC 的密钥分发边界有什么不同？

## 11. 当前结论

InfiniteChat 不是单纯的“错误项目”。它完成了大量组件的端到端串联，适合用来理解系统全貌；其问题也具有代表性，例如万能 Common、共享数据库、客户端身份不可信、异步双写、无幂等、时间戳式离线同步、WebSocket 集群路由错位和测试不足。

NexusAI 的价值不是换一个目录重新实现相同代码，而是针对这些可复现的问题建立明确约束，并用代码和测试证明改进有效。最终项目介绍将围绕这些有证据的改进展开，而不是泛泛声称“高并发、高可用”。
