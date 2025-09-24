package tech.blastmc.lights.handler.interpolators;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import tech.blastmc.lights.cue.Permutation;
import tech.blastmc.lights.cue.Permutation.Intensity;
import tech.blastmc.lights.cue.SimulatedCue;
import tech.blastmc.lights.effect.Effect;
import tech.blastmc.lights.type.model.Fixture;

import java.util.Objects;

public class IntensityEffectInterpolator extends EffectInterpolator {

    public IntensityEffectInterpolator(Plugin plugin, int channel, Fixture fixture, Effect effect, int periodTicks, int offsetTicks, int startIntensity, SimulatedCue simulatedCue) {
        super(plugin, channel, 0, 0, 0, fixture, effect, periodTicks, offsetTicks, startIntensity, startIntensity, simulatedCue);
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

        task = new BukkitRunnable() {
            @Override public void run() {
                tickCounter++;

                int phaseTick = Math.floorMod(offsetTicks + tickCounter, periodTicks);

                // base morph
                double s = baseMorphTicks <= 0 ? 1.0
                        : smoothstep(Math.min(1.0, ++baseMorphTick / (double) baseMorphTicks));
                int base = lerpInt(baseStart, baseTarget, s);

                // envelope
                double env = currentEnvelope();

                // effect absolute value (fallback to base if not provided)
                Integer effVal = null;
                var perms = effect.sample(phaseTick);
                for (Permutation p : perms) {
                    if (p instanceof Permutation.Intensity i) { effVal = i.getValue(); break; }
                }
                int effectVal = effVal != null ? clamp01_100(effVal) : base;

                // dampen: mix base with effect by env
                int out = lerpInt(base, effectVal, env);
                if (!Objects.equals(lastSent, out)) {
                    fixture.handleIntensityChange(out);
                    fixture.setIntensity(out);
                    lastSent = out;
                }

                if (s >= 1.0 && Math.abs(env - ampEnd) < 1e-6 && ampEnd == 0.0) {
                    cancel(); task = null; done = true;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        done = true;
    }

    @Override
    public void recordFalloffCueToSim(SimulatedCue sim, int target) {
        sim.record(channel, new Intensity(target));
    }
}