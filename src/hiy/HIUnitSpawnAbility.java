package hiy;

import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;

/**
 * 「装配母舰」的生成部分：周期性在周围生成子单位。
 *
 * 复刻自 DeepSpace 的 {@code universecore.world.ability.UnitSpawnAbility}，
 * 增加了 {@link #limit} 上限（原作没有，容易刷爆实体数）。
 */
public class HIUnitSpawnAbility extends Ability{

    /** 要生成的单位。 */
    public UnitType spawnUnit;
    /** 一次生成几个。 */
    public int amount = 1;
    /** 生成间隔（帧）。 */
    public float spawnTime = 60f * 14f;
    /** 相对本体的生成点偏移（沿本体朝向）。 */
    public float sX = 0f, sY = 0f;
    /** 生成点随机散布半径。 */
    public float spread = 12f;
    /** 场上同类型子单位上限（含其它来源）。 */
    public int limit = 6;
    public Effect spawnEffect = Fx.spawn;
    public Color color = Pal.accent;

    protected float timer;

    public HIUnitSpawnAbility(){
    }

    public HIUnitSpawnAbility(UnitType spawnUnit, float spawnTime){
        this.spawnUnit = spawnUnit;
        this.spawnTime = spawnTime;
    }

    @Override
    public void update(Unit unit){
        if(spawnUnit == null || unit.team == null) return;

        timer += Time.delta;
        if(timer < spawnTime) return;
        timer = 0f;

        int alive = Groups.unit.count(u -> u.type == spawnUnit && u.team == unit.team);
        if(alive >= limit) return;

        for(int i = 0; i < amount; i++){
            float dx = unit.x + Angles.trnsx(unit.rotation, sY, -sX) + Mathf.range(spread);
            float dy = unit.y + Angles.trnsy(unit.rotation, sY, -sX) + Mathf.range(spread);

            Unit u = spawnUnit.create(unit.team);
            u.x = dx;
            u.y = dy;
            u.rotation = Mathf.random(360f);
            u.add();

            if(spawnEffect != null) spawnEffect.at(dx, dy, 0f, color);
        }
    }

    @Override
    public HIUnitSpawnAbility copy(){
        return (HIUnitSpawnAbility)super.copy();
    }
}
