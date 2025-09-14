package tech.blastmc.lights.type.model;

public interface Hued {

    void setColor(int color);
    int getColor();

    void handleColorChange(int color);

}
