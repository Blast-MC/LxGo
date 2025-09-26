package tech.blastmc.lights.handler.interpolators;

import lombok.Getter;
import org.bukkit.plugin.Plugin;
import tech.blastmc.lights.LxBoard;
import tech.blastmc.lights.cue.Permutation;
import tech.blastmc.lights.cue.SimulatedCue;
import tech.blastmc.lights.effect.Effect;
import tech.blastmc.lights.type.model.Fixture;

public abstract class EffectInterpolator extends Interpolator {

    protected final Fixture fixture;
    @Getter
    protected final Effect effect;
    protected final int periodTicks;
    protected final int offsetTicks;

    // base morph (old → target)
    protected int baseStart, baseTarget;
    protected int baseMorphTicks = 0, baseMorphTick = 0;

    protected int tickCounter = 0;
    protected Integer lastSent = null;

    protected double ampStart = 1.0, ampEnd = 1.0;
    protected int ampTicks = 0, ampTick = 0;

    public EffectInterpolator(LxBoard board, int channel, int startingValue, int endingValue, double timeInSeconds,
                              Fixture fixture, Effect effect, int periodTicks, int offsetTicks, int baseStart, int baseTarget, SimulatedCue simulatedCue) {
        super(board, channel, startingValue, endingValue, timeInSeconds, simulatedCue);
        this.fixture = fixture;
        this.effect = effect;
        this.periodTicks = Math.max(1, periodTicks);
        this.offsetTicks = Math.floorMod(offsetTicks, this.periodTicks);
        this.baseStart = baseStart;
        this.baseTarget = baseTarget;
    }

    /** Expand 0 → 1 over seconds. */
    public void beginExpand(double seconds) {
        this.ampStart = 0.0;
        this.ampEnd = 1.0;
        this.ampTicks = Math.max(1, (int)Math.round(Math.max(0, seconds) * 20.0));
        this.ampTick = 0;
        this.baseMorphTicks = Math.max(1, (int)Math.round(Math.max(0, seconds) * 20.0));
        this.baseMorphTick = 0;
    }

    public void beginFalloffTo(int target, double seconds, SimulatedCue simulatedCue) {
        board.debug("Falloff: 1");
        if (simulatedCue != null) {
            recordFalloffCueToSim(simulatedCue, target);
            if (this.simulatedCue != null)
                done = true;
            return;
        }
        board.debug("Falloff: 2");

        // 1) Anchor base to its *current* value (no discontinuity)
        double sNow = (baseMorphTicks <= 0)
                ? 1.0
                : Math.min(1.0, baseMorphTick / (double) baseMorphTicks);

        int currentBaseYaw = lerpAngleDeg(baseStart, baseTarget, sNow);

        this.baseStart   = currentBaseYaw;
        this.baseTarget  = target;

        this.baseMorphTicks = Math.max(1, (int) Math.round(Math.max(0, seconds) * 20.0));
        this.baseMorphTick  = 0;

        // 2) Seed amplitude falloff from the *current* envelope without advancing it
        double envNow = peekEnvelope();
        this.ampStart = envNow;
        this.ampEnd   = 0.0;
        this.ampTicks = Math.max(1, (int) Math.round(Math.max(0, seconds) * 20.0));
        this.ampTick  = 0;
    }

    protected double currentEnvelope() {
        if (ampTicks <= 0) return ampEnd;
        double t = Math.min(1.0, ++ampTick / (double) ampTicks);
        return lerpDouble(ampStart, ampEnd, smoothstep(t));
    }

    protected double peekEnvelope() {
        if (ampTicks <= 0) return ampEnd;
        double t = Math.min(1.0, ampTick / (double) ampTicks);
        return lerpDouble(ampStart, ampEnd, smoothstep(t));
    }

    protected static double smoothstep(double t){ return t*t*(3-2*t); }
    protected static int lerpInt(int a, int b, double t){ return (int)Math.round(a + (b - a) * t); }
    protected static double lerpDouble(double a, double b, double t){ return a + (b - a) * t; }
    protected static int wrapDeg(int d){ int x=((d%360)+360)%360; return x; }
    protected static int clampPitch(int p){ return Math.max(-90, Math.min(90, p)); }
    protected static int clamp01_100(int v){ return Math.max(0, Math.min(100, v)); }
    protected static int lerpAngleDeg(int a, int b, double t) {
        int da = ((b - a + 540) % 360) - 180;
        return wrapDeg(a + (int)Math.round(da * t));
    }

    @Override
    public void setValue(int value) { }

    @Override
    public void recordToSim() {
        simulatedCue.record(channel, new Permutation.Effect(effect.getId(), offsetTicks));
    }

    public abstract void recordFalloffCueToSim(SimulatedCue sim, int target);

}
