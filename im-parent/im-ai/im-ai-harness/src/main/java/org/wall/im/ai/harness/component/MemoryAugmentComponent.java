package org.wall.im.ai.harness.component;

import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.memory.MemoryEntry;
import org.wall.im.ai.core.memory.MemoryStore;
import org.wall.im.ai.core.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 记忆增强组件
 * <p>在Agent处理消息前自动注入历史记忆上下文</p>
 */
public class MemoryAugmentComponent implements HarnessComponent {

    private final MemoryStore memoryStore;
    private final int contextWindowSize;

    public MemoryAugmentComponent(MemoryStore memoryStore, int contextWindowSize) {
        this.memoryStore = memoryStore;
        this.contextWindowSize = contextWindowSize;
    }

    @Override
    public String getName() {
        return "memory-augment";
    }

    @Override
    public String getDescription() {
        return "在消息处理前注入历史记忆上下文";
    }

    @Override
    public void initialize() {
        // 无需额外初始化
    }

    @Override
    public List<Message> execute(Agent agent, List<Message> input) {
        List<Message> result = new ArrayList<>();

        // 从记忆中检索最近的历史消息
        if (memoryStore != null) {
            List<MemoryEntry> history = memoryStore.retrieveRecent(
                    agent.getName() + ":conversation", contextWindowSize);

            // 将历史记忆转换为消息注入
            for (MemoryEntry entry : history) {
                result.add(new Message(entry.getRole(), entry.getContent()));
            }
        }

        // 追加当前输入
        result.addAll(input);
        return result;
    }

    @Override
    public void destroy() {
        // 无需额外清理
    }
}
