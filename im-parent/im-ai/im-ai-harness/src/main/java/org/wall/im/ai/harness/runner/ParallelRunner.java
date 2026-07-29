package org.wall.im.ai.harness.runner;

import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.AgentResult;
import org.wall.im.ai.core.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 并行Agent运行器
 * <p>支持多个Agent并行执行，并汇总结果</p>
 */
public class ParallelRunner implements AgentRunner {

    private final ExecutorService executor;

    public ParallelRunner() {
        this.executor = Executors.newCachedThreadPool();
    }

    public ParallelRunner(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * 并行运行多个Agent
     *
     * @param agents   Agent列表
     * @param messages 消息列表
     * @return 汇总结果
     */
    public AgentResult runParallel(List<Agent> agents, List<Message> messages) {
        long startTime = System.currentTimeMillis();
        List<Future<AgentResult>> futures = new ArrayList<>();

        for (Agent agent : agents) {
            futures.add(executor.submit(() -> agent.execute(messages)));
        }

        StringBuilder combinedOutput = new StringBuilder();
        int totalTokens = 0;

        for (Future<AgentResult> future : futures) {
            try {
                AgentResult result = future.get(60, TimeUnit.SECONDS);
                combinedOutput.append(result.getOutput()).append("\n");
                totalTokens += result.getTokenUsage();
            } catch (Exception e) {
                combinedOutput.append("[ERROR] ").append(e.getMessage()).append("\n");
            }
        }

        AgentResult combined = new AgentResult();
        combined.setSuccess(true);
        combined.setOutput(combinedOutput.toString().trim());
        combined.setCostTimeMs(System.currentTimeMillis() - startTime);
        combined.setTokenUsage(totalTokens);
        return combined;
    }

    @Override
    public AgentResult run(Agent agent, List<Message> messages) {
        return agent.execute(messages);
    }

    @Override
    public String getType() {
        return "parallel";
    }
}
