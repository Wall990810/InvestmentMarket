package org.wall.im.ai.sandbox.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.SandboxContext;
import org.wall.im.ai.core.sandbox.SandboxResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HttpRemoteSandboxClient 单元测试
 * <p>
 * 用 JDK 内置 {@link HttpServer} 起本地 stub，验证 REST 调用与 JSON 编解码， 不依赖真实远端服务。
 * </p>
 */
@DisplayName("HttpRemoteSandboxClient测试")
class HttpRemoteSandboxClientTest {

	private HttpServer server;

	private HttpRemoteSandboxClient client;

	private final AtomicReference<String> capturedAgentId = new AtomicReference<>();

	private final AtomicReference<String> capturedSessionId = new AtomicReference<>();

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", this::handle);
		server.start();
		int port = server.getAddress().getPort();
		client = new HttpRemoteSandboxClient("http://127.0.0.1:" + port);
	}

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	private void handle(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();
		String method = exchange.getRequestMethod();
		capturedAgentId.set(exchange.getRequestHeaders().getFirst("X-Agent-Id"));
		capturedSessionId.set(exchange.getRequestHeaders().getFirst("X-Session-Id"));

		if ("POST".equals(method) && path.equals("/sandboxes")) {
			respond(exchange, 201, "{\"sandboxId\":\"sb-123\",\"status\":\"READY\"}");
		}
		else if ("POST".equals(method) && path.equals("/sandboxes/sb-5xx/execute")) {
			respond(exchange, 500, "{\"error\":\"internal\",\"code\":\"INTERNAL\"}");
		}
		else if ("POST".equals(method) && path.matches("/sandboxes/.+/execute")) {
			respond(exchange, 200, "{\"success\":true,\"output\":\"hello\\n\",\"exitCode\":0,\"executionTimeMs\":12}");
		}
		else if ("POST".equals(method) && path.matches("/sandboxes/.+/command")) {
			respond(exchange, 200, "{\"success\":true,\"output\":\"cmd-out\",\"exitCode\":0,\"executionTimeMs\":5}");
		}
		else if ("GET".equals(method) && path.matches("/sandboxes/.+/paths")) {
			respond(exchange, 200, "{\"allowed\":true}");
		}
		else if ("DELETE".equals(method) && path.matches("/sandboxes/.+")) {
			exchange.sendResponseHeaders(204, -1);
		}
		else {
			exchange.sendResponseHeaders(404, -1);
		}
		exchange.close();
	}

	private void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	@Test
	@DisplayName("initialize 应返回 sandboxId")
	void initialize_shouldReturnSandboxId() {
		SandboxConfig config = new SandboxConfig();
		config.setRemoteEndpoint("http://example.com");
		config.setContext(SandboxContext.of("agent-1", "session-1"));

		String sandboxId = client.initialize(config);

		assertEquals("sb-123", sandboxId);
		assertEquals("agent-1", capturedAgentId.get());
		assertEquals("session-1", capturedSessionId.get());
	}

	@Test
	@DisplayName("execute 应解析远端响应并返回 success")
	void execute_shouldReturnSuccess() {
		SandboxConfig config = new SandboxConfig();
		config.setContext(SandboxContext.of("agent-1", "session-1"));
		client.initialize(config);

		SandboxResult result = client.execute("sb-123", "echo hello", "/workspace");

		assertTrue(result.isSuccess());
		assertEquals("hello\n", result.getOutput());
	}

	@Test
	@DisplayName("executeCommand 应解析远端响应")
	void executeCommand_shouldReturnSuccess() {
		client.initialize(new SandboxConfig());

		SandboxResult result = client.executeCommand("sb-123", "ls -la");

		assertTrue(result.isSuccess());
		assertEquals("cmd-out", result.getOutput());
	}

	@Test
	@DisplayName("isPathAllowed 应解析 allowed 字段")
	void isPathAllowed_shouldParseAllowed() {
		client.initialize(new SandboxConfig());

		boolean allowed = client.isPathAllowed("sb-123", "/workspace/x.txt");

		assertTrue(allowed);
	}

	@Test
	@DisplayName("destroy 应发送 DELETE 请求不抛异常")
	void destroy_shouldNotThrow() {
		client.initialize(new SandboxConfig());

		client.destroy("sb-123");
	}

	@Test
	@DisplayName("5xx 错误应映射为 failure 不抛异常")
	void execute_5xx_shouldReturnFailure() {
		client.initialize(new SandboxConfig());

		SandboxResult result = client.execute("sb-5xx", "echo hello", "/workspace");

		assertFalse(result.isSuccess());
		assertTrue(result.getErrorOutput().contains("internal"));
	}

}
