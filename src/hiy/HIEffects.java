package hiy;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import mindustry.entities.Effect;
import mindustry.entities.effect.MultiEffect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;

/**
 * 自定义特效。
 *
 * 分两部分：
 *   1. 复刻自 DeepSpace（裂片集群原有的两个）
 *   2. 新增的「开火特效」—— 参考 DS 炮台（阳炎 / 霜降 / 冬至 / 罪碑）的写法：
 *      炮口闪光 + 冲击波 + 光源（{@code Drawf.light}）。
 *      Effect 本身在 Layer.effect 上绘制，正好落在 Mindustry 的 bloom 捕获区间里，
 *      所以这些特效会自带泛光。
 */
public class HIEffects{

    /** 复刻 IceEffects.rand（原作用它做随机散布）。 */
    public static final Rand rand = new Rand();

    // ================= DeepSpace 原作 =================

    /** 子弹飞行途中不断喷出的小三角。 */
    public static Effect layerBullet;

    /** 子弹命中 / 消散时的多边形扩散。 */
    public static Effect polyHit;

    // ================= 新增：开火特效 =================

    /** 小口径炮口闪光（主炮用）。 */
    public static Effect muzzle;

    /** 大口径炮口闪光（轨道主炮用）。 */
    public static Effect muzzleHeavy;

    /** 蓄力特效（武器 chargeEffect 用）。 */
    public static Effect chargeGlow;

    /** 蓄力起手特效。 */
    public static Effect chargeStart;

    public static void load(){
        // ---------- 裂片集群原有的两个 ----------
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

        // ---------- 炮口闪光（主炮）----------
        muzzle = new Effect(16f, e -> {
            Draw.blend(Blending.additive);
            Draw.color(Color.white, HIColors.b4, e.fin());

            float out = e.fout();
            float len = 30f * out;
            float wid = 9f * out;
            // 前向主焰 + 后向回焰
            Drawf.tri(e.x, e.y, wid, len, e.rotation);
            Drawf.tri(e.x, e.y, wid * 0.8f, len * 0.55f, e.rotation + 180f);
            // 侧向两片小焰
            for(int i = 0; i < 2; i++){
                float a = e.rotation + (i == 0 ? 60f : -60f);
                Drawf.tri(e.x, e.y, wid * 0.55f, len * 0.45f, a);
            }
            // 冲击环
            Draw.color(HIColors.b4);
            Lines.stroke(2.6f * out);
            Lines.circle(e.x, e.y, 8f + 30f * e.fin());

            Draw.blend();
            Draw.reset();

            Drawf.light(e.x, e.y, 70f, HIColors.b4, 0.75f * out);
        });

        // ---------- 炮口闪光（轨道炮）----------
        muzzleHeavy = new MultiEffect(
            new Effect(22f, e -> {
                Draw.blend(Blending.additive);
                Draw.color(Color.white, Color.valueOf("ffe9a8"), e.fin());

                float out = e.fout();
                Drawf.tri(e.x, e.y, 16f * out, 58f * out, e.rotation);
                Drawf.tri(e.x, e.y, 12f * out, 34f * out, e.rotation + 180f);

                Draw.color(Color.valueOf("ffd479"));
                Lines.stroke(4f * out);
                Lines.circle(e.x, e.y, 14f + 56f * Interp.pow2Out.apply(e.fin()));
                // 十字射线
                for(int i = 0; i < 4; i++){
                    Lines.lineAngle(e.x, e.y, e.rotation + 45f + i * 90f, (24f + 46f * e.fin()) * out);
                }

                Draw.blend();
                Draw.reset();
                Drawf.light(e.x, e.y, 130f, Color.valueOf("ffd479"), 0.9f * out);
            }),
            new Effect(30f, e -> {
                // 慢速扩散的尘环
                Draw.color(Color.white, Color.valueOf("bbaa88"), e.fin());
                Lines.stroke(1.6f * e.fout());
                Lines.circle(e.x, e.y, 30f + 70f * Interp.pow3Out.apply(e.fin()));
                Draw.reset();
            })
        );

        // ---------- 蓄力：收缩光环 ----------
        chargeGlow = new Effect(40f, e -> {
            Draw.blend(Blending.additive);
            Draw.color(HIColors.b4);

            float fin = e.fin();
            Draw.alpha(0.55f * (1f - fin * 0.4f));
            Lines.stroke(2f + 2.5f * (1f - fin));
            Lines.circle(e.x, e.y, 46f * (1f - fin) + 8f);

            // 向内收拢的刻度
            float rot = e.id * 7f + fin * 180f;
            for(int i = 0; i < 10; i++){
                float a = rot + i * 36f;
                float r1 = 46f * (1f - fin) + 10f;
                float r2 = r1 + 12f * e.fout();
                Lines.lineAngle(e.x + arc.math.Angles.trnsx(a, r1), e.y + arc.math.Angles.trnsy(a, r1), a, r2 - r1);
            }

            Draw.blend();
            Draw.reset();
            Drawf.light(e.x, e.y, 90f * (1f - fin) + 20f, HIColors.b4, 0.5f * (1f - fin));
        });

        // ---------- 蓄力起手 ----------
        chargeStart = new Effect(50f, e -> {
            Draw.blend(Blending.additive);
            Draw.color(HIColors.b4);
            float fin = Interp.pow3Out.apply(e.fin());
            Draw.alpha(0.5f * e.fout());
            Fill.circle(e.x, e.y, 8f + 34f * fin);
            Draw.blend();
            Draw.reset();
            Drawf.light(e.x, e.y, 70f, HIColors.b4, 0.6f * e.fout());
        });
    }

    private HIEffects(){
    }
}
