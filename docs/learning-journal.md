# NexusAI 开发

## 工程初始化

### 1.1 初始化 Maven 多模块工程

项目开始时先确认了本机环境：Java 21、Maven 3.9.9、Git、Docker 和 Docker Compose。Docker 实际由 OrbStack 提供。

目前有三个 Maven 层级：

```text
NexusAI                  父工程
├── nexus-common-kernel  公共内核
└── nexus-auth-service   认证服务
```

父 POM 目前只负责：

- 聚合子模块。
- 继承 Spring Boot Parent。
- 统一 Java 版本。
- 管理第三方依赖版本。

一开始我纠结过 Web、Validation、Actuator 这些依赖是不是也应该放到父 POM。最后没有这样做，因为放在父 POM 的 `dependencies` 中会让所有子模块都继承这些依赖。Common 并不需要 Web 和数据库，所以具体 Starter 应该由需要它的服务自己声明。

我现在对两个标签的理解是：

```text
dependencies
真正引入依赖

dependencyManagement
只管理版本，不会自动引入依赖
```

MyBatis-Plus 不是 Spring Boot BOM 管理的依赖，所以我在父 POM 中导入它自己的 BOM，再由 AuthService 声明实际使用的 Starter。

Common 目前还是空 JAR，构建会提示：

```text
JAR will be empty
```

当前这是正常的。我不想为了消除警告就提前往 Common 塞工具类、Entity 或配置。等后面真的出现稳定且跨模块的类型，再决定是否放进去。

验证命令：

```bash
./mvnw clean verify
./mvnw -pl nexus-common-kernel dependency:tree
```

### 1.2 创建最小 AuthService

AuthService 最开始只加入这些依赖：

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-boot-starter-test`
- `spring-boot-maven-plugin`

这里我搞清楚了 Starter 的作用。Starter 不只是一个类库，它把一组常用依赖组合起来；Spring Boot 再根据 Classpath、配置和已有 Bean 决定启用哪些自动配置。

Actuator 目前只暴露：

```text
health
info
```

我没有直接暴露所有端点，也没有显示完整健康详情。现在用不到更多端点，暴露太多还可能泄露内部信息。

## 数据库基础

### 2.1 用 Compose 管理本地 MySQL

一开始我觉得容器是不是应该部署时再做。后来决定开发阶段就用 Compose 固定 MySQL 版本、端口、账号、健康检查和数据卷。这样换机器或者以后让别人运行项目时，不需要重新手工搭数据库。

目前 Compose 只启动 MySQL 8.4，没有提前把 Redis、Kafka 等全部加进来。我的原则是用到什么再编排什么。

本地 MySQL 的数据目录挂在命名卷上：

```yaml
volumes:
  - mysql-data:/var/lib/mysql
```

所以容器删除或重建后，只要没有删除 `mysql-data`，数据仍然存在。

这里要区分三个东西：

```text
镜像：创建容器的模板
容器：正在运行的 MySQL
Volume：独立于容器的数据
```

密码通过环境变量传入，没有直接写进 `compose.yaml`。当前 `.env` 只用于本地开发，不应该提交真实密码。

常用命令：

```bash
docker compose config
docker compose up -d mysql
docker compose ps
docker volume ls
```

### 2.2 接入 JDBC 和 Flyway

为了让 AuthService 连接数据库，我加入了：

- `spring-boot-starter-jdbc`
- `mysql-connector-j`
- `flyway-core`
- `flyway-mysql`

`spring-boot-starter-jdbc` 提供 DataSource、JdbcTemplate、事务基础设施和默认的 HikariCP 连接池。MySQL Connector 是真正负责连接 MySQL 的 JDBC 驱动。

应用启动时的大致顺序是：

```text
读取 datasource 配置
→ HikariCP 创建连接池
→ Flyway 检查迁移记录
→ 执行未执行的迁移
→ 应用继续启动
```

#### 2.2.1 第一张表：account

V1 迁移创建了 `account` 表，当前包含：

- 自增主键 `id`
- 唯一邮箱 `email`
- 密码哈希 `password_hash`
- 账号状态 `status`
- 邮箱验证时间
- 创建时间和更新时间

邮箱唯一索引很重要。应用层以后可以先查询邮箱是否存在，用来返回友好的错误；但并发注册时，最终仍要依靠数据库唯一约束保证不会出现两个相同邮箱。

状态字段同时有默认值和 CHECK 约束，目前约定：

```text
1 = ACTIVE
2 = LOCKED
3 = DISABLED
```

#### 2.2.2 小坑：Flyway 校验和不一致

V1 在本地数据库执行后，我修改了 SQL 里的注释标点。再次启动服务时出现：

```text
Migration checksum mismatch for migration version 1
Applied to database : -2048284618
Resolved locally    : -1817018923
```

虽然我改的只是注释，Flyway 仍然认为迁移文件发生了变化。

这次问题让我记住：已经执行过的迁移不能随意修改。以后表结构有变化应该增加 V2，而不是回头改 V1。`flyway repair` 也不能看到报错就直接执行，它会改历史记录，但不一定让真实数据库结构和脚本重新一致。

目前 Testcontainers 使用全新数据库，所以测试能通过；本地 Compose 数据库的校验和问题还没有处理。后面需要根据本地数据是否需要保留，决定恢复原始 V1，还是重建可丢弃的开发数据库。

## 数据库自动化测试

### 3.1 为什么改用 Testcontainers

最开始测试直接依赖本地数据库。这样必须手工建表、准备数据，而且本地已有数据会影响测试结果，所以后面改成 Testcontainers。

### 3.2 配置测试容器

测试配置放在：

```text
nexus-auth-service/src/test/java/
└── io/github/codebyzl/nexus/auth/support/
    └── TestcontainersConfiguration.java
