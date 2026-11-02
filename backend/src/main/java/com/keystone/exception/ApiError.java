package com.keystone.exception;

import java.time.Instant;
import java.util.List;

/** The one error shape every endpoint returns. Never contains a stack trace. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, List<FieldIssue> errors) {
    public record FieldIssue(String field, String message) {}
}
