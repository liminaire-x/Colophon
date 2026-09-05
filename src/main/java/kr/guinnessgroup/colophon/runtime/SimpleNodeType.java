package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.function.Function;

/** A {@link NodeType} defined inline by a descriptor and a factory function. */
public record SimpleNodeType(String id, String label, String category,
                             List<FieldSpec> fields, Function<JsonObject, Node> factory)
        implements NodeType {

    @Override
    public Node create(JsonObject config) {
        return factory.apply(config);
    }
}
