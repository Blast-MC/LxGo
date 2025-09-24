package tech.blastmc.lights.type.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.UUID;

public abstract class Fixture implements ConfigurationSerializable {

    @Setter
    @Getter
    int intensity;

    public abstract void handleIntensityChange(int intensity);

    public abstract void spawn(Location location, BlockFace face);

    public abstract World getWorld();

    public abstract void handleEntityAddToWorld(UUID uuid);

}
