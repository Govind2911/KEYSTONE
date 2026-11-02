package com.keystone.exception;

/** Base for errors that map straight onto an HTTP status. Deliberately free of Spring types. */
public class ApiException extends RuntimeException {
    private final int status;

    protected ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static class BadRequest extends ApiException {
        public BadRequest(String message) { super(400, message); }
    }

    public static class Forbidden extends ApiException {
        public Forbidden(String message) { super(403, message); }
    }

    public static class NotFound extends ApiException {
        public NotFound(String message) { super(404, message); }
    }

    public static class Conflict extends ApiException {
        public Conflict(String message) { super(409, message); }
    }
}
