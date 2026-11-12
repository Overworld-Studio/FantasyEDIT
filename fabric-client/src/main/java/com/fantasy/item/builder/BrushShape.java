package com.fantasy.item.builder;

/**
 * 笔刷形状。每个形状都围绕准星方块生成一组偏移量。
 */
public enum BrushShape {

    SPHERE("球体"),
    CUBE("立方"),
    CYLINDER("圆柱"),
    SMOOTH("平滑"),
    ERODE("侵蚀"),
    NOISE("噪点");

    private final String label;

    BrushShape(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public BrushShape next() {
        BrushShape[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
