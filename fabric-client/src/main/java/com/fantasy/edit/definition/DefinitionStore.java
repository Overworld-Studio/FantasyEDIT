package com.fantasy.edit.definition;

import com.fantasy.edit.FantasyEdit;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 定义仓库：读写 config/fantasy_edit/definitions/*.json。
 * 客户端与 Paper 端共用完全相同的 JSON 结构。
 */
public final class DefinitionStore {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path directory;
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();

    public DefinitionStore(Path directory) {
        this.directory = directory;
    }

    public Path directory() {
        return directory;
    }

    public void load() {
        definitions.clear();
        try {
            Files.createDirectories(directory);
            try (Stream<Path> files = Files.list(directory)) {
                files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(this::loadFile);
            }
        } catch (IOException e) {
            FantasyEdit.LOGGER.error("无法读取定义目录 {}", directory, e);
        }
        FantasyEdit.LOGGER.info("已加载 {} 条虚空造物定义", definitions.size());
    }

    private void loadFile(Path file) {
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            ItemDefinition definition = GSON.fromJson(json, ItemDefinition.class);
            if (definition == null) {
                return;
            }
            if (definition.blockProperties == null) {
                definition.blockProperties = new BlockSettings();
            }
            if (definition.lore == null) {
                definition.lore = new java.util.ArrayList<>();
            }
            if (definition.behaviors == null) {
                definition.behaviors = new java.util.ArrayList<>();
            }
            if (!definition.isValid()) {
                FantasyEdit.LOGGER.warn("跳过非法定义 {}（namespace/path 必须为小写合法 ID）", file.getFileName());
                return;
            }
            definitions.put(definition.key(), definition);
        } catch (IOException | JsonSyntaxException e) {
            FantasyEdit.LOGGER.error("解析定义失败 {}", file.getFileName(), e);
        }
    }

    public void save(ItemDefinition definition) {
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve(definition.fileName() + ".json");
            Files.writeString(file, GSON.toJson(definition), StandardCharsets.UTF_8);
            definitions.put(definition.key(), definition);
        } catch (IOException e) {
            FantasyEdit.LOGGER.error("保存定义失败 {}", definition.key(), e);
        }
    }

    public void delete(ItemDefinition definition) {
        definitions.remove(definition.key());
        try {
            Files.deleteIfExists(directory.resolve(definition.fileName() + ".json"));
        } catch (IOException e) {
            FantasyEdit.LOGGER.error("删除定义失败 {}", definition.key(), e);
        }
    }

    public Collection<ItemDefinition> all() {
        return definitions.values();
    }

    public ItemDefinition get(String key) {
        return definitions.get(key);
    }

    public static String toJson(ItemDefinition definition) {
        return GSON.toJson(definition);
    }

    public static ItemDefinition fromJson(String json) {
        ItemDefinition definition = GSON.fromJson(json, ItemDefinition.class);
        if (definition != null) {
            if (definition.blockProperties == null) {
                definition.blockProperties = new BlockSettings();
            }
            if (definition.lore == null) {
                definition.lore = new java.util.ArrayList<>();
            }
            if (definition.behaviors == null) {
                definition.behaviors = new java.util.ArrayList<>();
            }
        }
        return definition;
    }
}
