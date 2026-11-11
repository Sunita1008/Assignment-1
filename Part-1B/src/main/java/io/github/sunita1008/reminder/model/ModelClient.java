package io.github.sunita1008.reminder.model;

import io.github.sunita1008.reminder.api.ModelUnavailable;
import java.util.List;

/** Transport boundary for one model completion. */
public interface ModelClient {
  /** Requests one completion for the supplied chat messages. */
  String complete(List<ChatMessage> messages) throws ModelUnavailable;
}
