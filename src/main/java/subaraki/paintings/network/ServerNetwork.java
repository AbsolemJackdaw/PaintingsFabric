package subaraki.paintings.network;

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

public class ServerNetwork {

    public static final ResourceLocation SERVER_PACKET = new ResourceLocation(Paintings.MODID, "server_packet");

    public static void registerServerPackets() {
        //Handles when server packet is received on server
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
                    ServerPlayNetworking.send(player, ClientNetwork.CLIENT_PACKET, byteBuf);
                }
            });
        });
    }
}
