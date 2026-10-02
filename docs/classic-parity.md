# Classic Parity Matrix

基准：本地 `../1.20.1/` 工作树，而非在线上游。其 HEAD 为 `d15e3ea`
（0.6.0 Quilt 版本）；本地未提交修改迁移到 Fabric 1.20.1：Loom 1.3.10、
Loader 0.14.22、Yarn 1.20.1+build.10、Java 17，客户端入口改用
ClientModInitializer，GuiGraphics 改成 DrawContext。检测分支与 HEAD 一致。
旧版无 Fabric API 依赖、无独立检测服务；HUD tick 尾部计算结果，
renderCrosshair 的首个 drawTexture 由 ordinal=0 Redirect 替换。

| 情况 | 本地 1.20.1 实际行为 | 26.3 状态 |
| --- | --- | --- |
| MISS | DOT | 已恢复；集成测试通过 |
| BLOCK / valid harvest | 硬度 >= 0 且 player.canHarvest(state) → BLOCK | 已恢复；集成测试通过 |
| BLOCK / invalid harvest | BLOCK_NO_DROP → ERROR 纹理 | 已恢复；集成测试通过 |
| ENTITY | 所有 ENTITY hit → ATTACK，无生物/敌对/可攻击性额外过滤 | 已恢复；集成测试通过 |
| Creative | 方块直接 BLOCK，跳过硬度与工具判定 | 已恢复；集成测试通过 |
| Unbreakable | 生存 ERROR；创造 BLOCK | 已恢复；集成测试通过 |
| Empty hand / tools | 使用 vanilla canHarvest；非需工具方块空手可 BLOCK | 已恢复；集成测试通过 |
| Bow | 无覆盖，代码明确返回 false（包括拉弓中） | 已恢复；集成测试通过 |
| Crossbow | 已装填 → ATTACK，覆盖 MISS/BLOCK/ENTITY；未装填保留目标结果 | 已恢复；集成测试通过 |
| 双手弓/弩 | 副手弓或弩覆盖主手候选；副手弓可阻止主手已装填弩覆盖 | 已恢复；集成测试通过 |
| Null target | 不更新，可能保留先前结果；初始无结果时 vanilla fallback | Vanilla fallback；测试通过 |
| F1 / third person / spectator / debug | 仅替换 vanilla 内部绘制调用，因此继承其入口及分支条件；没有独立强制绘制 | 保留 Vanilla 判定；见下文 |
| Spyglass / GUI | 无额外处理，完全取决于 vanilla 是否调用被 Redirect 的绘制点 | 保留 Vanilla 判定；见下文 |

四种纹理位于 `assets/situational_crosshair/textures/gui/`，绘制尺寸为
15×15。BLOCK_SILK 与 BLOCK_HARVEST 同纹理，前者没有选择路径。
ERROR 枚举与 BLOCK_NO_DROP 同纹理；新实现应将它作为 Classic 呈现选择。

## 26.3 实施结果

现已完成迁移：MISS / ENTITY /
valid harvest / invalid harvest / creative / unbreakable / bow / crossbow /
双手优先级均由真实客户端集成测试验证（16 项）。
四种 Classic 资源已原样迁移到 `buildup_situational_crosshair` namespace。

| 边界 | 26.3 结果与验证 |
| --- | --- |
| F1 | 隐藏；调用实际 Hud.toggle 并验证无自定义或普通 Vanilla 准星提交 |
| 第三人称 | 隐藏；实际 HUD 提交测试通过 |
| Spectator MISS | 隐藏；实际 HUD 提交测试通过 |
| Spectator 可交互目标 | 保留 Vanilla 内部判定；尚待逐项人工检查 |
| Debug 3D crosshair | 不提交 Classic 或普通 Vanilla 2D 准星；测试通过 |
| Spyglass | 26.3 Vanilla 仍执行普通准星绘制；Classic 跟随，测试通过 |
| Null target | 使用 Vanilla fallback，不再显示上一 tick 的陈旧 Classic 结果 |
| GUI / loading | 保留原始 HUD 层调用条件；主菜单无自定义绘制。完整 GUI 组合待人工检查 |

实现差异：逐帧读取 vanilla hitResult，减少旧 tick 缓存延迟；使用当前
BlockState.getDestroySpeed(level,pos) 而非旧 Block 的固定硬度；未知目标保守回退。
无额外敌对实体过滤，弓仍无特判，副手优先规则原样保留。

SHA-256（新旧四张 PNG 逐字节一致）：

| 文件 | SHA-256 |
| --- | --- |
| crosshair_attack.png | 225EA90E45BA23425744FD0F3D42D505CC254BE5277B72F189AB4E7908BD20E7 |
| crosshair_block.png | 314B3519A188EBD8D1928F3C97BF9623F8448F5044624859FD283B1A85E4CDA3 |
| crosshair_dot.png | 52E03987189F2BED70092FD89304E2FF4036B19B8450E84B37DAC30342E6F0DA |
| crosshair_error.png | 80CD6439C9E49F263327F257C1DC53A9D179953B5D9D90A8F93F16B8564AB67B |
