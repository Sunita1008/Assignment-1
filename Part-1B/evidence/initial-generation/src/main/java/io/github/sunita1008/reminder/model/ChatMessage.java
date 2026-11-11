package io.github.sunita1008.reminder.model;

/** A role/content pair for an OpenAI-compatible chat-completions request. */
public record ChatMessage(String role, String content) {}
