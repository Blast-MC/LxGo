package tech.blastmc.lights;

import gg.projecteden.commands.models.CustomCommand;
import gg.projecteden.commands.models.annotations.Path;
import gg.projecteden.commands.models.annotations.Permission;
import gg.projecteden.commands.models.annotations.Permission.Group;
import gg.projecteden.commands.models.events.CommandEvent;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import tech.blastmc.lights.Permutation.Pitch;
import tech.blastmc.lights.Permutation.Yaw;
import tech.blastmc.lights.cue.CueBuilder;
import tech.blastmc.lights.cue.CueTimesBuilder;
import tech.blastmc.lights.map.Channel;
import tech.blastmc.lights.map.ChannelList;
import tech.blastmc.lights.type.base.SmartLight;

@Permission(Group.ADMIN)
public class LxGoCommand extends CustomCommand {

    public LxGoCommand(CommandEvent event) {
        super(event);
    }

    @Path("forceNewBoard")
    void forceNewBoard() {
        LxGo.boards.set(0, new LxBoard.Builder()
                .plugin(LxGo.getInstance())
                .name("test")
                .channels(new ChannelList())
                .cue(new CueBuilder(5)
                        .channel(1, new Pitch(60), new Yaw(50))
                        .times(new CueTimesBuilder()
                                .direction(1.5)
                                .build())
                        .build())
                .build());
    }

    @Path("spawn")
    void spawn() {
        Location location = getTargetBlockRequired().getLocation();
        BlockFace face = player().getTargetBlockFace(500);

        SmartLight light = new SmartLight();
        light.spawn(location, face);

        Channel channel = new Channel();
        if (LxGo.boards.get(0).getChannels().get(1) != null)
            channel = LxGo.boards.get(0).getChannels().get(1);
        else
            LxGo.boards.get(0).getChannels().add(channel);

        channel.setId(1);
        channel.getAddresses().add(light);
    }

    @Path("go")
    void go() {
        LxGo.boards.get(0).go();
    }

    @Path("goToCue <cue>")
    void goToCue(int cue) {
        if (cue == 0)
            LxGo.boards.get(0).goToZero();
        else
            LxGo.boards.get(0).goToCue(cue);
    }

}
