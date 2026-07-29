package org.wall.im.ai.agent.lifecycle;

import org.wall.im.ai.core.tool.Tool;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表
 */
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool get(String name) {
        return tools.get(name);
    }

    public Collection<Tool> getAll() {
        return tools.values();
    }

    public void unregister(String name) {
        tools.remove(name);
    }
}
