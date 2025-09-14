package tech.blastmc.lights;

import gg.projecteden.commands.Commands;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.plugin.java.JavaPlugin;
import tech.blastmc.lights.Permutation.Color;
import tech.blastmc.lights.Permutation.Effect;
import tech.blastmc.lights.Permutation.Intensity;
import tech.blastmc.lights.Permutation.Pitch;
import tech.blastmc.lights.Permutation.Yaw;
import tech.blastmc.lights.cue.Cue;
import tech.blastmc.lights.cue.CueBuilder;
import tech.blastmc.lights.cue.CueTimes;
import tech.blastmc.lights.cue.CueTimesBuilder;
import tech.blastmc.lights.map.Channel;
import tech.blastmc.lights.map.ChannelList;
import tech.blastmc.lights.map.Group;
import tech.blastmc.lights.type.base.SmartLight;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LxGo extends JavaPlugin {

    @Getter
    private static LxGo instance;

    private Commands commands;
    public static List<LxBoard> boards;

    private LxGo() {
        instance = this;
    }

    @Override
    public void onEnable() {
        registerSerializable(LxBoard.class);
        registerSerializable(Cue.class);
        registerSerializable(CueTimes.class);
        registerSerializable(Channel.class);
        registerSerializable(Group.class);
        registerSerializable(Yaw.class);
        registerSerializable(Pitch.class);
        registerSerializable(Color.class);
        registerSerializable(Intensity.class);
        registerSerializable(Effect.class);
        registerSerializable(SmartLight.class);

        this.commands = new Commands(this)
                .register(LxGoCommand.class);

        loadConfig();

        if (boards.isEmpty())
            boards.add(new LxBoard.Builder()
                    .plugin(this)
                    .name("test")
                    .channels(new ChannelList())
                    .cue(new CueBuilder(5)
                            .channel(1, new Pitch(60), new Yaw(50))
                            .times(new CueTimesBuilder()
                                    .direction(1.5)
                                    .build())
                            .build())
                    .build());
        else
            for (LxBoard board : boards) {
                board.setPlugin(this);
                board.goToZero();
            }

        System.out.println(boards.size() + " boards loaded");
    }

    private void loadConfig() {
        if (!LxGo.getInstance().getDataFolder().exists())
            LxGo.getInstance().getDataFolder().mkdir();

        FileConfiguration config = getInstance().getConfig();
        if (config.contains("boards"))
            boards = (List<LxBoard>) config.getList("boards");
        else
            boards = new ArrayList<>();

        boards.removeIf(Objects::isNull);

        saveConfig();
    }

    @Override
    public void onDisable() {
        this.commands.unregisterExcept();

        FileConfiguration config = getInstance().getConfig();
        config.set("boards", boards.stream().filter(Objects::nonNull).toList());
        saveConfig();
    }

    public void registerSerializable(Class<? extends ConfigurationSerializable> clazz) {
        String alias = clazz.getAnnotation(SerializableAs.class).value();
        ConfigurationSerialization.registerClass(clazz, alias);
    }

}
