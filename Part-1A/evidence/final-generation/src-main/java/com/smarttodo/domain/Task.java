package com.smarttodo.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** An immutable task and its creation metadata. */
public record Task(
    long id,
    String title,
    String description,
    Priority priority,
    List<String> tags,
    TaskStatus status,
    Instant createdAt) {

  /** Creates a task after validating and normalizing its mutable input fields. */
  public Task {
    if (id <= 0) {
      throw new IllegalArgumentException("Task IDs must be positive.");
    }
    title = Objects.requireNonNull(title, "title").strip();
    if (title.isEmpty()) {
      throw new IllegalArgumentException("Task titles must not be empty.");
    }
    priority = Objects.requireNonNull(priority, "priority");
    tags = Collections.unmodifiableList(new ArrayList<>(normalizeTags(tags)));
    status = Objects.requireNonNull(status, "status");
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  /** Trims, lowercases, and deduplicates tags while preserving their first occurrence. */
  public static List<String> normalizeTags(List<String> suppliedTags) {
    Objects.requireNonNull(suppliedTags, "tags");
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String tag : suppliedTags) {
      String value = Objects.requireNonNull(tag, "tag").strip().toLowerCase(Locale.ROOT);
      if (value.isEmpty()) {
        throw new IllegalArgumentException("Tags must not be empty.");
      }
      normalized.add(value);
    }
    return List.copyOf(new ArrayList<>(normalized));
  }

  @Override
  public List<String> tags() {
    return tags;
  }
}
