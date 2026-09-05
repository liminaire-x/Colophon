package kr.guinnessgroup.colophon.nodes.flow;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;
import kr.guinnessgroup.colophon.runtime.ResumeCondition;

import java.util.List;

/** Flow: waits N server ticks, then continues (exercises Suspend/resume). */
public final class DelayNode implements NodeType {

    @Override public String id() { return "delay"; }
    @Override public String label() { return "Delay"; }
    @Override public String category() { return "flow"; }
    @Override public List<FieldSpec> fields() { return List.of(new FieldSpec("ticks", "number", "20")); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public Node create(JsonObject config) {
        final int ticks = readInt(config, "ticks", 20);
        return ctx -> NodeResult.suspend(ResumeCondition.afterTicks(ticks));
    }

    private static int readInt(JsonObject config, String key, int fallback) {
        if (config != null && config.has(key) && config.get(key).isJsonPrimitive()) {
            try {
                return config.get(key).getAsInt();
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }
}
