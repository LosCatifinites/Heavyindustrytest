package hiy;

import arc.util.Time;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

/**
 * 模块：死亡爆发 + 瘫痪（复刻 EU「海幻」narwhal 的死亡效果，
 * 原文：死亡后持续 5s 对范围内随机最多 20 个建筑和单位造成伤害，并使攻击过的目标瘫痪 5s）。
 *
 * 这里做成一次次性的范围爆发（更容易平衡、也不需要在单位死后保留一个 tick 对象）：
 *   - {@code Damage.damage} 对范围内单位+建筑造成伤害
 *   - 对范围内敌方单位施加 disarmed + unmoving（瘫痪）
 *   - 播放特效
 */
public class HIDeathBlastAbility extends Ability{

    /** 爆发半径。 */
    public float range = 180f;
    /** 爆发伤害。 */
    public float damage = 1200f;
    /** 是否打建筑。 */
    public boolean hitBuildings = true;
    /** 瘫痪时长（帧）。 */
    public float paralyze = 60f * 5f;
    /** 瘫痪时是否禁足。 */
    public boolean paralyzeMovement = true;
    public Effect effect;

    @Override
    public void death(Unit unit){
        if(unit.team == null) return;

        Damage.damage(unit.team, unit.x, unit.y, range, damage, true, true);

        if(paralyze > 0f){
            float dur = paralyze / 60f;
            Units.nearbyEnemies(unit.team, unit.x, unit.y, range, u -> {
                u.apply(StatusEffects.disarmed, dur);
                if(paralyzeMovement) u.apply(StatusEffects.unmoving, dur);
            });
        }

        if(effect != null) effect.at(unit.x, unit.y, range);
    }

    @Override
    public HIDeathBlastAbility copy(){
        return (HIDeathBlastAbility)super.copy();
    }
}
