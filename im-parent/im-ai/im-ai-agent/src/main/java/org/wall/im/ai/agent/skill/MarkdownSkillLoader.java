package org.wall.im.ai.agent.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.agent.lifecycle.SkillRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * Markdown技能加载器
 * <p>
 * 从classpath或文件系统扫描.md技能文件，解析YAML frontmatter和正文模板，
 * 创建{@link MarkdownSkill}实例并注册到{@link SkillRegistry}。
 * </p>
 *
 * <h3>MD技能文件格式</h3>
 * <pre>
 * ---
 * name: skill-name
 * description: 技能描述
 * tools: [tool1, tool2]
 * ---
 *
 * # 技能指令
 * 支持 {{input}}、{{skillName}} 等变量占位符
 * </pre>
 */
public class MarkdownSkillLoader {

    private static final Logger log = LoggerFactory.getLogger(MarkdownSkillLoader.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final String FRONTMATTER_DELIMITER = "---";

    private final SkillRegistry skillRegistry;

    public MarkdownSkillLoader(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    /**
     * 从classpath目录加载所有MD技能文件
     *
     * @param classpathDir classpath目录路径（如 "skills"）
     * @return 加载的技能数量
     */
    public int loadFromClasspath(String classpathDir) {
        int count = 0;
        try {
            ClassLoader cl = getClass().getClassLoader();
            Enumeration<URL> resources = cl.getResources(classpathDir);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                List<Path> mdFiles = listMdFilesFromUrl(url);
                for (Path file : mdFiles) {
                    try {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
                        String source = classpathDir + "/" + file.getFileName();
                        MarkdownSkill skill = parseSkill(content, source);
                        if (skill != null) {
                            skillRegistry.register(skill);
                            count++;
                            log.info("Loaded markdown skill '{}' from classpath: {}", skill.getName(), source);
                        }
                    } catch (Exception e) {
                        log.error("Failed to load skill from file: {}", file, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to load skills from classpath: {}", classpathDir, e);
        }
        if (count == 0) {
            log.debug("No markdown skills found in classpath directory: {}", classpathDir);
        }
        return count;
    }

    /**
     * 从文件系统目录加载所有MD技能文件
     *
     * @param dirPath 目录路径
     * @return 加载的技能数量
     */
    public int loadFromFileSystem(String dirPath) {
        Path dir = Path.of(dirPath);
        if (!Files.exists(dir)) {
            log.warn("Directory not found: {}", dirPath);
            return 0;
        }
        int[] count = {0};
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".md") || file.toString().endsWith(".MD")) {
                        try {
                            String content = Files.readString(file, StandardCharsets.UTF_8);
                            MarkdownSkill skill = parseSkill(content, file.getFileName().toString());
                            if (skill != null) {
                                skillRegistry.register(skill);
                                count[0]++;
                                log.info("Loaded markdown skill '{}' from file: {}", skill.getName(), file);
                            }
                        } catch (Exception e) {
                            log.error("Failed to load skill from file: {}", file, e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Failed to walk directory: {}", dirPath, e);
        }
        return count[0];
    }

    /**
     * 从单个MD文件内容解析技能
     *
     * @param mdContent MD文件内容
     * @param source    来源描述（用于日志）
     * @return 解析出的MarkdownSkill，解析失败返回null
     */
    public MarkdownSkill parseSkill(String mdContent, String source) {
        if (mdContent == null || mdContent.isBlank()) {
            log.warn("Empty markdown content from: {}", source);
            return null;
        }

        try {
            String[] parts = splitFrontmatter(mdContent);
            if (parts == null) {
                log.warn("No YAML frontmatter found in: {}", source);
                return null;
            }

            String frontmatter = parts[0];
            String body = parts[1];

            ObjectNode metadata = (ObjectNode) YAML_MAPPER.readTree(frontmatter);

            String name = getRequiredText(metadata, "name", source);
            if (name == null) {
                return null;
            }

            String description = getOptionalText(metadata, "description", name + " skill");
            List<String> tools = getOptionalTools(metadata);
            Map<String, Object> extraMetadata = new HashMap<>();
            metadata.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                if (!"name".equals(fieldName) && !"description".equals(fieldName) && !"tools".equals(fieldName)) {
                    if (entry.getValue().isTextual()) {
                        extraMetadata.put(fieldName, entry.getValue().asText());
                    } else {
                        extraMetadata.put(fieldName, entry.getValue().toString());
                    }
                }
            });

            return new MarkdownSkill(name, description, body, tools, extraMetadata);
        } catch (Exception e) {
            log.error("Failed to parse markdown skill from: {}", source, e);
            return null;
        }
    }

    /**
     * 扫描并加载多个classpath目录
     *
     * @param classpathDirs classpath目录列表
     * @return 加载的技能总数
     */
    public int loadFromClasspath(String... classpathDirs) {
        int total = 0;
        for (String dir : classpathDirs) {
            total += loadFromClasspath(dir);
        }
        return total;
    }

    private String[] splitFrontmatter(String content) {
        int firstDelimiter = content.indexOf(FRONTMATTER_DELIMITER);
        if (firstDelimiter != 0) {
            return null;
        }
        int secondDelimiter = content.indexOf(FRONTMATTER_DELIMITER, firstDelimiter + FRONTMATTER_DELIMITER.length());
        if (secondDelimiter == -1) {
            return null;
        }
        String frontmatter = content.substring(firstDelimiter + FRONTMATTER_DELIMITER.length(), secondDelimiter).trim();
        String body = content.substring(secondDelimiter + FRONTMATTER_DELIMITER.length()).trim();
        return new String[]{frontmatter, body};
    }

    private String getRequiredText(ObjectNode node, String fieldName, String source) {
        if (node.has(fieldName) && node.get(fieldName).isTextual()) {
            return node.get(fieldName).asText();
        }
        log.warn("Missing required field '{}' in markdown skill: {}", fieldName, source);
        return null;
    }

    private String getOptionalText(ObjectNode node, String fieldName, String defaultValue) {
        if (node.has(fieldName) && node.get(fieldName).isTextual()) {
            return node.get(fieldName).asText();
        }
        return defaultValue;
    }

    private List<String> getOptionalTools(ObjectNode node) {
        List<String> tools = new ArrayList<>();
        if (node.has("tools") && node.get("tools").isArray()) {
            node.get("tools").forEach(t -> {
                if (t.isTextual()) {
                    tools.add(t.asText());
                }
            });
        }
        return tools;
    }

    private String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private List<Path> listMdFilesFromUrl(URL url) throws IOException {
        List<Path> result = new ArrayList<>();
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            try {
                Path dir = Path.of(url.toURI());
                if (Files.isDirectory(dir)) {
                    try (var stream = Files.list(dir)) {
                        stream.filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".MD"))
                                .forEach(result::add);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to resolve file URL: {}", url, e);
            }
        } else if ("jar".equals(protocol)) {
            loadSkillsFromJarUrl(url);
        }
        return result;
    }

    private void loadSkillsFromJarUrl(URL url) {
        String jarPath = url.getPath();
        int bangIndex = jarPath.indexOf("!/");
        if (bangIndex < 0) {
            return;
        }
        String jarFilePath = jarPath.substring(5, bangIndex);
        String entryPath = jarPath.substring(bangIndex + 2);
        try {
            Path jarFile = Path.of(URI.create("file:" + jarFilePath));
            if (!Files.exists(jarFile)) {
                return;
            }
            try (var zf = new ZipFile(jarFile.toFile())) {
                zf.stream()
                        .filter(e -> !e.isDirectory()
                                && e.getName().startsWith(entryPath)
                                && (e.getName().endsWith(".md") || e.getName().endsWith(".MD")))
                        .forEach(e -> {
                            try (var is = zf.getInputStream(e)) {
                                String content = readStream(is);
                                MarkdownSkill skill = parseSkill(content, e.getName());
                                if (skill != null) {
                                    skillRegistry.register(skill);
                                    log.info("Loaded markdown skill '{}' from JAR: {}", skill.getName(), e.getName());
                                }
                            } catch (Exception ex) {
                                log.warn("Failed to load skill from JAR entry: {}", e.getName(), ex);
                            }
                        });
            }
        } catch (Exception e) {
            log.warn("Failed to read JAR for skills: {}", url, e);
        }
    }
}
