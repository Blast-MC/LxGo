package tech.blastmc.lights.handler.interpolators;

import org.bukkit.plugin.Plugin;
import tech.blastmc.lights.LxBoard;
import tech.blastmc.lights.cue.Permutation.Intensity;
import tech.blastmc.lights.cue.SimulatedCue;
import tech.blastmc.lights.type.model.Fixture;

public class IntensityInterpolator extends Interpolator {

    Fixture fixture;

    public IntensityInterpolator(LxBoard board, int channel, int startingValue, int endingValue, double timeInSeconds, Fixture fixture, SimulatedCue simulatedCue) {
        super(board, channel, startingValue, endingValue, timeInSeconds, simulatedCue);
        this.fixture = fixture;
    }

    @Override
    public void setValue(int value) {
        fixture.handleIntensityChange(value);
        fixture.setIntensity(value);
    }

    @Override
    public void recordToSim() {
        simulatedCue.record(channel, new Intensity(endingValue));
    }
}