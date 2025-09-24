package tech.blastmc.lights;

import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import tech.blastmc.lights.cue.Cue;
import tech.blastmc.lights.cue.CueBuilder;
import tech.blastmc.lights.cue.CueTimes;
import tech.blastmc.lights.cue.CueTimesBuilder;
import tech.blastmc.lights.cue.Permutation.Color;
import tech.blastmc.lights.cue.Permutation.Effect;
import tech.blastmc.lights.cue.Permutation.Intensity;
import tech.blastmc.lights.cue.Permutation.Pitch;
import tech.blastmc.lights.cue.Permutation.StopEffect;
import tech.blastmc.lights.cue.Permutation.Yaw;
import tech.blastmc.lights.cue.SimulatedCue;
import tech.blastmc.lights.effect.EffectRegistry;
import tech.blastmc.lights.handler.CueHandler;
import tech.blastmc.lights.handler.interpolators.Interpolator;
import tech.blastmc.lights.map.Channel;
import tech.blastmc.lights.map.ChannelList;
import tech.blastmc.lights.map.Group;
import tech.blastmc.lights.type.base.SmartLight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Consumer;

@Data
@SerializableAs("LxBoard")
public class LxBoard implements ConfigurationSerializable {

    private @NonNull Plugin plugin;
    private @NonNull String name;
    private @NonNull LinkedList<Cue> cues;
    private @NonNull ChannelList channels;
    private @NonNull List<Group> groups;

    public LxBoard(@NonNull Plugin plugin, @NonNull String name, @NonNull LinkedList<Cue> cues, @NonNull ChannelList channels, @NonNull List<Group> groups) {
        this.plugin = plugin;
        this.name = name;
        this.cues = cues;
        this.channels = channels;
        this.groups = groups;

        this.listeners = new LxBoardListeners(this);
        this.plugin.getServer().getPluginManager().registerEvents(listeners, plugin);
    }

    public void setPlugin(@NonNull Plugin plugin) {
        this.plugin = plugin;
        if (this.listeners != null)
            HandlerList.unregisterAll(this.listeners);
        this.listeners = new LxBoardListeners(this);
        this.plugin.getServer().getPluginManager().registerEvents(listeners, plugin);
    }

    private List<String> debugLines = new ArrayList<>();
    private boolean debug = false;

    private LxBoardListeners listeners;

    private int currentCue = 0;
    private ListIterator<Cue> cueIterator;

    private List<Interpolator> interpolators = new ArrayList<>();
    private List<CueHandler> cueHandlers = new ArrayList<>();

    private EffectRegistry effectRegistry = new EffectRegistry();

    public Cue getCueZero() {
        CueBuilder builder = new CueBuilder(0);
        for (Channel channel : channels)
            builder.channel(channel.getId(), new Intensity(0), new Pitch(0), new Yaw(0), new Color(0));
        return builder.build();
    }

    public void go() {
        if (cueIterator == null) {
            cueHandlers.clear();
            cueIterator = cues.listIterator();
        }
        if (cueIterator.hasNext()) {
            Cue cue = cueIterator.next();
            goToCue(cue.getId(), currentCue, cue.getTimes(), true);
        }
    }

    public void goToCue(int cue) {
        goToCue(cue, new CueTimesBuilder(5).build());
    }

    public void goToCue(int cue, CueTimes times) {
        for (Cue c : cues) {
            if (c.getId() == cue) {
                goToCue(c.getId(), currentCue, times);
                return;
            }
        }
        if (cue == 0)
            goToCue(0, currentCue, times);
    }

    public void goToCue(int cueNum, int fromCueNum, CueTimes times) {
        goToCue(cueNum, fromCueNum, times, false);
    }

