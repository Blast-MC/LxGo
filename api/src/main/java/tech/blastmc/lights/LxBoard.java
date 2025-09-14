package tech.blastmc.lights;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import tech.blastmc.lights.Permutation.Color;
import tech.blastmc.lights.Permutation.Intensity;
import tech.blastmc.lights.Permutation.Pitch;
import tech.blastmc.lights.Permutation.Yaw;
import tech.blastmc.lights.cue.Cue;
import tech.blastmc.lights.cue.CueBuilder;
import tech.blastmc.lights.cue.CueTimes;
import tech.blastmc.lights.effect.EffectRegistry;
import tech.blastmc.lights.map.Channel;
import tech.blastmc.lights.map.ChannelList;
import tech.blastmc.lights.map.Group;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

@Data
@RequiredArgsConstructor
@SerializableAs("LxBoard")
public class LxBoard implements ConfigurationSerializable {

    private @NonNull Plugin plugin;
    private @NonNull String name;
    private @NonNull LinkedList<Cue> cues;
    private @NonNull ChannelList channels;
    private @NonNull List<Group> groups;

    private ListIterator<Cue> cueIterator;
    private List<CueHandler> cueHandlers = new ArrayList<>();

    private EffectRegistry effectRegistry = new EffectRegistry();

    public Cue getCueZero() {
        CueBuilder builder = new CueBuilder(0);
        for (Channel channel : channels)
            builder.channel(channel.getId(), new Intensity(0), new Pitch(0), new Yaw(0), new Color(0));
        return builder.build();
    }

    public void go() {
        if (cueIterator == null)
            cueIterator = cues.listIterator();
        if (cueIterator.hasNext())
            goToCue(cueIterator.next().getId());
    }

    public void goToZero() {
        cueHandlers.forEach(CueHandler::stop);
        cueHandlers.clear();

        Cue cue = getCueZero();
        var cueHandler = new CueHandler(plugin, this, cue, cue.getTimes());
        cueHandler.start();
        cueIterator = null;
    }

    public void goToCue(int cue) {
        for (Cue c : cues) {
            if (c.getId() == cue) {
                goToCue(c.getId(), c.getTimes());
                break;
            }
        }
    }

    public void goToCue(int cue, CueTimes times) {
        for (Cue c : cues) {
            if (c.getId() == cue) {
                for (CueHandler cueHandler : cueHandlers)
                    cueHandler.stopConflicting(c);
                cueHandlers.removeIf(CueHandler::isDone);

                var cueHandler = new CueHandler(plugin,this, c, times);
                cueHandler.start();
                cueHandlers.add(cueHandler);
            }
        }
    }

    public LxBoard(Map<String, Object> map) {
        this.name = map.get("name").toString();
        this.cues = new LinkedList<>((List<Cue>) map.get("cues"));
        this.channels = new ChannelList((List<Channel>) map.get("channels"));
        this.groups = (List<Group>) map.get("groups");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return new HashMap<>() {{
            put("name", name);
            put("cues", new ArrayList<>(cues));
            put("channels", new ArrayList<>(channels));
            put("groups", new ArrayList<>(groups));
        }};
    }

    public static class Builder {
        private Plugin plugin;
        private String name;
        private LinkedList<Cue> cues = new LinkedList<>();
        private ChannelList channels = new ChannelList();
        private List<Group> groups = new ArrayList<>();

        public Builder plugin(Plugin plugin) {
            this.plugin = plugin;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder cues(List<Cue> cues) {
            this.cues = new LinkedList<>(cues);
            this.cues.sort(Comparator.comparingInt(Cue::getId));
            return this;
        }

        public Builder cue(Cue cue) {
            this.cues.add(cue);
            this.cues.sort(Comparator.comparing(Cue::getId));
            return this;
        }

        public Builder channels(ChannelList channels) {
            this.channels = channels;
            return this;
        }

        public Builder groups(List<Group> groups) {
            this.groups = groups;
            return this;
        }

        public Builder group(Group group) {
            this.groups.add(group);
            return this;
        }

        public LxBoard build() {
            return new LxBoard(this.plugin, this.name, this.cues, this.channels, this.groups);
        }
    }

}
