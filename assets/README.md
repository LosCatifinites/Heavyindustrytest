# assets/ 目录说明

**这里放的全部资源，构建时会平铺进 jar 的根目录。**

例如 `assets/sprites/a.png` → jar 内就是 `sprites/a.png`。
Mindustry 读取模组资源的位置正是模组根目录（官方 `Mods.java` 里
`mod.root.child("sprites")` / `mod.root.child("bundles")` 等），
所以源码放 `assets/`、打包后自动落到根目录 —— 这是 Java 模组的标准约定。

## 各子目录对应关系

| 源码目录 | jar 内位置 | 用途 |
|---|---|---|
| `assets/bundles/` | `bundles/` | 本地化文案 `bundle_zh_CN.properties` |
| `assets/sprites/` | `sprites/` | 贴图（会被自动加 `重工业-` 前缀） |
| `assets/sprites-override/` | `sprites-override/` | 覆盖原版贴图 |
| `assets/content/` | `content/` | 官方 ContentParser 读的内容 JSON（**JS 版原样搬过来即可**） |
| `assets/sounds/` | `sounds/` | 音效 .ogg |
| `assets/maps/` | `maps/` | 地图 .msav |
| `assets/scripts/` | `scripts/` | **JS 脚本，Java 模组里依然会执行** |

## 当前状态

处于「先搭框架」阶段，各目录基本为空。
请用项目根目录的 `同步素材.sh` 从现有 JS 版一键同步资源，或手动复制。

> ⚠️ `assets/sprites/frog.png` 是官方模板自带的冒烟测试贴图，
> 用来验证「贴图打包 + atlas 查找」链路是否正常，确认无误后可删除。
