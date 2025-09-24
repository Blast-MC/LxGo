package tech.blastmc.lights.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@SerializableAs("LxGroup")
public class Group implements ConfigurationSerializable {

    int id;
    List<Integer> channels;

    public Group(Map<String, Object> map) {
        this.id = (int) map.get("id");
        this.channels = (List<Integer>) map.get("channels");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return new HashMap<>() {{
            put("id", id);
            put("channels", channels);
        }};
    }
}
