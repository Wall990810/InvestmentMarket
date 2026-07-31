package org.wall.im.ai.monitor.composite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.monitor.AgentMonitor;
import org.wall.im.ai.core.monitor.CustomMetricRegistry;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CompositeAgentMonitor单元测试
 */
@DisplayName("CompositeAgentMonitor测试")
class CompositeAgentMonitorTest {

	private AgentMonitor primary;

	private AgentMonitor secondary;

	private CompositeAgentMonitor composite;

	@BeforeEach
	void setUp() {
		primary = mock(AgentMonitor.class);
		secondary = mock(AgentMonitor.class);
		CustomMetricRegistry mockRegistry = mock(CustomMetricRegistry.class);
		when(primary.getCustomMetricRegistry()).thenReturn(mockRegistry);

		composite = new CompositeAgentMonitor(primary);
		composite.addMonitor(secondary);
	}

	@Test
	@DisplayName("traceStart应调用primary和所有secondary")
	void traceStart_shouldCallAllMonitors() {
		when(primary.traceStart("agent", "input")).thenReturn("trace-1");

		String traceId = composite.traceStart("agent", "input");

		assertEquals("trace-1", traceId);
		verify(primary).traceStart("agent", "input");
		verify(secondary).traceStart("agent", "input");
	}

	@Test
	@DisplayName("traceEnd应调用所有monitor")
	void traceEnd_shouldCallAllMonitors() {
		composite.traceEnd("t1", "agent", "output", 100, 50);

		verify(primary).traceEnd("t1", "agent", "output", 100, 50);
		verify(secondary).traceEnd("t1", "agent", "output", 100, 50);
	}

	@Test
	@DisplayName("secondary失败不应影响primary")
	void secondaryFailure_shouldNotAffectPrimary() {
		when(primary.traceStart("agent", "input")).thenReturn("trace-1");
		doThrow(new RuntimeException("fail")).when(secondary).traceStart(anyString(), anyString());

		String traceId = composite.traceStart("agent", "input");

		assertEquals("trace-1", traceId);
		verify(primary).traceStart("agent", "input");
	}

	@Test
	@DisplayName("traceError应调用所有monitor")
	void traceError_shouldCallAllMonitors() {
		composite.traceError("t1", "agent", "error");

		verify(primary).traceError("t1", "agent", "error");
		verify(secondary).traceError("t1", "agent", "error");
	}

	@Test
	@DisplayName("getCustomMetricRegistry应委托给primary")
	void getCustomMetricRegistry_shouldDelegateToPrimary() {
		assertNotNull(composite.getCustomMetricRegistry());
		verify(primary).getCustomMetricRegistry();
	}

}
