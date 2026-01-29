package com.lingframe.core.proxy;

import com.lingframe.core.kernel.GovernanceKernel;
import com.lingframe.core.kernel.InvocationContext;
import com.lingframe.core.plugin.PluginRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SmartServiceProxy 单元测试")
class SmartServiceProxyTest {

    @Mock
    private PluginRuntime runtime;

    @Mock
    private GovernanceKernel kernel;

    private SmartServiceProxy proxy;

    @BeforeEach
    void setUp() {
        when(runtime.getPluginId()).thenReturn("target-plugin");

        // 模拟 Kernel 执行：直接执行 Supplier
        when(kernel.invoke(eq(runtime), any(Method.class), any(InvocationContext.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(3)).get());

        proxy = new SmartServiceProxy("caller-plugin", runtime, Runnable.class, kernel);
    }

    @Nested
    @DisplayName("代理调用")
    class InvocationTests {

        @Test
        @DisplayName("invoke 应正确委托给 Runtime")
        void invokeShouldDelegateToRuntime() throws Throwable {
            // 准备
            Object expectedResult = "Result";
            when(runtime.invoke(eq("caller-plugin"), eq("java.lang.Runnable:run"), any())).thenReturn(expectedResult);

            // 执行
            Method runMethod = Runnable.class.getMethod("run");
            proxy.invoke(null, runMethod, new Object[0]);

            // 验证
            // 1. 验证 Runtime.invoke 被调用
            verify(runtime, times(1)).invoke(eq("caller-plugin"), eq("java.lang.Runnable:run"), any(Object[].class));
        }

        @Test
        @DisplayName("invoke 应透传参数")
        void invokeShouldPassArguments() throws Throwable {
            // 验证参数透传逻辑
            // 由于我们 mock 了 runtime.invoke，我们可以验证它是否接收到了正确的参数

            // 准备
            Object[] args = new Object[] { "test" };
            when(runtime.invoke(any(), any(), any())).thenReturn(null);

            // 执行：我们需要一个方法来触发 invoke。
            // SmartServiceProxy 绑定的是 Runnable，没有带参数的方法。
            // 为了测试参数透传，我们需要重新构造一个绑定到带参接口的 Proxy。

            SmartServiceProxy paramProxy = new SmartServiceProxy("caller-plugin", runtime, Comparable.class, kernel);
            Method compareToMethod = Comparable.class.getMethod("compareTo", Object.class);

            paramProxy.invoke(null, compareToMethod, args);

            // 验证
            // 验证 Runtime.invoke 被调用，且参数正确 (index 2 is args)
            verify(runtime, times(1)).invoke(eq("caller-plugin"), contains("compareTo"), eq(args));
        }
    }
}