package org.wall.im.ai.harness.pipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.model.Message;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MessagePipeline单元测试
 */
@DisplayName("MessagePipeline测试")
class MessagePipelineTest {

    @Nested
    @DisplayName("管道构建测试")
    class PipelineBuildTest {

        @Test
        @DisplayName("应支持链式添加stage")
        void shouldSupportChainedStageAddition() {
            MessagePipeline pipeline = new MessagePipeline("test-pipeline")
                    .addStage(new FunctionalStage("stage1", msgs -> msgs))
                    .addStage(new FunctionalStage("stage2", msgs -> msgs));

            assertEquals("test-pipeline", pipeline.getName());
            assertEquals(2, pipeline.getStages().size());
        }
    }

    @Nested
    @DisplayName("管道执行测试")
    class PipelineExecutionTest {

        @Test
        @DisplayName("空管道应原样返回消息")
        void emptyPipeline_shouldReturnMessagesAsIs() {
            MessagePipeline pipeline = new MessagePipeline("empty");
            List<Message> input = List.of(Message.user("hello"));

            List<Message> result = pipeline.process(input);

            assertEquals(1, result.size());
            assertEquals("hello", result.get(0).getContent());
        }

        @Test
        @DisplayName("单阶段管道应执行对应处理")
        void singleStagePipeline_shouldProcessMessages() {
            MessagePipeline pipeline = new MessagePipeline("single")
                    .addStage(new FunctionalStage("upper", msgs -> {
                        List<Message> result = new ArrayList<>();
                        for (Message msg : msgs) {
                            result.add(new Message(msg.getRole(), msg.getContent().toUpperCase()));
                        }
                        return result;
                    }));

            List<Message> result = pipeline.process(List.of(Message.user("hello")));

            assertEquals("HELLO", result.get(0).getContent());
        }

        @Test
        @DisplayName("多阶段管道应按顺序执行")
        void multiStagePipeline_shouldExecuteInOrder() {
            MessagePipeline pipeline = new MessagePipeline("multi")
                    .addStage(new FunctionalStage("add-prefix", msgs -> {
                        List<Message> result = new ArrayList<>();
                        for (Message msg : msgs) {
                            result.add(new Message(msg.getRole(), "[1]" + msg.getContent()));
                        }
                        return result;
                    }))
                    .addStage(new FunctionalStage("add-suffix", msgs -> {
                        List<Message> result = new ArrayList<>();
                        for (Message msg : msgs) {
                            result.add(new Message(msg.getRole(), msg.getContent() + "[2]"));
                        }
                        return result;
                    }));

            List<Message> result = pipeline.process(List.of(Message.user("msg")));

            assertEquals("[1]msg[2]", result.get(0).getContent());
        }

        @Test
        @DisplayName("管道不应修改原始输入列表")
        void pipeline_shouldNotModifyOriginalList() {
            MessagePipeline pipeline = new MessagePipeline("safe")
                    .addStage(new FunctionalStage("filter", msgs -> new ArrayList<>()));

            List<Message> original = new ArrayList<>(List.of(Message.user("hello")));
            pipeline.process(original);

            assertEquals(1, original.size());
        }
    }
}
