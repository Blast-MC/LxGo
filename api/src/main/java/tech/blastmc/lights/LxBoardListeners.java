package tech.blastmc.lights;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tech.blastmc.lights.map.Channel;
import tech.blastmc.lights.type.model.Fixture;

import java.util.UUID;

public record LxBoardListeners(LxBoard board) implements Listener {

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        for (Channel channel : this.board.getChannels())
            for (Fixture fixture : channel.getAddresses()) {
                if (!fixture.getWorld().equals(event.getWorld()))
                    return;
                fixture.handleEntityAddToWorld(uuid);
            }
    }

}
