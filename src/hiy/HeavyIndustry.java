package hiy;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;

/**
 * Heavy industry（重工业）Java 版入口。
 *
 * ============================ 重要说明 ============================
 * Mindustry 的 Java 模组里，scripts/*.js 依然会照常执行。
 * 官方 Mods.java#loadScripts() 遍历所有模组，只要目录里存在 scripts/ 就跑，
 * 并不检查 isJava()。content/*.json 也照样由官方 ContentParser 解析。
 *
 * 所以本工程当前处于「混合模式」：
 *   - content/ sprites/ bundles/ sounds/ maps/  -> 原样复用（无需 Java 化）
 *   - scripts/*.js                              -> 继续跑（无需 Java 化）
 *   - 本 Java 类                                -> 目前只做入口占位
 *
 * 后续要把某个 JS 模块翻成 Java 时，就在这里挂载它，
 * 并把 scripts/main.js 里对应的 require 注释掉，逐模块迁移即可。
 * =================================================================
 */
public class HeavyIndustry extends Mod{

    /** 模组内部名，必须与 mod.hjson 的 name 保持一致。
     *  官方 ContentParser 会给内容名与 bundle key 加上「本名 + "-"」前缀，
     *  改动它会让现有内容/贴图/存档全部失配。 */
    public static final String MOD_NAME = "重工业";

    public HeavyIndustry(){
        Log.info("[重工业/Java] 入口类已构造");

        // 客户端加载完成后触发（纯服务端 / headless 下不会触发）
        Events.on(ClientLoadEvent.class, e -> {
            Log.info("[重工业/Java] ClientLoadEvent —— Java 侧骨架运行正常");
        });
    }

    /**
     * Java 侧的内容注册入口。
     * 目前留空：内容全部由 assets/content/*.json 与 assets/scripts/*.js 提供。
     * 迁移某个 JS 模块时，在这里调用它的 load()。
     */
    @Override
    public void loadContent(){
        Log.info("[重工业/Java] loadContent() —— 当前未接管任何内容，仍由 JSON/JS 提供");

        // 将来启用（示例）：
        // HIBlocks.load();
        // HIUnits.load();
        // HIStatusEffects.load();
    }

    /** 是否无头服务端。绘制 / UI 相关代码必须先判断，否则服务端会崩。 */
    public static boolean headless(){
        return Vars.headless;
    }

    /** 贴图查找：结果为 "重工业-<name>"，与 JS 版 lib.region() 行为一致。 */
    public static TextureRegion region(String name){
        if(Vars.headless) return null;
        return Core.atlas.find(MOD_NAME + "-" + name, "error");
    }
}
