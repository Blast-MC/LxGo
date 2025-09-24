package tech.blastmc.lights.cue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
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

    @SerializableAs("LxPitch")
    public static class Pitch extends Permutation {
        public Pitch(int value) { super(value); }

        public Pitch(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }

        @Override
        public String toString() {
            return "Pitch(value=" + value + ")";
        }
    }

    @SerializableAs("LxYaw")
    public static class Yaw extends Permutation {
        public Yaw(int value) { super(value); }

        public Yaw(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }

        @Override
        public String toString() {
            return "Yaw(value=" + value + ")";
        }
    }

    @SerializableAs("LxIntensity")
    public static class Intensity extends Permutation {
        public Intensity(int value) { super(value); }

        public Intensity(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }

        @Override
        public String toString() {
            return "Intensity(value=" + value + ")";
        }
    }

    @SerializableAs("LxColor")
    public static class Color extends Permutation {
        public Color(int value) { super(value); }

        public Color(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }

        @Override
        public String toString() {
            return "Color(value=" + value + ")";
        }
    }

    @SerializableAs("LxEffect")
    public static class Effect extends Permutation {
        @Getter
        Integer offset;

        public Effect(int value) { super(value); }
        public Effect(int value, int offset) {
            super(value);
            this.offset = offset;
        }

        public Effect(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }

        @Override
        public String toString() {
            return "Effect(value=" + value + (offset == null ? "" : ",offset=" + offset) + ")";
        }
    }

    @SerializableAs("LxStopEffect")
    public static class StopEffect extends Permutation {
        public StopEffect(int value) { super(value); }

        public StopEffect(Map<String, Object> map) {
            this.value = (int) map.get("value");
        }

        @Override
        public String toString() {
            return "StopEffect(value=" + value + ")";
        }
    }

}
