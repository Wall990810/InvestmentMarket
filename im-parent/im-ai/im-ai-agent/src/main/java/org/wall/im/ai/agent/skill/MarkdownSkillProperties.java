package org.wall.im.ai.agent.skill;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown技能加载配置属性
 * <p>
 * 可通过 application.yml 配置：
 * <pre>
 * im.ai.markdown-skills:
 *   enabled: true
 *   classpath-dirs:
 *     - skills
 *   file-system-dirs: []
 * </pre>
 */
@ConfigurationProperties(prefix = "im.ai.markdown-skills")
public class MarkdownSkillProperties {

    /**
     * 是否启用Markdown技能加载
     */
    private boolean enabled = true;

    /**
     * classpath下扫描技能文件的目录列表
     */
    private List<String> classpathDirs = new ArrayList<>(List.of("skills"));

    /**
     * 文件系统中扫描技能文件的目录列表
     */
    private List<String> fileSystemDirs = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getClasspathDirs() {
        return classpathDirs;
    }

    public void setClasspathDirs(List<String> classpathDirs) {
        this.classpathDirs = classpathDirs;
    }

    public List<String> getFileSystemDirs() {
        return fileSystemDirs;
    }

    public void setFileSystemDirs(List<String> fileSystemDirs) {
        this.fileSystemDirs = fileSystemDirs;
    }
}
