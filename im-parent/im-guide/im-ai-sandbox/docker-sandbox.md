← [返回索引](../README.md)

# DockerLocalSandbox · 本地 Docker 容器沙盒

`DockerLocalSandbox` 通过调用本地 **docker CLI** 启动并销毁容器，把 execute/executeCommand 转发为 `docker exec -i`，提供真正的 CPU / 内存 / 进程数 / 网络 / 文件系统 五重隔离。

源码：[DockerLocalSandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/DockerLocalSandbox.java)
工厂：[DockerLocalSandboxFactory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/DockerLocalSandboxFactory.java)
CLI 抽象：[DockerCommandExecutor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/DockerCommandExecutor.java)
CLI 默认实现：[ProcessDockerCommandExecutor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/ProcessDockerCommandExecutor.java)

---

## 前置条件

| 项目 | 要求 | 验证方法 |
| --- | --- | --- |
| Docker daemon | Docker Desktop（Win/Mac）或 Linux Docker Engine | `docker info` 正常输出 |
| docker CLI | 在 PATH 中 | `docker --version` 返回 ≥ 24.x |
| 默认镜像 | `openjdk:26-slim`（可通过 SandboxConfig.image 覆盖） | `docker image inspect openjdk:26-slim`；若缺失且 `autoPull=true`，initialize 时自动 `docker pull` |

> 推荐在生产环境把所用镜像 **缓存到本地私有 Registry**，避免每次 initialize 走公网拉取。

---

## 常量约定（源码与文档一致）

| 常量 | 值 | 用途 |
| --- | --- | --- |
| `CONTAINER_WORK_DIR` | `/workspace` | 容器内工作目录（宿主 `workDirectory` 的挂载目标） |
| `CONTAINER_TMP_DIR` | `/tmp` | 容器内 tmpfs 挂载点（可选） |
| `DEFAULT_DOCKER_IMAGE` | `openjdk:26-slim` | 镜像默认值 |
| `CONTAINER_NAME_PREFIX` | `im-sandbox-` | 容器名前缀，其后追加 `UUID 短 8 位` |
| `MAX_WAIT_RUNNING_ATTEMPTS` | 20 | `docker inspect --format='{{.State.Running}}'` 轮询次数 |
| `WAIT_RUNNING_INTERVAL_MS` | 500 | 每次轮询间隔 |

---

## 初始化流程（`initialize`）

```
传入 SandboxConfig + SandboxContext
        │
        ▼
1. 确定镜像
   - config.image 非空 → 用它
   - 否则用 Spring SandboxProperties.docker.image
   - 最后回退常量 DEFAULT_DOCKER_IMAGE = openjdk:26-slim
        │
        ▼
2. 镜像就绪
   docker image inspect <image>
     ├─ 存在 → 继续
     └─ 不存在 且 (autoPull=true 或属性配置)
          → docker pull <image>
        │
        ▼
3. 生成容器名
   im-sandbox-<short-uuid>
        │
        ▼
4. docker run 参数构造
   --name <name>
   --detach
   --workdir /workspace
   -v <workDirectory>:/workspace:rw    (共享工作目录)
   [--cpus <cpuCores>]
   [--memory <memoryMb>m --memory-swap <memoryMb>m]
   [--pids-limit <maxProcesses>]
   [--network none | default]          (默认 none，策略严格)
   [--tmpfs /tmp:rw,noexec,nosuid,size=<diskMb>m]
   [-e K=V ...]                        (来自 config.envVars，仅注入容器)
   [--stop-timeout <maxExecutionTimeSec + 5>]
   <image> sleep infinity              (保活)
        │
        ▼
5. 就绪轮询
   for (1..20):
     docker inspect --format={{.State.Running}} <name>
     if "true": break
     else: sleep 500ms
   全部失败 → IllegalStateException
```

### 资源限制 → docker CLI 参数映射表

| ResourceLimits 字段 | docker 参数 | 启用条件 |
| --- | --- | --- |
| `cpuCores` | `--cpus=1.5` | 非 null 且 > 0 |
| `memoryMb` | `--memory=1024m --memory-swap=1024m` | 非 null 且 > 0（memory-swap = memory 表示禁止使用 swap） |
| `maxProcesses` | `--pids-limit=512` | 非 null 且 > 0 |
| `diskMb` | `--tmpfs /tmp:rw,noexec,nosuid,size=<N>m` | 非 null 且 > 0（/tmp 强制 tmpfs） |
| `maxExecutionTimeSec` | `--stop-timeout=<N+5>` | 取 config 回退值；执行阶段另外 onExit().orTimeout |

> 注意：`/workspace` 是 **rw 挂载宿主机目录**，非 tmpfs。若希望结果完全易逝（销毁即删），可以把 `workDirectory` 指向一个临时目录并由上层 `destroy` 后 `Files.deleteIfExists` 清理，或者自行扩展 DockerLocalSandbox 使用 tmpfs 卷。

---

## 执行阶段

### 1. `execute(code, language=bash)`

等价：

