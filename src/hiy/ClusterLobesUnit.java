package hiy;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Groups;
import mindustry.gen.Healthc;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Trail;

/**
 * 「裂片集群」的实体实现。
 *
 * 从 DeepSpace 的 ice.content.unit.裂片集群.ClusterLobesUnit 复刻：
 *   - 原版基类 ice.world.content.unit.entity.base.Entity  -> 官方 UnitEntity
 *   - 原版 trails 字段（UnitEntity 没有）                 -> 自带 Trail[]
 *   - EntityRegistry.getClassId()                        -> EntityMapping 注册的 id
 *   - universecore 的 IMathf.sint                        -> 本类的 sint()
 *
 * 机制：
 *   1. 外圈 20 个顶点每帧旋转，并逐个拦截敌方子弹（挡下的子弹计入 resistCont）
 *   2. resistCont 转化为护甲与伤害减免（上限 30%）
 *   3. 每 60 帧朝圈上每个顶点发射环状激光
 *   4. 对 80 距离内的敌人持续施加「电磁脉冲」
 *
 * ★ 绘制分三层（重要）：
 *   drawTrails()  —— 尾迹，画在本体之下
 *   super.draw()  —— 官方 UnitType.draw()：本体 / 武器 / 部件 / 护盾 / 【全部 Ability】
 *   drawOverlay() —— 自定义环线与眼睛，走 bloom 区间所以会泛光
 *
 *   DeepSpace 原作在这里写的是 `//super.draw()`（注释掉了），所以本体、武器、护盾与
 *   所有 Ability 的视觉效果都不会画。重工业已经加了镜盾/护盾/光环，**必须**调用 super。
 */
public class ClusterLobesUnit extends HIUnitEntity{

    /** resistCont 每积累这么多，护甲 +1。 */
    public static final float armorCont = 50f;
    /** resistCont 每积累这么多，伤害减免 +1%。 */
    public static final float immunityCont = 250f;
    /** 外圈顶点数。 */
    public static final int ringPoints = 20;
    /** 尾迹长度。 */
    public static final int trailLength = 80;
    /** 尾迹宽度。 */
    public static final float trailWidth = 4f;

    public float timer = 0f;
    public Teamc target;
    public int resistCont = 0;

    /** 尾迹当前的平滑位置。 */
    public float xx = 0f, yy = 0f;

    public final Trail[] trails = {new Trail(trailLength), new Trail(trailLength)};

    /** 眼睛相对本体的偏移，会朝目标转动。 */
    public final Vec2 eye = new Vec2(0f, 8f);

    /** 本体前方的三根尖刺。 */
    public final Vec2[] tris = {
        new Vec2(0f, 24f), new Vec2(5f, 24f), new Vec2(10f, 24f),
        new Vec2(0f, -24f), new Vec2(-5f, -24f), new Vec2(-10f, -24f)
    };

    /** 外圈顶点，半径 80，均匀分布。 */
    public final Vec2[] outsideRing = new Vec2[ringPoints];

    /** 外圈半径。 */
    public final float rs = 80f;
    /** 内圈半径。 */
    public final float r = 0.5f * 80f;

    public ClusterLobesUnit(){
        for(int i = 0; i < ringPoints; i++){
            outsideRing[i] = new Vec2(0f, 80f).rotate(360f / ringPoints * i);
        }
        shadowAlpha = 0f;
    }

    /** 复刻 universecore.math.IMathf.sint：基于当前时间的正弦波。 */
    public static float sint(float amplitude, float angularVelocity, float initialPhase, float offset){
        return amplitude * Mathf.sin(angularVelocity * Time.time + initialPhase) + offset;
    }

    /** 极坐标转向量。 */
    public static Vec2 angleTrns(float ang, float rad){
        return new Vec2(Angles.trnsx(ang, rad), Angles.trnsy(ang, rad));
    }

    @Override
    public boolean isFlying(){
        return true;
    }

    /** 由 resistCont 换算的伤害减免，上限 30%。 */
    public float immunity(){
        return Math.min(resistCont / immunityCont / 100f, 0.3f);
    }

    @Override
    public void damage(float amount){
        super.damage(amount - (amount * immunity()));
    }

