package kr.guinnessgroup.colophon.runtime;

import java.util.List;

/** Thrown when a published graph fails validation; carries the error list. */
public final class GraphValidationException extends RuntimeException {

    private final List<String> errors;

    public GraphValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
