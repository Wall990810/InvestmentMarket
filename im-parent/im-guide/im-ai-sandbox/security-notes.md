← [返回索引](../README.md)

# 安全注意事项

沙盒是直接执行不受信任代码/命令的高风险组件，无论选择哪一种后端（Local Process / Local Docker / Remote HTTP），请务必执行以下安全建议。

---

## 一、通用（所有后端都要做）

1. **enabled=false 作为紧急逃生通道**  
   线上发生 0-day 漏洞或 Agent 被 prompt 注入时，立刻在 YAML 中 `im.ai.sandbox.enabled=false` 即可让所有 execute* 短路返回 "Sandbox is disabled"，**无需重启业务部署**（应用侧可通过 Spring Cloud Config / Nacos 热更新）。

2. **禁止在沙盒外直接执行 Agent 生成的代码**  
   任何 LLM 生成的脚本、命令行，一定要走 SandboxManager.execute* 通道，不要直接 ProcessBuilder。

3. **最小权限账户原则**  
   运行沙盒的操作系统/容器账户一定是**低权限普通用户**，禁止 sudo/root；禁止把宿主机的 `/var/run/docker.sock` 挂载进任何沙盒。

4. **危险关键字不可替代 AST 级策略**  
   默认 `DefaultCommandPolicy`（11 关键字 + 6 网络关键字）是基于 "字符串 contains" 的粗粒度防护，攻击者可通过 Base64 解码、变量拼接、$() 子命令、通配符、数字转义、`eval`、`source /proc/self/fd/0` 等绕过。生产必须叠加 [extension-guide.md 扩展 2](extension-guide.md#扩展-2自定义-commandpolicy-链3-步) 介绍的 AST 级策略（JavaParser / LibCST / Semgrep）。

5. **审计落库**  
   使用 `SandboxLifecycleListener.afterExecute()` 把每次执行的：`agentId/sessionId/codeOrCommand/success/exitCode/output(截断 1M)/耗时` 写到不可变审计日志；高危路径写失败要告警。

6. **执行资源下限**  
   即便是生产级强隔离沙盒，也要设置 **最小**的资源配额而不是"够用就行"：
   - maxExecutionTimeSec ≤ 300（避免长驻脚本）
   - maxProcesses ≤ 1024（防 fork bomb）
   - memoryMb ≤ 宿主机 30%（防 OOM 连锁）

---

## 二、LOCAL_PROCESS 特有（额外风险）

7. **ProcessSandbox 不要用于生产不可信代码**  
   它**没有文件系统隔离**，只是通过 `isPathAllowed` 前缀判断；攻击者只要用 `cd /`、`readlink /proc/self/cwd`、`~` 展开、符号链接、`mount --bind` 即可绕过工作目录限制。仅用于开发调试 + 已人工 review 的脚本。

8. **宿主文件系统权限收紧**  
   运行 ProcessSandbox 的宿主用户对磁盘的 write 权限应仅限于 `workDirectory` + `allowedPaths`；对 `/etc`、`/boot`、内核、`java.io.tmpdir` 做只读挂载或 `chmod 500`。

---

## 三、LOCAL_DOCKER 特有

9. **默认 --network=none 不要轻易改**  
   DockerLocalSandbox 默认 `--network=none`，DefaultCommandPolicy 额外追加 curl/wget/nc/ssh 黑名单，形成双保险。如果确实需要联网，请至少：
   - 自定义 Factory 改为 `--network=bridge` 且加 `--iptables=false` 避免污染宿主机 iptables；
   - 用 CNI 插件限制目标 IP/端口白名单（只允许内网 Artifactory / 内部 PyPI）；
   - 放宽策略链但保留关键字（不要简单的 restrictNetworkCommands=false）。

10. **镜像来源与加固**  
    - 只使用**受信任镜像源**（私有 Harbor 或受信 public）；
    - 生产镜像必须基于 distroless/非 root 用户，镜像内不能有 `sudo`、`su`、setuid `bash`；
    - 定期 `trivy image` / `docker scout cves` 扫描 CVE；
    - `--read-only` 运行根文件系统（只读），只把 `/workspace` + `/tmp` 挂 rw/tmpfs（可在自定义 DockerCommandExecutor 中加）。

11. **不要加 --privileged，不要挂 /sys、/proc 的额外路径**  
    `--privileged` 完全等于宿主机 root；任何挂载 /dev、/sys/fs/cgroup、/proc/sys 都要高度警惕。

12. **容器逃逸监测**  
    在宿主机使用 Falco / Tracee / AppArmor / SECCOMP 配置文件（`--security-opt seccomp=default.json`）监测 `mount`、`ptrace`、`bpf` 等系统调用。

---

## 四、REMOTE_HTTP 特有

13. **HTTPS + 强鉴权是硬性要求**  
    - baseEndpoint 必须 `https://`，证书链可信（禁用 `setDisableSSLVerification` 之类的自定义操作）。
    - 远端服务端必须对每个请求做 **Bearer Token + HMAC-SHA256 签名（带时间戳 + nonce 防重放）** 或 **mTLS** 三选一；不能裸 HTTP 无鉴权在内网都不行。

14. **服务端限流 + 配额**  
    - 按 `X-Agent-Id`（甚至 `X-Meta-Tenant`）维度设置：每分钟请求数 ≤ N；每小时沙盒创建数 ≤ M；单 Agent 并发运行沙盒数 ≤ K；累计 CPU 分钟/日 ≤ L；
    - 全部命中后直接返回 429；SandboxManager 会把 4xx 透传为 failure(output, statusCode)，调用方据此做降级（切 LOCAL_DOCKER 或报用户稍后重试）。

15. **远端执行结果不要作为真相来源**  
    返回值可能被中间人或受攻击的远端服务篡改；涉及金钱交易、下单、权限变更的 Agent 流程，不要把 `SandboxResult.output` 直接作为后续业务输入，应二次校验 + 人工复核。

16. **加密敏感上下文**  
    `X-Meta-*`、代码、输出中可能包含密钥、PII；传输层 TLS 不够时，对高敏字段在业务层做信封加密（KMS + AES-256-GCM）再传输。

---

## 五、组合建议（不同部署阶段）

| 阶段 | 推荐后端 | 策略链 | 隔离等级 |
| --- | --- | --- | --- |
| 本地开发 | LOCAL_PROCESS | DefaultCommandPolicy(restrict=false) | 低 |
| CI/CD 自测 | LOCAL_DOCKER | Default + 简单语义检查 | 中高 |
| 预发布 | LOCAL_DOCKER 或 REMOTE_HTTP（独立集群） | Default + AST + 审计 logger | 高 |
| 生产 · 金融级 | REMOTE_HTTP（多租户独立集群 + K8s gVisor/Kata） | Default + AST + LLM 判定 + 审计 | 极高 |

---

## 六、参考清单（上线前自查）

上线前逐条回答：

- [ ] 是否禁止 Agent 直接使用 ProcessBuilder/Runtime.exec？
- [ ] 是否叠加字符串关键字之外的 AST 级策略？
- [ ] 失败是否落入 `afterExecute` 审计？
- [ ] 低权限账户运行，不挂载 docker.sock？
- [ ] `enabled=false` 逃生通道是否在 YAML 中预留并已演练过？
- [ ] Docker：镜像最小化、无 root、无 privileged、网络默认关闭？
- [ ] Remote：HTTPS + Bearer/HMAC/mTLS、限流 + 配额、PII 加密？
- [ ] 资源限制：执行超时、pids-limit、内存、磁盘、并发沙盒数都设了上限？

如以上有任何一个 "否"，请不要在生产暴露给不可信 Agent。
