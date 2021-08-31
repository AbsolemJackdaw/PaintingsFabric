package subaraki.paintings.util.json;

import com.google.gson.*;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.Motive;
import subaraki.paintings.mod.Paintings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PaintingPackReader {

    private static ArrayList<PaintingEntry> addedPaintings = new ArrayList<>();

    public static void registerToMinecraft() {
        try {
            for (PaintingEntry entry : addedPaintings) {
                ResourceLocation name = new ResourceLocation(Paintings.MODID, entry.getRefName());
                Registry.register(Registry.MOTIVE, name, new Motive(entry.getSizeX(), entry.getSizeY()));
                Paintings.LOGGER.info("Registered painting " + name);
            }
        } catch (ResourceLocationException e) {
            Paintings.LOGGER.error("Skipping. Found Error: {}", e.getMessage());
        }
    }

    /**
     * called once on mod class initialization. the loadFromJson called in here reads
     * json files directly out of a directory.
     */
    public PaintingPackReader init() {

        Paintings.LOGGER.info("loading json file and contents for paintings.");
        loadFromJson();

        return this;
    }

    private void loadFromJson() {

        // duplicate the gbase paintings template to our custom folder
        try {
            Paintings.LOGGER.info("Copying Over Base Template to /paintings");
            Path dir = Paths.get("./paintings");

            if (!Files.exists(dir)) {

                Files.createDirectory(dir);
                Files.copy(getClass().getResourceAsStream("/assets/paintings/paintings.json"), dir.resolve("paintings.json"));
                // copyJsonToFolder
            }
        } catch (IOException e) {
            Paintings.LOGGER.warn("************************************");
            Paintings.LOGGER.warn("!*!*!*!*!");
            Paintings.LOGGER.error("Copying Base Template Failed");
            Paintings.LOGGER.error(e.getMessage());
            Paintings.LOGGER.warn("!*!*!*!*!");
            Paintings.LOGGER.warn("************************************");
        }

        // read out all resourcepacks, exclusively in zips,
        // to look for any other pack
        // and copy their json file over
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get("./resourcepacks"))) {
            Paintings.LOGGER.info("Reading out ResourcePacks to find painting related json files");

            for (Path resourcePackPath : directoryStream) {
                Paintings.LOGGER.info("Reading `{}`", resourcePackPath.getFileName().toString());
                try {
                    URI jarUri = new URI("jar:%s".formatted(resourcePackPath.toUri().getScheme()), resourcePackPath.toUri().getPath(), null);

                    try (FileSystem system = initFileSystem(jarUri)) {
                        Iterator<Path> resourcePacks = Files.walk(system.getPath("/")).iterator();
                        while (resourcePacks.hasNext()) {
                            boolean copyOver = false;

                            Path next = resourcePacks.next();
                            if (Files.isRegularFile(next) && next.toString().endsWith("json")) {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(next)))) {

                                    Gson gson = new GsonBuilder().create();
                                    JsonElement je = gson.fromJson(reader, JsonElement.class);
                                    JsonObject json = je.getAsJsonObject();

                                    if (json.has("paintings")) {
                                        copyOver = true;
                                        Paintings.LOGGER.info("Validated: {}", next.getFileName().toString());
                                    }
                                } catch (Exception e) {
                                    Paintings.LOGGER.warn("************************************");
                                    Paintings.LOGGER.error("`{}` Errored. Skipping.", next.getFileName().toString());
                                    Paintings.LOGGER.error(e.getMessage());
                                    Paintings.LOGGER.warn("************************************");
                                }
                            }

                            if (copyOver) {
                                Path fileToCopy = Path.of("./paintings").resolve(next.getFileName().toString());
                                if (Files.notExists(fileToCopy))
                                    Files.copy(next, fileToCopy);
                            }

                        }
                    } catch (Exception e) {
                        Paintings.LOGGER.warn("************************************");
                        Paintings.LOGGER.error("Invalid ResourcePack  {}", resourcePackPath.getFileName().toString());
                        Paintings.LOGGER.error(e.getMessage());
                        Paintings.LOGGER.warn("************************************");

                    }
                } catch (URISyntaxException e) {
                    Paintings.LOGGER.warn("************************************");
                    Paintings.LOGGER.error("Error Detected in ResourcePack `{}` ", resourcePackPath.getFileName().toString());
                    Paintings.LOGGER.warn(e);
                    Paintings.LOGGER.warn("************************************");
                }
            }
        } catch (IOException e) {

            Paintings.LOGGER.warn("************************************");
            Paintings.LOGGER.warn("!*!*!*!*!");
            Paintings.LOGGER.error("A fatal error occurred reading the resource pack directory");
            Paintings.LOGGER.error("SKIPPING ENTIRE PROCESS");
            Paintings.LOGGER.warn("!*!*!*!*!");
            Paintings.LOGGER.warn("************************************");
            Paintings.LOGGER.warn(e);
        }

        // read out all json files in the painting directory

        Path dir = Paths.get("./paintings");

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            Paintings.LOGGER.info("Started Reading all json files in /painting directory");

            for (Path filesInDirPath : ds) {
                Paintings.LOGGER.info(filesInDirPath);
                Iterator<Path> jsonFiles = Files.walk(filesInDirPath).iterator();

                while (jsonFiles.hasNext()) {
                    Path nextJson = jsonFiles.next();

                    if (Files.isRegularFile(nextJson) && nextJson.toString().endsWith(".json")) {
                        InputStream stream = Files.newInputStream(nextJson);
                        Gson gson = new GsonBuilder().create();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        JsonElement je = gson.fromJson(reader, JsonElement.class);
                        JsonObject json = je.getAsJsonObject();

                        JsonArray array = json.getAsJsonArray("paintings");

                        for (int index = 0; index < array.size(); index++) {

                            JsonObject jsonObject = array.get(index).getAsJsonObject();

                            String textureName = jsonObject.get("name").getAsString();

                            int sizeX = 0;
                            int sizeY = 0;
                            int sizeSquare = 0;

                            if (jsonObject.has("x")) {
                                sizeX = jsonObject.get("x").getAsInt();
                            }

                            if (jsonObject.has("y")) {
                                sizeY = jsonObject.get("y").getAsInt();
                            }

                            if (jsonObject.has("square")) {
                                sizeSquare = jsonObject.get("square").getAsInt();
                            }

                            if (sizeSquare == 0)
                                if ((sizeX == 0 || sizeY == 0)) {
                                    Paintings.LOGGER.error("Tried loading a painting where one of the sides was 0 ! ");
                                    Paintings.LOGGER.error("Painting name is : " + textureName);
                                    Paintings.LOGGER.error("Skipping...");
                                    continue;
                                }

                            if (sizeSquare % 16 != 0)
                                if ((sizeX % 16 != 0 || sizeY % 16 != 0)) {
                                    Paintings.LOGGER.error("Tried loading a painting with a size that is not a multiple of 16 !! ");
                                    Paintings.LOGGER.error("Painting name is : " + textureName);
                                    Paintings.LOGGER.error("Skipping...");
                                    continue;
                                }

                            PaintingEntry entry = new PaintingEntry(textureName, sizeX, sizeY, sizeSquare);
                            Paintings.LOGGER.info(String.format("Loaded json painting %s , %d x %d", entry.getRefName(), entry.getSizeX(), entry.getSizeY()));
                            addedPaintings.add(entry);

                        }
                    }
                }

            }
        } catch (IOException e) {
            Paintings.LOGGER.warn("************************************");
            Paintings.LOGGER.warn("!*!*!*!*!");
            Paintings.LOGGER.warn("No Painting Packs Detected. You will not be able to use ");
            Paintings.LOGGER.warn("the Paintings ++ Mod correctly.");
            Paintings.LOGGER.warn("Make sure to select or set some in the resourcepack folder and/or ingame gui !");
            Paintings.LOGGER.warn("!*!*!*!*!");
            Paintings.LOGGER.warn("************************************");

            e.printStackTrace();
        }
    }

    private FileSystem initFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            return FileSystems.newFileSystem(uri, env);
        }
    }
}
