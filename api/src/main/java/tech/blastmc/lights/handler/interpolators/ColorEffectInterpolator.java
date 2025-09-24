package tech.blastmc.lights.handler.interpolators;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import tech.blastmc.lights.cue.Permutation;
import tech.blastmc.lights.cue.Permutation.Color;
import tech.blastmc.lights.cue.SimulatedCue;
import tech.blastmc.lights.effect.Effect;
import tech.blastmc.lights.type.model.Fixture;
import tech.blastmc.lights.type.model.Hued;

import java.util.Objects;

public class ColorEffectInterpolator extends EffectInterpolator {

    public ColorEffectInterpolator(Plugin plugin, int channel, Fixture fixture, Effect effect, int periodTicks, int offsetTicks, int startRGB, SimulatedCue simulatedCue) {
        super(plugin, channel, 0, 0, 0, fixture, effect, periodTicks, offsetTicks, startRGB, startRGB, simulatedCue);
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
                int baseRGB = lerpRGB(baseStart, baseTarget, s);

                // envelope
                double env = currentEnvelope();

                // effect absolute color (fallback to base if not provided)
                Integer effRGB = null;
                var perms = effect.sample(phaseTick);
                for (Permutation p : perms) {
                    if (p instanceof Permutation.Color c) { effRGB = c.getValue(); break; }
                }
                int effectRGB = effRGB != null ? effRGB : baseRGB;

                // dampen: mix base with effect by env
                int outRGB = lerpRGB(baseRGB, effectRGB, env);

                if (!Objects.equals(lastSent, outRGB)) {
                    if (fixture instanceof Hued hued) {
                        hued.handleColorChange(outRGB);
                        hued.setColor(outRGB);
                    }
                    lastSent = outRGB;
                }

                if (s >= 1.0 && Math.abs(env - ampEnd) < 1e-6 && ampEnd == 0.0) {
                    cancel(); task = null; done = true;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static int lerpRGB(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int)Math.round(ar + (br - ar) * t);
        int g = (int)Math.round(ag + (bg - ag) * t);
        int bl= (int)Math.round(ab + (bb - ab) * t);
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (bl & 0xFF);
    }

    @Override
    public void recordFalloffCueToSim(SimulatedCue sim, int target) {
        sim.record(channel, new Color(target));
    }
}