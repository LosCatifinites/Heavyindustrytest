package hiy;

import arc.util.Time;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

/**
 * 把「锻炉蓄能」转成火力：蓄能越高，伤害越高、装填越快，开火后逐发消耗。
 *
 * ⚠ 实现说明（踩过的坑）：
 *   DeepSpace 的 ChargeWeapon 用 {@code AttachedProperty} 往 WeaponMount 上**自己挂**了一个
 *   reloadMultiplier 字段；官方 v160.5 的 {@code WeaponMount} **没有**这个字段
 *   （它是 reload / rotation / recoil / heat / warmup / charge / smoothReload …）。
 *   官方的装填逻辑在 {@code Weapon.update}：
 *       mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0);
 *   所以这里不挂任何自定义字段，而是**在 super.update 之前给 mount.reload 追加一段扣减**，
 *   既不改 unit.reloadMultiplier（会被单位每帧重算，改了也不稳），也不影响其它武器。
 */
public class HIChargeWeapon extends Weapon{

    /** 满蓄能时的伤害加成倍率（1.0 = +100%）。 */
    public float damageBoost = 2f;
    /** 满蓄能时额外获得的装填速度倍率（1.0 = 装填快一倍）。 */
    public float reloadBoost = 1f;
    /** 每发消耗的蓄能（0 = 不消耗，只吃加成）。 */
    public float chargePerShot = 0f;

    public HIChargeWeapon(){
        super("");
    }

    public HIChargeWeapon(String name){
        super(name);
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        // --release 8：不能用 instanceof 模式匹配
        if(unit instanceof HIUnitEntity && reloadBoost > 0f){
            float c = ((HIUnitEntity)unit).forgeCharge;
            if(c > 0f && mount.reload > 0f){
                // 追加扣减：相当于装填速度 ×(1 + reloadBoost*c)
                mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier * reloadBoost * c, 0f);
            }
        }
        super.update(unit, mount);
    }

    @Override
    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
        if(!(unit instanceof HIUnitEntity) || bullet == null){
            super.shoot(unit, mount, shootX, shootY, rotation);
            return;
        }

        HIUnitEntity e = (HIUnitEntity)unit;

        float old = bullet.damage;
        bullet.damage = old * (1f + damageBoost * e.forgeCharge);

        super.shoot(unit, mount, shootX, shootY, rotation);

        bullet.damage = old;
        if(chargePerShot > 0f) e.forgeCharge = Math.max(e.forgeCharge - chargePerShot, 0f);
    }

    @Override
    public HIChargeWeapon copy(){
        return (HIChargeWeapon)super.copy();
    }
}
