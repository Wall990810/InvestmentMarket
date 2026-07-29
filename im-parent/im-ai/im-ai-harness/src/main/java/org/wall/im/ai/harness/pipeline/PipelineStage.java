package org.wall.im.ai.harness.pipeline;

import org.wall.im.ai.core.model.Message;

import java.util.List;

/**
 * 管道处理阶段接口
 */
public interface PipelineStage {

    /**
     * 获取阶段名称
     */
    String getName();

    /**
     * 执行处理
     *
     * @param messages 输入消息
     * @return 处理后的消息
     */
    List<Message> execute(List<Message> messages);
}
