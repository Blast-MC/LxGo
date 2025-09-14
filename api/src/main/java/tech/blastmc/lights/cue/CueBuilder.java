package tech.blastmc.lights.cue;

import tech.blastmc.lights.Permutation;
import tech.blastmc.lights.map.Channel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CueBuilder {

    private int id;
    private CueTimes times;
    private Map<Integer, List<Permutation>> channelDiffs;
    private Map<Integer, List<Permutation>> groupDiffs;

    public CueBuilder(int id) {
        this.id = id;
        this.times = new CueTimesBuilder(5).build();
        this.channelDiffs = new HashMap<>();
        this.groupDiffs = new HashMap<>();
    }

    public CueBuilder times(CueTimes times) {
        this.times = times;
        return this;
    }

    public CueBuilder diffs(Map<Integer, List<Permutation>> diffs) {
        this.channelDiffs = diffs;
        return this;
    }

    public CueBuilder channel(int channel, Permutation... permutations) {
        this.channelDiffs.put(channel, Arrays.stream(permutations).toList());
        return this;
    }

    public CueBuilder group(int group, Permutation... permutations) {
        this.groupDiffs.put(group, Arrays.stream(permutations).toList());
        return this;
    }

    public Cue build() {
        return new Cue(id, times, channelDiffs, groupDiffs);
    }

}
