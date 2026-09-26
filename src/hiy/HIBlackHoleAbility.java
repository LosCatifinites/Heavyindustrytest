package hiy;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;

/**
 * 单位用的「黑洞 / 能量吸引」能力。
 *
 * 吸引力来源：
 *   - EU（ExtraUtilities）scripts/block/turret/sucker.js —— 牵引逻辑
 *   - DeepSpace ice/entities/bullet/BlockHoleBulletType.kt —— 距离衰减 + 百分比持续伤害
 *
 * 视觉：**纯矢量绘制的外环拉伸**（不吞掉本体、不使用像素扭曲着色器）。
 *   - 外环半径默认 = outRadius（贴到最外圈）
 *   - 环上若干条放射状「拉伸线」，长度随时间脉动 → 拉伸感
 *   - 环本身按 ellipse 变成椭圆并脉动
 *   若仍想要 EU 那种整屏像素扭曲，把 {@link #shader} 设 true（会调用 {@link HIBlackHoles}）。
 *
 * 挂到单位上（示例见 HIUnits）：
 *   unitType.abilities.add(new HIBlackHoleAbility(220f, 200f));
 */
public class HIBlackHoleAbility extends Ability{

    // ---------- 力学 ----------
    /** 吸引半径（像素）。 */
    public float range = 220f;
    /** 基础吸引加速度（像素/帧²，60fps 基准）。 */
    public float pullAccel = 0.16f;
    /** 越靠近中心越强的附加加速度。 */
    public float pullBonus = 0.30f;
    /** 被吸单位的速度上限，防止瞬间糊脸。 */
    public float maxPullSpeed = 6f;
    /** true = 走 impulse（力/质量，巨型单位更抗拉）。 */
    public boolean useImpulse = false;
    /** useImpulse = true 时的「力」大小。 */
    public float force = 400f;

    // ---------- 伤害 / 状态 ----------
    /** 每秒固定伤害。 */
    public float damagePerSecond = 0f;
    /** 每秒按目标最大生命值扣的百分比（0.05 = 5%/s）。 */
    public float healthPercentPerSecond = 0.05f;
    /** 施加的状态；null 表示不施加。 */
    public StatusEffect status = StatusEffects.sapped;
    /** 状态刷新时长（秒）。 */
    public float statusDuration = 1.2f;
    /** 状态 / 伤害的结算间隔（帧）。 */
    public float statusInterval = 10f;

    // ---------- 视觉：外环拉伸 ----------
    /** 是否画外环拉伸。 */
    public boolean drawRing = true;
    /** 外环半径；<= 0 时自动取 outRadius。 */
    public float ringRadius = 0f;
    /** 环上放射状拉伸线的数量。 */
    public int stretchCount = 28;
    /** 每条拉伸线的基准长度（像素）。 */
    public float stretchLength = 34f;
    /** 拉伸脉动幅度（0~1，越大长短差异越明显）。 */
    public float stretchPulse = 0.5f;
    /** 外环椭圆拉伸量（0 = 正圆，0.14 = 明显椭圆）。 */
    public float ellipse = 0.14f;
    /** 外环整体旋转速度（度/帧）。 */
    public float ringSpin = 0.3f;
    /** 外环线宽。 */
    public float ringStroke = 2f;
    /** 环 / 拉伸线颜色。 */
    public Color edgeColor = Color.valueOf("be92f9");

    // ---------- 视觉：可选的整屏像素扭曲（默认关闭） ----------
    /** true 才会登记到 TearingSpace 整屏扭曲着色器（会扭曲地形像素，默认不用）。 */
    public boolean shader = false;
    /** 着色器内半径（仅 shader = true 时有意义）。 */
    public float inRadius = 28f;
    /** 着色器外半径（仅 shader = true 时有意义）。 */
    public float outRadius = 200f;

    /** 每实例的结算计时（Ability 会按单位 copy 一份）。 */
    protected float timer = 0f;

    public HIBlackHoleAbility(){
    }

    public HIBlackHoleAbility(float range, float outRadius){
        this.range = range;
        this.outRadius = outRadius;
        this.ringRadius = outRadius;
    }

    @Override
    public void update(Unit unit){
        if(Vars.headless) return;

        if(shader){
            HIBlackHoles.add(unit.x, unit.y, inRadius, outRadius);
        }

        timer += Time.delta;
        boolean tick = timer >= statusInterval;
        if(tick) timer = 0f;

        final float tickScale = tick ? statusInterval / 60f : 0f;

        Units.nearbyEnemies(unit.team, unit.x, unit.y, range, u -> {
            if(u == unit || !u.hittable() || !u.checkTarget(true, true)) return;

            // 0 = 引力场边缘，1 = 中心
            float dst = 1f - u.dst(unit) / range;

            // 方向：从目标指向本体 = 吸引
            Tmp.v3.set(unit).sub(u).nor();

            if(useImpulse){
                u.impulseNet(Tmp.v3.scl((force + dst * force * 0.5f) * Time.delta / 60f));
            }else{
                float a = (pullAccel + dst * pullBonus) * Time.delta;
                u.vel.add(Tmp.v3.x * a, Tmp.v3.y * a);
                if(u.vel.len() > maxPullSpeed) u.vel.setLength(maxPullSpeed);
            }

            if(tick){
                if(status != null){
                    u.apply(status, statusDuration);
                }

                float dps = u.type.health * healthPercentPerSecond + damagePerSecond;
                // 越靠近中心伤害越高（0.3x ～ 1.3x）
                u.damageContinuousPierce(dps * tickScale * (0.3f + dst));
            }
        });
    }

    @Override
    public void draw(Unit unit){
        if(Vars.headless || !drawRing) return;

        float rad = ringRadius > 0f ? ringRadius : outRadius;
        if(rad <= 0f) return;

        float t = Time.time;
        float spin = t * ringSpin;
        float pulse = Mathf.absin(t * 0.05f, 1f, 1f);      // -1 .. 1

        // 椭圆拉伸：两个轴反向脉动，环会周期性被"拉长"
        float ex = 1f + ellipse * pulse;
        float ey = 1f - ellipse * pulse;

        Draw.z(Layer.effect + 0.5f);
        Draw.blend(Blending.additive);

        // ① 外环（椭圆，贴在最小圈之外）
        Draw.color(edgeColor);
        Draw.alpha(0.5f + 0.2f * Math.abs(pulse));
        Lines.stroke(ringStroke + pulse * 0.8f);

        int seg = 48;
        Lines.beginLine();
        for(int i = 0; i <= seg; i++){
            float a = i * 360f / seg;
            Lines.linePoint(unit.x + Angles.trnsx(a, rad * ex), unit.y + Angles.trnsy(a, rad * ey));
        }
        Lines.endLine();

        // ② 放射状"拉伸线"：从外环向外拖出，长度脉动
        for(int i = 0; i < stretchCount; i++){
            float a = spin + i * 360f / stretchCount;
            float k = Mathf.absin(t * 0.07f + i * 3.7f, 1f, 1f);          // -1 .. 1
            float len = stretchLength * (1f - stretchPulse * 0.5f + stretchPulse * k);

            float sx = unit.x + Angles.trnsx(a, rad * ex);
            float sy = unit.y + Angles.trnsy(a, rad * ey);

            Draw.alpha(0.35f + 0.4f * Math.abs(k));
            Lines.stroke(2.2f);
            Lines.lineAngle(sx, sy, a, len);
        }

        Draw.blend();
        Draw.reset();
    }

    @Override
    public HIBlackHoleAbility copy(){
        return (HIBlackHoleAbility)super.copy();
    }
}
