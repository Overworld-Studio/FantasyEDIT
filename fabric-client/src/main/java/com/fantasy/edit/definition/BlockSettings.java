package com.fantasy.edit.definition;

/**
 * 方块附加属性，序列化为 JSON 的 block_properties 字段。
 */
public final class BlockSettings {

    public BlockTemplate template = BlockTemplate.SOLID;

    /** 发光等级 0~15。 */
    public int lightLevel = 0;

    public BlockSettings() {
    }

    public BlockSettings(BlockTemplate template, int lightLevel) {
        this.template = template == null ? BlockTemplate.SOLID : template;
        this.lightLevel = Math.max(0, Math.min(15, lightLevel));
    }

    public BlockSettings copy() {
        return new BlockSettings(template, lightLevel);
    }
}
