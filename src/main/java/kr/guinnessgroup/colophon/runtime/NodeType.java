package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * A registered node type: its editor-facing descriptor plus a factory that
 * builds a runnable {@link Node} from a node's saved config.
 */
public interface NodeType {

    String id();

    String label();

    /** "trigger" (an entry point bound to a game event) or "action". */
    String category();

    List<FieldSpec> fields();

    Node create(JsonObject config);
}
