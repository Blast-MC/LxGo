package tech.blastmc.lights.effect;

import java.util.ArrayList;
import java.util.List;

public class EffectRegistry {

    private final List<Effect> effects = new ArrayList<>();

    public void register(Effect effect) {
        effects.add(effect);
    }

    public Effect get(int effect) {
        for (Effect e : effects)
            if (e.getId() == effect)
                return e;
        return null;
    }

}