```

主要配置：

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                .withDatabaseName("nexusai_test")
                .withUsername("nexusai")
                .withPassword("test-password");
    }
}
```

### 3.3 使用过程中的理解和小坑

我现在的理解是：

- `@TestConfiguration` 表示它只用于测试，不进入生产代码。
- `@Bean` 让 Spring 管理容器生命周期。
- `@ServiceConnection` 把容器生成的 JDBC 地址、用户名和密码交给 Spring Boot。
- `proxyBeanMethods=false` 是因为这个配置类没有 Bean 方法互相调用，不需要代理。

我之前以为测试还必须写：

```java
@SpringBootTest(properties = "MYSQL_PASSWORD=test-placeholder")
```

后来实际清空所有 MySQL 环境变量再运行测试，仍然可以成功。原因是 `@ServiceConnection` 已经提供了完整的连接信息，所以这个占位配置在当前测试中不需要。

Testcontainers 只是 JVM 中控制 Docker 的 Java 库。MySQL 并不运行在 JVM 里，而是运行在 OrbStack 的 Docker 容器中：

```text
JUnit JVM
→ Testcontainers 调用 Docker API
→ OrbStack 创建 MySQL 容器
```

当前测试容器没有 Volume，也没有复用。测试结束后 JVM 退出，Ryuk 会清理容器，里面的数据也一起消失。这正是测试需要的隔离效果。

### 3.4 当前验证结果

目前有两个数据库测试：

1. `NexusAuthApplicationTests`：确认 Flyway 执行后 `account` 表存在。
2. `AccountMapperTest`：插入账号、验证主键回填，再根据主键查询并检查字段。

完整验证命令：

```bash
env -u MYSQL_PASSWORD -u MYSQL_PORT -u MYSQL_DATABASE -u MYSQL_USER \
  ./mvnw clean verify
```

当前结果：

```text
Tests run: 2
Failures: 0
Errors: 0
BUILD SUCCESS
```

## Account 持久化

### 4.1 接入 MyBatis-Plus

我选择继续使用 MyBatis-Plus，但不打算让业务代码直接到处使用 `BaseMapper` 和 `QueryWrapper`。目前先把数据库映射跑通，后面再加 Repository 这一层。

#### 4.1.1 小坑：Starter 选错

第一次引入的是：

```xml
mybatis-plus-boot-starter
```

它是 Spring Boot 2 的 Starter，带进来了 `mybatis-spring 2.1.2`，和当前 Spring Boot 3.5、Spring Framework 6 不兼容。启动时出现：

```text
Invalid value type for attribute 'factoryBeanObjectType': java.lang.String
```

后来改成：

```xml
mybatis-plus-spring-boot3-starter
```

问题解决。这个错误说明 Starter 不只是普通依赖，它还包含和 Spring 版本相关的自动配置，不能只看框架名字和版本号。

目前没有分页需求，所以没有因为旧项目用了分页插件就立刻添加 `mybatis-plus-jsqlparser`。

### 4.2 完成 Account 表映射

现在有：

```text
AccountPo
→ AccountMapper
→ account 表
```

`AccountPo` 使用 `@TableName` 和 `@TableId` 对应数据库表。测试已经验证：

- 插入返回1行。
- 自增主键可以回填。
- `password_hash` 能映射到 `passwordHash`。
- 数据库默认生成的创建、更新时间能读回来。

`AccountMapperTest` 使用 `@Transactional`，测试结束后默认回滚，避免测试数据留在同一个测试数据库里。

### 4.3 测试过程中遇到的问题

#### 4.3.1 JUnit 4 和 JUnit 5

第一个问题是临时测试错误导入了 JUnit 4：

```java
import org.junit.Test;
```

当前项目统一使用 JUnit 5：

```java
import org.junit.jupiter.api.Test;
```

IntelliJ 还保留过旧的 `SampleTest` 运行配置，尝试加载 JUnit Vintage。Maven 实际测试是正常的，重新加载 Maven 项目并重新创建 JUnit 5 运行配置后解决。

#### 4.3.2 测试依赖本地 user 表

第二个问题是临时测试依赖本地 `user` 表固定有5条数据。这种测试只能临时验证，不适合保留。正式测试应该自己准备数据，并在独立数据库中运行。

#### 4.3.3 改用 AssertJ

断言现在使用 AssertJ：

```java
assertThat(saved.getEmail()).isEqualTo("mapper-test@example.com");
```

MyBatis-Plus 自己的 `Assert` 更偏向运行时参数检查；AssertJ 才是测试断言库，失败时给出的期望值和实际值也更清楚。

### 4.4 从传统三层到按业务分包

我以前比较熟悉的结构是：

```text
controller
→ IService
→ ServiceImpl
→ Mapper
→ 数据库
```

项目目录通常也是按技术类型拆成 `controller`、`service`、`mapper` 和 `entity`。所以刚开始调整 Account 目录时，我会自然地问：为什么没有 `IAccountService` 和 `AccountServiceImpl`，是不是不继承 MyBatis-Plus 的 `IService` 就少了分页、批量操作这些能力？

