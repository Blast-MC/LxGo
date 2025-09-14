package tech.blastmc.lights.effect;

import lombok.Data;
import tech.blastmc.lights.Permutation;

import java.util.List;
import java.util.function.Function;

@Data
public class Effect {

    int id;
    OffsetType offsetType;
    EffectType effectType;
    double durationInSeconds;
    Function<Integer, List<Permutation>> sampler;

    Effect(int id, OffsetType offsetType, EffectType effectType, double durationInSeconds, Function<Integer, List<Permutation>> sampler) {
        this.id = id;
        this.offsetType = offsetType;
        this.effectType = effectType;
        this.durationInSeconds = durationInSeconds;
        this.sampler = sampler;
    }

    public List<Permutation> sample(int tick) {
        return sampler.apply(tick);
    }

}
