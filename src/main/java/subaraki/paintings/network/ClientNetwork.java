package subaraki.paintings.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Motive;
import net.minecraft.world.entity.decoration.Painting;
import subaraki.paintings.mod.Paintings;
import subaraki.paintings.util.ClientReferences;

public class ClientNetwork {

    public static final ResourceLocation CLIENT_PACKET = new ResourceLocation(Paintings.MODID, "client_packet");


    public static void registerClientPackets(){
        //Handles when client packet is received on client
        ClientPlayNetworking.registerGlobalReceiver(CLIENT_PACKET, (client, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            String[] resLocNames = new String[buf.readInt()];
            for (int i = 0; i < resLocNames.length; i++) {
                resLocNames[i] = buf.readUtf();
            }
            client.execute(() -> {
                if (resLocNames.length == 1) // we know what painting to set
                {
                    Entity entity = ClientReferences.getClientPlayer().level.getEntity(entityId);
                    if (entity instanceof Painting painting) {
                        Motive type = Registry.MOTIVE.get(new ResourceLocation(resLocNames[0]));
                        Paintings.UTILITY.setArt(painting, type);
                        Paintings.UTILITY.updatePaintingBoundingBox(painting);
                    }
                } else // we need to open the painting gui to select a painting
                {
                    Motive[] types = new Motive[resLocNames.length];
                    int dex = 0;
                    for (String path : resLocNames) {
                        types[dex++] = Registry.MOTIVE.get(new ResourceLocation(path));
                    }
                    ClientReferences.openPaintingScreen(types, entityId);
                }
            });
        });
    }
}
