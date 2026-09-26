package hiy;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
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
 * 复刻自两处：
 *   1) EU（ExtraUtilities）scripts/block/turret/sucker.js —— 牵引逻辑
 *      unit.impulseNet( (this - unit).limit(force + (1 - dst/range) * scaledForce) * edelta );
 *   2) DeepSpace ice/entities/bullet/BlockHoleBulletType.kt —— 距离衰减 + 百分比持续伤害
 *      dst  = 1 - u.dst(b) / range
 *      dmg  = (u.type.health * percent + damages) * dst / 60   （每帧）
 *   3) 视觉扭曲走 {@link HIBlackHoles}（TearingSpace 整屏着色器）
 *
 * 与两者的差别（本实现的取舍）：
 *   - 原作 sucker 是「炮塔牵引」：目标是建筑；这里改成「单位自身排开引力场」，跟着单位跑。
 *   - 原作用 impulse（力 / 质量），巨型单位几乎拉不动；这里默认直接用速度累加（引力场对
 *     所有单位效果一致），并单独提供 maxPullSpeed 限速。想恢复质量影响把 useImpulse 设 true。
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
    /** useImpulse = true 时的「力」大小（参照 EU sucker：24 / NH：force + scaledForce）。 */
    public float force = 400f;

    // ---------- 伤害 / 状态 ----------
    /** 每秒固定伤害。 */
    public float damagePerSecond = 0f;
    /** 每秒按目标最大生命值扣的百分比（0.05 = 5%/s）。 */
    public float healthPercentPerSecond = 0.05f;
    /** 施加的状态；null 表示不施加。 */
    public StatusEffect status = StatusEffects.sapped;
    /** 状态刷新时长（秒）。每 statusInterval 帧刷一次，所以给个小值即可。 */
    public float statusDuration = 1.2f;
    /** 状态 / 伤害的结算间隔（帧）。 */
    public float statusInterval = 10f;

    // ---------- 视觉 ----------
    /** 是否登记到整屏扭曲着色器。 */
    public boolean shader = true;
    /** 着色器内半径（黑洞「视界」大小）。 */
    public float inRadius = 28f;
    /** 着色器外半径（扭曲影响范围）。 */
    public float outRadius = 200f;
    /** 是否在本体位置画叠加光环（无贴图，纯矢量）。 */
    public boolean drawRing = true;
    public Color coreColor = Color.valueOf("665c9f");
    public Color edgeColor = Color.valueOf("be92f9");

    /** 每实例的结算计时（Ability 会按单位 copy 一份）。 */
    protected float timer = 0f;

    public HIBlackHoleAbility(){
    }

    public HIBlackHoleAbility(float range, float outRadius){
        this.range = range;
        this.outRadius = outRadius;
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

        float pulse = Mathf.absin(Time.time * 0.06f, 1f, 0.12f);

        Draw.z(Layer.effect);
        Draw.blend(Blending.additive);

        Draw.color(coreColor);
        Draw.alpha(0.55f);
        Fill.circle(unit.x, unit.y, inRadius * (0.85f + pulse));

        Draw.color(edgeColor);
        Draw.alpha(0.28f);
        Fill.circle(unit.x, unit.y, inRadius * 1.6f * (0.9f + pulse));

        Lines.stroke(2f + pulse * 2f);
        Draw.alpha(0.35f);
        Lines.circle(unit.x, unit.y, outRadius * 0.55f);

        Draw.blend();
        Draw.reset();
    }

    @Override
    public HIBlackHoleAbility copy(){
        return (HIBlackHoleAbility)super.copy();
    }
}
