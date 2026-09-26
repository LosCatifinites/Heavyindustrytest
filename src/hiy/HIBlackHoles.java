package hiy;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.Shader;
import arc.struct.FloatSeq;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;

/**
 * 黑洞「空间扭曲」渲染器。
 *
 * 复刻自 EU（ExtraUtilities）的：
 *   - {@code ExtraUtilities/graphics/MainRenderer$BlackHole}（黑洞数据 + 每帧登记）
 *   - {@code ExtraUtilities/graphics/MainShader$HoleShader}（加载 shaders/TearingSpace.frag）
 *   - 资产 {@code assets/shaders/TearingSpace.frag}（原作者 MEEPofFaith）
 * 渲染管线本身照抄官方 {@code mindustry.graphics.Shaders.ShockwaveShader} 的写法：
 *   Trigger.preDraw  → 把世界画进 FrameBuffer
 *   Trigger.postDraw → 用 TearingSpace 着色器把该 FrameBuffer blit 回屏幕（整屏扭曲）
 *
 * 用法（由 {@link HIBlackHoleAbility} 每帧自动调用）：
 *   HIBlackHoles.add(世界x, 世界y, 内半径, 外半径);
 * 数据每帧清空，所以必须每帧重新登记（这正是 Ability.update 的节奏）。
 */
public class HIBlackHoles{

    /** 同时生效的黑洞上限，必须与 TearingSpace.frag 里的 MAX_COUNT 一致。 */
    public static final int max = 8;
    /** 每个黑洞占用的 float 数：x, y, 内半径, 外半径。 */
    public static final int stride = 4;

    /** x, y, inRadius, outRadius … */
    private static final FloatSeq data = new FloatSeq();
    private static final FloatSeq uniforms = new FloatSeq();
    private static final FrameBuffer buffer = new FrameBuffer();

    private static boolean hadAny = false;
    private static BlackHoleShader shader;

    public static boolean enabled = true;

    /** 在入口类里调用一次（必须在 loadContent 之前或其中，客户端才会生效）。 */
    public static void load(){
        if(Vars.headless){
            shader = null;
            return;
        }

        shader = new BlackHoleShader();

        Events.run(Trigger.preDraw, () -> {
            hadAny = enabled && data.size > 0;

            if(hadAny){
                buffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
                buffer.begin(Color.clear);
            }
        });

        Events.run(Trigger.postDraw, () -> {
            if(hadAny){
                buffer.end();
                Draw.blend(Blending.disabled);
                buffer.blit(shader);
                Draw.blend();
            }

            // 每帧重新登记，避免单位消失后黑洞残留
            data.clear();
        });
    }

    /** 登记一个黑洞（世界坐标 + 内外半径）。 */
    public static void add(float x, float y, float inRadius, float outRadius){
        if(shader == null || !enabled) return;
        if(data.size / stride >= max) return;
        data.add(x, y, inRadius, outRadius);
    }

    public static boolean active(){
        return shader != null && data.size > 0;
    }

    public static class BlackHoleShader extends Shader{
        public BlackHoleShader(){
            super(modFile("screenspace.vert"), modFile("TearingSpace.frag"));
        }

        @Override
        public void apply(){
            int count = data.size / stride;

            setUniformi("u_blackholecount", count);

            if(count > 0){
                setUniformf("u_resolution", Core.camera.width, Core.camera.height);
                setUniformf("u_campos",
                        Core.camera.position.x - Core.camera.width / 2f,
                        Core.camera.position.y - Core.camera.height / 2f);

                uniforms.clear();
                for(int i = 0; i < count; i++){
                    int o = i * stride;
                    uniforms.add(data.items[o], data.items[o + 1], data.items[o + 2], data.items[o + 3]);
                }

                setUniform4fv("u_blackholes", uniforms.items, 0, uniforms.size);
            }
        }
    }

    /** 模组包内的 shaders/ 目录（jar 内即 shaders/xxx，与 Core.files.internal 同级）。 */
    private static Fi modFile(String name){
        return Vars.mods.getMod(HeavyIndustry.class).root.child("shaders").child(name);
    }

    private HIBlackHoles(){
    }
}
