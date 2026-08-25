package org.wall.im.ai.sandbox.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.ResourceLimits;
import org.wall.im.ai.core.sandbox.SandboxContext;
import org.wall.im.ai.core.sandbox.SandboxResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于 JDK {@link HttpClient} 的远端沙盒客户端
 * <p>
 * 通过 REST 调用远端执行服务。所有失败一律返回 {@link SandboxResult#failure}（不抛异常），与本地沙盒行为一致， 避免 Agent
 * 工具调用炸裂。请求头 {@code X-Agent-Id}/{@code X-Session-Id}/{@code X-Meta-*} 取自
 * {@link SandboxContext}（initialize 时记录，后续请求复用）。
 * </p>
 *
 * <h3>REST 协议</h3>
 * <ul>
 * <li>POST /sandboxes - 创建沙盒，返回 {@code {sandboxId, status}}</li>
 * <li>POST /sandboxes/{id}/execute - 执行代码，返回 RemoteExecuteResponse</li>
 * <li>POST /sandboxes/{id}/command - 执行命令，返回 RemoteExecuteResponse</li>
 * <li>GET /sandboxes/{id}/paths?path=... - 路径校验，返回 {@code {allowed:bool}}</li>
 * <li>DELETE /sandboxes/{id} - 销毁沙盒</li>
 * </ul>
 */
public class HttpRemoteSandboxClient implements RemoteSandboxClient {

	private static final Logger log = LoggerFactory.getLogger(HttpRemoteSandboxClient.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final String baseUrl;

	private final HttpClient httpClient;

	/** initialize 时记录的上下文，后续 execute 等请求复用 */
	private SandboxContext currentContext;

	public HttpRemoteSandboxClient(String baseUrl) {
		this(baseUrl, defaultClient());
	}

	public HttpRemoteSandboxClient(String baseUrl, HttpClient httpClient) {
		this.baseUrl = baseUrl == null ? ""
				: (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
		this.httpClient = httpClient;
	}

	private static HttpClient defaultClient() {
		return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
	}

	@Override
	public String initialize(SandboxConfig config) {
		this.currentContext = config != null ? config.getContext() : null;
		try {
			Map<String, Object> body = buildInitBody(config);
			HttpRequest req = post("/sandboxes", body);
			HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			if (isSuccess(resp.statusCode())) {
				RemoteExecuteResponse r = MAPPER.readValue(resp.body(), RemoteExecuteResponse.class);
				if (r.getSandboxId() != null) {
					return r.getSandboxId();
				}
			}
			throw new RuntimeException("Remote initialize failed: HTTP " + resp.statusCode() + ", body=" + resp.body());
		}
		catch (Exception e) {
			throw new RuntimeException("Remote initialize error: " + e.getMessage(), e);
		}
	}

	@Override
	public SandboxResult execute(String sandboxId, String code, String workDir) {
		return sendExecute("/sandboxes/" + sandboxId + "/execute", RemoteExecuteRequest.forCode(code, workDir, null));
	}

	@Override
	public SandboxResult executeCommand(String sandboxId, String command) {
		return sendExecute("/sandboxes/" + sandboxId + "/command", RemoteExecuteRequest.forCommand(command, null));
	}

	@Override
	public boolean isPathAllowed(String sandboxId, String path) {
		try {
			String encoded = URLEncoder.encode(path == null ? "" : path, StandardCharsets.UTF_8);
			HttpRequest.Builder b = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/sandboxes/" + sandboxId + "/paths?path=" + encoded))
				.GET();
			applyContextHeaders(b);
			HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
			if (isSuccess(resp.statusCode())) {
				JsonNode node = MAPPER.readTree(resp.body());
				return node.has("allowed") && node.get("allowed").asBoolean();
			}
			return false;
		}
		catch (Exception e) {
			log.warn("Remote isPathAllowed error: sandboxId={}, path={}", sandboxId, path, e);
			return false;
		}
	}

	@Override
	public void destroy(String sandboxId) {
		try {
			HttpRequest.Builder b = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/sandboxes/" + sandboxId))
				.DELETE();
			applyContextHeaders(b);
			httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
		}
		catch (Exception e) {
			log.warn("Remote destroy error: sandboxId={}", sandboxId, e);
		}
	}

	// --- 内部辅助 ---

	private SandboxResult sendExecute(String path, RemoteExecuteRequest reqBody) {
		long start = System.currentTimeMillis();
		try {
			HttpRequest req = post(path, reqBody);
			HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			long cost = System.currentTimeMillis() - start;
			if (isSuccess(resp.statusCode())) {
				RemoteExecuteResponse r = MAPPER.readValue(resp.body(), RemoteExecuteResponse.class);
				return r.toSandboxResult();
			}
			// 非成功：尝试解析错误体
			try {
				RemoteExecuteResponse err = MAPPER.readValue(resp.body(), RemoteExecuteResponse.class);
				String errMsg = err.getError() != null ? err.getError()
						: (err.getErrorOutput() != null ? err.getErrorOutput() : "HTTP " + resp.statusCode());
				return SandboxResult.failure(errMsg, resp.statusCode(), cost);
			}
			catch (Exception parseEx) {
				return SandboxResult.failure("HTTP " + resp.statusCode() + ": " + resp.body(), resp.statusCode(), cost);
			}
		}
		catch (Exception e) {
			return SandboxResult.failure("Remote call error: " + e.getMessage(), -1,
					System.currentTimeMillis() - start);
		}
	}

	private HttpRequest post(String path, Object body) throws Exception {
		String json = MAPPER.writeValueAsString(body);
		HttpRequest.Builder b = HttpRequest.newBuilder()
			.uri(URI.create(baseUrl + path))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(json));
		applyContextHeaders(b);
		return b.build();
	}

	private void applyContextHeaders(HttpRequest.Builder b) {
		if (currentContext == null) {
			return;
		}
		if (currentContext.getAgentId() != null) {
			b.header("X-Agent-Id", currentContext.getAgentId());
		}
		if (currentContext.getSessionId() != null) {
			b.header("X-Session-Id", currentContext.getSessionId());
		}
		Map<String, String> meta = currentContext.getMetadata();
		if (meta != null) {
			meta.forEach((k, v) -> b.header("X-Meta-" + k, v == null ? "" : v));
		}
	}

	private Map<String, Object> buildInitBody(SandboxConfig config) {
		Map<String, Object> body = new HashMap<>();
		if (config == null) {
			return body;
		}
		body.put("workDir", config.getWorkDir());
		body.put("allowedPaths", config.getAllowedPaths());
		body.put("networkAccess", config.isNetworkAccess());
		body.put("image", config.getImage());
		ResourceLimits limits = config.getResourceLimits() != null ? config.getResourceLimits()
				: ResourceLimits.from(config);
		body.put("resourceLimits", limits);
		body.put("envVars", config.getEnvVars());
		return body;
	}

	private boolean isSuccess(int statusCode) {
		return statusCode >= 200 && statusCode < 300;
	}

}
