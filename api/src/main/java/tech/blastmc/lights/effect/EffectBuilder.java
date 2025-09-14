package tech.blastmc.lights.effect;

import tech.blastmc.lights.Permutation;

import java.util.List;
import java.util.function.Function;

public class EffectBuilder {

    int id;
    OffsetType offsetType;
    EffectType effectType;
    double durationInSeconds;
    Function<Integer, List<Permutation>> sampler;

    public EffectBuilder id(int id) {
        this.id = id;
        return this;
    }

    public EffectBuilder offsetType(OffsetType offsetType) {
        this.offsetType = offsetType;
        return this;
    }

    public EffectBuilder effectType(EffectType effectType) {
        this.effectType = effectType;
        return this;
    }

    public EffectBuilder durationInSeconds(double durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
        return this;
    }

    public EffectBuilder sampler(Function<Integer, List<Permutation>> sampler) {
        this.sampler = sampler;
        return this;
    }

    public Effect build() {
        return new Effect(id, offsetType, effectType, durationInSeconds, sampler);
    }

}
