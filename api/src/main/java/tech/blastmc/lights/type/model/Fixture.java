package tech.blastmc.lights.type.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public abstract class Fixture implements ConfigurationSerializable {

    @Setter
    @Getter
    int intensity;

    public abstract void handleIntensityChange(int intensity);

    public abstract void spawn(Location location, BlockFace face);

}
