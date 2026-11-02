package com.smarttodo.storage;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smarttodo.domain.Priority;
import com.smarttodo.domain.Task;
import com.smarttodo.domain.TaskStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** UTF-8 version-1 JSON persistence with replacement through a narrow test seam. */
public final class JsonTaskRepository implements TaskRepository {

  private static final int STORE_VERSION = 1;
  private static final Set<String> STORE_FIELDS = Set.of("version", "nextId", "tasks");
  private static final Set<String> TASK_FIELDS =
      Set.of("id", "title", "description", "priority", "tags", "status", "createdAt");
  private static final ObjectMapper MAPPER =
      new ObjectMapper(
          JsonFactory.builder()
              .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
              .build());

  private final Path file;
  private final FileReplacer replacer;

  public JsonTaskRepository(Path workingDirectory) {
    this(workingDirectory, FileReplacer.atomicMove());
  }

  /** Creates a repository with an injectable store replacement operation. */
  public JsonTaskRepository(Path workingDirectory, FileReplacer replacer) {
    file = workingDirectory.resolve("tasks.json").toAbsolutePath().normalize();
    FileReplacer checkedReplacer = Objects.requireNonNull(replacer, "replacer");
    this.replacer = (temp, target) -> checkedReplacer.replace(temp, target);
  }

  @Override
  public TaskStore load() {
    if (Files.notExists(file)) {
      return TaskStore.empty();
    }
    try {
      byte[] bytes = Files.readAllBytes(file);
      JsonNode root;
      try (JsonParser parser = MAPPER.getFactory().createParser(bytes)) {
        root = MAPPER.readTree(parser);
        if (parser.nextToken() != null) {
          throw new IllegalArgumentException("Trailing JSON content.");
        }
      }
      return decodeStore(root);
    } catch (IOException | IllegalArgumentException | DateTimeParseException exception) {
      throw new StorageReadException(exception);
    }
  }

  @Override
  public void save(TaskStore store) {
    byte[] bytes;
    try {
      bytes = MAPPER.writeValueAsBytes(encodeStore(store));
    } catch (IOException exception) {
      throw new StorageWriteException(exception);
    }

    Path temp = null;
    try {
      Path parent = file.getParent();
      if (parent == null) {
        throw new IOException("The tasks file must have a parent directory.");
      }
      temp = Files.createTempFile(parent, "tasks.json.", ".tmp");
      Files.write(temp, bytes);
      replacer.replace(temp, file);
    } catch (IOException exception) {
      if (temp != null) {
        try {
          Files.deleteIfExists(temp);
        } catch (IOException cleanupException) {
          exception.addSuppressed(cleanupException);
        }
      }
      throw new StorageWriteException(exception);
    }
  }

  private static TaskStore decodeStore(JsonNode root) {
    requireObjectWithFields(root, STORE_FIELDS, STORE_FIELDS);
    long version = requiredLong(root, "version");
    if (version != STORE_VERSION) {
      throw new IllegalArgumentException("Unsupported store version.");
    }
    long nextId = requiredLong(root, "nextId");
    JsonNode taskNodes = root.get("tasks");
    if (taskNodes == null || !taskNodes.isArray()) {
      throw new IllegalArgumentException("tasks must be an array.");
    }
    List<Task> tasks = new ArrayList<>();
    for (JsonNode node : taskNodes) {
      tasks.add(decodeTask(node));
    }
    return new TaskStore(nextId, tasks);
  }

  private static Task decodeTask(JsonNode node) {
    if (!(node instanceof ObjectNode)) {
      throw new IllegalArgumentException("A task must be an object.");
    }
    Set<String> required = new java.util.HashSet<>(TASK_FIELDS);
    required.remove("description");
    requireObjectWithFields(node, required, TASK_FIELDS);

    final long id = requiredLong(node, "id");
    String title = requiredText(node, "title");
    if (!title.equals(title.strip())) {
      throw new IllegalArgumentException("Stored titles must be trimmed.");
    }
    String description = null;
    if (node.has("description")) {
      description = requiredText(node, "description");
    }
    final Priority priority = Priority.valueOf(requiredText(node, "priority"));
    final TaskStatus status = TaskStatus.valueOf(requiredText(node, "status"));
    JsonNode tagsNode = node.get("tags");
    if (tagsNode == null || !tagsNode.isArray()) {
      throw new IllegalArgumentException("tags must be an array.");
    }
    List<String> tags = new ArrayList<>();
    for (JsonNode tagNode : tagsNode) {
      if (!tagNode.isTextual()) {
        throw new IllegalArgumentException("Tags must be strings.");
      }
      tags.add(tagNode.textValue());
    }
    List<String> normalizedTags = Task.normalizeTags(tags);
    if (!tags.equals(normalizedTags)) {
      throw new IllegalArgumentException("Stored tags must be normalized and unique.");
    }
    String timestamp = requiredText(node, "createdAt");
    if (!timestamp.endsWith("Z")) {
      throw new IllegalArgumentException("createdAt must be UTC.");
    }
    Instant createdAt = Instant.parse(timestamp);
    return new Task(id, title, description, priority, tags, status, createdAt);
  }

  private static void requireObjectWithFields(
      JsonNode node, Set<String> requiredFields, Set<String> allowedFields) {
    if (!(node instanceof ObjectNode)) {
      throw new IllegalArgumentException("Expected a JSON object.");
    }
    Iterator<String> names = node.fieldNames();
    while (names.hasNext()) {
      if (!allowedFields.contains(names.next())) {
        throw new IllegalArgumentException("Unexpected JSON field.");
      }
    }
    for (String field : requiredFields) {
      if (!node.has(field)) {
        throw new IllegalArgumentException("Missing JSON field.");
      }
    }
  }

  private static long requiredLong(JsonNode object, String field) {
    JsonNode value = object.get(field);
    if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
      throw new IllegalArgumentException("Expected an integer.");
    }
    return value.longValue();
  }

  private static String requiredText(JsonNode object, String field) {
    JsonNode value = object.get(field);
    if (value == null || !value.isTextual()) {
      throw new IllegalArgumentException("Expected a string.");
    }
    return value.textValue();
  }

  private static Map<String, Object> encodeStore(TaskStore store) {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("version", STORE_VERSION);
    document.put("nextId", store.nextId());
    List<Map<String, Object>> records = new ArrayList<>();
    for (Task task : store.tasks()) {
      Map<String, Object> record = new LinkedHashMap<>();
      record.put("id", task.id());
      record.put("title", task.title());
      if (task.description() != null) {
        record.put("description", task.description());
      }
      record.put("priority", task.priority().name());
      record.put("tags", task.tags());
      record.put("status", task.status().name());
      record.put("createdAt", task.createdAt().toString());
      records.add(record);
    }
    document.put("tasks", records);
    return document;
  }

}
