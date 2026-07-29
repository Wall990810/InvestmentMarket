package org.wall.im.ai.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体配置模型，对应YAML配置文件中的Agent定义
 */
public class AgentConfig {

    /** 智能体唯一标识 */
    private String name;

    /** 智能体描述 */
    private String description;

    /** 智能体类型，如: chat, task, workflow */
    private String type = "chat";

    /** 使用的模型配置 */
    private ModelConfig model;

    /** 技能列表 */
    private List<String> skills = new ArrayList<>();

    /** 工具列表 */
    private List<String> tools = new ArrayList<>();

    /** 记忆配置 */
    private MemoryConfig memory;

    /** 沙盒配置 */
    private SandboxConfig sandbox;

    /** 监控配置 */
    private MonitorConfig monitor;

    /** 执行环境配置 */
    private ExecutionConfig execution;

    /** 扩展属性 */
    private Map<String, Object> properties = new HashMap<>();

    // --- Getters and Setters ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public ModelConfig getModel() { return model; }
    public void setModel(ModelConfig model) { this.model = model; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getTools() { return tools; }
    public void setTools(List<String> tools) { this.tools = tools; }

    public MemoryConfig getMemory() { return memory; }
    public void setMemory(MemoryConfig memory) { this.memory = memory; }

    public SandboxConfig getSandbox() { return sandbox; }
    public void setSandbox(SandboxConfig sandbox) { this.sandbox = sandbox; }

    public MonitorConfig getMonitor() { return monitor; }
    public void setMonitor(MonitorConfig monitor) { this.monitor = monitor; }

    public ExecutionConfig getExecution() { return execution; }
    public void setExecution(ExecutionConfig execution) { this.execution = execution; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
}