现在我理解了，`IService + ServiceImpl` 是 MyBatis-Plus 提供的一套面向数据库对象的 CRUD 便利封装，不是使用 MyBatis-Plus 的必要条件。`BaseMapper` 本身已经提供插入、查询、更新、删除、分页和批量操作。`IService` 主要增加了 `lambdaQuery()`、`saveBatch()`、`getOptById()` 等更方便的写法，并不会自动解决注册、登录、账号状态流转这些业务问题。

NexusAI 现在采用的是：

```text
按业务分包
+ 轻量 DDD 分层
+ 六边形架构的端口/适配器思想
+ 依赖倒置
```

它不是某个必须额外安装的框架，而是一套组织代码和控制依赖方向的方法。先按 `account`、`authentication`、`verification` 这样的业务划分，再在业务内部按职责分层：

```text
account
├── domain
├── application
├── infrastructure
└── interfaces
```

我现在对各层的理解是：

| 层 | 负责什么 |
| --- | --- |
| `domain` | 账号本身的状态、行为和必须遵守的业务规则 |
| `application` | 编排注册、禁用账号等完整用例，并控制事务 |
| `infrastructure` | 使用 MyBatis、MySQL、Redis、BCrypt 等技术实现业务需要的能力 |
| `interfaces` | 把 HTTP、WebSocket、消息队列等外部输入转换成应用层能理解的请求 |

这里最重要的不是多了几个目录，而是依赖方向发生了变化。

传统写法通常是：

```text
Service
→ Mapper
→ MySQL
```

现在是：

```text
AccountRegistrationService
→ AccountRepository 接口
← MyBatisAccountRepository 实现
→ AccountMapper
→ MySQL
```

`AccountRepository` 不是账号所有业务功能的集合，它只表示领域层需要怎样保存和取得 `Account`：

```text
save(Account)
findByEmail(Email)
existsByEmail(Email)
```

注册、登录、刷新 Token 这些是业务用例，应该由 Application Service 编排，而不是写成 Repository 方法。`AccountMapper` 则是物理数据访问接口，只负责对 `AccountPo` 执行 SQL，不放业务逻辑。

运行时虽然还是一路调用到 MyBatis，但编译时业务层只依赖自己定义的接口。Spring 启动后会把 `MyBatisAccountRepository` 注入 `AccountRepository`，这就是我现在对依赖倒置的理解：业务层声明自己需要什么，基础设施层来实现，而不是让领域对象知道 MyBatis。

### 4.5 Entity、PO 和外部接口对象为什么分开

目前 `Account` 和 `AccountPo` 不再承担同一个职责：

```text
Account
领域实体，负责 canLogin、lock、disable 等业务行为

AccountPo
数据库对象，负责 account 表、主键和字段映射
```

数据库的 `status` 是 `TINYINT`，所以 `AccountPo` 使用 `Integer`；领域层使用 `AccountStatus`。一开始我为了方便映射，在 `AccountStatus` 上加了 MyBatis-Plus 的 `@EnumValue`。虽然测试可以通过，但这样会让 `domain` 依赖 MyBatis。后来删除了这个注解，准备由 `AccountPersistenceConverter` 完成：

```text
AccountStatus.ACTIVE ⇄ 1
Account              ⇄ AccountPo
```

这个改动让我理解了“领域层保持纯 Java”不是口号。`Account` 不需要知道表名、SQL 和 MyBatis；`AccountPo` 也不应该直接返回给前端。

HTTP 入口以后放在 `interfaces/rest`。这里的 `rest` 只是表示 HTTP REST 接口，不是新的框架。它主要放：

```text
Controller
Request
Response
ExceptionHandler
```

一次注册请求最终会经过：

```text
RegisterAccountRequest
→ RegisterAccountCommand
→ AccountRegistrationService
→ Account
→ AccountRepository
→ MyBatisAccountRepository
→ AccountMapper / AccountPo
→ MySQL
```

Request 负责 HTTP 参数，Command 表达注册意图，Account 维护业务规则，PO 对应数据库，Response 控制可以返回给客户端的字段。这样类确实会比传统 CRUD 多，但可以避免数据库结构、密码哈希和 MyBatis 类型一路泄漏到 Controller。

### 4.6 为什么项目选择这套结构

这套结构不是所有功能都必须照搬。它的优势主要出现在业务复杂以后：

- 账号状态变化有明确入口，不需要到处写 `setStatus(3)`。
- MyBatis 和数据库字段被限制在 Infrastructure。
- Domain 和 Application 可以脱离 Spring、MySQL 做快速单元测试。
- Account、Authentication、Verification 的代码不会散落在全局技术目录中。
- 后面加入 Redis、Kafka、邮件或新的持久化方式时，业务用例不需要直接依赖这些实现。

代价也很明确：类更多，需要做 Request、Command、Entity、PO、Response 之间的转换，刚开始理解成本更高。所以我的选择不是“所有表都上完整 DDD”，而是：

```text
账号、会话、消息等复杂业务
→ 使用当前分层和领域模型

系统字典、简单配置、只读报表
→ 可以使用更直接的 Service + Mapper
```

