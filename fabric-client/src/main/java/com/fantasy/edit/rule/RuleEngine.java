package com.fantasy.edit.rule;

import com.fantasy.edit.FantasyEdit;
import com.fantasy.edit.client.assets.AssetWorkspace;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 运行时规则引擎：把原本写死在代码里的规则搬到一张可热改的注册表上。
 *
 * <p>两条来源：</p>
 * <ul>
 *     <li><b>本地规则</b>：模组自身的硬编码常量（体积上限、笔刷半径范围、预览密度、历史深度…），
 *         修改后由 {@code applier} 直接作用在内存状态上，下一 tick 就生效。</li>
 *     <li><b>原版 GameRule</b>：反射枚举 {@code GameRules} 的全部静态字段，
 *         改动时通过 {@code /gamerule <id> <value>} 下发到服务端，同样无需重启。</li>
 * </ul>
 *
 * <p>取值持久化在 {@code config/fantasy_edit/rules.json}，世界内的存活存档不会被打断。</p>
 */
public final class RuleEngine {

    public enum Kind {
        BOOLEAN,
        INT,
        DOUBLE
    }

    /** 一条可热改规则。字段公开供规则面板直接读取。 */
    public static final class Rule {

        public final String id;
        public final String category;
        public final String label;
        public final Kind kind;
        public final double min;
        public final double max;
        public final double step;
        public final String defaultValue;

        private final Consumer<String> applier;
        private final String gameRuleId;

        Rule(String id, String category, String label, Kind kind, double min, double max, double step,
             String defaultValue, Consumer<String> applier, String gameRuleId) {
            this.id = id;
            this.category = category;
            this.label = label;
            this.kind = kind;
            this.min = min;
            this.max = max;
            this.step = step;
            this.defaultValue = defaultValue;
            this.applier = applier;
            this.gameRuleId = gameRuleId;
        }

        public boolean isGameRule() {
            return this.gameRuleId != null;
        }
    }

    public static final String CATEGORY_BUILD = "世界快速搭建";
    public static final String CATEGORY_BEHAVIOR = "行为编排";
    public static final String CATEGORY_VANILLA = "原版 GameRule";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<String, Rule> RULES = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();
    private static final Set<String> CATEGORIES = new LinkedHashSet<>();

    private static boolean bootstrapped;
    private static String status = "§7规则尚未加载";

    private static Level lastLevel;
    private static int applyDelay = -1;

    private RuleEngine() {
    }

    // ------------------------------------------------------------------ 初始化

    /** 由客户端入口调用一次。注册规则、读取 rules.json 并立即套用。 */
    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        registerLocalRules();
        registerVanillaGameRules();
        load();

