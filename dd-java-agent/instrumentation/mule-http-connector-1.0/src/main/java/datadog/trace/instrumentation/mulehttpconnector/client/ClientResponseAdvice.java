package datadog.trace.instrumentation.mulehttpconnector.client;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.instrumentation.mulehttpconnector.client.ClientDecorator.DECORATE;

public class ClientResponseAdvice {

  @Advice.OnMethodExit(suppress = Throwable.class)
  public static void stopSpan(
      @Advice.This final AsyncCompletionHandler handler,
      @Advice.Argument(0) final Response response) {
    final ContextStore<AsyncCompletionHandler, AgentSpan> contextStore =
        InstrumentationContext.get(AsyncCompletionHandler.class, AgentSpan.class);
    final AgentSpan span = contextStore.get(handler);
    final AgentScope scope = activateSpan(span, true);
    if (span != null) {
      contextStore.put(handler, null);
      DECORATE.afterStart(span);
      DECORATE.onResponse(span, response);
      DECORATE.beforeFinish(span);
      span.finish();
    }
    scope.close();
  }
}
