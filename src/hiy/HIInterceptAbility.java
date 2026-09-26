package hiy;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.ui.Bar;

/**
 * 「锻炉」：拦截敌方子弹 → 储存为蓄能 → 转化成减伤与反打伤害。
 *
 * 拦截思路来自 DeepSpace 的 {@code universecore.world.ability.InterceptAbilty}
 * （用 Groups.bullet.intersect 扫范围敌弹），
 * 「蓄能」这一层是重工业自己的设计：把拦下来的伤害变成一个可被武器消费的资源。
 *
 * 闭环：
 *   HIInterceptAbility 累积 forgeCharge
 *        ├─→ HIUnitEntity.damage()  按 forgeCharge 减伤
 *        └─→ HIChargeWeapon         按 forgeCharge 提升伤害/射速，并逐发消耗
 */
public class HIInterceptAbility extends Ability{

    /** 拦截半径。 */
    public float range = 170f;
    /** 每 1 点子弹伤害转化为多少蓄能（0.0025 → 400 点伤害充满）。 */
    public float chargePerDamage = 0.0025f;
    /** 每秒自然衰减比例。 */
    public float decayPerSecond = 0.06f;
    /** 满蓄能时的减伤比例。 */
    public float damageReductionMax = 0.6f;
    /** 拦截时是否回血。 */
    public boolean healOnAbsorb = false;
    public float healPerDamage = 0.5f;
    /** 单发可吸收的伤害上限，防止一发超模弹瞬间充满。 */
    public float maxAbsorbPerBullet = 300f;
    /** 是否拦截不可反射的子弹（false 时只拦 reflectable）。 */
    public boolean absorbUnreflectable = false;

    public Color color = HIColors.b4;
    public Effect absorbEffect = null;

    protected boolean enabled = true;

    @Override
    public void update(Unit unit){
        // --release 8：不能用 instanceof 模式匹配
        if(!(unit instanceof HIUnitEntity)) return;
        final HIUnitEntity e = (HIUnitEntity)unit;

        if(e.forgeCharge > 0f){
            e.forgeCharge = Mathf.clamp(e.forgeCharge - decayPerSecond / 60f * Time.delta, 0f, 1f);
        }
        e.damageReduction = damageReductionMax * e.forgeCharge;

        if(!enabled || unit.team == null) return;

        float cx = unit.x, cy = unit.y;
        Groups.bullet.intersect(cx - range, cy - range, range * 2f, range * 2f, b -> {
            if(b.team == unit.team || b.type == null) return;
            if(!b.type.reflectable && !(absorbUnreflectable && b.type.absorbable)) return;
            if(b.dst(cx, cy) > range) return;

            float dmg = Mathf.clamp(b.damage, 1f, maxAbsorbPerBullet);
            e.forgeCharge = Mathf.clamp(e.forgeCharge + dmg * chargePerDamage, 0f, 1f);

            if(healOnAbsorb) unit.heal(dmg * healPerDamage);
            if(absorbEffect != null) absorbEffect.at(b.x, b.y, b.rotation(), color);

            b.remove();
        });
    }

    @Override
    public void displayBars(Unit unit, Table bars){
        if(!(unit instanceof HIUnitEntity)) return;
        final HIUnitEntity e = (HIUnitEntity)unit;
        bars.add(new Bar("锻炉蓄能", color, () -> Mathf.clamp(e.forgeCharge))).row();
    }

    @Override
    public HIInterceptAbility copy(){
        return (HIInterceptAbility)super.copy();
    }
}
