package tech.blastmc.lights;

import gg.projecteden.commands.models.CustomCommand;
import gg.projecteden.commands.models.annotations.Path;
import gg.projecteden.commands.models.annotations.Permission;
import gg.projecteden.commands.models.annotations.Permission.Group;
import gg.projecteden.commands.models.events.CommandEvent;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import tech.blastmc.lights.cue.CueTimes;
import tech.blastmc.lights.cue.CueTimesBuilder;
import tech.blastmc.lights.map.Channel;
import tech.blastmc.lights.type.base.SmartLight;

@Permission(Group.ADMIN)
public class LxGoCommand extends CustomCommand {

    public LxGoCommand(CommandEvent event) {
        super(event);
    }

    @Path("forceNewBoard")
    void forceNewBoard() {
        LxGo.boards.set(0, LxGo.getInstance().getDefaultBoard());
    }

    @Path("updateCuesFromDefault")
    void updateCuesFromDefault() {
        LxGo.boards.get(0).setCues(LxGo.getInstance().getDefaultBoard().getCues());
        LxGo.boards.get(0).setGroups(LxGo.getInstance().getDefaultBoard().getGroups());
    }

    @Path("spawn")
    void spawn() {
        Location location = getTargetBlockRequired().getLocation();
        BlockFace face = player().getTargetBlockFace(500);

        SmartLight light = new SmartLight();
        light.spawn(location, face);

        Channel channel = new Channel();
        channel.setId(LxGo.boards.get(0).getChannels().size() + 1);
        channel.getAddresses().add(light);

        LxGo.boards.get(0).getChannels().add(channel);
    }

    @Path("go")
    void go() {
        LxGo.boards.get(0).go();
    }

    @Path("goToCue <cue>")
    void goToCue(int cue) {
        LxGo.boards.get(0).goToCue(cue);
    }

    @Path("bp <color>")
    void bp (DyeColor color) {
        int cue = (color.ordinal() * 5) + 10;
        LxGo.boards.get(0).goToCue(cue, new CueTimesBuilder()
                .intensity(0)
                .color(0)
                .direction(1.25)
                .autoFollow(0)
                .build());
    }

    @Path("debug")
    void debug() {
        LxGo.boards.get(0).setDebug(!LxGo.boards.get(0).isDebug());
        send(PREFIX + "Debug " + (LxGo.boards.get(0).isDebug() ? "&eenabled" : "&cdisabled"));
    }

}
