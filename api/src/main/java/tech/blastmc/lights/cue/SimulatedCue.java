package tech.blastmc.lights.cue;

import tech.blastmc.lights.LxBoard;
import tech.blastmc.lights.cue.Permutation.Color;
import tech.blastmc.lights.cue.Permutation.Effect;
import tech.blastmc.lights.cue.Permutation.Intensity;
import tech.blastmc.lights.cue.Permutation.Pitch;
import tech.blastmc.lights.cue.Permutation.StopEffect;
import tech.blastmc.lights.cue.Permutation.Yaw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulatedCue {

    private final LxBoard board;
    private final Map<Integer, List<Permutation>> permutations = new HashMap<>();

    public SimulatedCue(LxBoard board) {
        this.board = board;
    }

    public void record(int channel, Permutation... perms) {
        List<Permutation> list = permutations.computeIfAbsent(channel, ch -> new ArrayList<>());

        for (Permutation perm : perms) {
            if (perm == null)
                continue;

            board.debug("Record perm to sim: " + perm);

            List<Class<? extends Permutation>> classes = new ArrayList<>();
            if (perm instanceof Effect effect) {
                tech.blastmc.lights.effect.Effect ef = board.getEffectRegistry().get(effect.getValue());
                switch (ef.getEffectType()) {
                    case DIRECTION -> { classes.add(Yaw.class); classes.add(Pitch.class); }
                    case COLOR -> classes.add(Color.class);
                    case INTENSITY -> classes.add(Intensity.class);
                };
            }
            else if (perm instanceof StopEffect stopEffect) {
                int value = stopEffect.getValue();
                list.removeIf(p -> p instanceof Effect effect && effect.getValue() == value);
            }
            else
                classes.add(perm.getClass());
            list.removeIf(p -> classes.contains(p.getClass()));

            list.add(perm);
        }
    }

    public int getValue(int channel, Class<? extends Permutation> clazz) {
        if (!permutations.containsKey(channel))
            return 0;
        for (Permutation p : permutations.get(channel)) {
            if (p.getClass() == clazz)
                return p.getValue();
        }
        return 0;
    }

    public Cue toCue() {
        CueBuilder builder = new CueBuilder(-1);
        for (int channel : permutations.keySet()) {
            Permutation[] perms = permutations.get(channel).toArray(new Permutation[0]);
            builder.channel(channel, perms);
        }
        return builder.build();
    }

}
