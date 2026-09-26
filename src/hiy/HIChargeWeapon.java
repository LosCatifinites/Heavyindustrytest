package hiy;

import arc.math.Mathf;
import arc.util.Time;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

/**
 * 把「锻炉蓄能」转成火力：蓄能越高，伤害越高、装填越快，开火后逐发消耗。
 *
 * 开火特效（参考 DS 炮台阳炎 / 霜降 / 冬至 的写法）：
 *   - {@link HIEffects#muzzle} 炮口闪光（每次开火）
 *   - {@link HIEffects#chargeGlow} 蓄能越高越频繁的蓄力光环
 *   两者都是 Effect，在 Layer.effect 上绘制，正好落在 Mindustry 的 bloom 捕获区间内，
 *   所以自带泛光。
 *
 * ⚠ 关于装填：官方 v160.5 的 {@code WeaponMount} **没有** reloadMultiplier 字段
 *   （DS 是用 AttachedProperty 自己挂的）。官方装填逻辑在 Weapon.update：
 *       mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0);
 *   所以这里在 super.update 之前给 mount.reload 追加一段扣减。
 */
public class HIChargeWeapon extends Weapon{

    /** 满蓄能时的伤害加成倍率（1.0 = +100%）。 */
    public float damageBoost = 2f;
    /** 满蓄能时额外获得的装填速度倍率（1.0 = 装填快一倍）。 */
    public float reloadBoost = 1f;
    /** 每发消耗的蓄能（0 = 不消耗，只吃加成）。 */
    public float chargePerShot = 0f;
    /** 是否播放炮口闪光。 */
    public boolean muzzleEffect = true;

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
            float c = ((HIUnitEntity)unit).forgeCharge;

            // 蓄能越高越频繁的蓄力光环
            if(c > 0.05f && HIEffects.chargeGlow != null && Mathf.chanceDelta(c * 0.30f)){
                HIEffects.chargeGlow.at(unit.x, unit.y, unit.rotation);
            }

            if(reloadBoost > 0f && c > 0f && mount.reload > 0f){
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

        // 炮口闪光（在极坐标位置上，按武器朝向）
        if(muzzleEffect && HIEffects.muzzle != null){
            HIEffects.muzzle.at(shootX, shootY, rotation);
        }

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
