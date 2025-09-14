package tech.blastmc.lights;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Permutation implements ConfigurationSerializable {

    int value;

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of("value", value);
    }

    @SerializableAs("Pitch")
    public static class Pitch extends Permutation {
        public Pitch(int value) { super(value); }

        public Pitch(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }
    }

    @SerializableAs("Yaw")
    public static class Yaw extends Permutation {
        public Yaw(int value) { super(value); }

        public Yaw(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }
    }

    @SerializableAs("Intensity")
    public static class Intensity extends Permutation {
        public Intensity(int value) { super(value); }

        public Intensity(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }
    }

    @SerializableAs("Color")
    public static class Color extends Permutation {
        public Color(int value) { super(value); }

        public Color(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }
    }

    @SerializableAs("Effect")
    public static class Effect extends Permutation {
        public Effect(int value) { super(value); }

        public Effect(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }
    }

}
