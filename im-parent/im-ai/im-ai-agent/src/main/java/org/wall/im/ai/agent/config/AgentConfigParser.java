package org.wall.im.ai.agent.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.config.AgentsDefinition;
import org.wall.im.ai.core.model.AgentConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Agent配置解析器
 * <p>
 * 从YAML文件中解析Agent配置，支持classpath和文件系统路径
 * </p>
 */
public class AgentConfigParser {

	private static final Logger log = LoggerFactory.getLogger(AgentConfigParser.class);

	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	/**
	 * 从classpath资源解析配置
	 * @param resourcePath classpath资源路径
	 * @return Agent配置集合
	 */
	public AgentsDefinition parseFromClasspath(String resourcePath) {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			if (is == null) {
				throw new IllegalArgumentException("Resource not found: " + resourcePath);
			}
			AgentsDefinition definition = YAML_MAPPER.readValue(is, AgentsDefinition.class);
			log.info("Parsed {} agents from classpath resource: {}", definition.getAgents().size(), resourcePath);
			return definition;
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to parse agent config from: " + resourcePath, e);
		}
	}

	/**
	 * 从文件路径解析配置
	 * @param filePath 文件路径
	 * @return Agent配置集合
	 */
	public AgentsDefinition parseFromFile(String filePath) {
		try {
			Path path = Paths.get(filePath);
			if (!Files.exists(path)) {
				throw new IllegalArgumentException("File not found: " + filePath);
			}
			AgentsDefinition definition = YAML_MAPPER.readValue(path.toFile(), AgentsDefinition.class);
			log.info("Parsed {} agents from file: {}", definition.getAgents().size(), filePath);
			return definition;
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to parse agent config from: " + filePath, e);
		}
	}

	/**
	 * 从YAML字符串解析配置
	 * @param yamlContent YAML内容
	 * @return Agent配置集合
	 */
	public AgentsDefinition parseFromString(String yamlContent) {
		try {
			AgentsDefinition definition = YAML_MAPPER.readValue(yamlContent, AgentsDefinition.class);
			log.info("Parsed {} agents from string content", definition.getAgents().size());
			return definition;
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to parse agent config from string", e);
		}
	}

	/**
	 * 合并默认配置到每个Agent配置中
	 */
	public static void applyDefaults(AgentsDefinition definition) {
		AgentConfig defaults = definition.getDefaults();
		if (defaults == null) {
			return;
		}
		for (AgentConfig agent : definition.getAgents()) {
			if (agent.getModel() == null && defaults.getModel() != null) {
				agent.setModel(defaults.getModel());
			}
			if (agent.getMemory() == null && defaults.getMemory() != null) {
				agent.setMemory(defaults.getMemory());
			}
			if (agent.getSandbox() == null && defaults.getSandbox() != null) {
				agent.setSandbox(defaults.getSandbox());
			}
			if (agent.getMonitor() == null && defaults.getMonitor() != null) {
				agent.setMonitor(defaults.getMonitor());
			}
			if (agent.getExecution() == null && defaults.getExecution() != null) {
				agent.setExecution(defaults.getExecution());
			}
		}
	}

}
