package com.fantasy.item.client.assets;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 虚空造物的本地工作区布局：
 *
 * <pre>
 * config/fantasy_item/
 * ├── definitions/          # 统一 JSON 定义（Paper 端可直接复用）
 * ├── models/               # 用户放置 Blockbench .json / .bbmodel
 * ├── textures/             # 用户放置 PNG
 * └── resourcepack/         # 自动生成的动态资源包（运行时挂载）
 *     └── assets/&lt;ns&gt;/...
 * </pre>
 */
public final class AssetWorkspace {

    public static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("fantasy_item");

    private AssetWorkspace() {
    }

    public static Path definitionsDir() {
        return ROOT.resolve("definitions");
    }

    public static Path modelsDir() {
        return ROOT.resolve("models");
    }

    public static Path texturesDir() {
        return ROOT.resolve("textures");
    }

    public static Path packRoot() {
        return ROOT.resolve("resourcepack");
    }

    public static Path namespaceRoot(String namespace) {
        return packRoot().resolve("assets").resolve(namespace);
    }

    public static void ensureLayout() {
        try {
            Files.createDirectories(definitionsDir());
            Files.createDirectories(modelsDir());
            Files.createDirectories(texturesDir());
            Files.createDirectories(packRoot().resolve("assets"));
        } catch (IOException e) {
            throw new IllegalStateException("无法创建 Fantasy:Item 工作目录", e);
        }
    }

    public static List<Path> listModels() {
        return listFiles(modelsDir(), ".json", ".bbmodel");
    }

    public static List<Path> listTextures() {
        return listFiles(texturesDir(), ".png");
    }

    private static List<Path> listFiles(Path dir, String... extensions) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> result = new ArrayList<>(stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        for (String extension : extensions) {
                            if (name.endsWith(extension)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .toList());
            result.sort(Comparator.comparing(path -> path.getFileName().toString()));
            return result;
        } catch (IOException e) {
            return List.of();
        }
    }

    public static String baseName(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
