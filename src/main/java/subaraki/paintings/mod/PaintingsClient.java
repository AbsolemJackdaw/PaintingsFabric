package subaraki.paintings.mod;

import subaraki.paintings.network.ClientNetwork;
import subaraki.paintings.events.ClientEvents;


public class PaintingsClient implements net.fabricmc.api.ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetwork.registerClientPackets();
        ClientEvents.fixBoundingBoxEvent();
    }
}