    @Override
    public void update(){
        super.update();

        // ---- 尾迹：两个反向旋转的点，平滑追随 ----
        if(!Vars.headless && trailLength > 0){
            for(int i = 0; i < trails.length; i++){
                float angle0 = Time.time;
                float angle1 = -1.1f * angle0;
                Vec2 xy = angleTrns(angle0, rs);
                Vec2 xy2 = angleTrns(angle1, r);

                float targetX = x + xy.x + xy2.x;
                float targetY = y + xy.y + xy2.y;

                float smoothSpeed = 0.01f;
                xx = Mathf.lerpDelta(xx, targetX, smoothSpeed);
                yy = Mathf.lerpDelta(yy, targetY, smoothSpeed);
                trails[i].length = trailLength;
                trails[i].update(xx, yy);
            }
        }

        // ---- 环状激光：每 60 帧，圈上每个顶点各打一发 ----
        target = Units.closestTarget(team, x, y, range());
        timer++;
        if(timer > 60f && target != null){
            timer = 0f;
            for(Vec2 it : outsideRing){
                float subx = it.x + x;
                float suby = it.y + y;
                if(Mathf.dst(subx, suby, target.getX(), target.getY()) < HIBullets.ringLaser.length){
                    float ang = Angles.angle(target.getX(), target.getY(), subx, suby) + 180f;
                    HIBullets.ringLaser.create(this, subx, suby, ang);
                }
            }
        }

        for(Vec2 it : tris){
            it.rotate(1f);
        }

        // ---- 电磁脉冲光环 ----
        Units.nearbyEnemies(team, x, y, 80f, u -> u.apply(HIStatus.electromagneticPulse, 60f * 30f));

        // ---- 外圈拦截敌方子弹，挡下即累加 resistCont ----
        for(Vec2 it : outsideRing){
            float bx = it.x + x - 4f;
            float by = it.y + y;
            Groups.bullet.intersect(bx, by, 8f, 8f, b -> {
                if(b.team == team) return;
                if(b.owner instanceof Healthc){
                    ((Healthc)b.owner).damage(b.damage);
                }
                b.remove();
                resistCont++;
            });
            it.rotate(-0.25f);
        }

        // ---- resistCont 转护甲（上限 100）----
        if(armor < 100f){
            armor = resistCont / armorCont;
        }
    }

    // ==================================================================
    //  绘制
    // ==================================================================

    @Override
    public void draw(){
        drawTrails();

        // ★★★ 官方 UnitType.draw()：
        //   本体 / 武器 / 部件(HaloPart) / 护盾(drawShield) / 全部 Ability 的 draw
        //   漏掉这一句，镜盾、光环场、黑洞外环、护盾再生场的效果全都不会显示。
        super.draw();

        drawOverlay();
    }

    /** 尾迹（画在本体之下）。 */
    private void drawTrails(){
        if(Vars.headless || trailLength <= 0) return;

        float z = Draw.z();
        Draw.z(Layer.effect);
        for(Trail t : trails){
            t.draw(HIColors.b4, trailWidth);
        }
        Draw.z(z);
    }

    /**
     * 自定义环线与眼睛。
     *
     * 发光的部分（环线 / 三角形 / 尖刺 / 眼睛 / 内环）走 {@link HIGlow}，
     * 也就是提交到 Layer.effect 上 —— 正好在 Mindustry 的 bloom 捕获区间内，所以会泛光。
     * 黑色底盘环留在 bloom 区间之外（Layer.bullet 之下），保持"实心挡光"的观感。
     */
    private void drawOverlay(){
        if(Vars.headless) return;

        HIGlow.draw(() -> {
            // ---- 环线 ----
            Draw.color(HIColors.b4);
            Lines.stroke(3f);
            Fill.circle(xx, yy, sint(0.5f, 0.1f, 0f, 5f));

            for(int i = 0; i + 1 < outsideRing.length; i++){
                Vec2 cur = outsideRing[i], next = outsideRing[i + 1];
                Lines.line(cur.x + x, cur.y + y, next.x + x, next.y + y, false);
            }
            Lines.line(outsideRing[0].x + x, outsideRing[0].y + y,
                       outsideRing[outsideRing.length - 1].x + x,
                       outsideRing[outsideRing.length - 1].y + y, false);

            // ---- 圈上的脉动三角 ----
            for(int i = 0; i < outsideRing.length; i++){
                Vec2 it = outsideRing[i];
                float fl = 10f * Mathf.sin(Time.time * 0.1f - i) + 6f;
                Drawf.tri(x + it.x, y + it.y, 8f, 8f + fl, it.angle());
                Drawf.tri(x + it.x, y + it.y, 8f, -8f - fl, it.angle());
            }

            // ---- 本体尖刺 ----
            for(int i = 0; i < tris.length; i++){
                Vec2 it = tris[i];
                it.setLength(10f * Mathf.sin(Time.time * 0.1f - i) + 24f + 8f);
                Drawf.tri(x + it.x, y + it.y, 8f, 40f, it.angle());
            }

            // ---- 内环 ----
            Lines.stroke(sint(0.5f, 0.2f, 0f, 8f));
            Lines.circle(x, y, 24f);

            // ---- 眼睛：玩家操控时看准星，否则看目标 ----
            Unit playUnit = Vars.player == null ? null : Vars.player.unit();
            float ang;
            if(playUnit == this){
                ang = Angles.angle(x, y, aimX, aimY);
            }else if(target != null){
                ang = Angles.angle(x, y, target.getX(), target.getY());
            }else{
                ang = 0f;
            }
            eye.setAngle(Angles.moveToward(eye.angle(), ang, 6f * Time.delta));
            Fill.circle(x + eye.x, y + eye.y, sint(0.5f, 0.1f, 0f, 5f));

            Draw.reset();

            // ---- 光源：让整团特效在暗处也发光（真正的"泛光"来源之一）----
            Drawf.light(x, y, rs * 1.9f, HIColors.b4, 0.45f);
        });

        // ---- 黑色底盘环（保持在 bloom 区间之外，压在最下层）----
        Draw.color(Color.black);
        Draw.z(Layer.bullet - 1f);
        Fill.circle(x, y, 24f);
        Draw.reset();
    }

    @Override
    public void read(Reads read){
        super.read(read);
        resistCont = read.i();
    }

    @Override
    public void write(Writes write){
        super.write(write);
        write.i(resistCont);
    }
}
