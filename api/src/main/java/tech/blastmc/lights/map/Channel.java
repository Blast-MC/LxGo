package tech.blastmc.lights.map;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import tech.blastmc.lights.type.model.Fixture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@SerializableAs("LxChannelList")
public class Channel implements ConfigurationSerializable {

    int id;
    List<Fixture> addresses = new ArrayList<>();

    public static Channel of(ChannelList list, int id) {
        return list.get(id);
    }

    public Channel(Map<String, Object> map) {
        this.id = (int) map.get("id");
        this.addresses = (List<Fixture>) map.get("addresses");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return new HashMap<>(){{
            put("id", id);
            put("addresses", addresses);
        }};
    }
}
