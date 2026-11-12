# Fantasy:Item

> 双端动态创作者引擎 —— 在游戏内“虚空造物”，让自定义物品与方块零重启出现。
>
> Mod ID / 指令命名空间：`fantasy_item` · 适配 Minecraft 26.3 · Fabric 客户端 + Paper 服务端

---

## 1. 功能一览

| 能力 | 客户端（Fabric 26.3） | 服务端（Paper 26.3） |
| --- | --- | --- |
| 自定义命名空间 / ID | ✅ 运行时注册 `namespace:path` | ✅ JSON 读取与校验 |
| 物品 / 方块 | ✅ 动态 `Item` + `Block` + `BlockItem` | ✅ 以原版物品为载体发放 |
| 多行 Lore / 颜色代码 | ✅ `&` / `§` | ✅ Adventure Legacy 序列化 |
| 附魔流光 / 稀有度 / 堆叠 | ✅ Data Components | ✅ ItemMeta |
| 物理模板 / 发光等级 | ✅ SOLID / GLASS / PASSABLE / DIRECTIONAL | ✅ 记录在定义中 |
| Blockbench 模型 | ✅ JSON → 原版模型热烘焙 | — |
| 热重载 | ✅ 保存后自动 `reloadResourcePacks()`，无需 F3+T | ✅ `/fantasyitem reload` |
| 行为编排（触发器 → 动作） | ✅ 行为编排面板，保存即注入事件总线 | ✅ 动态行为池 + 服务端权威执行 |
| 双端行为同步 | ✅ `fantasy_item:behavior` → 服务端 | ✅ 校验后广播 `fantasy_item:behavior_sync` |
| 可视化工作室 | ✅ 快捷键 `U` / 创造物品栏按钮 | — |
| 世界快速搭建（仅创造） | ✅ 选区 / 笔刷 / 剪贴板 / 撤销重做 | ✅ 权威执行 + 限额 + 独立历史 |

---

## 2. 工程结构

