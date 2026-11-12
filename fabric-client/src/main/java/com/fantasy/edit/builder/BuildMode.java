package com.fantasy.edit.builder;

/**
 * 快速搭建的四种操作模式（参考 Axiom 的工具分组）。
 */
public enum BuildMode {

    SELECT("选区", "点选两个角点后整块填充/替换"),
    BRUSH("笔刷", "左键抹除、右键涂抹，按住可连续绘制"),
    REPLACE("替换", "把选区内的目标方块批量替换为手持方块"),
    PASTE("粘贴", "把剪贴板结构贴到准星位置");

    private final String label;
    private final String hint;

    BuildMode(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }

    public String label() {
        return this.label;
    }

    public String hint() {
        return this.hint;
    }

    public BuildMode next() {
        BuildMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static BuildMode parse(String raw) {
        if (raw != null) {
            for (BuildMode value : values()) {
                if (value.name().equalsIgnoreCase(raw)) {
                    return value;
                }
            }
        }
        return SELECT;
    }
}
