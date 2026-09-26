package hiy;

import arc.graphics.g2d.Draw;
import mindustry.graphics.Layer;

/**
 * 泛光（bloom）工具。
 *
 * Mindustry 的 bloom 只捕获一帧中 z 落在 **(Layer.bullet-0.02, Layer.effect+0.02)**
 * 区间内的绘制（见 Renderer.java:395-396）：
 * <pre>
 *   Draw.draw(Layer.bullet - 0.02f, bloom::capture);
 *   Draw.draw(Layer.effect + 0.02f, bloom::render);
 * </pre>
 * 而单位本体是整体画在 {@code Layer.flyingUnit = 115} 的，**在区间之外**，
 * 所以单位自己的特效默认不会发光 —— 这就是 DS 炮台"泛光好"的原因：
 * 它们的开火特效走 {@code Effect}（在 Layer.effect 上绘制），天然落进捕获区间。
 *
 * 用法：把想发光的绘制包起来即可（官方 ArmorPlateAbility / UnitSpawnAbility 也是这么做的）：
 * <pre>
 *   HIGlow.draw(() -> { Lines.circle(x, y, r); });
 * </pre>
 */
public class HIGlow{

    /** 泛光区间内的安全 z（Layer.bullet=100, Layer.effect=110）。 */
    public static final float z = Layer.effect;

    /** 在泛光区间内绘制一段内容。 */
    public static void draw(Runnable runnable){
        Draw.draw(z, runnable);
    }

    private HIGlow(){
    }
}