```
FantasyItem/
├── settings.gradle                 # 双模块聚合构建
├── gradle.properties               # 版本矩阵（MC / Loader / Fabric API / Paper）
├── fabric-client/                  # Fabric 26.3 客户端模组
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/fantasy/item/
│       │   ├── FantasyItem.java                    # 公共入口：解冻注册表 + 加载定义
│       │   ├── definition/                         # JSON 模型与读写
│       │   │   ├── ItemDefinition.java
│       │   │   ├── BlockSettings.java
│       │   │   ├── BlockTemplate.java
│       │   │   ├── DefinitionType.java
│       │   │   └── DefinitionStore.java
│       │   ├── registry/
│       │   │   ├── DynamicRegistry.java            # 动态物品/方块工厂
│       │   │   ├── FantasyBlock.java               # 朝向方块
│       │   │   └── RegistryUnfreezer.java          # 反射兜底解冻
│       │   ├── builder/                            # 世界快速搭建（仅创造模式）
│       │   │   ├── BuildMode.java                  # 选区 / 笔刷 / 替换 / 粘贴
│       │   │   ├── BrushShape.java                 # 球体 / 立方 / 圆柱 / 平滑 / 侵蚀 / 噪点
│       │   │   ├── BuildSelection.java             # 双角点立方选区
│       │   │   ├── BuildClipboard.java             # 结构剪贴板（旋转 / 镜像）
│       │   │   ├── BuildHistory.java               # 撤销重做快照
│       │   │   ├── ClientBuildEngine.java          # 编辑原语 + 粒子预览
│       │   │   ├── BuildKeybinds.java              # B / P / [ ] / Z / Y …
│       │   │   └── BuildHud.java                   # 建造 HUD
│       │   ├── behavior/                           # 运行时行为编排
│       │   │   ├── TriggerType.java                # ON_RIGHT_CLICK / PLACE / HIT / TICK
│       │   │   ├── ActionType.java                 # 粒子 / 音效 / 状态 / 指令 / 脚本钩子
│       │   │   ├── BehaviorRule.java               # 单条「触发 → 动作」规则
│       │   │   ├── DynamicEventRouter.java         # 动态事件总线（运行时热挂载）
│       │   │   ├── ClientBehaviorRuntime.java      # 表现层执行器 + 冷却
│       │   │   └── ScriptHooks.java                # 轻量脚本钩子白名单
│       │   ├── network/                            # 双端同步
│       │   │   ├── BehaviorPayload.java            # C2S fantasy_item:behavior
│       │   │   ├── BehaviorSyncPayload.java        # S2C fantasy_item:behavior_sync
│       │   │   ├── BehaviorMessage.java            # 报文模型
│       │   │   ├── BehaviorSync.java               # 推送 / 接收 / 回写定义
│       │   │   ├── BuildPayload.java               # C2S fantasy_item:build
│       │   │   ├── BuildAckPayload.java            # S2C fantasy_item:build_ack
│       │   │   ├── BuildMessage.java               # 建造报文
│       │   │   └── BuildSync.java                  # 建造操作双端同步
│       │   ├── mixin/MappedRegistryMixin.java      # 强制解除注册表冻结
│       │   ├── text/LegacyText.java                # & 颜色代码 → Component
│       │   └── client/
│       │       ├── FantasyItemClient.java
│       │       ├── StudioKeybinds.java             # 快捷键 U
│       │       ├── creativetab/FantasyTabs.java    # 专属 CreativeModeTab
│       │       ├── gui/StudioScreen.java           # 三栏可视化工作室
│       │       ├── gui/BehaviorEditorScreen.java   # 行为编排面板
│       │       ├── gui/BuildToolbarScreen.java     # 快速搭建面板
│       │       ├── assets/
│       │       │   ├── AssetWorkspace.java         # 配置目录布局
│       │       │   ├── BlockbenchModel.java        # Blockbench JSON 解析器
│       │       │   ├── ModelWriter.java            # 生成资源包结构
│       │       │   ├── DynamicPackSource.java      # 挂载动态资源包
│       │       │   └── AssetReloader.java          # 主动热重载
│       │       └── mixin/
│       │           ├── PackRepositoryMixin.java    # 注入动态资源包源
│       │           └── CreativeInventoryScreenMixin.java
│       └── resources/
│           ├── fabric.mod.json
│           ├── fantasy_item.mixins.json
│           ├── fantasy_item.client.mixins.json
│           └── assets/fantasy_item/lang/*.json
│
├── paper-server/                   # Paper / Purpur 服务端插件
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/fantasy/item/paper/
│       │   ├── FantasyItemPlugin.java
│       │   ├── command/FantasyItemCommand.java     # reload / list / behaviors / build / undo / redo / give
│       │   ├── item/ItemFactory.java               # ItemMeta + PDC
│       │   ├── build/
│       │   │   ├── BuildLimits.java                # 体积 / 半径 / 历史限额
│       │   │   ├── BuildHistoryService.java        # 每玩家撤销重做
│       │   │   ├── WorldEditService.java           # 填充 / 替换 / 笔刷 / 粘贴
│       │   │   ├── BuildMessage.java
│       │   │   └── BuildChannelListener.java       # fantasy_item:build 校验与执行
│       │   ├── behavior/
│       │   │   ├── TriggerType.java
│       │   │   ├── ActionType.java
│       │   │   ├── BehaviorRule.java
│       │   │   ├── BehaviorPool.java               # 动态行为池
│       │   │   ├── BehaviorRouter.java             # 事件分发 + 冷却 + Tick 任务
│       │   │   └── PaperActionExecutor.java        # 权威动作 + 脚本钩子白名单
│       │   ├── net/
│       │   │   ├── BehaviorMessage.java
│       │   │   └── BehaviorChannelListener.java    # 插件消息校验与广播
│       │   └── model/
│       │       ├── ItemDefinition.java
│       │       └── DefinitionStore.java
│       └── resources/
│           ├── plugin.yml
│           └── config.yml
│
└── examples/                       # 可直接拷进配置目录的示例
    ├── definitions/hyperlab__quantum_core.json
    ├── models/quantum_core.json    # Blockbench Java Block/Item 导出
    └── textures/quantum_core.png
```

