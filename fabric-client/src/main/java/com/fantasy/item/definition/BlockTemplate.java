package com.fantasy.item.definition;

/**
 * 方块的物理模板，对应 GUI 中的预设下拉项。
 */
public enum BlockTemplate {
    /** 完整固体方块（默认碰撞/遮挡）。 */
    SOLID,
    /** 玻璃质感：不遮挡、玻璃音效。 */
    GLASS,
    /** 可穿行：无碰撞、不遮挡。 */
    PASSABLE,
    /** 朝向方块：拥有 FACING 属性，放置时朝向玩家。 */
    DIRECTIONAL
}
