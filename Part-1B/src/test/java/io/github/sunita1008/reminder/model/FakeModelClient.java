package io.github.sunita1008.reminder.model;

import io.github.sunita1008.reminder.api.ModelUnavailable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/** Scripted model transport for deterministic tests; never included in the production JAR. */
public final class FakeModelClient implements ModelClient {
  private final Queue<ScriptedResult> scriptedResults = new ArrayDeque<>();
  private List<ChatMessage> lastMessages = List.of();
  private int callCount;

  /** Creates a fake that returns the supplied responses in order. */
  public FakeModelClient(String... responses) {
    Arrays.stream(responses).map(ScriptedResult::response).forEach(scriptedResults::add);
  }

  /** Adds a scripted service failure to the response sequence. */
  public void enqueueUnavailable(ModelUnavailable failure) {
    scriptedResults.add(ScriptedResult.unavailable(failure));
  }

  @Override
  public String complete(List<ChatMessage> messages) throws ModelUnavailable {
    callCount++;
    lastMessages = List.copyOf(messages);
    ScriptedResult result = scriptedResults.poll();
    if (result == null) {
      throw new IllegalStateException("FakeModelClient script exhausted.");
    }
    if (result.failure() != null) {
      throw result.failure();
    }
    return result.text();
  }

  /** Returns the number of attempted completions. */
  public int callCount() {
    return callCount;
  }

  /** Returns the messages from the most recent attempted completion. */
  public List<ChatMessage> lastMessages() {
    return lastMessages;
  }

  private record ScriptedResult(String text, ModelUnavailable failure) {
    private static ScriptedResult response(String text) {
      return new ScriptedResult(text, null);
    }

    private static ScriptedResult unavailable(ModelUnavailable failure) {
      return new ScriptedResult(null, failure);
    }
  }
}
