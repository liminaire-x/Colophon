package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.function.Function;

/**
 * A {@link NodeType} defined inline by descriptor values and a factory function.
 * Convenience for trivial nodes; dedicated classes are preferred for real ones.
 */
public record SimpleNodeType(String id, String label, String category,
                             List<FieldSpec> fields, boolean hasFlowIn,
                             List<String> flowOutPorts, Function<JsonObject, Node> factory)
        implements NodeType {

    @Override
    public Node create(JsonObject config) {
        return factory.apply(config);
    }
}