---

## 3. 统一 JSON 定义规范

客户端与服务端读取**完全相同**的 JSON。客户端保存在
`config/fantasy_item/definitions/`，服务端保存在 `plugins/FantasyItem/definitions/`。

```json
{
  "namespace": "hyperlab",
  "path": "quantum_core",
  "type": "ITEM",
  "display_name": "&b量子核心",
  "lore": ["&7虚空造物 · 第一批实验体", "&8—— 记录于零号实验室"],
  "block_properties": {
    "template": "GLASS",
    "light_level": 7
  },
  "model": "quantum_core.json",
  "texture": "quantum_core.png",
  "glint": true,
  "rarity": "RARE",
  "max_stack_size": 16,
  "base_item": "PAPER",
  "behaviors": [
    {
      "trigger": "ON_RIGHT_CLICK",
      "action": "SPAWN_PARTICLE",
      "value": "minecraft:end_rod",
      "amount": 18,
      "duration": 100,
      "amplifier": 0,
      "as_player": true,
      "cooldown_ticks": 10
    }
  ]
}
```

| 字段 | 说明 |
| --- | --- |
| `namespace` | 自定义命名空间，小写 `[a-z0-9_.-]`，禁止 `minecraft` |
| `path` | 注册路径 / ID |
| `type` | `ITEM` 或 `BLOCK` |
| `display_name` | 支持 `&` / `§` 颜色代码 |
| `lore` | 字符串数组，每项一行 |
| `block_properties.template` | `SOLID` / `GLASS` / `PASSABLE` / `DIRECTIONAL` |
| `block_properties.light_level` | 0 ~ 15 |
| `model` | `config/fantasy_item/models/` 下的 Blockbench 文件名 |
| `texture` | `config/fantasy_item/textures/` 下的 PNG；留空则按模型名 / path 自动匹配 |
| `glint` | 是否强制附魔流光 |
| `rarity` | `COMMON` / `UNCOMMON` / `RARE` / `EPIC` |
| `max_stack_size` | 1 ~ 99 |
| `base_item` | **仅服务端**：承载外观的原版物品，默认 `PAPER` |
| `behaviors` | 行为规则数组，见第 6 节；最多 16 条 |

---

## 4. 客户端使用流程

1. 启动客户端，按 `U`（或创造物品栏右上角“虚空造物”按钮）打开工作室。
2. 把 Blockbench 导出的 `.json` 放进 `config/fantasy_item/models/`，
   PNG 放进 `config/fantasy_item/textures/`。
3. 在工作室中填写命名空间、路径 ID、显示名称、Lore，选择模型与纹理，
   设置物理模板 / 发光 / 流光 / 稀有度 / 堆叠。
4. 点击 **保存并凭空生成**：
   - 动态注册 `namespace:path` 到 `BuiltInRegistries`；
   - 生成 `config/fantasy_item/resourcepack/assets/<ns>/...`；
   - 触发一次资源热重载（等价自动 F3+T，无需手动按键）；
   - 物品直接进入背包，可用 `F3+H` 看到真实注册 ID。
5. 专属创造标签页 `Fantasy:Item · 虚空造物` 会列出全部自创物品。

### 关键实现

- **注册表解冻**：[MappedRegistryMixin.java](fabric-client/src/main/java/com/fantasy/item/mixin/MappedRegistryMixin.java)
  在 `MappedRegistry.freeze()` 返回后把 `frozen` 置回 `false`；
  [RegistryUnfreezer.java](fabric-client/src/main/java/com/fantasy/item/registry/RegistryUnfreezer.java)
  提供反射兜底。26.x 使用官方可读类名（`net.minecraft.core.MappedRegistry`），无需 mappings。
- **动态工厂**：[DynamicRegistry.java](fabric-client/src/main/java/com/fantasy/item/registry/DynamicRegistry.java)
  负责 `Item` / `Block` / `BlockItem` 的创建、`Item.BY_BLOCK` 与
  `Block.BLOCK_STATE_REGISTRY` 的幂等补齐。
