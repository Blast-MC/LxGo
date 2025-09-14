package tech.blastmc.lights.cue;

public class CueTimesBuilder {

    private double intensity = 5;
    private double direction = 5;
    private double color = 5;
    private double autoFollow = -1;

    public CueTimesBuilder() {
        this(5);
    }

    public CueTimesBuilder(int all) {
        this.intensity = all;
        this.direction = all;
        this.color = all;
    }

    public CueTimesBuilder intensity(double intensity) {
        this.intensity = intensity;
        return this;
    }

    public CueTimesBuilder direction(double direction) {
        this.direction = direction;
        return this;
    }

    public CueTimesBuilder color(double color) {
        this.color = color;
        return this;
    }

    public CueTimesBuilder autoFollow(double autoFollow) {
        this.autoFollow = autoFollow;
        return this;
    }

    public CueTimes build() {
        return new CueTimes(intensity, direction, color, autoFollow);
    }

}