对于 NexusAI，后面还有验证码、Token 轮换、多设备会话、好友状态、消息幂等和离线同步，只用一个不断膨胀的 `ServiceImpl` 会越来越难维护。因此核心业务继续采用当前结构，但不为了形式给每个 Service 强制创建接口，也不提前创建没有代码的空层。

#### 面试提问

> NexusAI 采用按业务分包的轻量 DDD 分层架构，并结合六边形架构的端口与适配器思想。接口层处理 HTTP 协议，应用层编排注册、登录等用例，领域层维护账号、会话和消息的业务状态与规则，基础设施层实现 MyBatis、Redis、Kafka 等技术能力。领域层通过 Repository 等端口声明持久化需求，由基础设施适配器实现，避免 MyBatis PO 和 Wrapper 渗透到业务层。

问为什么不用 `IService + ServiceImpl`：

> MyBatis-Plus 的 `IService` 更适合面向 PO 的通用 CRUD。账号、会话和消息存在状态转换、并发约束和安全规则，因此项目保留 `BaseMapper` 的数据库能力，但在业务层前增加领域 Repository 边界。对于简单字典和后台配置类功能，则不会强制使用完整领域模型。

## ACC-03～ACC-05：账号注册闭环

### 目标与业务流程

账号注册已经形成以下调用链：

```text
POST /api/accounts
→ RegisterAccountRequest
→ RegisterAccountCommand
→ RegisterAccountService
→ Account.create
→ PasswordHasher
→ AccountRepository
→ MyBatisAccountRepository
→ account
```

接口成功时返回 `201 Created` 和注册结果 DTO。HTTP 请求对象只负责参数校验，应用层 Command 表达注册意图，领域对象维护账号状态，持久化适配器负责 MyBatis 和数据库约束。

邮箱重复有两层保护：

1. 应用层先执行 `existsByEmail`，用于尽早返回可理解的业务错误。
2. 数据库唯一索引作为最终约束，并把 `DuplicateKeyException` 转换为 `EmailAlreadyRegisteredException`。

第二层不能省略，因为两个并发请求可能同时通过第一层检查。`synchronized(email.intern())` 只能保护单 JVM，不能解决多实例并发注册。

### ACC-04：密码哈希

密码能力通过应用层端口表达：

```text
PasswordHasher
← SpringSecurityPasswordHasher
← DelegatingPasswordEncoder
```

数据库只保存带算法标识的密码哈希，不保存明文密码。当前 Spring Security 默认使用 bcrypt，哈希自带随机盐；`DelegatingPasswordEncoder` 的 `{bcrypt}` 标识也为以后升级算法和兼容旧哈希保留了空间。

这比 InfiniteChat 的固定盐 MD5 更抗离线暴力破解。代价是密码计算更慢，但这正是密码哈希需要的安全属性。

### ACC-05：HTTP 接口与统一错误边界

业务异常由 `BusinessException` 和字符串业务码表达，REST 层通过 `GlobalExceptionHandler` 转换为统一的 `ApiErrorResponse`。成功响应直接返回 DTO，不再给所有接口额外套一层固定响应对象。

注册阶段已经处理：

- 请求参数校验失败。
- 邮箱已注册。
- 数据库唯一键冲突。
- 未预期服务器异常。

当前 `ACC-05` 保持 `VERIFY`，因为 HTTP 参数、重复邮箱和并发注册的专项自动化测试尚未补齐。

## AUTH-01：账号密码登录

### 目标与业务流程

密码登录接口为：

```text
POST /api/auth/login
```

应用服务依次完成：

```text
规范化邮箱
→ 查询账号
→ 检查账号状态
→ PasswordHasher.matches
→ 创建登录会话
→ 返回 Token
```

邮箱不存在、密码错误、账号被锁定或禁用，对客户端统一返回：

```text
HTTP 401
INVALID_CREDENTIALS
```

统一失败语义可以减少通过响应差异枚举已注册账号的风险。当前仍未做固定耗时或虚拟密码哈希，因此更严格的时序枚举防护留到 `AUTH-07`。

## AUTH-02：Access Token

### 实现

JWT 使用 Spring Security OAuth2 JOSE，而不是再混用 JJWT。应用层只依赖：

```text
AccessTokenIssuer
```

基础设施层由 `JwtAccessTokenIssuer` 使用 `NimbusJwtEncoder` 实现。这样登录用例不依赖 Nimbus、HS256 或 Spring Security 的具体 API。

Access Token 当前配置：

```text
算法：HS256
有效期：15 分钟
签发者：nexus-auth-service
```

Claims 包含：

```text
iss         签发者
sub         accountId
sid         登录会话 UUID
iat         签发时间
exp         过期时间
jti         Token 唯一标识
token_type  access
```

没有放入邮箱、昵称、密码哈希或账号状态。这些数据会变化或具有敏感性，不应该复制到短期身份凭据中。

JWT 密钥通过 `JWT_ACCESS_TOKEN_SECRET` 环境变量注入。启动时检查：

- 必须是合法 Base64。
- 解码后至少 32 字节，即 256 bit。

测试使用 `src/test/resources/application.properties` 中明确的非生产密钥，因此 `clean verify` 不依赖开发者本地的 `.env`，正式 JAR 也不会包含测试配置。

## AUTH-03：登录会话与 Refresh Token

### 实现

Access Token 和 Refresh Token 采用不同状态模型：