```bash
docker exec -i <container> bash -s   ← stdin 灌入 code
```

- 把 `code` 写入进程 stdin（不通过 shell 命令行拼接，避免命令行参数长度限制 + 转义坑）
- 输出捕获 stdout / stderr 合并
- 超时由 `resourceLimits.maxExecutionTimeSec` 或旧字段 `maxExecutionTimeSec` 控制（`process.onExit().orTimeout`，超时则 `destroyForcibly`）

### 2. `executeCommand(command, args)`

等价：

```bash
docker exec <container> bash -c "<command> <args[0]> <args[1]> ..."
```

- 在 bash 模式下执行单条命令，便于管道/重定向等 Shell 语法
- 命令字符串先经过 SandboxManager 注入的 `CommandPolicy.isAllowed` 预检

### 3. `isPathAllowed(path)`

容器内路径以 `/workspace` 为根，判断规则：
```
return "/workspace".equals(path)
    || path.startsWith("/workspace/")
    || "/tmp".equals(path)
    || path.startsWith("/tmp/")
```

- 不允许访问宿主机目录（docker 也暴露不了其他卷，因为 initialize 只挂载了 `-v <workDirectory>:/workspace:rw`）
- 若 config.allowedPaths 非空：会按 **容器内路径语义**（非宿主绝对路径）判断

---

## 销毁流程（`destroy`）

```
destroy()
  if containerId/name == null: return (幂等)
  docker rm -f <name>       // 强制停止 + 删除
  catch all exceptions → warn
  set containerId=null
```

重复调用 destroy 是安全的。

---

## 典型配置示例（编程式）

```java
SandboxConfig config = new SandboxConfig()
    .setWorkDirectory("/home/me/docker-work")     // 宿主工作目录
    .setType(SandboxType.LOCAL_DOCKER)
    .setImage("eclipse-temurin:26-jre-alpine")    // 自定义镜像
    .setResourceLimits(new ResourceLimits(
        2.0,          // cpuCores
        2048,         // memoryMb
        120,          // maxExecutionTimeSec
        512,          // diskMb（→ tmpfs /tmp 大小）
        1024          // maxProcesses（→ pids-limit）
    ))
    .setEnvVars(Map.of("TZ", "Asia/Shanghai",
                       "APP_ENV", "production"))
    .setAllowedPaths(List.of("/workspace/out"));  // 容器内路径
```

YAML + Spring Boot 方式见 [configuration-guide.md](configuration-guide.md)。

---

## DockerCommandExecutor 替换点

源码：[DockerCommandExecutor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/DockerCommandExecutor.java)

```java
public interface DockerCommandExecutor {
    ProcessResult exec(List<String> args) throws IOException, InterruptedException;
    ProcessResult execWithStdin(List<String> args, String stdin) throws IOException, InterruptedException;
    record ProcessResult(int exitCode, String stdout, String stderr) {}
}
```

默认实现 [ProcessDockerCommandExecutor](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/ProcessDockerCommandExecutor.java) 把 args 前面追加 `docker` 然后用 `ProcessBuilder.redirectErrorStream(true)` 执行。

常见替换需求（见 [extension-guide.md](extension-guide.md)）：
- 使用 **Podman**：重写 `exec` 前置命令为 `podman`
- 使用 **远端 Docker Host / Docker-in-Docker**：前置加 `ssh user@host docker` 或 `docker -H tcp://host:2375`
- **可测试 mock**：单元测试里注入假的 executor 避免真实启动容器（[DockerLocalSandboxTest](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/test/java/org/wall/im/ai/sandbox/local/DockerLocalSandboxTest.java) 就是这么做的）

替换方法：在 `DockerLocalSandboxFactory` 构造容器时调用 `sandbox.setDockerCommandExecutor(yourImpl)`（或直接 new DockerLocalSandbox(executor)），然后用自定义 SandboxFactory 替换默认工厂（见扩展指南）。

---

## 已知限制

1. **容器镜像体积**：`openjdk:26-slim` ≈ 270MB，首次拉取较慢。生产推荐自建小镜像（基于 alpine 或 distroless）。
2. **Windows/WSL2 上文件系统性能**：WSL2 跨 Windows 文件系统挂载（/mnt/c）性能较差。建议 `workDirectory` 放在 WSL2 本地 ext4（`/home/<user>/...`）。
3. **Mac 内存限制（Docker Desktop）**：Docker Desktop 默认只分配 2GB 内存，若 `--memory` 超过宿主机配置值，`docker run` 会失败。
4. **`/workspace` 持久化**：工作目录卷是宿主 rw 绑定挂载，容器内写入会落地到宿主磁盘。若需要"销毁即消失"的工作区，可自行在上层用临时目录 + destroy 后删除。
5. **--network 默认 none**：容器内无外网访问（策略上与 DefaultCommandPolicy 追加的网络关键字黑名单形成双保险）。若需要联网，可在 DockerLocalSandboxFactory 重写，把 `--network=none` 改为 `--network=bridge`，并放宽策略。
