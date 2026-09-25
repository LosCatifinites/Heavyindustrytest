package hiy;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Interp;
import arc.math.Rand;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;

/**
 * 自定义特效。
 * 复刻自 DeepSpace 的 ice.world.meta.IceEffects 中裂片集群用到的两个。
 */
public class HIEffects{

    /** 复刻 IceEffects.rand（原作用它做随机散布）。 */
    public static final Rand rand = new Rand();

    /** 子弹飞行途中不断喷出的小三角，复刻 IceEffects.layerBullet。 */
    public static Effect layerBullet;

    /** 子弹命中 / 消散时的多边形扩散，复刻裂片集群内联的 hitEffect。 */
    public static Effect polyHit;

    public static void load(){
        layerBullet = new Effect(45f, e -> {
            Draw.color(e.color);
            Draw.z(Layer.effect);
            e.x += rand.random(-1f, 1f);
            e.y += rand.random(-1f, 1f);
            float fl = 8f * Interp.pow3Out.apply(e.fout());
            float rot = e.data instanceof Number ? ((Number)e.data).floatValue() : 0f;
            Drawf.tri(e.x, e.y, fl, fl, rot);
        });

        polyHit = new Effect(60f, e -> {
            Draw.color(HIColors.b4);
            Lines.stroke(Interp.pow3Out.apply(e.fout()) * 3f);
            Lines.poly(e.x, e.y, 8, Interp.pow3Out.apply(e.fin()) * 36f + 36f, e.rotation);
        });
    }

    private HIEffects(){
    }
}
