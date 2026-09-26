package hiy;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.ui.Bar;

/**
 * 「镜甲卫」：本体某朝向展开的多边形镜面护盾，把进入范围的敌弹反射回去。
 *
 * 复刻自 DeepSpace 的 {@code singularity.world.unit.abilities.MirrorFieldAbility}（187 行）+
 * {@code MirrorArmorAbility}，做了两处简化：
 *   - 不做护盾片移动动画（ShieldShape/ShapeMove），只保留固定多边形 + 自转 + 受击闪烁
 *   - 反射不做按角度的伤害修正，统一按 reflectDamageScl 放大
 *
 * 视觉：绘制走 {@link HIGlow}（提交到 Layer.effect，落在 bloom 捕获区间内），
 * 所以护盾和闪烁都会泛光；另外在盾心加一盏 {@code Drawf.light}。
 *
 * 反射的具体做法（官方没有 Bullet.reflect，必须手写）：
 *   改 team / owner，把速度方向设为「从盾心指向子弹」，并按倍率重设速度大小。
 */
public class HIMirrorShieldAbility extends Ability{

    /** 护盾多边形边数。 */
    public int sides = 5;
    /** 护盾半径。 */
    public float radius = 58f;
    /** 有效防护角度（度）。 */
    public float arc = 150f;
    /** 两次反射之间的最小间隔（帧）。 */
    public float reload = 8f;
    /** 反射后的速度倍率。 */
    public float reflectSpeedScl = 1.25f;
    /** 反射后的伤害倍率。 */
    public float reflectDamageScl = 1.5f;
    /** 反射后把子弹剩余寿命减少的帧数（避免刚反射就消散）。 */
    public float reflectTimeBonus = 12f;

    /** 护盾相对本体朝向的偏移。 */
    public float angleOffset = 0f;
    /** 护盾自转速度（度/帧）。 */
    public float spin = 0f;
    /** 相对本体的位置偏移。 */
    public float x = 0f, y = 0f;

    /** 关掉就不再反射/绘制（可被 HealthRequire 之类动态控制）。 */
    public boolean active = true;
    public boolean drawShield = true;
    /** 护盾基础不透明度（之前太淡，这里调高）。 */
    public float baseAlpha = 0.34f;
    public Color color = HIColors.b4;

    protected float timer;
    /** 受击闪烁 0~1。 */
    public float flash;
    /** 累计自转角。 */
    protected float shieldRotation;

    @Override
    public void update(Unit unit){
        if(!active || unit.team == null) return;

        shieldRotation += spin * Time.delta;
        if(flash > 0f) flash = Math.max(flash - Time.delta / 12f, 0f);

        timer += Time.delta;
        if(timer < reload) return;

        float cx = unit.x + Angles.trnsx(unit.rotation - 90f, x, y);
        float cy = unit.y + Angles.trnsy(unit.rotation - 90f, x, y);
        float dir = unit.rotation - 90f + angleOffset + shieldRotation;

        Groups.bullet.intersect(cx - radius, cy - radius, radius * 2f, radius * 2f, b -> {
            if(b.team == unit.team || b.type == null || !b.type.reflectable) return;
            if(b.dst(cx, cy) > radius) return;
            if(!Angles.within(Angles.angle(cx, cy, b.x, b.y), dir, arc / 2f)) return;

            reflect(unit, b, cx, cy);
            timer = 0f;
            flash = 1f;
        });
    }

    /** 把子弹反射出去。 */
    protected void reflect(Unit unit, Bullet b, float cx, float cy){
        float out = Math.max(b.vel.len(), 0.1f) * reflectSpeedScl;
        float ang = Angles.angle(cx, cy, b.x, b.y);

        b.team = unit.team;
        b.owner = unit;
        b.vel.setAngle(ang).setLength(out);
        b.damage *= reflectDamageScl;
        b.time = Math.max(b.time - reflectTimeBonus, 0f);
    }

    @Override
    public void draw(Unit unit){
        if(!drawShield || !active) return;

        final float cx = unit.x + Angles.trnsx(unit.rotation - 90f, x, y);
        final float cy = unit.y + Angles.trnsy(unit.rotation - 90f, x, y);
        final float dir = unit.rotation - 90f + angleOffset + shieldRotation;
        final float f = Math.min(flash, 1f);

        HIGlow.draw(() -> {
            Draw.color(color, Color.white, f);
            Draw.alpha(baseAlpha + 0.5f * f);
            Fill.poly(cx, cy, sides, radius, dir);

            Draw.alpha(0.85f);
            Lines.stroke(2.2f + 3f * f);
            Lines.poly(cx, cy, sides, radius, dir);

            // 受击瞬间再来一圈外扩环
            if(f > 0.01f){
                Lines.stroke(2f * f);
                Lines.circle(cx, cy, radius * (1f + 0.25f * (1f - f)));
            }

            Draw.reset();
            Drawf.light(cx, cy, radius * 1.8f, color, 0.35f + 0.5f * f);
        });
    }

    @Override
    public void displayBars(Unit unit, Table bars){
        bars.add(new Bar("镜盾", color, () -> 1f - Math.min(flash, 1f))).row();
    }

    @Override
    public HIMirrorShieldAbility copy(){
        return (HIMirrorShieldAbility)super.copy();
    }
}
