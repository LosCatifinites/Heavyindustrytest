package hiy;

import arc.scene.ui.layout.Table;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.ui.Bar;
import mindustry.world.meta.Stat;

/**
 * 复刻 DeepSpace 的 universecore.world.ability.BarAbility。
 * 在单位信息面板上追加三条：格挡数量 / 护甲 / 伤害减免。
 */
public class ClusterLobesBarAbility extends Ability{

    @Override
    public void displayBars(Unit unit, Table bars){
        if(!(unit instanceof ClusterLobesUnit)) return;
        ClusterLobesUnit u = (ClusterLobesUnit)unit;

        bars.add(new Bar("格挡数量 " + u.resistCont, HIColors.b4, () -> 1f)).row();
        bars.add(new Bar(Stat.armor.localized(), HIColors.b4, () -> u.armor / 100f)).row();
        bars.add(new Bar("伤害减免 " + trimmed(u.immunity() * 100f) + "%", HIColors.b4, u::immunity)).row();
    }

    /** 等价于 universecore 的 toTrimmedString(2)。 */
    private static String trimmed(float v){
        return String.valueOf(Math.round(v * 100f) / 100f);
    }
}
