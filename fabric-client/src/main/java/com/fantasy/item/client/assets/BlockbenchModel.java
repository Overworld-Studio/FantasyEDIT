package com.fantasy.item.client.assets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 轻量级 Blockbench 模型解析器。
 *
 * <p>支持两种输入：</p>
 * <ul>
 *   <li>Blockbench “Java Block/Item” 导出的 .json（元素坐标已经是 Minecraft 空间）；</li>
 *   <li>原生 .bbmodel（读取 elements / resolution / textures，并做基础坐标换算）。</li>
 * </ul>
 *
 * <p>输出为原版 {@code models/item|block/*.json} 结构，交给原版模型烘焙器处理，
 * 因此可以在不重写渲染管线的前提下支持任意数量的边、旋转与 UV。</p>
 */
public final class BlockbenchModel {

    private BlockbenchModel() {
    }

    /**
     * @param file      模型文件
     * @param textureId 纹理引用，例如 {@code hyperlab:item/quantum_core}
     * @return 原版模型 JSON；若文件没有 elements，则返回 null（调用方改用 generated 模型）
     */
    public static JsonObject convert(Path file, String textureId) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        JsonElement rootElement = JsonParser.parseString(json);
        if (!rootElement.isJsonObject()) {
            return null;
        }
        JsonObject root = rootElement.getAsJsonObject();
        if (!root.has("elements") || !root.get("elements").isJsonArray()) {
            return null;
        }

        JsonArray sourceElements = root.getAsJsonArray("elements");
        JsonArray elements = new JsonArray();
        for (JsonElement element : sourceElements) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject converted = convertElement(element.getAsJsonObject());
            if (converted != null) {
                elements.add(converted);
            }
        }
        if (elements.isEmpty()) {
            return null;
        }

        JsonObject model = new JsonObject();
        JsonObject textures = new JsonObject();
        textures.addProperty("0", textureId);
        textures.addProperty("particle", textureId);
        model.add("textures", textures);
        model.add("elements", elements);

        // 保留 Blockbench 已经导出的物品展示变换
        if (root.has("display") && root.get("display").isJsonObject()) {
            model.add("display", root.get("display").deepCopy());
        }
        return model;
    }

    private static JsonObject convertElement(JsonObject source) {
        if (!source.has("from") || !source.has("to")) {
            return null;
        }
        JsonObject element = new JsonObject();
        element.add("from", normalizeVector(source.getAsJsonArray("from")));
        element.add("to", normalizeVector(source.getAsJsonArray("to")));

        if (source.has("rotation") && source.get("rotation").isJsonObject()) {
            JsonObject rotation = source.getAsJsonObject("rotation");
            JsonObject out = new JsonObject();
            out.add("origin", rotation.has("origin")
                    ? normalizeVector(rotation.getAsJsonArray("origin"))
                    : vector(8, 8, 8));
            out.addProperty("axis", rotation.has("axis") ? rotation.get("axis").getAsString() : "y");
            out.addProperty("angle", rotation.has("angle") ? rotation.get("angle").getAsFloat() : 0.0F);
            out.addProperty("rescale", rotation.has("rescale") && rotation.get("rescale").getAsBoolean());
            element.add("rotation", out);
        }

        if (source.has("faces") && source.get("faces").isJsonObject()) {
            JsonObject faces = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : source.getAsJsonObject("faces").entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject face = entry.getValue().getAsJsonObject();
                // 仅导出有纹理引用的面，避免原版烘焙器报 “Missing texture”
                if (!face.has("texture") || face.get("texture").isJsonNull()) {
                    continue;
                }
                JsonObject out = new JsonObject();
                out.add("uv", face.has("uv") && face.get("uv").isJsonArray()
                        ? face.getAsJsonArray("uv").deepCopy()
                        : vector(0, 0, 16, 16));
                out.addProperty("texture", "#0");
                if (face.has("rotation")) {
                    out.addProperty("rotation", face.get("rotation").getAsInt());
                }
                if (face.has("tintindex")) {
                    out.addProperty("tintindex", face.get("tintindex").getAsInt());
                }
                faces.add(entry.getKey(), out);
            }
            if (!faces.isEmpty()) {
                element.add("faces", faces);
            }
        }

        if (source.has("shade")) {
            element.addProperty("shade", source.get("shade").getAsBoolean());
        }
        return element;
    }

    private static JsonArray normalizeVector(JsonArray input) {
        JsonArray output = new JsonArray();
        for (int i = 0; i < Math.min(3, input.size()); i++) {
            output.add(clamp(input.get(i).getAsFloat()));
        }
        while (output.size() < 3) {
            output.add(0);
        }
        return output;
    }

    private static JsonArray vector(float... values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(value);
        }
        return array;
    }

    private static float clamp(float value) {
        return Math.max(-16.0F, Math.min(32.0F, value));
    }
}
