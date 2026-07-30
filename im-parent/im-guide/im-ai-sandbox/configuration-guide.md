← 返回索引

# 配置示例

下面是一个典型的 `application.yml` 片段，对应 [SandboxConfig](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/SandboxConfig.java) 字段（字段名需与配置绑定方式对应）：

```yaml
im:
  ai:
    sandbox:
      enabled: true                       # 是否启用沙盒，false 则绕过所有限制
      work-dir: /var/lib/im/ai-sandbox    # 沙盒工作目录，留空则使用系统临时目录
      allowed-paths:                      # 额外可访问路径白名单
        - /var/lib/im/data
        - /tmp/im-shared
      network-access: false               # 是否允许网络
      max-execution-time: 60              # 单次执行最大耗时（秒）
      max-memory-mb: 512                  # 最大内存（MB）
```

对应的 Java 配置 Bean：

```java
@Configuration
public class SandboxConfiguration {

    @Bean
    public SandboxConfig sandboxConfig() {
        SandboxConfig config = new SandboxConfig();
        config.setEnabled(true);
        config.setWorkDir("/var/lib/im/ai-sandbox");
        config.setAllowedPaths(List.of("/var/lib/im/data", "/tmp/im-shared"));
        config.setMaxExecutionTime(60);
        config.setMaxMemoryMb(512);
        return config;
    }

    @Bean(destroyMethod = "destroy")
    public ProcessSandbox processSandbox(SandboxConfig config) {
        ProcessSandbox sandbox = new ProcessSandbox(config);
        sandbox.initialize();
        return sandbox;
    }

    @Bean
    public SandboxManager sandboxManager(ProcessSandbox sandbox, SandboxConfig config) {
        return new SandboxManager(sandbox, config);
    }
}
```

> 注意将 `ProcessSandbox` 声明为 Bean 并指定 `destroyMethod = "destroy"`，使容器关闭时自动清理沙盒目录。