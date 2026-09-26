package hiy;

import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Hitboxc;

/**
 * 打「电磁完整性」的子弹：命中带 {@link HIUnitEntity} 的单位时额外扣 EMP。
 * 普通伤害照常结算（走 super）。
 */
public class HIEmpBulletType extends BasicBulletType{

    /** 每次命中扣除的电磁值。 */
    public float empDamage = 90f;

    public HIEmpBulletType(){
        super();
    }

    public HIEmpBulletType(float speed, float damage){
        super(speed, damage);
    }

    @Override
    public void hitEntity(Bullet b, Hitboxc entity, float health){
        super.hitEntity(b, entity, health);
        if(entity instanceof HIUnitEntity e){
            e.damageEmp(empDamage);
        }
    }
}