- **Blockbench 热加载**：[BlockbenchModel.java](fabric-client/src/main/java/com/fantasy/item/client/assets/BlockbenchModel.java)
  解析 `.json` / `.bbmodel` 的 `elements`，转换为原版模型 JSON；
  [ModelWriter.java](fabric-client/src/main/java/com/fantasy/item/client/assets/ModelWriter.java)
  写入 `models` / `blockstates` / `items` / `textures`；
  [DynamicPackSource.java](fabric-client/src/main/java/com/fantasy/item/client/assets/DynamicPackSource.java)
  把目录挂载为必选资源包，[AssetReloader.java](fabric-client/src/main/java/com/fantasy/item/client/assets/AssetReloader.java)
  调用 `Minecraft.reloadResourcePacks()`。

> 说明：把 Blockbench 几何转换为原版模型 JSON，再交给原版烘焙器，
> 可以稳定支持任意面数、UV 与旋转，并且不需要重写 26.x 的 RenderPearl 渲染管线。
> 热重载由程序主动触发，因此使用体验上依然是“保存即生效”。

---

## 5. 服务端使用流程

1. 把 `fantasy-item-paper-*.jar` 放入 `plugins/`。
2. 启动一次生成 `plugins/FantasyItem/config.yml` 与 `definitions/`。
3. 把客户端 `config/fantasy_item/definitions/*.json` 复制到
   `plugins/FantasyItem/definitions/`（或修改 `definitions-directory` 指向共享目录）。
4. 指令：

```
/fantasyitem reload                               # 重载全部 JSON
/fantasyitem list                                 # 列出定义
/fantasyitem give hyperlab:quantum_core           # 给自己
/fantasyitem give hyperlab:quantum_core Steve 8   # 给指定玩家 8 个
```

服务端无法注册真实物品，因此以 `base_item` 为载体，并写入：

- `itemName` / `lore`（Adventure Component，支持 `&` 颜色）
- `enchantment_glint_override`
- `custom_model_data`（按定义 key 稳定哈希）
- `PersistentDataContainer: fantasy_item:definition = namespace:path`

客户端模组 / 配套资源包可据此识别并渲染为对应的虚空造物，
不会覆盖或污染任何原版物品。

---

## 6. 运行时行为热注入（Runtime UI & Hot-Injection）

### 6.1 可视化编排

工作室左栏底部的 **“行为编排 (N)…”** 按钮打开独立面板
[BehaviorEditorScreen.java](fabric-client/src/main/java/com/fantasy/item/client/gui/BehaviorEditorScreen.java)：

- 左栏：已有规则列表，点击即可载入编辑；
- 右栏：触发器 / 动作循环切换，参数、时长、等级、冷却均可递增递减，
  另有“本地预览”（不写盘、立即播放表现）与“写入 / 更新规则”。

| 触发器 | 说明 |
| --- | --- |
| `ON_RIGHT_CLICK` | 右键使用物品 / 右击方块 |
| `ON_BLOCK_PLACE` | 放置该物品（仅 BlockItem） |
| `ON_ENTITY_HIT` | 攻击实体 |
| `ON_TICK` | 手持时持续触发（受 `tick-interval` 与冷却限制） |

| 动作 | `value` 含义 | 执行方 |
| --- | --- | --- |
| `SPAWN_PARTICLE` | 粒子 ID，如 `minecraft:end_rod` | 客户端表现 / 服务端广播 |
| `PLAY_SOUND` | 音效 ID | 客户端表现 |
| `APPLY_POTION` | 状态效果 ID | **服务端权威** |
| `EXECUTE_COMMAND` | 指令（无需 `/`），`as_player` 决定身份 | **服务端权威** |
| `CUSTOM_SCRIPT_HOOK` | 内置钩子名（白名单） | 双端同名实现 |

内置钩子：`spark_ring`、`flame_burst`、`soul_aura`、`confetti`、`crit_spiral`；
服务端额外提供 `heal_1`、`glow`。未知钩子会被拒绝，不会执行任意代码。

### 6.2 毫秒级热注入

