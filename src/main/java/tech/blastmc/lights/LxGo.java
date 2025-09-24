package tech.blastmc.lights;

import gg.projecteden.commands.Commands;
import lombok.Getter;
import org.bukkit.DyeColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import tech.blastmc.lights.cue.Permutation;
import tech.blastmc.lights.cue.Permutation.Color;
import tech.blastmc.lights.cue.Permutation.Effect;
import tech.blastmc.lights.cue.Permutation.Intensity;
import tech.blastmc.lights.cue.Permutation.Pitch;
import tech.blastmc.lights.cue.Permutation.StopEffect;
import tech.blastmc.lights.cue.Permutation.Yaw;
import tech.blastmc.lights.cue.CueBuilder;
import tech.blastmc.lights.cue.CueTimesBuilder;
import tech.blastmc.lights.effect.EffectBuilder;
import tech.blastmc.lights.effect.EffectType;
import tech.blastmc.lights.effect.OffsetType;
import tech.blastmc.lights.map.ChannelList;
import tech.blastmc.lights.map.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LxGo extends JavaPlugin {

    @Getter
    private static LxGo instance;

    public static List<LxBoard> boards;

    private LxGo() {
        instance = this;
    }

    @Override
    public void onEnable() {
        LxBoard.initFileStore();

        new Commands(this)
            .add(LxGoCommand.class)
            .registerAll();

        loadConfig();

        if (boards.isEmpty())
            boards.add(getDefaultBoard());
        else
            for (LxBoard board : boards) {
                board.setPlugin(this);
                board.goToCue(0);
            }

        registerEffects();

        getLogger().info(boards.size() + " boards loaded");
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
        Commands.unregisterAll();

        FileConfiguration config = getInstance().getConfig();
        config.set("boards", boards.stream().filter(Objects::nonNull).toList());
        saveConfig();
    }



    private void registerEffects() {
        boards.get(0).getEffectRegistry().register(
                new EffectBuilder()
                        .id(1)
                        .effectType(EffectType.DIRECTION)
                        .durationInSeconds(4)
                        .offsetType(OffsetType.OFFSET_ORDERED)
                        .sampler(tick -> {
                            final int PERIOD_TICKS = 80;
                            final int YAW_AMP_DEG = 60;
                            final int PITCH_AMP_DEG = 25;
                            final double PHASE_SHIFT = Math.PI / 2.0;

                            int t = Math.floorMod(tick, PERIOD_TICKS);
                            double theta = (2.0 * Math.PI * t) / PERIOD_TICKS;

                            int dYaw   = (int) Math.round(YAW_AMP_DEG   * Math.sin(theta));
                            int dPitch = (int) Math.round(PITCH_AMP_DEG * Math.sin(2.0 * theta + PHASE_SHIFT));

                            return List.of(new Permutation.Yaw(dYaw), new Permutation.Pitch(dPitch));
                        })
                        .build()
        );

        boards.get(0).getEffectRegistry().register(
                new EffectBuilder()
                        .id(2)
                        .effectType(EffectType.COLOR)
                        .offsetType(OffsetType.SYNCHRONIZED)
                        .durationInSeconds(4)
                        .sampler(tick -> {
                            final int PERIOD_TICKS = 80;
                            float phase = (tick % PERIOD_TICKS) / (float) PERIOD_TICKS;
                            int desiredRgb = java.awt.Color.HSBtoRGB(phase, 1f, 1f);
                            return List.of(new Permutation.Color(desiredRgb));
                        })
                        .build());
    }

    public LxBoard getDefaultBoard() {
        LxBoard.Builder builder = new LxBoard.Builder()
                .plugin(this)
                .name("test")
                .channels(new ChannelList())
                .group(new Group(1, List.of(1, 2)))
                .group(new Group(2, List.of(3, 4, 5)))
                .cue(new CueBuilder(5)
                    .group(1, new Yaw(90), new Pitch(70))
                    .group(2, new Effect(1), new Effect(2), new Intensity(100))
                    .times(new CueTimesBuilder()
                        .color(.25)
                        .intensity(.25)
                        .direction(1)
                        .build())
                    .build());

        for (DyeColor color : DyeColor.values()) {
            int id = (color.ordinal() * 5) + 10;
            builder.cue(new CueBuilder(id)
                    .group(1, new Yaw(90), new Pitch(-70), new Color(color.getColor().asRGB()), new Intensity(100))
                    .group(2, new Effect(1), new Effect(2))
                    .times(new CueTimesBuilder()
                        .intensity(0)
                        .color(0)
                        .direction(2)
                        .autoFollow(0)
                        .build())
                .build()
            );
            builder.cue(new CueBuilder(id + 1)
                    .group(2, new Color(color.getColor().asRGB()))
                    .times(new CueTimesBuilder()
                        .color(.25)
                        .build())
                    .build()
            );
            builder.cue(new CueBuilder(id + 2)
                    .group(2, new StopEffect(1))
                    .times(new CueTimesBuilder()
                        .direction(.5)
                        .build())
                    .build()
            );
        }

        return builder.build();
    }

}