```text
Access Token
→ 短期 JWT
→ 服务端不保存
→ 适合高频访问验证

Refresh Token
→ 长期高熵随机字符串
→ 服务端保存状态
→ 用于刷新、撤销和重放检测
```

Flyway V2 创建了 `auth_session`。每次成功登录都会新增独立 Session，而不是按 userId 覆盖唯一 Token，因此同一账号可以存在多个登录会话。

Refresh Token 的生成过程是：

```text
SecureRandom 生成 32 字节随机数
→ Base64 URL Safe、无 Padding
→ 返回原文给客户端
→ 对原文计算 SHA-256
→ 数据库只保存 64 位十六进制哈希
```

Refresh Token 已经具有 256 bit 随机熵，攻击者无法通过低成本字典枚举原文，因此数据库索引使用 SHA-256，而不是为低熵密码设计的 bcrypt。

登录事务现在包含：

```text
校验账号密码
→ 生成 Refresh Token
→ 写入 auth_session
→ 签发带 sid 的 Access Token
→ 提交事务
```

Session 保存或 Token 签发发生运行时异常时，数据库事务会回滚，不留下半完成的登录会话。原始 Refresh Token 只在登录响应中返回，不写数据库和日志。

### 实现中遇到的问题

#### 配置属性没有自动注册

增加 `RefreshTokenProperties` 后，仅添加 `@ConfigurationProperties` 还不够。项目最终在启动类使用：

```java
@ConfigurationPropertiesScan
```

统一扫描 `JwtProperties` 和 `RefreshTokenProperties`，避免每组配置都创建一个只含 `@EnableConfigurationProperties` 的空配置类。

#### 领域对象不能直接作为 MyBatis PO

`LoginSessionMapper` 最初容易写成：

```java
BaseMapper<LoginSession>
```

最终改为：

```java
BaseMapper<LoginSessionPo>
```

由 `LoginSessionPersistenceConverter` 负责：

```text
UUID ⇄ CHAR(36)
LoginSessionStatus ⇄ TINYINT
LoginSession ⇄ LoginSessionPo
```

领域层因此不需要 MyBatis 注解和数据库字段类型。

#### 包结构逐渐混乱

安全能力增多后，应用层和基础设施层都按能力增加子包：

```text
security/jwt
security/password
security/token
```

应用层保存端口，基础设施层保存 Spring Security、Nimbus、SecureRandom 等具体实现。功能专属的 Session 领域和持久化代码继续放在 `authentication` 下。

### 验证结果与边界

执行：

```bash
env -u JWT_ACCESS_TOKEN_SECRET ./mvnw clean verify
```

结果：

```text
Flyway V1/V2 执行成功
Tests run: 9
Failures: 0
Errors: 0
BUILD SUCCESS
```

登录接口也已手动走通，并核对：

- Access Token TTL 为 900 秒。
- JWT 的 `sub`、`iss`、`sid`、`jti` 和 `token_type` 正确。
- Refresh Token 解码后为 32 字节。
- Refresh Token 有效期为 7 天。
- `auth_session` 中只保存 SHA-256，且与接口返回原文重新计算的哈希一致。

当前自动化测试主要覆盖账号与数据库基线，尚未直接覆盖登录 HTTP、JWT Claims 和 Session 写入，所以 `ACC-05`、`AUTH-01`、`AUTH-02`、`AUTH-03` 保持 `VERIFY`，不提前标记为 `DONE`。

## AUTH-04：Refresh Token 轮换

### 目标与最终业务流程

目标是让客户端在 Access Token 过期或即将过期时，用 Refresh Token 换取新的 Token 对，并保证一个 Refresh Token 最多完成一次正常轮换。

```text
客户端提交 Refresh Token R1 原文
→ SHA-256 得到 Hash
→ 普通查询 R1，获得 sessionId
→ FOR UPDATE 锁定 LoginSession
→ FOR UPDATE 重新读取并锁定 R1
→ 检查 Session、账号、Token 状态和过期时间
→ 创建后继 Refresh Token R2
→ R1 标记为 USED，并记录 replacedByTokenId=R2.id
→ 独立签发新的 Access Token A2
→ 返回 A2 + R2
```

Access Token A2 不是由 R1 在密码学上推导出来的。R1 只用于证明客户端仍持有某个有效 Session 的长期凭据；服务端通过 R1 的数据库记录得到 `sessionId/accountId`，随后由 JWT 签发器独立生成 A2，由随机数签发器独立生成 R2。

接口为：

```text
POST /api/auth/token/refresh
```

请求只需要 Refresh Token。普通业务请求只携带 Access Token；前端所谓“无感刷新”仍然是前端拦截器在后台自动调用刷新接口，不是服务端定时查询数据库并主动把 Token 推给浏览器。

### 数据模型与迁移

V3 新增 `refresh_token` 历史表，并把 V2 `auth_session.refresh_token_hash` 中的存量数据迁移进去，然后删除 Session 上的单值 Hash 字段。新表保存：

```text
id
session_id              同时作为 Token Family ID
token_hash              SHA-256，唯一索引
status                  ACTIVE / USED / REVOKED
created_at / expires_at
used_at / revoked_at
replaced_by_token_id    后继 Token
```

保存完整历史而不是覆盖当前 Hash，是因为覆盖后只能知道“提交的 Token 不等于当前 Token”，无法区分随机无效输入和曾经有效但已经被使用的旧 Token。保留 `USED` 记录后，旧 Token 再次出现就能触发复用检测。