- **动态事件总线**：
  [DynamicEventRouter.java](fabric-client/src/main/java/com/fantasy/item/behavior/DynamicEventRouter.java)
  在客户端初始化时一次性注册 `UseItemCallback`、`UseBlockCallback`、
  `AttackEntityCallback` 与 `ClientTickEvents`；每次事件发生时**实时**从
  `DefinitionStore` 读取规则，因此点击“保存并凭空生成”后**下一帧即可触发**，
  既不重载类，也不重启游戏。
- **服务端动态行为池**：
  [BehaviorPool.java](paper-server/src/main/java/com/fantasy/item/paper/behavior/BehaviorPool.java)
  以 `namespace:path` 为键保存规则；
  [BehaviorRouter.java](paper-server/src/main/java/com/fantasy/item/paper/behavior/BehaviorRouter.java)
  监听 `PlayerInteractEvent` / `BlockPlaceEvent` / `EntityDamageByEntityEvent`，
  并以可配置的间隔调度 `ON_TICK`，所有规则带独立冷却，避免刷屏。
- **一致性策略**：客户端只在“服务端未接管”时本地播放粒子与音效；
  一旦检测到插件通道，表现层也交给服务端广播，杜绝双份特效。

### 6.3 双端同步与安全沙盒

```
客户端 GUI ──保存──▶ BehaviorSync.push()
      │                │  C2S  fantasy_item:behavior
      │                ▼
      │        Paper: BehaviorChannelListener
      │          1) 权限 fantasyitem.admin
      │          2) key / 规则 / 钩子白名单校验
      │          3) BehaviorPool.replace()  →  立即生效
      │          4) 回写 definitions/*.json  →  重启仍在
      │          5) 广播  fantasy_item:behavior_sync
      ▼                ▼
  本地定义回写      所有在线客户端刷新表现
```

安全约束：

- 通道消息上限 32KB，超出直接拒绝；规则最多 16 条，`value` ≤ 128 字符且禁止换行；
- `EXECUTE_COMMAND` 默认只允许玩家身份执行，控制台身份需显式开启
  `behaviors.allow-console-commands: true`，并内置 `op` / `stop` / `ban` 等黑名单；
- 服务端只接受已注册的脚本钩子名，粒子 / 音效 / 药水均走官方注册表查找。

新增指令：

```
/fantasyitem behaviors     # 查看当前已注入的行为树
```

---

## 7. 世界快速搭建（仅创造模式）

参考 Axiom 的「工具化建造」思路：把建造拆成 **模式 / 笔刷 / 选区 / 剪贴板 / 历史** 五个正交维度，
客户端即时预览并立即写方块，服务端以插件消息做权威落盘。

### 7.1 四种模式

| 模式 | 键 | 行为 |
| --- | --- | --- |
| 选区 SELECT | `G` / `H` | 点选两个角点，`L` 用持手方块填充整个立方体 |
| 笔刷 BRUSH | 左键 / 右键 | 按住左键抹除、右键涂抹，逐 tick 连续绘制 |
| 替换 REPLACE | 面板 | 「替换同块」只替换角点 A 的方块类型，「替换全部」替换所有非空气 |
| 粘贴 PASTE | `C` / `V` | `C` 复制选区，`V` 把剪贴板贴到准星前方 |

### 7.2 笔刷形状

`球体 · 立方 · 圆柱 · 平滑 · 侵蚀 · 噪点`，半径 1~32。笔刷用粒子勾勒范围，
每次落笔都作为一条独立历史记录，可以逐笔撤销。

### 7.3 快捷键

| 键 | 功能 | 键 | 功能 |
| --- | --- | --- | --- |
| `B` | 开关快速搭建 | `P` | 打开搭建面板 |
| `X` | 切换模式 | `[` `]` | 笔刷半径 ±1 |
| `G` `H` | 选区角点 A / B | `N` | 清空选区 |
| `L` | 填充选区 | `K` | 涂抹 / 抹除切换 |
| `C` | 复制选区 | `V` | 粘贴到准星 |
| `R` | 旋转剪贴板 90° | `M` | 镜像剪贴板 |
| `Z` | 撤销 | `Y` | 重做 |

