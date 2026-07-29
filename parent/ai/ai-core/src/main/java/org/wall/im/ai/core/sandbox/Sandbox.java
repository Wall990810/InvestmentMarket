package org.wall.im.ai.core.sandbox;

/**
 * 沙盒环境接口
 * <p>限制Agent运行时的工作路径和资源访问</p>
 */
public interface Sandbox {

    /**
     * 初始化沙盒环境
     */
    void initialize();

    /**
     * 在沙盒中执行代码
     *
     * @param code    要执行的代码
     * @param workDir 工作目录
     * @return 执行结果
     */
    SandboxResult execute(String code, String workDir);

    /**
     * 在沙盒中执行命令
     *
     * @param command 命令
     * @return 执行结果
     */
    SandboxResult executeCommand(String command);

    /**
     * 检查路径是否在沙盒允许范围内
     *
     * @param path 文件路径
     * @return 是否允许
     */
    boolean isPathAllowed(String path);

    /**
     * 销毁沙盒环境
     */
    void destroy();
}