        for (Rule rule : RULES.values()) {
            apply(rule, valueOf(rule));
        }
        status = "§a已加载 " + RULES.size() + " 条运行时规则";
    }

    private static void registerLocalRules() {
        registerInt("build.max_volume", CATEGORY_BUILD, "单次操作方块上限", "250000", 1000, 2_000_000, 10_000, null);
        registerInt("build.radius_min", CATEGORY_BUILD, "笔刷半径下限", "1", 1, 32, 1, null);
        registerInt("build.radius_max", CATEGORY_BUILD, "笔刷半径上限", "32", 1, 128, 1, null);
        registerInt("build.brush_interval", CATEGORY_BUILD, "笔刷连续绘制间隔（tick）", "1", 1, 20, 1, null);
        registerInt("build.preview_interval", CATEGORY_BUILD, "选区预览刷新间隔（tick）", "6", 1, 40, 1, null);
        registerInt("build.preview_particles", CATEGORY_BUILD, "选区预览粒子上限", "220", 20, 2000, 20, null);
        registerInt("build.history_depth", CATEGORY_BUILD, "撤销栈深度", "64", 4, 512, 4, null);

        registerInt("behavior.max_rules", CATEGORY_BEHAVIOR, "单个物品的行为规则上限", "16", 1, 64, 1, null);
        registerInt("behavior.max_particles", CATEGORY_BEHAVIOR, "单次粒子动作数量上限", "64", 1, 512, 1, null);
        registerInt("behavior.default_cooldown", CATEGORY_BEHAVIOR, "行为默认冷却（tick）", "10", 0, 200, 1, null);
    }

    /**
     * 反射枚举 26.3 的 {@code GameRules} 静态字段，把每一条原版规则都注册进来。
     * 失败时只是少掉这一组规则，不影响其它功能。
     */
    private static void registerVanillaGameRules() {
        try {
            Class<?> gameRulesClass = Class.forName("net.minecraft.world.level.gamerules.GameRules");
            Class<?> gameRuleClass = Class.forName("net.minecraft.world.level.gamerules.GameRule");
            int count = 0;
            for (Field field : gameRulesClass.getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != gameRuleClass) {
                    continue;
                }
                Object gameRule = field.get(null);
                if (gameRule == null) {
                    continue;
                }
                String rawId = String.valueOf(gameRuleClass.getMethod("id").invoke(gameRule));
                if (rawId.startsWith("gamerule.")) {
                    rawId = rawId.substring("gamerule.".length());
                }
                Object defaultValue = gameRuleClass.getMethod("defaultValue").invoke(gameRule);
                Class<?> valueClass = (Class<?>) gameRuleClass.getMethod("valueClass").invoke(gameRule);
                boolean isBoolean = valueClass == Boolean.class;
                add(new Rule("gamerule." + rawId, CATEGORY_VANILLA, prettyGameRule(rawId),
                        isBoolean ? Kind.BOOLEAN : Kind.INT,
                        isBoolean ? 0 : 0, isBoolean ? 1 : 2_000_000_000, 1,
                        String.valueOf(defaultValue), null, rawId));
                count++;
            }
            FantasyEdit.LOGGER.info("已注册 {} 条原版 GameRule", count);
        } catch (Throwable throwable) {
            FantasyEdit.LOGGER.warn("枚举原版 GameRule 失败，规则面板将只包含模组自身规则", throwable);
        }
    }

    private static String prettyGameRule(String id) {
        StringBuilder builder = new StringBuilder(id.length() + 8);
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                builder.append(' ');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    private static void registerInt(String id, String category, String label, String defaultValue,
                                    int min, int max, int step, Consumer<String> applier) {
        add(new Rule(id, category, label, Kind.INT, min, max, step, defaultValue, applier, null));
    }

    private static void add(Rule rule) {
        RULES.put(rule.id, rule);
        CATEGORIES.add(rule.category);
    }

    // ------------------------------------------------------------------ 读取

    public static Collection<Rule> rules() {
        return RULES.values();
    }

    public static List<String> categories() {
        return new ArrayList<>(CATEGORIES);
    }

    public static String status() {
        return status;
    }

    public static String displayValue(Rule rule) {
        String value = valueOf(rule);
        if (rule.isGameRule() && value.equals(rule.defaultValue)) {
            return value + "（默认）";
        }
        return value;
    }

    public static String rawValue(String id) {
        Rule rule = RULES.get(id);
        return rule == null ? null : valueOf(rule);
    }

    public static int integer(String id) {
        Rule rule = RULES.get(id);
        if (rule == null) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(valueOf(rule)));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static double number(String id) {
        Rule rule = RULES.get(id);
        if (rule == null) {
            return 0;
        }
        try {
            return Double.parseDouble(valueOf(rule));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean bool(String id) {
        Rule rule = RULES.get(id);
        return rule != null && Boolean.parseBoolean(valueOf(rule));
    }

    private static String valueOf(Rule rule) {
        return VALUES.getOrDefault(rule.id, rule.defaultValue);
    }

    // ------------------------------------------------------------------ 修改

    /** 面板点「＋」或点击布尔项时调用。 */
    public static void bump(String id, boolean up) {
        Rule rule = RULES.get(id);
        if (rule == null) {
            return;
        }
        if (rule.kind == Kind.BOOLEAN) {
            set(id, Boolean.toString(!Boolean.parseBoolean(valueOf(rule))));
            return;
        }
        double next = Double.parseDouble(valueOf(rule)) + (up ? rule.step : -rule.step);
        set(id, Double.toString(next));
    }

    /** 直接写入一个值：校验范围 → 立即套用 → 持久化。 */
    public static void set(String id, String raw) {
        Rule rule = RULES.get(id);
        if (rule == null) {
            return;
        }
        String normalized = normalize(rule, raw);
        VALUES.put(id, normalized);
        apply(rule, normalized);
        save();
        status = "§a" + rule.label + " → " + normalized;
    }

    public static void reset(String id) {
        Rule rule = RULES.get(id);
        if (rule == null) {
            return;
        }
        VALUES.remove(id);
        apply(rule, rule.defaultValue);
        save();
        status = "§7" + rule.label + " 已恢复默认";
    }

    public static void resetAll() {
        for (Rule rule : RULES.values()) {
            VALUES.remove(rule.id);
            apply(rule, rule.defaultValue);
        }
        save();
        status = "§7全部规则已恢复默认";
    }

    private static String normalize(Rule rule, String raw) {
        switch (rule.kind) {
            case BOOLEAN:
                return Boolean.toString(Boolean.parseBoolean(raw));
            case INT: {
                long parsed;
                try {
                    parsed = Math.round(Double.parseDouble(raw.trim()));
                } catch (NumberFormatException e) {
                    parsed = Math.round(Double.parseDouble(rule.defaultValue));
                }
                long min = Math.round(rule.min);
                long max = Math.round(rule.max);
                return Long.toString(Math.max(min, Math.min(max, parsed)));
            }
            case DOUBLE:
            default: {
                double parsed;
                try {
                    parsed = Double.parseDouble(raw.trim());
                } catch (NumberFormatException e) {
                    parsed = Double.parseDouble(rule.defaultValue);
                }
                return Double.toString(Math.max(rule.min, Math.min(rule.max, parsed)));
            }
        }
    }

    private static void apply(Rule rule, String value) {
        if (rule.applier != null) {
            rule.applier.accept(value);
        }
        if (rule.gameRuleId != null) {
            dispatchGameRule(rule.gameRuleId, value);
        }
    }

    private static void dispatchGameRule(String gameRuleId, String value) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return;
        }
        // 直接走原版指令通道：服务端校验权限并广播，客户端无需等待重启
        minecraft.player.connection.sendCommand("gamerule " + gameRuleId + " " + value);
    }

    // ------------------------------------------------------------------ 世界切换

    /**
     * 每帧调用。进入世界后延迟 apply-stored，把 rules.json 里保存的 GameRule 重新下发，
     * 保证「上一次调好的规则」在新世界里依然成立。
     */
    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            lastLevel = null;
            applyDelay = -1;
            return;
        }
        if (minecraft.level != lastLevel) {
            lastLevel = minecraft.level;
            applyDelay = 40;
            return;
        }
        if (applyDelay < 0) {
            return;
        }
        if (--applyDelay == 0) {
            applyDelay = -1;
            applyStoredGameRules();
        }
    }

    private static void applyStoredGameRules() {
        int applied = 0;
        for (Rule rule : RULES.values()) {
            if (!rule.isGameRule()) {
                continue;
            }
            String value = VALUES.get(rule.id);
            if (value == null || value.equals(rule.defaultValue)) {
                continue;
            }
            dispatchGameRule(rule.gameRuleId, value);
            applied++;
        }
        if (applied > 0) {
            status = "§a已重新应用 " + applied + " 条 GameRule";
        }
    }

    // ------------------------------------------------------------------ 持久化

    private static Path file() {
        return AssetWorkspace.ROOT.resolve("rules.json");
    }

    private static void load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, String> stored = GSON.fromJson(json, new TypeToken<LinkedHashMap<String, String>>() {
            }.getType());
            if (stored == null) {
                return;
            }
            for (Map.Entry<String, String> entry : stored.entrySet()) {
                Rule rule = RULES.get(entry.getKey());
                if (rule != null && entry.getValue() != null) {
                    VALUES.put(entry.getKey(), normalize(rule, entry.getValue()));
                }
            }
        } catch (IOException | RuntimeException e) {
            FantasyEdit.LOGGER.warn("读取 rules.json 失败", e);
        }
    }

    private static void save() {
        Map<String, String> stored = new LinkedHashMap<>();
        for (Rule rule : RULES.values()) {
            String value = VALUES.get(rule.id);
            if (value != null && !value.equals(rule.defaultValue)) {
                stored.put(rule.id, value);
            }
        }
        try {
            Files.createDirectories(AssetWorkspace.ROOT);
            Files.writeString(file(), GSON.toJson(stored), StandardCharsets.UTF_8);
        } catch (IOException e) {
            FantasyEdit.LOGGER.warn("写入 rules.json 失败", e);
        }
    }
}
