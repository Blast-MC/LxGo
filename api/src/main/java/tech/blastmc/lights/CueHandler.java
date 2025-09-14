package tech.blastmc.lights;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tech.blastmc.lights.Permutation.Color;
import tech.blastmc.lights.Permutation.Intensity;
import tech.blastmc.lights.Permutation.Pitch;
import tech.blastmc.lights.Permutation.Yaw;
import tech.blastmc.lights.cue.Cue;
import tech.blastmc.lights.cue.CueTimes;
import tech.blastmc.lights.map.Channel;
import tech.blastmc.lights.map.Group;
import tech.blastmc.lights.type.model.Fixture;
import tech.blastmc.lights.type.model.Hued;
import tech.blastmc.lights.type.model.Mover;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class CueHandler {

    private final Plugin plugin;
    private final LxBoard board;
    private final Cue cue;
    private final CueTimes times;
    private final List<Interpolator> interpolators = new ArrayList<>();

    public CueHandler(Plugin plugin, LxBoard board, Cue cue, CueTimes times) {
        this.plugin = plugin;
        this.board = board;
        this.cue = cue;
        this.times = times;
    }

    public boolean isDone() {
        return interpolators.isEmpty() || interpolators.stream().noneMatch(interpolator -> !interpolator.done);
    }

    public void start() {
        for (Entry<Integer, List<Permutation>> entry : cue.getGroupDiffs().entrySet()) {
            for (int channel : board.getGroups().get(entry.getKey()).getChannels()) {
                handleChannelPermutations(channel, entry.getValue());
            }
        }

        for (Entry<Integer, List<Permutation>> entry : cue.getChannelDiffs().entrySet()) {
            handleChannelPermutations(entry.getKey(), entry.getValue());
        }

        interpolators.forEach(Interpolator::start);

        if (times.getAutoFollow() != -1)
            board.go();
    }

    public void stop() {
        interpolators.forEach(Interpolator::stop);
    }

    public void stopConflicting(Cue newCue) {
        Set<Integer> colorChannels   = new HashSet<>();
        Set<Integer> intensityChannels = new HashSet<>();
        Set<Integer> directionChannels = new HashSet<>();

        for (Entry<Integer, List<Permutation>> entry : newCue.getChannelDiffs().entrySet()) {
            int channelId = entry.getKey();
            for (Permutation perm : entry.getValue()) {
                if (perm instanceof Permutation.Color)
                    colorChannels.add(channelId);
                if (perm instanceof Permutation.Intensity)
                    intensityChannels.add(channelId);
                if (perm instanceof Permutation.Yaw || perm instanceof Permutation.Pitch)
                    directionChannels.add(channelId);
            }
        }

        for (Entry<Integer, List<Permutation>> entry : newCue.getGroupDiffs().entrySet()) {
            Group group = board.getGroups().get(entry.getKey());
            if (group == null) continue;
            List<Integer> groupChannels = group.getChannels();
            for (Permutation perm : entry.getValue()) {
                if (perm instanceof Permutation.Color)
                    colorChannels.addAll(groupChannels);
                if (perm instanceof Permutation.Intensity)
                    intensityChannels.addAll(groupChannels);
                if (perm instanceof Permutation.Yaw || perm instanceof Permutation.Pitch)
                    directionChannels.addAll(groupChannels);
            }
        }

        interpolators.stream()
                .filter(inter -> {
                    if (inter instanceof ColorInterpolator)
                        return colorChannels.contains(inter.channel);
                    else if (inter instanceof IntensityInterpolator)
                        return intensityChannels.contains(inter.channel);
                    else if (inter instanceof MoverInterpolator)
                        return directionChannels.contains(inter.channel);
                    return false;
                })
                .forEach(Interpolator::stop);
    }

    public void handleChannelPermutations(int id, List<Permutation> permutations) {
        Channel channel = board.getChannels().get(id);
        if (channel == null)
            return;

        Integer targetPitch = null;
        Integer targetYaw = null;
        Integer targetColor = null;
        Integer targetIntensity = null;

        boolean hasDirection = false;

        for (Permutation permutation : permutations) {
            if (permutation instanceof Yaw) {
                hasDirection = true;
                targetYaw = permutation.getValue();
            }
            if (permutation instanceof Pitch) {
                hasDirection = true;
                targetPitch = permutation.getValue();
            }
            if (permutation instanceof Color)
                targetColor = permutation.getValue();
            if (permutation instanceof Intensity)
                targetIntensity = permutation.getValue();
        }

        for (Fixture fixture : channel.getAddresses()) {
            if (targetIntensity != null) {
                double timeInSeconds = times.getIntensity();
                interpolators.add(new IntensityInterpolator(plugin, id, fixture.getIntensity(), targetIntensity, timeInSeconds, fixture));
            }

            if (hasDirection) {
                if (fixture instanceof Mover mover) {
                    double timeInSeconds = times.getDirection();
                    int fixtureTargetYaw = targetYaw == null ? mover.getYaw() : targetYaw;
                    int fixtureTargetPitch = targetPitch == null ? mover.getPitch() : targetPitch;
                    interpolators.add(new MoverInterpolator(plugin, id, mover.getYaw(), fixtureTargetYaw, mover.getPitch(), fixtureTargetPitch, timeInSeconds, mover));
                }
            }

            if (targetColor != null) {
                if (fixture instanceof Hued hued) {
                    double timeInSeconds = times.getColor();
                    interpolators.add(new ColorInterpolator(plugin, id, hued.getColor(), targetColor, timeInSeconds, hued));
                }
            }
        }
    }

    public static abstract class Interpolator {

        boolean done;
        int channel;
        Plugin plugin;
        int startingValue, endingValue;
        double timeInSeconds;
        BukkitTask task;

        public Interpolator(Plugin plugin, int channel, int startingValue, int endingValue, double timeInSeconds) {
            this.plugin = plugin;
            this.channel = channel;
            this.startingValue = startingValue;
            this.endingValue = endingValue;
            this.timeInSeconds = timeInSeconds;
        }

        public void start() {
            stop();

            if (timeInSeconds <= 0) {
                setValue(endingValue);
                return;
            }

            final int durationTicks = Math.max(1, (int) Math.round(timeInSeconds * 20.0));
            final int delta = endingValue - startingValue;

            task = new BukkitRunnable() {
                int tick = 0;
                int lastSent = Integer.MIN_VALUE;

                @Override
                public void run() {
                    tick++;
                    double t = Math.min(1.0, (double) tick / durationTicks);
                    int v = (int) Math.round(startingValue + delta * t);

                    if (v != lastSent) {
                        setValue(v);
                        lastSent = v;
                    }

                    if (t >= 1.0) {
                        cancel();
                        task = null;
                        done = true;
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        public void stop() {
            if (task != null) {
                task.cancel();
                task = null;
                done = true;
            }
        }

        public abstract void setValue(int value);

    }

    private static class IntensityInterpolator extends Interpolator {
        Fixture fixture;

        private IntensityInterpolator(Plugin plugin, int channel, int startingValue, int endingValue, double timeInSeconds, Fixture fixture) {
            super(plugin, channel, startingValue, endingValue, timeInSeconds);
            this.fixture = fixture;
        }

        @Override
        public void setValue(int value) {
            fixture.handleIntensityChange(value);
            fixture.setIntensity(value);
        }
    }

    private static class ColorInterpolator extends Interpolator {
        Hued hued;

        private ColorInterpolator(Plugin plugin, int channel, int startingValue, int endingValue, double timeInSeconds, Hued hued) {
            super(plugin, channel, startingValue, endingValue, timeInSeconds);
            this.hued = hued;
        }

        @Override
        public void setValue(int value) {
            hued.handleColorChange(value);
            hued.setColor(value);
        }
    }

    private static class MoverInterpolator extends Interpolator {
        Mover mover;
        int startingPitch, endingPitch;

        private MoverInterpolator(Plugin plugin, int channel, int startingYaw, int endingYaw, int startingPitch, int endingPitch, double timeInSeconds, Mover mover) {
            super(plugin, channel, startingYaw, endingYaw, timeInSeconds);
            this.mover = mover;
            this.startingPitch = startingPitch;
            this.endingPitch = endingPitch;
        }

        public void start() {
            stop();

            if (timeInSeconds <= 0) {
                setValue(endingValue);
                return;
            }

            final int durationTicks = Math.max(1, (int) Math.round(timeInSeconds * 20.0));
            final int deltaYaw = endingValue - startingValue;
            final int deltaPitch = endingPitch - startingPitch;

            task = new BukkitRunnable() {
                int tick = 0;
                int lastSentYaw = Integer.MIN_VALUE;
                int lastSentPitch = Integer.MIN_VALUE;

                @Override
                public void run() {
                    tick++;
                    double t = Math.min(1.0, (double) tick / durationTicks);
                    int yaw = (int) Math.round(startingValue + deltaYaw * t);
                    int pitch = (int) Math.round(startingPitch + deltaPitch * t);

                    boolean update = false;

                    if (yaw != lastSentYaw) {
                        update = true;
                        lastSentYaw = yaw;
                    }

                    if (pitch != lastSentPitch) {
                        update = true;
                        lastSentPitch = pitch;
                    }

                    if (update)
                        setDirection(yaw, pitch);

                    if (t >= 1.0) {
                        cancel();
                        task = null;
                        done = true;
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        @Override
        public void setValue(int value) { }

        public void setDirection(int yaw, int pitch) {
            mover.handlePositionChange(yaw, pitch);
            mover.setPosition(yaw, pitch);
        }

    }
}
