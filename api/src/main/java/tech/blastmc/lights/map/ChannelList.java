package tech.blastmc.lights.map;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class ChannelList extends ArrayList<Channel> {

    public ChannelList(List<Channel> channels) {
        super(channels);
    }

    @Override
    public Channel get(int index) {
        for (Channel ch : this)
            if (ch.id == index)
                return ch;
        return null;
    }

}
