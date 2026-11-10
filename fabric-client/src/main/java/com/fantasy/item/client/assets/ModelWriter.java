package com.fantasy.item.client.assets;

import com.fantasy.item.definition.BlockTemplate;
import com.fantasy.item.definition.DefinitionType;
import com.fantasy.item.definition.ItemDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 把定义 + Blockbench 模型 + PNG 写成原版资源包结构。
 * 写完调用 {@link AssetReloader#reload()} 即可零重启热更新外观。
 */
public final class ModelWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ModelWriter() {
    }

    public static void write(ItemDefinition definition) throws IOException {
        AssetWorkspace.ensureLayout();
        Path namespaceRoot = AssetWorkspace.namespaceRoot(definition.namespace);
        boolean block = definition.type == DefinitionType.BLOCK;
        String textureRef = definition.namespace + ":" + (block ? "block/" : "item/") + definition.path;

        Path textureTarget = namespaceRoot
                .resolve("textures")
                .resolve(block ? "block" : "item")
                .resolve(definition.path + ".png");
        Files.createDirectories(textureTarget.getParent());
        copyTexture(definition, textureTarget);

        Path sourceModel = resolveModelFile(definition);
        JsonObject model = null;
        if (sourceModel != null) {
            model = BlockbenchModel.convert(sourceModel, textureRef);
        }
        if (model == null) {
            model = fallbackModel(block, textureRef);
        }

        if (block) {
            Path blockModel = namespaceRoot.resolve("models/block").resolve(definition.path + ".json");
            writeJson(blockModel, model);
            writeJson(namespaceRoot.resolve("models/item").resolve(definition.path + ".json"),
                    parentModel(definition.namespace + ":block/" + definition.path));
            writeBlockState(definition, namespaceRoot);
        } else {
            Path itemModel = namespaceRoot.resolve("models/item").resolve(definition.path + ".json");
            writeJson(itemModel, model);
        }

        JsonObject itemDefinition = new JsonObject();
        JsonObject modelRef = new JsonObject();
        modelRef.addProperty("type", "minecraft:model");
        modelRef.addProperty("model", definition.namespace + ":" + (block ? "block/" : "item/") + definition.path);
        itemDefinition.add("model", modelRef);
        writeJson(namespaceRoot.resolve("items").resolve(definition.path + ".json"), itemDefinition);
    }

    public static void delete(ItemDefinition definition) {
        Path namespaceRoot = AssetWorkspace.namespaceRoot(definition.namespace);
        String path = definition.path + ".json";
        try {
            Files.deleteIfExists(namespaceRoot.resolve("models/item/" + path));
            Files.deleteIfExists(namespaceRoot.resolve("models/block/" + path));
            Files.deleteIfExists(namespaceRoot.resolve("blockstates/" + path));
            Files.deleteIfExists(namespaceRoot.resolve("items/" + path));
            Files.deleteIfExists(namespaceRoot.resolve("textures/item/" + definition.path + ".png"));
            Files.deleteIfExists(namespaceRoot.resolve("textures/block/" + definition.path + ".png"));
        } catch (IOException ignored) {
            // 删除失败不影响后续覆盖写入
        }
    }

    private static JsonObject fallbackModel(boolean block, String textureRef) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", block ? "minecraft:block/cube_all" : "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty(block ? "all" : "layer0", textureRef);
        model.add("textures", textures);
        return model;
    }

    private static JsonObject parentModel(String parent) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        return model;
    }

    private static void writeBlockState(ItemDefinition definition, Path namespaceRoot) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject variants = new JsonObject();
        String modelId = definition.namespace + ":block/" + definition.path;
        BlockTemplate template = definition.blockProperties == null
                ? BlockTemplate.SOLID
                : definition.blockProperties.template;

        if (template == BlockTemplate.DIRECTIONAL) {
            addVariant(variants, "facing=north", modelId, 0);
            addVariant(variants, "facing=east", modelId, 90);
            addVariant(variants, "facing=south", modelId, 180);
            addVariant(variants, "facing=west", modelId, 270);
        } else {
            addVariant(variants, "", modelId, 0);
        }
        root.add("variants", variants);
        writeJson(namespaceRoot.resolve("blockstates").resolve(definition.path + ".json"), root);
    }

    private static void addVariant(JsonObject variants, String key, String modelId, int yRotation) {
        JsonObject entry = new JsonObject();
        entry.addProperty("model", modelId);
        if (yRotation != 0) {
            entry.addProperty("y", yRotation);
        }
        JsonArray array = new JsonArray();
        array.add(entry);
        variants.add(key, array);
    }

    /**
     * 按“显式 texture 字段 → 模型同名 PNG → path.png”的顺序匹配纹理，
     * 都没有时生成一张占位棋盘图，保证物品不会变成紫黑格。
     */
    private static void copyTexture(ItemDefinition definition, Path target) throws IOException {
        Path source = null;
        if (definition.texture != null && !definition.texture.isBlank()) {
            source = AssetWorkspace.texturesDir().resolve(definition.texture);
        }
        if ((source == null || !Files.isRegularFile(source)) && definition.model != null && !definition.model.isBlank()) {
            String base = definition.model;
            int dot = base.lastIndexOf('.');
            if (dot > 0) {
                base = base.substring(0, dot);
            }
            source = AssetWorkspace.texturesDir().resolve(base + ".png");
        }
        if (source == null || !Files.isRegularFile(source)) {
            source = AssetWorkspace.texturesDir().resolve(definition.path + ".png");
        }
        if (source != null && Files.isRegularFile(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        writePlaceholderTexture(target, definition.key());
    }

    private static void writePlaceholderTexture(Path target, String key) throws IOException {
        int seed = key.hashCode();
        Color base = Color.getHSBColor(((seed % 360) + 360) % 360 / 360.0F, 0.55F, 0.9F);
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean dark = ((x / 4) + (y / 4)) % 2 == 0;
                graphics.setColor(dark ? base.darker() : base);
                graphics.fillRect(x, y, 1, 1);
            }
        }
        graphics.dispose();
        ImageIO.write(image, "png", target.toFile());
    }

    private static Path resolveModelFile(ItemDefinition definition) {
        if (definition.model == null || definition.model.isBlank()) {
            return null;
        }
        Path direct = AssetWorkspace.modelsDir().resolve(definition.model);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        for (Path candidate : AssetWorkspace.listModels()) {
            if (candidate.getFileName().toString().equalsIgnoreCase(definition.model)
                    || AssetWorkspace.baseName(candidate).equalsIgnoreCase(definition.model)) {
                return candidate;
            }
        }
        return null;
    }

    private static void writeJson(Path target, JsonObject json) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, GSON.toJson(json), StandardCharsets.UTF_8);
    }
}
