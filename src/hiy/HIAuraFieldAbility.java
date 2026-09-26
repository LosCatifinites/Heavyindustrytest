package hiy;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.type.StatusEffect;
import mindustry.ui.Bar;

/**
 * 模块：光环场（复刻 EU「冥域」suzerain 的「增强附近己方 / 削弱附近敌方」+「湮灭」的
 * {@code TerritoryFieldAbility}）。
 *
 * 一次结算做四件事，各自可以单独关掉：
 *   1. 范围内己方单位回血（按最大生命百分比 / 固定值）
 *   2. 范围内己方单位获得状态
 *   3. 范围内敌方单位受到固定伤害
 *   4. 范围内敌方单位获得状态（减益）
 *
 * 视觉走 {@link HIGlow}（bloom 捕获区间），圆环 + 旋转刻度 + 光源。
 */
public class HIAuraFieldAbility extends Ability{

    /** 光环半径。 */
    public float range = 220f;
    /** 结算间隔（帧）。 */
    public float reload = 30f;

    // ---- 对己方 ----
    public float allyHealFlat = 0f;
    public float allyHealPercent = 0.02f;
    public StatusEffect allyStatus;
    public float allyStatusDuration = 3f;
    /** 是否也给自己上状态 / 回血（默认 false，避免自我永动）。 */
    public boolean affectSelf = false;

    // ---- 对敌方 ----
    public float enemyDamage = 0f;
    public StatusEffect enemyStatus;
    public float enemyStatusDuration = 2f;

    // ---- 视觉 ----
    public boolean drawField = true;
    public Color color = Color.valueOf("7ee0ff");
    public float stroke = 2.5f;
    public int ticks = 24;
    public float spin = 0.25f;

    protected float timer;
    protected float phase;

    @Override
    public void update(Unit unit){
        if(unit.team == null) return;

        phase += Time.delta * 0.02f;
        timer += Time.delta;
        if(timer < reload) return;
        timer = 0f;

        Units.nearby(unit.team, unit.x, unit.y, range, u -> {
            if(u == unit && !affectSelf) return;
            float heal = allyHealFlat + u.maxHealth * allyHealPercent * (reload / 60f);
            if(heal > 0f) u.heal(heal);
            if(allyStatus != null) u.apply(allyStatus, allyStatusDuration);
        });

        if(enemyDamage > 0f || enemyStatus != null){
            Units.nearbyEnemies(unit.team, unit.x, unit.y, range, u -> {
                if(enemyDamage > 0f) u.damage(enemyDamage * (reload / 60f));
                if(enemyStatus != null) u.apply(enemyStatus, enemyStatusDuration);
            });
        }
    }

    @Override
    public void displayBars(Unit unit, Table bars){
        bars.add(new Bar("光环有效", color, () -> 1f)).row();
    }

    @Override
    public void draw(Unit unit){
        if(!drawField) return;

        final float ux = unit.x, uy = unit.y;

        HIGlow.draw(() -> {
            float pulse = Mathf.absin(Time.time * 0.05f, 1f, 1f);

            Draw.color(color);
            Draw.alpha(0.22f + 0.10f * Math.abs(pulse));
            Lines.stroke(stroke + pulse * 0.8f);
            Lines.circle(ux, uy, range);

            Draw.alpha(0.65f);
            Lines.stroke(stroke);
            float rot = Time.time * spin;
            for(int i = 0; i < ticks; i++){
                float a = rot + i * 360f / ticks;
                float inner = range - 10f - pulse * 3f;
                float outer = range + 8f + pulse * 3f;
                Lines.lineAngle(ux + Angles.trnsx(a, inner), uy + Angles.trnsy(a, inner), a, outer - inner);
            }

            Draw.reset();
            Drawf.light(ux, uy, range * 1.3f, color, 0.22f + 0.08f * Math.abs(pulse));
        });
    }

    @Override
    public HIAuraFieldAbility copy(){
        return (HIAuraFieldAbility)super.copy();
    }
}
