package org.wall.im.ai.sandbox.remote;

import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.SandboxResult;

/**
 * 远端沙盒客户端接口
 * <p>
 * 抽象远端执行服务的调用，便于替换协议（gRPC/WebSocket）或加鉴权/重试中间件。 默认实现 {@link HttpRemoteSandboxClient} 基于
 * JDK {@code HttpClient} + REST。
 * </p>
 */
public interface RemoteSandboxClient {

	/**
	 * 创建并初始化远端沙盒
	 * @param config 沙盒配置
	 * @return 沙盒 ID
	 */
	String initialize(SandboxConfig config);

	/**
	 * 在指定沙盒中执行代码
	 * @param sandboxId 沙盒 ID
	 * @param code 代码
	 * @param workDir 工作目录
	 * @return 执行结果
	 */
	SandboxResult execute(String sandboxId, String code, String workDir);

	/**
	 * 在指定沙盒中执行命令
	 * @param sandboxId 沙盒 ID
	 * @param command 命令
	 * @return 执行结果
	 */
	SandboxResult executeCommand(String sandboxId, String command);

	/**
	 * 检查路径是否允许访问
	 * @param sandboxId 沙盒 ID
	 * @param path 路径
	 * @return 是否允许
	 */
	boolean isPathAllowed(String sandboxId, String path);

	/**
	 * 销毁远端沙盒
	 * @param sandboxId 沙盒 ID
	 */
	void destroy(String sandboxId);

}
