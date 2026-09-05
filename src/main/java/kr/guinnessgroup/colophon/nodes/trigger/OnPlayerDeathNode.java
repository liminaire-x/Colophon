package kr.guinnessgroup.colophon.nodes.trigger;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;

import java.util.List;

/** Trigger: fires when a player dies. Entry point (no flow-in). */
public final class OnPlayerDeathNode implements NodeType {

    @Override public String id() { return "on_player_death"; }
    @Override public String label() { return "On Player Death"; }
    @Override public String category() { return "trigger"; }
    @Override public List<FieldSpec> fields() { return List.of(); }
    @Override public boolean hasFlowIn() { return false; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public Node create(JsonObject config) {
        return ctx -> NodeResult.cont();
    }
}
