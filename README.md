# Heavy industry（重工业）· Java 版工程骨架

本目录是「重工业」从 **JS 脚本模组** 转型为 **Java 模组（jar）** 的工程骨架，
基于官方 [Anuken/MindustryJavaModTemplate](https://github.com/Anuken/MindustryJavaModTemplate) 搭建。

> **当前阶段：只搭框架，尚未做任何逻辑改造。**
> 现有 JS 脚本与 JSON 内容可以原样继续跑。

---

## 一、先理解三件关键事实

### 1. Java 模组里 `scripts/*.js` 依然会执行

官方 `Mods.java#loadScripts()` 遍历**所有**模组，只要模组目录里存在 `scripts/` 就执行主脚本，
**并没有判断 `isJava()`**：

```java
public void loadScripts(){
    if(skipModCode) return;
    eachEnabled(mod -> {
        if(mod.root.child("scripts").exists()){   // ← 无 isJava() 门禁
            ...
            scripts.run(mod, main);
```

同理，`content/*.json` 也照旧由官方 `ContentParser` 解析。

**所以迁移可以是混合式、渐进的**：先让 jar 能编译能加载，再一个模块一个模块把 JS 翻成 Java。

### 2. 内部名必须保留 `重工业`

官方 `ContentParser.java` 会把内容名与 bundle key 自动加上「模组名 + `-`」前缀：

```java
block = make(resolve(...), mod + "-" + name);                       // 第 584 行
String entryName = type + "." + currentMod.name + "-" + name + "."; // 第 919 行
```

也就是说内容 `谷仓` 的实际名字是 `重工业-谷仓`，bundle key 是 `block.重工业-谷仓.name`，
贴图是 `重工业-xxx.png`。

**一旦把 `mod.hjson` 的 `name` 改成别的，现有的 113 个内容 JSON、bundle 文案、
贴图名、存档、地图、科技树引用会全部失配。** 所以：

| 字段 | 值 | 说明 |
|---|---|---|
| `name` | `重工业` | ★ 不要改，改了就是破坏性更新 |
| `displayName` | `Heavy industry` | 游戏内显示名，随便改 |
| `main` | `hiy.HeavyIndustry` | Java 入口类（ASCII，与内部名无关） |

### 3. 安卓只认 jar 里的 `classes.dex`

不能只打包 `.class`。构建链必须是 `javac → d8 → jar`，
本工程的 `gradlew deploy` 已自动完成这三步。

---

## 二、目录结构

```
Heavy industry-java/
├── mod.hjson                  # 模组清单（java: true）
├── build.gradle               # 构建脚本（桌面 jar + dex + 合并）
├── settings.gradle            # rootProject.name = "heavy-industry"（决定 jar 名）
├── gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/
├── .github/workflows/build.yml  # ★ GitHub 云编译
├── src/hiy/HeavyIndustry.java   # ★ Java 入口类（目前只做占位）
├── assets/                    # ★ 资源放这里，构建时平铺到 jar 根目录
│   ├── content/  sprites/  sprites-override/
│   ├── bundles/  sounds/   maps/   scripts/
│   └── README.md
└── 同步素材.sh                 # 从 JS 版一键同步资源
```

**为什么资源放 `assets/`？**
`build.gradle` 里用 `from("assets/")` 把 `assets/` 的**内容**平铺到 jar 根，
于是 `assets/sprites/a.png` 在 jar 内是 `sprites/a.png`。
而 Mindustry 读模组资源的位置正是模组根目录（`mod.root.child("sprites")`）。
这正是官方模板与其他 Java 模组（MCS / NewHorizon / DeepSpace）的统一约定。

---

## 三、怎么用

### 步骤 1：同步现有资源（可选，随时可做）

```bash
cd "/storage/emulated/0/文件/像素工厂/Heavy industry/Heavy industry-java"
bash 同步素材.sh
```

默认从 `素材/Heavy industry/`（内容最全的那份）同步 `content/ bundles/ sprites/ sounds/ maps/ scripts/`。
也可以指定源目录：`bash 同步素材.sh <源模组目录>`。

### 步骤 2：上传到 GitHub，用云编译出 jar

1. 在 GitHub 新建仓库，**仓库名建议就叫 `heavy-industry`**
   （工作流会校验 `build/libs/<仓库名>.jar`，与 `settings.gradle` 的 `rootProject.name` 对应）。
2. 把**本目录的全部内容**（含 `.github/`、`gradlew`、`gradle/`）上传上去。
3. push 后在仓库 **Actions** 页面等 1~3 分钟。
4. 在该次运行的 **Artifacts** 里下载 `heavy-industry-jar`，解压得到 `heavy-industry.jar`。
5. 安装到游戏：把 jar 放进
   `/storage/emulated/0/Android/data/io.anuke.mindustry/files/mods/`

> 没有 git 也能推：`MindustryBuild/gh-push.mjs` 支持用 token 直接上传文件到 GitHub
> （直连 github.com 不稳定时可走 `ghproxy.net` 代理）。

### 步骤 3（可选）：本地编译验证

本机 DSH 的 bash 里没有 java/d8，需在 **Termux** 里执行：

```bash
bash /storage/emulated/0/MindustryBuild/build-local.sh \
     "/storage/emulated/0/文件/像素工厂/Heavy industry/Heavy industry-java"
```

产物在 `MindustryBuild/out/`。

---

## 四、后续把 JS 翻成 Java 的路线

```
第 0 步  骨架能编译出 jar 且游戏能加载            ← 当前在这里
第 1 步  翻译最独立的模块（建议先翻 base/status.js 或 base/bullet.js）
         在 src/hiy/ 下建类 → 在 HeavyIndustry.loadContent() 里调用
         → 注释掉 assets/scripts/main.js 里对应的 require
第 2 步  逐模块推进（炮塔 → 单位 → 星球 → UI 功能）
第 3 步  全部翻完后删掉 assets/scripts/，变成纯 Java 模组
```

为了降低风险，建议**每次只翻一个模块**，翻完立刻 push 让云端编译，确认能加载再继续。

---

## 五、注意事项

1. **`minGameVersion` 必须是 154 或更高**（Java 模组的硬门槛，见官方 `Vars.minJavaModGameVersion`）。
   本项目设为 `158`，与本机 `MindustryBuild/tools/mindustry-core-v158.jar` 一致。
   若要支持更老的版本，需要靠 `legacyCompatible: true`，属于官方兼容模式，有未知风险。
2. **Mindustry / Arc 依赖只能是 `compileOnly`**，绝不能写 `implementation`，
   否则整个游戏 API 会被打进 jar。
3. 服务端（headless）相关代码必须先判断 `Vars.headless`，
   本工程的 `HeavyIndustry.region()` 已做示范。
4. 中文文件名与中文标识符在 Java 里能用，但**类名/包名建议保持 ASCII**，
   避免不同平台的编码问题（模组内部名 `重工业` 是字符串常量，不受此影响）。

---

## 六、参考

- 官方模板：https://github.com/Anuken/MindustryJavaModTemplate
- 官方游戏源码（本地）：`Heavy industry/其它/Mindustry-v156.zip`
- 本项目生态与 API 学习手册：
  `Heavy industry/学习笔记/像素工厂模组开发学习手册.md`
- 构建工具箱：`/storage/emulated/0/MindustryBuild/`（含本地编译脚本与 CI 模板）
