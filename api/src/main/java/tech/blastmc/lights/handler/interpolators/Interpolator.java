package tech.blastmc.lights.handler.interpolators;

import lombok.Data;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tech.blastmc.lights.cue.SimulatedCue;

@Data
public abstract class Interpolator {

    static int next_id = 0;

    int id;
    boolean done;
    int channel;
    Plugin plugin;
    int startingValue, endingValue;
    double timeInSeconds;
    BukkitTask task;
    SimulatedCue simulatedCue;

    public Interpolator(Plugin plugin, int channel, int startingValue, int endingValue, double timeInSeconds, SimulatedCue simulatedCue) {
        this.id = next_id++;
        this.plugin = plugin;
        this.channel = channel;
        this.startingValue = startingValue;
        this.endingValue = endingValue;
        this.timeInSeconds = timeInSeconds;
        this.simulatedCue = simulatedCue;
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
        }
        done = true;
    }

    public abstract void setValue(int value);

    public abstract void recordToSim();

}
