package hiy;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Hitboxc;
import mindustry.gen.UnitEntity;

/**
 * 重工业单位实体基类。
 *
 * 复刻自 DeepSpace 的 {@code ice.world.content.unit.entity.base.Entity}（440 行）中
 * **真正必需**的三件事，其余（腿系统 Legsc、自定义阴影、残骸）按需再加：
 *   1. {@link #classId()} 走 {@link HIEntities}（多单位共存必需）
 *   2. {@link #collides} 检查 {@link HICollideBlocker}（让能力能干预碰撞）
 *   3. {@code add/read/write} 转发到 {@link HIUnitType} 的钩子
 *
 * 额外提供两个跨单位共享的字段：锻炉蓄能（{@link HIInterceptAbility}）与 EMP 电磁完整性
 * （{@link HIEmpAbility}）。它们随单位一起序列化，所以存档后仍然有效。
 */
public class HIUnitEntity extends UnitEntity{

    // ---------- 锻炉（HIInterceptAbility） ----------
    /** 蓄能 0~1。 */
    public float forgeCharge;
    /** 当前减伤 0~1，由能力每帧写入。 */
    public float damageReduction;

    // ---------- EMP（HIEmpAbility） ----------
    /** 电磁完整性当前值。 */
    public float empHealth;
    /** 电磁完整性上限（首次 update 时按最大生命初始化）。 */
    public float empMax;

    @Override
    public int classId(){
        if(type instanceof HIUnitType t && t.entityId >= 0) return t.entityId;
        return super.classId();
    }

    @Override
    public boolean collides(Hitboxc other){
        for(Ability a : abilities){
            if(a instanceof HICollideBlocker b && b.blockedCollides(this, other)) return false;
        }
        return super.collides(other);
    }

    @Override
    public void add(){
        super.add();
        if(type instanceof HIUnitType t) t.initUnit(this);
    }

    @Override
    public void read(Reads read){
        super.read(read);
        if(type instanceof HIUnitType t) t.readUnit(this, read);
    }

    @Override
    public void write(Writes write){
        super.write(write);
        if(type instanceof HIUnitType t) t.writeUnit(this, write);
    }

    @Override
    public void damage(float amount){
        if(damageReduction > 0f && amount > 0f){
            amount = Math.max(amount * (1f - damageReduction), 0f);
        }
        super.damage(amount);
    }

    /** EMP 伤害入口（由 {@link HIEmpBulletType} 调用）。 */
    public void damageEmp(float amount){
        if(empMax <= 0f) return;
        empHealth = Math.max(0f, empHealth - Math.max(amount, 0f));
    }

    /** EMP 是否已被打瘫。 */
    public boolean empBroken(){
        return empMax > 0f && empHealth <= 0f;
    }
}