当前实现增加：

```text
RefreshToken
RefreshTokenStatus
RefreshTokenRepository
RefreshTokenPo
RefreshTokenMapper
RefreshTokenPersistenceConverter
MyBatisRefreshTokenRepository
RefreshTokenHasher
Sha256RefreshTokenHasher
RotateRefreshTokenService
RotateRefreshTokenCommand / Result
RotateRefreshTokenRequest / Response
```

首次登录现在同时写入 `auth_session` 和首个 `refresh_token` 记录，数据库只保存 Hash，原文仍只返回给客户端。

### 开发中遇到的问题与设计取舍

#### 1. 为什么轮换签发器需要 `issue(Instant expiresAt)`

首次登录调用：

```java
refreshTokenIssuer.issue();
```

内部使用“当前时间 + 7 天”。如果每次轮换都重新这样计算，会让新 Token 的过期时间不断向后移动。当前选择 7 天绝对会话期限，因此轮换调用：

```java
refreshTokenIssuer.issue(session.getExpiresAt());
```

例如 10 月 1 日登录、10 月 8 日会话过期，即使 10 月 5 日轮换，新 Token 仍在 10 月 8 日过期。这样 Refresh Token 和 Session 不会出现“Token 显示还有效但 Session 已过期”的不一致。

需要澄清的是，Refresh Token 是不透明随机字符串，过期时间没有编码在原文中，而是保存在数据库和响应元数据里。

#### 2. Access Token 与 Refresh Token 到底是什么关系

Access Token 是 15 分钟 HS256 JWT，Refresh Token 是 256 bit 随机字符串，两者没有密码学派生关系。刷新过程是：

```text
R1 授权一次刷新操作
├── JwtAccessTokenIssuer → A2
└── SecureRandomRefreshTokenIssuer → R2
```

A2 不包含 Refresh Token 原文、Hash 或 Refresh Token ID，只继续携带同一个 `accountId` 和 `sessionId`。到了 Session 绝对过期时间后不能继续刷新；当前 Access Token 仍固定签发 15 分钟，因此最后一个 Access Token 最多可能比 Session 多存活约 15 分钟。若后续要求严格截止，可以把 Access Token 的 `exp` 截断为 `min(now + 15 分钟, session.expiresAt)`。

#### 3. 为什么 Token 和 Session 都要锁

Token 行锁保护单个 Token 的一次性消费：

```text
两个请求同时使用 R1
→ 第一个把 R1 改为 USED 并创建 R2
→ 第二个等待后重新读取，不能再从 R1 创建另一个后继
```

否则可能形成分叉：

```text
     ┌→ R2
R1 ──┤
     └→ R3
```

Session 行锁保护整个 Token Family。正常客户端使用当前 R2 刷新时，攻击者可能同时重放已经 `USED` 的 R1；它们锁的是不同 Token 行，只有共同锁定 Session，才能让刷新、复用检测、退出和撤销在同一个 Family 内串行执行。

#### 4. 为什么锁写在查询方法上

数据库悲观锁通过锁定读取获得：

```sql
SELECT ... FOR UPDATE
```

轮换是“读取状态→判断→修改”的操作。如果先普通查询、等 UPDATE 时才加锁，两个事务可能都先读到 `ACTIVE` 并完成业务判断。`SELECT ... FOR UPDATE` 会读取当前记录并把行锁持有到事务提交或回滚，避免检查与使用之间状态变化。

UPDATE 本身也会加锁，但 Session 在正常刷新路径中不一定会更新；仍然必须保证从检查 Session 有效到创建后继 Token 之间不能被其他事务撤销，因此 Session 需要锁定查询。

#### 5. 为什么先普通查询 Token，再重新加锁查询

刷新请求只有 Token 原文，开始时并不知道 `sessionId`。为了统一全系统锁顺序并降低未来刷新与退出互相死锁的风险，当前采用：

```text
普通 findByHash，只用于定位 sessionId
→ findByIdForUpdate，先锁 Session
→ findByHashForUpdate，重新读取并锁 Token
```

第一次查询结果不能用于最终状态判断；真正判断必须以 Session 锁定后的第二次 Token 查询为准，并再次确认 Token 的 `sessionId` 与已锁 Session 一致。

Repository 中的 `requireTokenHash(tokenHash)` 只是抽取出来的空值/空字符串参数校验，不负责生成 Hash。Hash 由 `RefreshTokenHasher` 计算。

#### 6. 为什么发现重用后抛异常会导致撤销失效

重用分支需要：

```text
Session → REVOKED
当前 ACTIVE Refresh Token → REVOKED
然后向客户端返回 401
```

Spring 默认遇到运行时异常会回滚事务。如果更新完数据库后直接抛 `RefreshTokenReuseDetectedException`，撤销也会被回滚。当前应用服务使用：

```java
@Transactional(
        noRollbackFor =
                RefreshTokenReuseDetectedException.class
)
```

因此重用异常仍然返回 401，但撤销事务可以提交。随机无效 Token 和已使用 Token 对外统一返回：

```text
HTTP 401
INVALID_REFRESH_TOKEN
```

内部使用不同异常类型，只为控制事务和后续审计，不向客户端泄露 Token 是否曾经有效。

#### 7. 正常并发为什么也可能触发严格重用检测

