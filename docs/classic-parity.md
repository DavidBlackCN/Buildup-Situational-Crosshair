# Classic Parity Matrix

基准：本地 `../1.20.1/` 工作树，而非在线上游。其 HEAD 为 `d15e3ea`
（0.6.0 Quilt 版本）；本地未提交修改迁移到 Fabric 1.20.1：Loom 1.3.10、
Loader 0.14.22、Yarn 1.20.1+build.10、Java 17，客户端入口改用
ClientModInitializer，GuiGraphics 改成 DrawContext。检测分支与 HEAD 一致。
旧版无 Fabric API 依赖、无独立检测服务；HUD tick 尾部计算结果，
renderCrosshair 的首个 drawTexture 由 ordinal=0 Redirect 替换。

| 情况 | 本地 1.20.1 实际行为 | 26.3 状态 |
| --- | --- | --- |
| MISS | DOT | 待 Stage 0 验收后迁移 |
| BLOCK / valid harvest | 硬度 >= 0 且 player.canHarvest(state) → BLOCK | 待迁移 |
| BLOCK / invalid harvest | BLOCK_NO_DROP → ERROR 纹理 | 待迁移 |
| ENTITY | 所有 ENTITY hit → ATTACK，无生物/敌对/可攻击性额外过滤 | 待迁移 |
| Creative | 方块直接 BLOCK，跳过硬度与工具判定 | 待迁移 |
| Unbreakable | 生存 ERROR；创造 BLOCK | 待迁移 |
| Empty hand / tools | 使用 vanilla canHarvest；非需工具方块空手可 BLOCK | 待迁移 |
| Bow | 无覆盖，代码明确返回 false（包括拉弓中） | 待迁移 |
| Crossbow | 已装填 → ATTACK，覆盖 MISS/BLOCK/ENTITY；未装填保留目标结果 | 待迁移 |
| 双手弓/弩 | 副手弓或弩覆盖主手候选；副手弓可阻止主手已装填弩覆盖 | 待迁移 |
| Null target | 不更新，可能保留先前结果；初始无结果时 vanilla fallback | 待设计保守 fallback |
| F1 / third person / spectator / debug | 仅替换 vanilla 内部绘制调用，因此继承其入口及分支条件；没有独立强制绘制 | 待核对 26.3 实际源码 |
| Spyglass / GUI | 无额外处理，完全取决于 vanilla 是否调用被 Redirect 的绘制点 | 待核对 26.3 实际源码 |

四种纹理位于 `assets/situational_crosshair/textures/gui/`，绘制尺寸为
15×15。BLOCK_SILK 与 BLOCK_HARVEST 同纹理，前者没有选择路径。
ERROR 枚举与 BLOCK_NO_DROP 同纹理；新实现应将它作为 Classic 呈现选择。

本表是源码审计结果，不代表已进行旧版或新版游戏内视觉验收。
