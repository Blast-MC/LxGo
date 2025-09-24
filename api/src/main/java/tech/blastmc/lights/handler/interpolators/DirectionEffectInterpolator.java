package tech.blastmc.lights.handler.interpolators;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import tech.blastmc.lights.cue.Permutation;
import tech.blastmc.lights.cue.Permutation.Pitch;
import tech.blastmc.lights.cue.Permutation.Yaw;
import tech.blastmc.lights.cue.SimulatedCue;
import tech.blastmc.lights.effect.Effect;
import tech.blastmc.lights.type.model.Fixture;
import tech.blastmc.lights.type.model.Mover;

public class DirectionEffectInterpolator extends EffectInterpolator {

    private int basePitchStart;
    private int basePitchTarget;

    public DirectionEffectInterpolator(Plugin plugin, int channel, Fixture fixture, Effect effect, int periodTicks, int offsetTicks, int startYaw, int startPitch, SimulatedCue simulatedCue) {
        super(plugin, channel, 0, 0, 0, fixture, effect, periodTicks, offsetTicks, startYaw, 0, simulatedCue);
        this.basePitchStart = startPitch;
        this.basePitchTarget = 0;
    }

    /** Falloff: morph base to (yaw,pitch) and collapse amplitude 1 → 0 over seconds. */
    public void beginFalloffTo(int targetYaw, int targetPitch, double seconds, SimulatedCue simulatedCue) {
        if (simulatedCue != null) {
            simulatedCue.record(channel, new Yaw(targetYaw), new Pitch(targetPitch));
            done = true;
            return;
        }

        // 1) Anchor base to its *current* value (no discontinuity)
        double sNow = (baseMorphTicks <= 0)
                ? 1.0
                : Math.min(1.0, baseMorphTick / (double) baseMorphTicks); // NOTE: no ++ here

        int currentBaseYaw   = lerpAngleDeg(baseStart,   baseTarget,   sNow);
        int currentBasePitch = lerpInt     (basePitchStart, basePitchTarget, sNow);

        this.baseStart   = currentBaseYaw;
        this.basePitchStart = currentBasePitch;
        this.baseTarget  = targetYaw;
        this.basePitchTarget= targetPitch;

        this.baseMorphTicks = Math.max(1, (int) Math.round(Math.max(0, seconds) * 20.0));
        this.baseMorphTick  = 0;

        // 2) Seed amplitude falloff from the *current* envelope without advancing it
        double envNow = peekEnvelope();
        this.ampStart = envNow;
        this.ampEnd   = 0.0;
        this.ampTicks = Math.max(1, (int) Math.round(Math.max(0, seconds) * 20.0));
        this.ampTick  = 0;
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

                // phase with per-channel offset
                int phaseTick = Math.floorMod(offsetTicks + tickCounter, periodTicks);
                // amplitude envelope
                double env = currentEnvelope();

                // base morph progress
                double s = baseMorphTicks <= 0 ? 1.0
                        : smoothstep(Math.min(1.0, ++baseMorphTick / (double) baseMorphTicks));
                int baseYaw   = lerpAngleDeg(baseStart,   baseTarget,   s);
                int basePitch = lerpInt     (basePitchStart, basePitchTarget, s);

                // sample offsets (Δyaw, Δpitch)
                int dYaw = 0, dPitch = 0;
                var perms = effect.sample(phaseTick);
                for (Permutation p : perms) {
                    if (p instanceof Permutation.Yaw y)     dYaw   = y.getValue();
                    else if (p instanceof Permutation.Pitch pt) dPitch = pt.getValue();
                }

                int outYaw   = wrapDeg(baseYaw   + (int)Math.round(env * dYaw));
                int outPitch = clampPitch(basePitch + (int)Math.round(env * dPitch));

                if (fixture instanceof Mover mover) {
                    mover.handlePositionChange(outYaw, outPitch);
                    mover.setPosition(outYaw, outPitch);
                }

                // stop after completed falloff
                if (s >= 1.0 && Math.abs(env - ampEnd) < 1e-6 && ampEnd == 0.0) {
                    cancel(); task = null; done = true;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void recordFalloffCueToSim(SimulatedCue sim, int target) { } // overridden by direction for both yaw/pitch
}