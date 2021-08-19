package subaraki.paintings.mod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Motive;
import net.minecraft.world.entity.decoration.Painting;
import subaraki.paintings.util.ClientReferences;


public class PaintingsClient implements net.fabricmc.api.ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //Handles when client packet is received on client
        ClientPlayNetworking.registerGlobalReceiver(Paintings.CLIENT_PACKET, (client, handler, buf, responseSender) -> {
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
        // quick hook to fix paintings not having the correct bounding box when reloading
        // a world, and thus overlapping with other newly placed paintings
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Painting painting) {
                Paintings.UTILITY.updatePaintingBoundingBox(painting);
            }
        });
    }
}
