package hiy;

import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

/**
 * 「装配母舰」的修复场部分：周期性修复范围内的己方建筑。
 *
 * DeepSpace 的 {@code universecore.world.ability.RepairFieldAbility} 是继承官方
 * {@code SuppressionFieldAbility} 再整个覆写 update 的；实测官方那个类的字段里
 * 并没有 healAmount/healPercent（它本质是「压制场」），所以这里**直接自己写**，更清楚。
 */
public class HIRepairFieldAbility extends Ability{

    /** 修复半径。 */
    public float range = 130f;
    /** 每次结算的固定治疗量。 */
    public float amount = 40f;
    /** 每次结算按「被修建筑最大生命」的百分比治疗（0 = 不用）。 */
    public float percentAmount = 0f;
    /** 结算间隔（帧）。 */
    public float reload = 30f;
    /** 是否也修己方单位。 */
    public boolean healUnits = false;

    protected float timer;

    @Override
    public void update(Unit unit){
        if(unit.team == null) return;

        timer += Time.delta;
        if(timer < reload) return;
        timer = 0f;

        final float base = amount;
        final float pct = percentAmount;

        Units.nearbyBuildings(unit.x, unit.y, range, b -> {
            if(b.team != unit.team || b.health >= b.maxHealth) return;
            b.heal(base + b.maxHealth * pct);
        });

        if(healUnits){
            Units.nearby(unit.team, unit.x, unit.y, range, u -> {
                if(u != unit) u.heal(base + u.maxHealth * pct);
            });
        }
    }

    @Override
    public HIRepairFieldAbility copy(){
        return (HIRepairFieldAbility)super.copy();
    }
}
