package subaraki.paintings.mod;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Motive;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import subaraki.paintings.util.Events;
import subaraki.paintings.util.PaintingUtility;
import subaraki.paintings.util.json.PaintingPackReader;

public class Paintings implements ModInitializer {

    public static final String MODID = "paintings";
    public static final ResourceLocation SERVER_PACKET = new ResourceLocation(MODID, "server_packet");
    public static final ResourceLocation CLIENT_PACKET = new ResourceLocation(MODID, "client_packet");
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final PaintingUtility UTILITY = new PaintingUtility();
    public static ModConfig config;

    /* call init here, to read json files before any event is launched. */
    static {
        new PaintingPackReader().init();
    }

    public static void registerPacketHandlers() {
        //Handles when server packet is received on server
        ServerPlayNetworking.registerGlobalReceiver(SERVER_PACKET, (server, player, handler, buf, responseSender) -> {
            String name = buf.readUtf();
            Motive type = Registry.MOTIVE.get(new ResourceLocation(name));
            int entityID = buf.readInt();
            server.execute(() -> {
                Level level = player.level;
                Entity entity = level.getEntity(entityID);
                if (entity instanceof Painting painting) {
                    UTILITY.setArt(painting, type);
                    UTILITY.updatePaintingBoundingBox(painting);
                    FriendlyByteBuf byteBuf = PacketByteBufs.create();
                    byteBuf.writeInt(entityID);
                    byteBuf.writeInt(1);
                    byteBuf.writeUtf(Registry.MOTIVE.getKey(type).toString());
                    ServerPlayNetworking.send(player, CLIENT_PACKET, byteBuf);
                }
            });
        });
    }

    @Override
    public void onInitialize() {
        PaintingPackReader.registerToMinecraft();
        AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        registerPacketHandlers();
        Events.events();
    }
}
