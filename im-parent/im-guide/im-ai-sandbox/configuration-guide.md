← [返回索引](../README.md)

# 配置指南

本模块支持三种配置方式，从"最小依赖纯 Java"到"Spring Boot 一键装配"：

| 方式 | 适用场景 | 需要 spring-boot-autoconfigure |
| --- | --- | --- |
| 1. 编程式构造 SandboxConfig | 单元测试、非 Spring 应用、命令行工具 | 否 |
| 2. YAML `@ConfigurationProperties` 绑定 + 手动装配 | Spring/Spring Boot 应用、不希望走条件装配 | 否（但建议） |
| 3. Spring Boot 自动装配 | Spring Boot 应用（推荐） | 是（optional 依赖，需显式声明或上游 transitve） |

---

## 1. 编程式（纯 Java）

构造 `SandboxConfig` 并直接传给 `SandboxManager` 或 `SandboxFactory`：

```java
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.ResourceLimits;
import org.wall.im.ai.core.sandbox.SandboxContext;
import org.wall.im.ai.core.sandbox.SandboxType;

SandboxConfig config = new SandboxConfig()
    // ===== 6 个兼容旧字段 =====
    .setWorkDirectory("/tmp/im-work")
    .setMaxExecutionTimeSec(60)
    .setMaxMemoryMb(1024)
    .setAllowedPaths(List.of("/data/shared"))
    .setAllowedCommands(List.of("ls", "cat", "python3"))
    .setEnabled(true)
    // ===== 6 个新增字段 =====
    .setType(SandboxType.LOCAL_DOCKER)
    .setResourceLimits(new ResourceLimits(
        /*cpuCores*/         2.0,
        /*memoryMb*/         2048,
        /*maxExecTimeSec*/   60,
        /*diskMb*/           512,
        /*maxProcesses*/     1024))
    .setCommandPolicy("default")
    .setImage("eclipse-temurin:26-jre")
    .setRemoteEndpoint("https://sb.example.com")
    .setEnvVars(Map.of("PYTHONDONTWRITEBYTECODE", "1",
                       "TZ", "Asia/Shanghai"));
```

