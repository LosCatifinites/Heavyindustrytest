package hiy;

import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

/**
 * 把「锻炉蓄能」转成火力：蓄能越高，伤害越高、装填越快，开火后逐发消耗。
 *
 * 参考 DeepSpace 的 {@code ice.world.content.unit.weapon.ChargeWeapon}
 * （它用 AttachedProperty 往 WeaponMount 上挂 reloadMultiplier；
 * 这里直接用官方 {@code WeaponMount.reloadMultiplier} 字段，不需要额外挂载层）。
 */
public class HIChargeWeapon extends Weapon{

    /** 满蓄能时的伤害加成倍率（1.0 = +100%）。 */
    public float damageBoost = 2f;
    /** 满蓄能时的射速加成倍率（1.0 = +100%）。 */
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
        if(unit instanceof HIUnitEntity){
            mount.reloadMultiplier = 1f + reloadBoost * ((HIUnitEntity)unit).forgeCharge;
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
