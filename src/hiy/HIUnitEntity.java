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
 * 真正必需的三件事，其余（腿系统 Legsc、自定义阴影、残骸）按需再加：
 *   1. classId() 走 HIEntities（多单位共存必需）
 *   2. collides() 检查 HICollideBlocker（让能力能干预碰撞）
 *   3. add/read/write 转发到 HIUnitType 的钩子
 *
 * 额外提供两个跨单位共享的字段：锻炉蓄能（HIInterceptAbility）与 EMP 电磁完整性
 * （HIEmpAbility）。它们随单位一起序列化，所以存档后仍然有效。
 *
 * 注意：本工程 CI 使用 {@code javac --release 8}，
 * 因此全部 instanceof 都写成「判断 + 强转」的经典形式，不能用 Java 16 的模式匹配。
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
        if(type instanceof HIUnitType){
            int id = ((HIUnitType)type).entityId;
            if(id >= 0) return id;
        }
        return super.classId();
    }

    @Override
    public boolean collides(Hitboxc other){
        for(Ability a : abilities){
            if(a instanceof HICollideBlocker){
                if(((HICollideBlocker)a).blockedCollides(this, other)) return false;
            }
        }
        return super.collides(other);
    }

    @Override
    public void add(){
        super.add();
        if(type instanceof HIUnitType) ((HIUnitType)type).initUnit(this);
    }

    @Override
    public void read(Reads read){
        super.read(read);
        if(type instanceof HIUnitType) ((HIUnitType)type).readUnit(this, read);
    }

    @Override
    public void write(Writes write){
        super.write(write);
        if(type instanceof HIUnitType) ((HIUnitType)type).writeUnit(this, write);
    }

    @Override
    public void damage(float amount){
        if(damageReduction > 0f && amount > 0f){
            amount = Math.max(amount * (1f - damageReduction), 0f);
        }
        super.damage(amount);
    }

    /** EMP 伤害入口（由 HIEmpBulletType 调用）。 */
    public void damageEmp(float amount){
        if(empMax <= 0f) return;
        empHealth = Math.max(0f, empHealth - Math.max(amount, 0f));
    }

    /** EMP 是否已被打瘫。 */
    public boolean empBroken(){
        return empMax > 0f && empHealth <= 0f;
    }
}