### 7.4 剪贴板

复制时只记录非空气方块与相对坐标；粘贴时按 `旋转 → 镜像` 顺序变换坐标，
方块状态自身也会调用 `BlockState#rotate / #mirror`，因此楼梯、原木朝向会一并旋转。

### 7.5 双端同步与安全沙盒

```
Fabric 客户端                          Paper 服务端
BuildPayload(fantasy_item:build)  →   BuildChannelListener
                                       ├ 权限 fantasyitem.build
                                       ├ 创造模式校验（creative-only）
                                       ├ 维度一致性
                                       ├ 体积 ≤ max-volume
                                       ├ 半径 ≤ max-radius
                                       └ WorldEditService 执行
                                       ↓
BuildAckPayload(build_ack)        ←   回执 {ok, changed, message}
```

- 客户端与插件都会写方块，但写方块是幂等的，重复执行不会产生偏差；
- 没有安装插件（单人 / 原版服务器）时 `canSend` 为 false，自动退化为纯客户端建造；
- 服务端为每个玩家维护独立撤销栈，客户端 `Z` / `Y` 与 `/fantasyitem undo|redo` 等效；
- 一次报文上限 60000 字符，超出时客户端只做本地编辑。

### 7.6 服务端配置

```yaml
build:
  enabled: true          # 总开关
  creative-only: true    # 仅创造模式
  max-volume: 250000     # 单次操作最大方块数
  max-radius: 16         # 笔刷最大半径
  history-depth: 32      # 每玩家撤销步数
```

---

## 8. 构建

```bash
./gradlew :fabric-client:build     # 输出 fabric-client/build/libs/fantasy_item-1.0.0.jar
./gradlew :paper-server:build      # 输出 paper-server/build/libs/fantasy-item-paper-1.0.0.jar
```

版本矩阵集中在 [gradle.properties](gradle.properties)：

| 项 | 值 |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.165.2+26.3 |
| Paper API | 26.3.build.245-stable |
| 客户端 Java | 25（26.3 生态要求） |
| 服务端 Java | 25（Paper 26.3 API 的 class 文件版本为 69） |

> 26.x 的 Minecraft jar 已不再混淆，Loom 配置中不需要 `mappings` 行；
> 代码直接使用 Mojang 官方类名（`MappedRegistry`、`Identifier`、`BlockStateModel` 等）。
>
> 虽然需求文档写的是 Java 21，但 Minecraft 26.3 与 Paper 26.3 的字节码均为
> class file 69（Java 25），用 Java 21 的 `javac` 无法读取依赖，因此两个模块统一以
> Java 25 编译（`options.release = 25`）。`settings.gradle` 已引入 foojay 工具链解析器，
> Gradle 会自动下载所需的 JDK 25。

---

## 9. 已知限制

- 动态注册的物品/方块在**本次运行内无法注销**；删除 JSON 后重启即可完全移除。
- 修改显示名 / Lore 后，新发放的 `ItemStack` 立即生效；世界中已存在的旧物品保留旧文本。
- Blockbench 解析器聚焦 Java Block/Item 导出；`.bbmodel` 支持基础 elements，
  复杂骨骼 / 动画不会被烘焙（本引擎定位为静态物品与方块）。
- 服务端只是“载体 + 元数据”，完整的 3D 外观需要 Fabric 客户端与同步的资源包。
- 服务端行为规则只对携带 PDC 标记 `fantasy_item:definition` 的发放物品生效；
  原版物品不会触发任何动态行为。
- `ON_TICK` 由 `behaviors.tick-interval`（默认 5 tick）驱动，并按规则冷却限流；
  规则数量过多时请适当增大间隔。
- 快速搭建的笔刷同步按 250ms 节流，且服务端会按 `shape + radius` 重算范围；
  `NOISE` 形状两端使用各自的随机数，落块结果可能存在细微差异。
- 服务端修改方块使用 `applyPhysics = false`，不会触发沙砾下落、流体更新等连锁反应。

---

## 10. 许可证

MIT License，详见 [LICENSE](LICENSE)。
