package hiy;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.ui.Bar;

/**
 * 「电磁封锁者」：给单位加一条**独立的电磁完整性**（第二血条）。
 *
 * 复刻自 DeepSpace 的 {@code singularity.world.unit.EMPHealthManager}（279 行）的**机制**，
 * 但去掉了它的 JSON 声明层（那套依赖 {@code Mods} 的解析钩子，重工业用 Java 字段更直接）：
 *   - 上限 = 单位最大生命 * {@link #empFraction}（首次 update 时初始化）
 *   - 归零后进入**瘫痪**：持续刷 {@code disarmed}（不能开火）+ {@code unmoving}（不能移动）
 *   - 未归零时按 {@link #empRepairPerSecond} 自修复
 *
 * 数据存在 {@link HIUnitEntity} 上，因此随存档保存。
 * 电磁伤害来源见 {@link HIEmpBulletType}。
 */
public class HIEmpAbility extends Ability{

    /** 电磁上限占最大生命的比例。 */
    public float empFraction = 0.35f;
    /** 每秒自修复比例（相对上限）。 */
    public float empRepairPerSecond = 0.02f;
    /** 瘫痪时状态的刷新时长（秒）。 */
    public float paralyzeRefresh = 0.25f;
    /** 瘫痪时是否也禁止移动。 */
    public boolean paralyzeMovement = true;
    /** 电磁条颜色。 */
    public Color color = Color.valueOf("a9d8ff");

    @Override
    public void update(Unit unit){
        if(!(unit instanceof HIUnitEntity e)) return;

        if(e.empMax <= 0f){
            e.empMax = unit.type.health * empFraction;
            e.empHealth = e.empMax;
        }

        if(e.empHealth <= 0f){
            unit.apply(StatusEffects.disarmed, paralyzeRefresh);
            if(paralyzeMovement) unit.apply(StatusEffects.unmoving, paralyzeRefresh);
        }else{
            e.empHealth = Math.min(e.empMax, e.empHealth + e.empMax * empRepairPerSecond / 60f * Time.delta);
        }
    }

    @Override
    public void displayBars(Unit unit, Table bars){
        if(!(unit instanceof HIUnitEntity e) || e.empMax <= 0f) return;
        bars.add(new Bar("电磁完整性", color, () -> Mathf.clamp(e.empHealth / e.empMax))).row();
    }

    @Override
    public HIEmpAbility copy(){
        return (HIEmpAbility)super.copy();
    }
}
