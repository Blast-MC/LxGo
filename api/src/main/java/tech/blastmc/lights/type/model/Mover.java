package tech.blastmc.lights.type.model;

public interface Mover {

    void setPosition(int yaw, int pitch);
    int getYaw();
    int getPitch();

    void handlePositionChange(int yaw, int pitch);

}