同一个 Access Token 可以被多个普通业务请求并发使用。但当它过期后，如果三个请求各自收到 401 并独立提交同一个 R1，就会产生三个刷新请求。数据库锁保证只有一个请求能正常消费 R1；严格模式下，后续请求读到 `USED` 会撤销整个 Session。

因此当前安全策略要求前端在 `WEB-02` 实现 single-flight：

```text
多个业务请求发现 Access Token 过期
→ 只创建一个共享 refreshPromise
→ 其他请求等待同一个刷新结果
→ 获得 A2 后分别重试原业务请求
```

这不是说普通并发请求不允许，而是同一个一次性 Refresh Token 不能并发成功。多设备使用独立 Session 和独立 Refresh Token，不会锁同一行。严格模式安全边界清晰，但对网络重试、多标签页和丢失刷新响应更敏感；如果以后用户体验需要，可以评估短宽限窗口或 BFF，但当前不提前增加复杂度。

#### 8. 后继 Token 为什么先 INSERT，再更新旧 Token

`refresh_token.replaced_by_token_id` 外键指向同表的后继记录。必须先插入 R2，再把 R1 的 `replaced_by_token_id` 更新为 R2.id；反过来会违反外键约束。两个操作与 Access Token 签发都位于同一应用事务内，后续步骤失败时数据库变更回滚，未返回客户端的随机 Token 原文直接废弃。

### 当前实现状态

已经完成：

```text
V3 refresh_token 迁移和旧数据迁移
Refresh Token 历史领域模型与持久化适配器
首次登录创建首个 Token 记录
指定 Session 绝对过期时间的签发
Session 与 Token 锁定查询
统一 Session→Token 锁顺序
正常轮换和后继链
旧 Token 复用检测
Session 与 ACTIVE Token 撤销
POST /api/auth/token/refresh
```

### 验证

执行：

```bash
env -u JWT_ACCESS_TOKEN_SECRET ./mvnw clean verify
```

结果：

```text
Flyway V1/V2/V3 执行成功
Tests run: 9
Failures: 0
Errors: 0
BUILD SUCCESS
```

这证明当前代码可以编译、打包，V3 可以在全新 MySQL 8.4 容器执行，但现有 9 个测试仍是账号和数据库基线测试，不能证明 AUTH-04 的并发与撤销语义。

### 尚未完成的验证

AUTH-04 暂时进入 `VERIFY`，不标记为 `DONE`。下一步需要补：

1. 正常 R1→R2 轮换，验证 R1=`USED`、R2=`ACTIVE` 和 `replaced_by_token_id`。
2. 连续 R1→R2→R3，验证所有 Token 都继承同一个 Session 绝对过期时间。
3. 再次提交 R1，验证返回 401，并确认 Session 与当前 R3 的撤销没有因异常回滚。
4. 两个请求并发使用 R1，验证最多一个请求进入正常轮换分支。
5. 两个独立 Session 中，一个 Family 被重放时不影响另一个 Session。
6. 整理新增 Java 文件的行首缩进和显式 import，再进行代码审核。

退出、主动撤销和设备会话管理留到 `AUTH-05`；Redis、验证码和登录限流留到 `AUTH-06`；前端预刷新、401 重试、single-flight 和多标签页协调留到 `WEB-02`。

### 第七阶段：API Gateway 和可信身份

- 创建响应式 Gateway，并把请求路由到 AuthService。
- 在 Gateway 验证 Token，处理白名单和统一认证失败响应。
- 由服务端生成可信用户身份，禁止客户端通过参数冒充其他用户。
- 处理伪造身份 Header 和绕过 Gateway 直连服务的问题。
- 增加越权和认证绕过测试。

完成标志：下游服务只信任经过验证的身份，不能通过修改 `userId` 或 Header 越权。

### 第八阶段：用户资料、好友和群组

- 将认证账号和用户资料分开建模。
- 实现资料查询和修改。
- 实现好友申请、同意、拒绝、删除和拉黑。
- 处理重复申请、双方同时操作和唯一约束。
- 实现群组、群成员和角色权限。
- 实现建群、邀请、踢人和退群。
- 增加好友申请过期任务和权限、并发测试。

完成标志：用户关系和群成员操作具备明确状态、权限和并发约束。

### 第九阶段：先用 HTTP 完成会话和消息核心

- 设计私聊、群聊、会话成员和消息表。
- 为每个会话分配单调递增的消息序号 `seq`。
- 先通过 HTTP 完成文本消息发送，不急着接 WebSocket。
- 使用 `clientMessageId` 保证重试发送幂等。
- 使用游标分页查询历史消息。
- 实现回复、撤回和消息状态。
- 测试并发发送、重复请求、事务回滚和热点会话。

完成标志：即使没有实时连接，消息也能被可靠保存、去重并按稳定顺序查询。

### 第十阶段：Outbox 和 Kafka 可靠事件

- 增加 Outbox 表，让消息和待发布事件在同一数据库事务中写入。
- 实现 Outbox Publisher 的扫描、并发抢占、发布和重试。
- 编排 Kafka，设计 Topic、Partition Key 和事件版本。
- 发布 `message.created` 等领域事件。
- 实现至少一次消费下的幂等消费者。
- 增加重试、死信队列和告警。
- 模拟数据库或 Kafka 故障，检查重复发布和恢复过程。

完成标志：数据库或 Kafka 短暂故障不会让已经保存的消息永久丢失事件。

