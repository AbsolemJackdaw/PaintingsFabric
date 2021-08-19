package subaraki.paintings.packet;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Motive;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.Level;
import subaraki.paintings.mod.Paintings;
import subaraki.paintings.util.ClientReferences;

public class NetworkHandler {

    public static final ResourceLocation CLIENT_PACKET = new ResourceLocation(Paintings.MODID, "client_packet");
    public static final ResourceLocation SERVER_PACKET = new ResourceLocation(Paintings.MODID, "server_packet");

    public static void registerPacketHandlers() {
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
        ServerPlayNetworking.registerGlobalReceiver(SERVER_PACKET, (server, player, handler, buf, responseSender) -> {
            String name = buf.readUtf();
            Motive type = Registry.MOTIVE.get(new ResourceLocation(name));
            int entityID = buf.readInt();
            server.execute(() -> {
                Level level = player.level;
                Entity entity = level.getEntity(entityID);
                if (entity instanceof Painting painting) {
                    Paintings.UTILITY.setArt(painting, type);
                    Paintings.UTILITY.updatePaintingBoundingBox(painting);
                    FriendlyByteBuf byteBuf = PacketByteBufs.create();
                    byteBuf.writeInt(entityID);
                    byteBuf.writeInt(1);
                    byteBuf.writeUtf(Registry.MOTIVE.getKey(type).toString());
                    ServerPlayNetworking.send(player, NetworkHandler.CLIENT_PACKET, byteBuf);
                }
            });
        });
    }
}
