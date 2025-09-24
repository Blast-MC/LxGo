package tech.blastmc.lights.cue;

import lombok.Data;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@SerializableAs("LxCue")
public class Cue implements ConfigurationSerializable {

    int id;
    CueTimes times;
    Map<Integer, List<Permutation>> channelDiffs;
    Map<Integer, List<Permutation>> groupDiffs;

    public Cue(int id, CueTimes times, Map<Integer, List<Permutation>> channelDiffs, Map<Integer, List<Permutation>> groupDiffs) {
        this.id = id;
        this.times = times;
        this.channelDiffs = channelDiffs;
        this.groupDiffs = groupDiffs;
    }

    public Cue(Map<String, Object> map) {
        this.id = (int) map.get("id");
        this.times = (CueTimes) map.get("times");
        this.channelDiffs = (Map<Integer, List<Permutation>>) map.get("channelDiffs");
        this.groupDiffs = (Map<Integer, List<Permutation>>) map.get("groupDiffs");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return new HashMap<>() {{
            put("id", id);
            put("times", times);
            put("channelDiffs", channelDiffs);
            put("groupDiffs", groupDiffs);
        }};
    }
}