### 第十一阶段：Netty WebSocket 实时通信

- 创建独立 Realtime Gateway。
- 实现 WebSocket 握手认证和 Channel 身份绑定。
- 定义带版本、`requestId` 和错误响应的协议帧。
- 管理用户、设备和 Channel 的连接生命周期。
- 增加心跳、空闲连接检测和断线处理。
- WebSocket 发送消息时复用 MessageService，不复制消息业务逻辑。
- 定义 `STORED`、`DELIVERED`、`READ` 三类 ACK。
- 处理慢连接、背压、限流和恶意协议帧。

完成标志：单节点下可以安全连接、发送消息、接收 ACK，并在断线后恢复。

### 第十二阶段：多节点路由、离线同步和缓存

- 使用 Redis 保存用户、设备和实时节点的 Presence，并设置 TTL。
- 根据连接所在节点定向投递消息，而不是随机让某个节点消费。
- 处理节点宕机、陈旧 Presence 和客户端重连。
- 保存 `lastDeliveredSeq`、`lastReadSeq` 和未读数。
- 提供基于 `afterSeq` 的断线增量同步接口。
- 对热点消息使用 Cache-Aside，并保留数据库回源能力。
- 在真实需求出现后再评估 Canal/CDC，而不是默认依赖它保证正确性。
- 完成两节点故障和恢复测试。

完成标志：多节点和多设备场景下，消息可以定位到正确连接，重连后能够补齐而不依赖客户端时间。

### 第十三阶段：文件与 MinIO

- 设计文件元数据、所有权和 `PENDING/READY` 状态。
- 使用预签名 URL 让客户端直传 MinIO。
- 上传完成后校验大小、类型、哈希和对象是否存在。
- 实现带权限的私有下载。
- 将头像和聊天附件接入文件模型。
- 清理长期未完成和失去引用的孤儿对象。

完成标志：文件上传不经过业务服务中转大流量，并且不能通过对象地址绕过权限下载。

### 第十四阶段：AI Chat

- 创建 AI Service 和模型提供商适配器。
- 实现同步和流式对话，以及取消、超时和限流。
- 让 AI 会话复用已有会话和消息模型。
- 管理上下文窗口、历史摘要和异步生成任务。
- 接入时间、邮件和 MCP 搜索等工具。
- 对有外部副作用的工具增加权限、用户确认、幂等和审计。
- 记录 Token 成本、延迟和失败指标。

完成标志：AI 对话可以流式输出、保存历史，并且工具调用不会绕过服务端授权。

### 第十五阶段：RAG 知识库

- 设计知识库、文档、Chunk 和摄取任务模型。
- 使用 MinIO 保存原始文档，并异步解析 PDF 和文本。
- 实现切块、去重、Embedding 和版本管理。
- 使用 PostgreSQL 和 pgvector 保存向量。
- 检索时先做用户和知识库权限过滤。
- 增加关键词与向量混合检索、Rerank 和引用。
- 支持文档增量更新、删除和索引重建。
- 建立固定测试集，评估召回和回答是否有依据。

完成标志：用户只能检索有权限的文档，回答能够返回引用，并且知识库可以安全更新和重建。

### 第十六阶段：React Web 和完整用户流程

- 创建 React、TypeScript 和 Vite 工程。
- 完成注册、登录、Token 刷新和退出页面。
- 完成资料、好友、群组、会话和历史消息页面。
- 实现 WebSocket 客户端、心跳、ACK、重连和补拉。
- 实现附件上传进度和失败重试。
- 实现 AI 流式渲染、取消和知识库引用展示。
- 增加前端单元测试和关键流程 E2E 测试。

完成标志：可以从浏览器完整走通注册、社交、聊天、文件、AI 和知识库流程。

### 第十七阶段：上线准备和项目交付

- 完善 Actuator、指标、结构化日志和链路追踪。
- 增加限流、超时、熔断、降级和优雅关闭。
- 建立 CI，执行编译、测试、依赖检查和 Secret 扫描。
- 为服务制作镜像并补全部署配置。
- 演练数据库、对象存储备份与恢复。
- 做容量压测和数据库、Kafka、Redis、节点宕机等故障演练。
- 更新 README、架构图、接口文档和部署说明。
- 根据实际测试数据整理简历项目描述和面试复盘。

完成标志：项目能够从空环境部署、观测、恢复并完成核心流程演示，简历中的每项描述都有代码或测试证据。

## 后续记录模板

后面每完成一个小阶段，我用下面这个格式继续追加。内容以实际开发为主，不为了完整而硬凑。

````markdown
## TASK-ID：模块名称

> 实际记录按 `docs/development-roadmap.md` 的任务 ID 组织，例如 `AUTH-04：Refresh Token 轮换`，不再只按宽泛阶段汇总。

### 目标与核心业务流程

简单写清楚目标和开始前不理解的地方。

### 实现

- 改了哪些代码。
- 数据或请求怎么流转。
- 为什么选这个方案。

### 遇到的问题

错误现象：

```text
只保留关键错误
```

原因和处理：

- 最后确认的原因。
- 怎么验证。
- 修改了什么。

### 当前理解

用自己的话解释这一阶段最重要的原理。

### 验证

```bash
实际执行的命令
```

记录测试结果和还没覆盖的情况。

### 下一步

- 还缺什么。
- 接下来做什么。
````