    public void goToCue(int cueNum, int fromCueNum, CueTimes times, boolean isNext) {
        if (cueNum == fromCueNum && cueNum != 0)
            return;

        if (!cueHandlers.isEmpty() && !interpolators.isEmpty() && !isNext) {
            SimulatedCue simulatedCue = new SimulatedCue(this);

            if (cueNum < fromCueNum) {
                debug("Creating backwards state");

                List<CueHandler> cueHandlers = new ArrayList<>();
                List<Interpolator> interpolators = new ArrayList<>();
                ListIterator<Cue> cueIterator = cues.listIterator();
                handle(getCueZero(), times, simulatedCue, cueHandlers, interpolators);
                while (cueIterator.hasNext()) {
                    Cue cue = cueIterator.next();
                    if (cue.getId() > cueNum)
                        break;
                    handle(cue, times, simulatedCue, cueHandlers, interpolators);
                }
            } else {
                for (Cue cue : cues) {
                    if (cue.getId() <= fromCueNum)
                        continue;
                    if (cue.getId() > cueNum)
                        break;
                    handle(cue, times, simulatedCue, cueHandlers, interpolators);
                }
            }

            Cue cue = simulatedCue.toCue();
            debug("Cue: " + cue);
            handle(cue, times, null, cueHandlers, interpolators);
        }
        else {
            Cue cue = getCueZero();
            if (cueNum != 0) {
                for (Cue c : cues) {
                    if (c.getId() == cueNum) {
                        cue = c;
                        break;
                    }
                }
            }
            debug("Cue: " + cue);
            handle(cue, times, null, cueHandlers, interpolators);
        }

        if (isDebug())
            flushDebugLines();

        if (cueNum == 0) {
            cueIterator = null;
            currentCue = 0;
            return;
        }

        cueIterator = cues.listIterator();
        while (cueIterator.hasNext()) {
            if (cueIterator.next().getId() == cueNum) break;
        }
        currentCue = cueNum;
    }

    public void handle(Cue cue, CueTimes times, SimulatedCue sim, List<CueHandler> cueHandlers, List<Interpolator> interpolators) {
        if (sim != null)
            debug("Simulating cue: " + cue.getId());

        for (CueHandler ch : cueHandlers)
            ch.stopConflicting(cue, times, sim);

        debug("Interpolators: " + interpolators.size() + ", CueHandlers: " + cueHandlers.size());

        interpolators.removeIf(Interpolator::isDone);
        cueHandlers.removeIf(CueHandler::isDone);

        debug("Cleaned");
        debug("Interpolators: " + interpolators.size() + ", CueHandlers: " + cueHandlers.size());

        CueHandler handler = new CueHandler(plugin, this, cue, times, interpolators, sim);
        handler.start();

        cueHandlers.add(handler);
    }

    public void shutdown() {
        if (this.listeners != null)
            HandlerList.unregisterAll(this.listeners);
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

    public void log(String message) {
        this.plugin.getLogger().info(message);
    }

    public void debug(String message) {
        if (isDebug())
            debugLines.add(message);
    }

    private void flushDebugLines() {
        String paste = paste(String.join("\n", debugLines));
        if (paste != null)
            log("Debug transaction: " + paste);
        debugLines.clear();
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

    public static void initFileStore() {
        Consumer<Class<? extends ConfigurationSerializable>> registerSerializable = clazz -> {
            String alias = clazz.getAnnotation(SerializableAs.class).value();
            ConfigurationSerialization.registerClass(clazz, alias);
        };

        registerSerializable.accept(LxBoard.class);
        registerSerializable.accept(Cue.class);
        registerSerializable.accept(CueTimes.class);
        registerSerializable.accept(Channel.class);
        registerSerializable.accept(Group.class);
        registerSerializable.accept(Yaw.class);
        registerSerializable.accept(Pitch.class);
        registerSerializable.accept(Color.class);
        registerSerializable.accept(Intensity.class);
        registerSerializable.accept(Effect.class);
        registerSerializable.accept(StopEffect.class);
        registerSerializable.accept(SmartLight.class);
    }

    private static String paste(String content) {
        try {
            Request request = (new Request.Builder()).url("https://paste.projecteden.gg/documents").post(RequestBody.create(MediaType.get("text/plain"), content)).build();

            try (Response response = (new OkHttpClient()).newCall(request).execute()) {
                PasteResult result = (new Gson()).fromJson(response.body().string(), PasteResult.class);
                return "https://paste.projecteden.gg/" + result.getKey();
            }
        } catch (Throwable $ex) {
            $ex.printStackTrace();
        }
        return null;
    }

    private static class PasteResult {
        @Getter
        private String key;
    }

}
