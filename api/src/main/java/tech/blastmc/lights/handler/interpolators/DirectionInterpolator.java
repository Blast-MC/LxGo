package tech.blastmc.lights.handler.interpolators;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import tech.blastmc.lights.LxBoard;
import tech.blastmc.lights.cue.Permutation.Pitch;
import tech.blastmc.lights.cue.Permutation.Yaw;
import tech.blastmc.lights.cue.SimulatedCue;
import tech.blastmc.lights.type.model.Mover;

public class DirectionInterpolator extends Interpolator {
    Mover mover;
    int startingPitch, endingPitch;

    public DirectionInterpolator(LxBoard board, int channel, int startingYaw, int endingYaw, int startingPitch, int endingPitch, double timeInSeconds, Mover mover, SimulatedCue simulatedCue) {
        super(board, channel, startingYaw, endingYaw, timeInSeconds, simulatedCue);
        this.mover = mover;
        this.startingPitch = startingPitch;
        this.endingPitch = endingPitch;
    }

    public void start() {
        if (simulatedCue != null) {
            recordToSim();
            stop();
            return;
        }

        stop();
        done = false;

        if (timeInSeconds <= 0) {
            setDirection(endingValue, endingPitch);
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

    @Override
    public void recordToSim() {
        simulatedCue.record(channel, new Yaw(endingValue), new Pitch(endingPitch));
    }

    public void setDirection(int yaw, int pitch) {
        mover.handlePositionChange(yaw, pitch);
        mover.setPosition(yaw, pitch);
    }
}