12 字段完整语义见 [sandbox-spi.md §6](sandbox-spi.md#6-沙盒配置-sandboxconfig)。

### 旧代码零改动兼容构造（6 参构造器）

```java
// 只传 6 个旧字段，其他新增字段取默认值：type=LOCAL_PROCESS，resourceLimits=null 等
SandboxConfig config = new SandboxConfig(
    "/tmp/work", 60, 512, List.of(), List.of(), true
);
```

---

## 2. YAML 配置（@ConfigurationProperties 绑定）

源码：[SandboxProperties.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/config/SandboxProperties.java)

```yaml
# application.yml
im:
  ai:
    sandbox:
      enabled: true                              # 总开关（ConditionalOnProperty 的开关）
      default-type: LOCAL_DOCKER                 # Registry 路由时 config.type 为空的默认（Spring 层使用）

      # 旧 6 字段的全局默认值
      work-directory: /srv/im-sandbox-work
      max-execution-time-sec: 60
      max-memory-mb: 1024
      allowed-paths: [/var/data, /srv/reports]
      allowed-commands: [ls, cat, head, tail, python3, node]

      # Docker 子属性（LOCAL_DOCKER 类型专用）
      docker:
        image: eclipse-temurin:26-jre-alpine     # 默认镜像
        auto-pull: true                          # image 不存在时自动 pull

      # Remote 子属性（REMOTE_HTTP 类型专用）
      remote:
        endpoint: https://sb.internal.corp:8443  # 远端 base URL（不含 /sandboxes）
        connect-timeout-sec: 10                  # TCP+TLS 握手超时
```

在非 Spring Boot 的 Spring 应用中，手动启用：

```java
@Configuration
@EnableConfigurationProperties(SandboxProperties.class)
public class SandboxConfigConfig { ... }
```

---

## 3. Spring Boot 自动装配（推荐）

源码：[SandboxAutoConfiguration.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/config/SandboxAutoConfiguration.java)

自动装配通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 被 Spring Boot 自动发现：

```
org.wall.im.ai.sandbox.config.SandboxAutoConfiguration
```

类上的注解：

```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "im.ai.sandbox", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SandboxProperties.class)
public class SandboxAutoConfiguration { ... }
```

### 装配的 2 个 Bean

| Bean 名/类型 | scope | 条件覆盖 | 说明 |
| --- | --- | --- | --- |
| `SandboxRegistry` | singleton | `@ConditionalOnMissingBean` | 构造参数 `List<SandboxFactory>` 来自 Spring 容器；再 ServiceLoader 合并默认 3 个；同一 type 按 priority 取大 |
| `DefaultCommandPolicy` | singleton | `@ConditionalOnMissingBean(CommandPolicy.class)` | 无参构造（等价 restrictNetworkCommands=true 的严格策略） |

> **注意**：`SandboxManager` **不会被自动装配成 Bean**——因为它是"每任务"的状态对象，不应共享使用。每次 Agent 发起任务时用代码显式 new：
>
> ```java
> @Service
> public class TaskRunner {
>     private final SandboxRegistry registry;
>     private final SandboxProperties props;
>
>     public TaskRunner(SandboxRegistry r, SandboxProperties p) { this.registry = r; this.props = p; }
>
>     public void runTask(String workDir, SandboxContext ctx) {
>         // 每次任务：构造 config（从 props 取默认值）
>         SandboxConfig config = new SandboxConfig()
>             .setType(props.getDefaultType())           // LOCAL_DOCKER
>             .setWorkDirectory(workDir)
>             .setEnabled(props.isEnabled())
>             .setImage(props.getDocker().getImage())
>             .setResourceLimits(new ResourceLimits(2.0, props.getMaxMemoryMb(),
>                                                  props.getMaxExecutionTimeSec(), null, null));
>         try (var mgr = new SandboxManager(registry, config, ctx)) {
>             ...
>         }
>     }
> }
> ```

### 启用 / 禁用总开关

- YAML 中 `im.ai.sandbox.enabled=false` → **整个自动装配不生效**，Registry/Policy Bean 都不存在（是紧急逃生通道）。
- 运行时若只是想**单次任务禁用沙盒**，把 `SandboxConfig.enabled=false` 即可，不会影响其他任务。

### 下游覆盖 Bean（自定义 Registry 或 Policy）

```java
@Configuration
public class MySandboxOverrides {

    // 覆盖默认策略：default关键字 + 企业合规策略（链组合）
    @Bean
    public CommandPolicy commandPolicy() {
        return new DefaultCommandPolicy(true)
            .add(new EnterpriseAstCompliancePolicy());
    }

    // 覆盖默认 Registry：加入自定义 PodmanFactory，priority=10 覆盖 Docker 官方工厂
    @Bean
    public SandboxRegistry sandboxRegistry(ObjectProvider<SandboxFactory> providers) {
        return new SandboxRegistry(providers.orderedStream().toList());
    }
}
```

> 自定义 factory 只要是 Spring @Bean，就会自动进入 `List<SandboxFactory>` 参数，无需额外声明。

---

## 4. LOCAL_DOCKER / REMOTE_HTTP 的配置优先级

不同字段的优先级（从高到低）：

| 场景 | 优先级 1（最高） | 优先级 2 | 优先级 3（最后回退） |
| --- | --- | --- | --- |
| LOCAL_DOCKER 镜像 | `SandboxConfig.image` 非空 | `SandboxProperties.docker.image` YAML 配置 | 常量 `DEFAULT_DOCKER_IMAGE = openjdk:26-slim` |
| REMOTE_HTTP endpoint | `SandboxConfig.remoteEndpoint` | `SandboxProperties.remote.endpoint` YAML 配置 | 抛 IllegalArgumentException |
| 超时 maxExecutionTimeSec | `resourceLimits.maxExecutionTimeSec` | `config.maxExecutionTimeSec` 旧字段 | 默认 60 秒 |
| 内存 memoryMb | `resourceLimits.memoryMb` | `config.maxMemoryMb` 旧字段 | 默认 512 MB |

---

## 5. 完整 YAML 示例（三种后端并存）

不同任务通过 `config.setType()` 选择不同后端，全局默认在 YAML 中声明：

```yaml
im:
  ai:
    sandbox:
      enabled: true
      default-type: LOCAL_PROCESS        # 本地调试用

      work-directory: /tmp/im-local
      max-execution-time-sec: 30
      max-memory-mb: 512
      allowed-commands: [bash, ls, cat, grep, wc]

      docker:
        image: eclipse-temurin:26-jre
        auto-pull: false                 # 内网CI预拉

      remote:
        endpoint: https://sandbox.k8s.corp:6443
        connect-timeout-sec: 5
```

然后代码按任务切：

```java
// 调试任务：LOCAL_PROCESS（用默认 type）
SandboxConfig debugCfg = new SandboxConfig()
    .setWorkDirectory("/tmp/im-local/debug");

// 生产任务：LOCAL_DOCKER
SandboxConfig prodCfg = new SandboxConfig()
    .setType(SandboxType.LOCAL_DOCKER)
    .setWorkDirectory("/srv/im-jobs/prod");

// 多租户任务：REMOTE_HTTP
SandboxConfig tenantCfg = new SandboxConfig()
    .setType(SandboxType.REMOTE_HTTP)
    .setWorkDirectory("/workspace");
```
