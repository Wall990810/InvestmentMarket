package org.wall.im.ai.harness.runner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.AgentResult;
import org.wall.im.ai.core.model.Message;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SequentialRunner和ParallelRunner单元测试
 */
@DisplayName("AgentRunner测试")
class AgentRunnerTest {

    @Nested
    @DisplayName("SequentialRunner测试")
    class SequentialRunnerTest {

        @Test
        @DisplayName("应委托给agent.execute")
        void shouldDelegateToAgentExecute() {
            SequentialRunner runner = new SequentialRunner();
            Agent agent = mock(Agent.class);
            AgentResult expectedResult = AgentResult.success("output");
            List<Message> messages = List.of(Message.user("hello"));
            when(agent.execute(messages)).thenReturn(expectedResult);

            AgentResult result = runner.run(agent, messages);

            assertEquals(result, expectedResult);
            verify(agent).execute(messages);
        }

        @Test
        @DisplayName("getType应返回sequential")
        void getType_shouldReturnSequential() {
            assertEquals("sequential", new SequentialRunner().getType());
        }
    }

    @Nested
    @DisplayName("ParallelRunner测试")
    class ParallelRunnerTest {

        @Test
        @DisplayName("runParallel应汇总多个Agent的结果")
        void runParallel_shouldCombineResults() {
            ParallelRunner runner = new ParallelRunner();

            Agent agent1 = mock(Agent.class);
            Agent agent2 = mock(Agent.class);
            List<Message> messages = List.of(Message.user("test"));

            when(agent1.execute(messages)).thenReturn(AgentResult.success("result1"));
            when(agent2.execute(messages)).thenReturn(AgentResult.success("result2"));

            AgentResult combined = runner.runParallel(List.of(agent1, agent2), messages);

            assertTrue(combined.isSuccess());
            assertTrue(combined.getOutput().contains("result1"));
            assertTrue(combined.getOutput().contains("result2"));
        }

        @Test
        @DisplayName("单个Agent失败不应影响其他Agent")
        void singleAgentFailure_shouldNotAffectOthers() {
            ParallelRunner runner = new ParallelRunner();

            Agent goodAgent = mock(Agent.class);
            Agent badAgent = mock(Agent.class);
            List<Message> messages = List.of(Message.user("test"));

            when(goodAgent.execute(messages)).thenReturn(AgentResult.success("good"));
            when(badAgent.execute(messages)).thenThrow(new RuntimeException("boom"));

            AgentResult combined = runner.runParallel(List.of(goodAgent, badAgent), messages);

            assertTrue(combined.isSuccess());
            assertTrue(combined.getOutput().contains("good"));
            assertTrue(combined.getOutput().contains("[ERROR]"));
        }

        @Test
        @DisplayName("getType应返回parallel")
        void getType_shouldReturnParallel() {
            assertEquals("parallel", new ParallelRunner().getType());
        }
    }
}
