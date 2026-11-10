package com.fantasy.item.paper.model;

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
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * 服务端定义仓库：读取 plugins/FantasyItem/definitions/*.json。
 * 与客户端 config/fantasy_item/definitions 使用同一份格式，可直接拷贝同步。
 */
public final class DefinitionStore {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path directory;
    private final Logger logger;
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();

    public DefinitionStore(Path directory, Logger logger) {
        this.directory = directory;
        this.logger = logger;
    }

    public void load() {
        definitions.clear();
        try {
            Files.createDirectories(directory);
            try (Stream<Path> files = Files.list(directory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .forEach(this::loadFile);
            }
        } catch (IOException e) {
            logger.severe("无法读取定义目录 " + directory + ": " + e.getMessage());
        }
    }

    private void loadFile(Path file) {
        try {
            ItemDefinition definition = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), ItemDefinition.class);
            if (definition == null) {
                return;
            }
            if (definition.blockProperties == null) {
                definition.blockProperties = new ItemDefinition.BlockProperties();
            }
            if (definition.lore == null) {
                definition.lore = new java.util.ArrayList<>();
            }
            if (definition.behaviors == null) {
                definition.behaviors = new java.util.ArrayList<>();
            }
            if (!definition.isValid()) {
                logger.warning("跳过非法定义 " + file.getFileName() + "（namespace/path 必须为小写合法 ID）");
                return;
            }
            definitions.put(definition.key(), definition);
        } catch (IOException | JsonSyntaxException e) {
            logger.severe("解析定义失败 " + file.getFileName() + ": " + e.getMessage());
        }
    }

    public void save(ItemDefinition definition) {
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(definition.namespace + "__" + definition.path + ".json"),
                    GSON.toJson(definition), StandardCharsets.UTF_8);
            definitions.put(definition.key(), definition);
        } catch (IOException e) {
            logger.severe("保存定义失败 " + definition.key() + ": " + e.getMessage());
        }
    }

    public ItemDefinition get(String key) {
        if (key == null) {
            return null;
        }
        return definitions.get(key.contains(":") ? key.toLowerCase(java.util.Locale.ROOT) : key);
    }

    public Collection<ItemDefinition> all() {
        return definitions.values();
    }

    public int size() {
        return definitions.size();
    }

    public Path directory() {
        return directory;
    }
}
