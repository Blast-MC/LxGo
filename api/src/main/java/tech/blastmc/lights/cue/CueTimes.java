package tech.blastmc.lights.cue;

import lombok.Data;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Data
@SerializableAs("LxCueTimes")
public class CueTimes implements ConfigurationSerializable {

    CueTimes(double intensity, double direction, double color, double autoFollow) {
        this.intensity = intensity;
        this.direction = direction;
        this.color = color;
        this.autoFollow = autoFollow;
    }

    private double intensity;
    private double direction;
    private double color;
    private double autoFollow = -1;

    public CueTimes(Map<String, Object> map) {
        this.intensity = (double) map.get("intensity");
        this.direction = (double) map.get("direction");
        this.color = (double) map.get("color");
        this.autoFollow = (double) map.getOrDefault("autoFollow", -1);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return new HashMap<>() {{
            put("intensity", intensity);
            put("direction", direction);
            put("color", color);
            put("autoFollow", autoFollow);
        }};
    }
}
