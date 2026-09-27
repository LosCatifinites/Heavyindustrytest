package hiy;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.util.Tmp;
import mindustry.entities.Effect;
import mindustry.entities.effect.MultiEffect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;

/**
 * 自定义特效。
 *
 * 三部分：
 *   1. 复刻自 DeepSpace（裂片集群原有的两个）
 *   2. 开火特效（参考 DS 炮台 阳炎 / 霜降 / 冬至 / 罪碑 的写法）
 *   3. 「空间撕裂」——复刻 NewHorizon 的 {@code NHFx.collapserBulletExplode}（坍缩爆炸）
 *
 * 全部是 Effect，在 Layer.effect 上绘制，正好落在 Mindustry 的 bloom 捕获区间
 * (Layer.bullet-0.02, Layer.effect+0.02) 内，所以自带泛光。
 */
public class HIEffects{

    /** 复刻 IceEffects.rand（原作用它做随机散布）。 */
    public static final Rand rand = new Rand();

    // ================= DeepSpace 原作 =================

    /** 子弹飞行途中不断喷出的小三角。 */
    public static Effect layerBullet;

    /** 子弹命中 / 消散时的多边形扩散。 */
    public static Effect polyHit;

    // ================= 开火特效 =================

    /** 小口径炮口闪光（主炮用）。 */
    public static Effect muzzle;

    /** 大口径炮口闪光（轨道主炮用）。 */
    public static Effect muzzleHeavy;

    /** 蓄力光环（蓄能越高越频繁）。 */
    public static Effect chargeGlow;

    /** 蓄力起手闪光。 */
    public static Effect chargeStart;

    // ================= 空间撕裂 =================

    /**
     * 「空间撕裂」——复刻 NewHorizon 的 {@code NHFx.collapserBulletExplode}（坍缩爆炸）。
     * 结构：24 根随机放射的撕裂三角 + 收缩光环 + 中心闪光 + 30 根溅射细线 + 光源。
     * 用在主炮弹命中处（也可换到任何命中特效位）。
     */
    public static Effect spaceTear;

    public static void load(){
        // ---------- ① 裂片集群原有的两个 ----------
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

        // ---------- ② 炮口闪光（主炮）----------
        muzzle = new Effect(16f, e -> {
            Draw.blend(Blending.additive);
            Draw.color(Color.white, HIColors.b4, e.fin());

            float out = e.fout();
            float len = 30f * out;
            float wid = 9f * out;
            Drawf.tri(e.x, e.y, wid, len, e.rotation);
            Drawf.tri(e.x, e.y, wid * 0.8f, len * 0.55f, e.rotation + 180f);
            for(int i = 0; i < 2; i++){
                float a = e.rotation + (i == 0 ? 60f : -60f);
                Drawf.tri(e.x, e.y, wid * 0.55f, len * 0.45f, a);
            }
            Draw.color(HIColors.b4);
            Lines.stroke(2.6f * out);
            Lines.circle(e.x, e.y, 8f + 30f * e.fin());

            Draw.blend();
            Draw.reset();
            Drawf.light(e.x, e.y, 70f, HIColors.b4, 0.75f * out);
        });

        // ---------- ② 炮口闪光（轨道炮，重）----------
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
                for(int i = 0; i < 4; i++){
                    Lines.lineAngle(e.x, e.y, e.rotation + 45f + i * 90f, (24f + 46f * e.fin()) * out);
                }

                Draw.blend();
                Draw.reset();
                Drawf.light(e.x, e.y, 130f, Color.valueOf("ffd479"), 0.9f * out);
            }),
            new Effect(30f, e -> {
                Draw.color(Color.white, Color.valueOf("bbaa88"), e.fin());
                Lines.stroke(1.6f * e.fout());
                Lines.circle(e.x, e.y, 30f + 70f * Interp.pow3Out.apply(e.fin()));
                Draw.reset();
            })
        );

        // ---------- ② 蓄力：收缩光环 ----------
        chargeGlow = new Effect(40f, e -> {
            Draw.blend(Blending.additive);
            Draw.color(HIColors.b4);

            float fin = e.fin();
            Draw.alpha(0.55f * (1f - fin * 0.4f));
            Lines.stroke(2f + 2.5f * (1f - fin));
            Lines.circle(e.x, e.y, 46f * (1f - fin) + 8f);

            float rot = e.id * 7f + fin * 180f;
            for(int i = 0; i < 10; i++){
                float a = rot + i * 36f;
                float r1 = 46f * (1f - fin) + 10f;
                float r2 = r1 + 12f * e.fout();
                Lines.lineAngle(e.x + Angles.trnsx(a, r1), e.y + Angles.trnsy(a, r1), a, r2 - r1);
            }

            Draw.blend();
            Draw.reset();
            Drawf.light(e.x, e.y, 90f * (1f - fin) + 20f, HIColors.b4, 0.5f * (1f - fin));
        });

        // ---------- ② 蓄力起手 ----------
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

        // ---------- ③ 空间撕裂（NH 坍缩爆炸复刻，已大幅缩小 + 降亮）----------
        //   NH 原版 rad=132、外环 = rad*3 ≈ 396、中心纯白实心 Fill → 跨度近 800px，
        //   挂在每 3 秒 12 发的主炮弹上会整屏糊白。这里：
        //     rad 132→46（跨度 ~270px）、三角 24→14、中心不再用纯白、光源半径与强度减半。
        //   并且改挂到**轨道主炮命中**（reload 120，约 2 秒一发），局部展示。
        spaceTear = new Effect(70f, 520f, e -> {
            float rad = 46f;
            rand.setSeed(e.id);

            Draw.color(HIColors.b4, e.color, Math.min(e.fin() + 0.4f, 1f));
            float circleRad = e.fin(Interp.circleOut) * rad * 3f;
            Lines.stroke(5.5f * e.fout());
            Lines.circle(e.x, e.y, circleRad);

            // 14 根随机放射的撕裂三角
            for(int i = 0; i < 14; i++){
                Tmp.v1.set(1f, 0f).setToRandomDirection(rand).scl(circleRad);
                Drawf.tri(e.x + Tmp.v1.x, e.y + Tmp.v1.y,
                    rand.random(circleRad / 16f, circleRad / 12f) * e.fout(),
                    rand.random(circleRad / 4f, circleRad / 1.5f) * (1f + e.fin()) / 2f,
                    Tmp.v1.angle() - 180f);
            }

            // 中心闪光 + 溅射细线（降不透明度，避免糊成白团）
            e.scaled(32f, i -> {
                Draw.color(HIColors.b4, i.color, Math.min(i.fin() + 0.4f, 1f));
                Draw.alpha(0.5f * i.fout());
                Fill.circle(i.x, i.y, rad * i.fout() * 0.75f);
                Draw.alpha(1f);
                Lines.stroke(7f * i.fout());
                Lines.circle(i.x, i.y, i.fin(Interp.circleOut) * rad * 1.2f);
                Angles.randLenVectors(i.id, 18, rad / 3f, rad * i.fin(Interp.pow2Out), (x, y) -> {
                    Lines.lineAngle(i.x + x, i.y + y, Mathf.angle(x, y), i.fslope() * 14f + 6f);
                });
            });

            Draw.reset();
            Drawf.light(e.x, e.y, rad * 2.2f, e.color, 0.35f * e.fout());
        });
    }

    private HIEffects(){
    }
}
