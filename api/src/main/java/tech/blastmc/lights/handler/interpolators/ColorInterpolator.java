package tech.blastmc.lights.handler.interpolators;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import tech.blastmc.lights.cue.Permutation.Color;
import tech.blastmc.lights.cue.SimulatedCue;
import tech.blastmc.lights.type.model.Hued;

public class ColorInterpolator extends Interpolator {

    Hued hued;

    public ColorInterpolator(Plugin plugin, int channel, int startingValue, int endingValue, double timeInSeconds, Hued hued, SimulatedCue simulatedCue) {
        super(plugin, channel, startingValue, endingValue, timeInSeconds, simulatedCue);
        this.hued = hued;
    }

    @Override
    public void start() {
        if (simulatedCue != null) {
            recordToSim();
            stop();
            return;
        }

        stop();
        done = false;

        if (timeInSeconds <= 0) {
            setValue(endingValue);
            return;
        }

        final int durationTicks = Math.max(1, (int) Math.round(timeInSeconds * 20.0));

        task = new BukkitRunnable() {
            int tick = 0;
            int lastSent = Integer.MIN_VALUE;

            @Override
            public void run() {
                tick++;
                double t = Math.min(1.0, (double) tick / durationTicks);
                int v = lerpRGB(startingValue, endingValue, t);

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

    @Override
    public void setValue(int value) {
        hued.handleColorChange(value);
        hued.setColor(value);
    }

    @Override
    public void recordToSim() {
        simulatedCue.record(channel, new Color(endingValue));
    }

    private int lerpRGB(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int)Math.round(ar + (br - ar) * t);
        int g = (int)Math.round(ag + (bg - ag) * t);
        int bl= (int)Math.round(ab + (bb - ab) * t);
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (bl & 0xFF);
    }

}