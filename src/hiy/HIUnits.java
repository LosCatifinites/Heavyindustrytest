package hiy;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.ShieldRegenFieldAbility;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.part.HaloPart;
import mindustry.entities.pattern.ShootMulti;
import mindustry.entities.pattern.ShootPattern;
import mindustry.type.ItemStack;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.weapons.PointDefenseWeapon;

/**
 * 单位内容注册（模块化版本）。
 *
 * 每个单位的所有能力都按「模块」排列，模块之间互不依赖，可以整块注释掉：
 *
 *   【框架模块】  实体绑定 / 描边 / 雾半径           → HIUnitType
 *   【基础模块】  数值 / 体型 / 形态
 *   【移动模块】  引擎 / 尾迹 / 光照
 *   【功能模块】  载运 / 采矿 / 物品容量
 *   【武器模块】  蓄力主炮 / 点防御 / 轨道炮 / 曳光弹 / 空中爆点
 *   【能力模块】  锻炉 / 镜盾 / 修复场 / 光环场 / 护盾再生场 / 电磁 / 母舰孵化 / 死亡爆发
 *   【视觉模块】  光环部件 / 黑洞外环
 *
 * 注册顺序：被生成的单位必须先建（母舰的孵化能力直接引用 welder）。
 */
public class HIUnits{

    /** 工蜂：装配母舰生成的小型采矿/维修单位（暂无专属贴图，兜底用原版 flare）。 */
    public static HIUnitType welder;

    /** 裂片集群：重工业的主力旗舰。 */
    public static HIUnitType clusterLobes;

    public static void load(){
        loadWelder();
        loadClusterLobes();
    }

    // ==================================================================
    //  工蜂（模块：移动 / 采矿 / 光照 / 修复）
    // ==================================================================
    private static void loadWelder(){
        welder = HIUnitType.create("welder", HIUnitEntity::new);
        welder.fallbackRegion = "flare";

        // ---- 基础模块 ----
        welder.health = 900f;
        welder.armor = 2f;
        welder.hitSize = 10f;
        welder.speed = 3.4f;
        welder.accel = 0.09f;
        welder.drag = 0.06f;
        welder.rotateSpeed = 9f;
        welder.flying = true;
        welder.lowAltitude = true;
        welder.engineSize = 0f;
        welder.itemCapacity = 20;

        // ---- 移动模块：引擎 ----
        welder.engines.add(new UnitType.UnitEngine(0f, -4.5f, 2.2f, -90f));

        // ---- 移动模块：尾迹 ----
        welder.trailLength = 10;
        welder.trailScl = 1.4f;

        // ---- 光照模块 ----
        welder.lightRadius = 60f;
        welder.lightOpacity = 0.12f;

        // ---- 功能模块：采矿 ----
        welder.mineWalls = true;
        welder.mineFloor = true;
        welder.mineTier = 2;
        welder.mineSpeed = 3f;

        // ---- 能力模块：修复场 ----
        HIRepairFieldAbility repair = new HIRepairFieldAbility();
        repair.range = 95f;
        repair.amount = 26f;
        repair.reload = 30f;
        welder.abilities.add(repair);
    }

    // ==================================================================
    //  裂片集群
    // ==================================================================
    private static void loadClusterLobes(){
        clusterLobes = HIUnitType.create("clusterLobes", ClusterLobesUnit::new);

        // ---------------- 基础模块 ----------------
        clusterLobes.health = 96000f;
        clusterLobes.armor = 0f;
        clusterLobes.hitSize = 32f;
        clusterLobes.speed = 18f / 7.5f;
        clusterLobes.rotateSpeed = 4f;
        clusterLobes.drag = 0.05f;
        clusterLobes.range = 8f * 60f;
        clusterLobes.engineSize = 0f;
        clusterLobes.itemCapacity = 120;
        clusterLobes.outlineRadius = 5;          // 模块：加粗描边（NH 同款）

        // ---------------- 形态模块 ----------------
        clusterLobes.flying = true;
        clusterLobes.lowAltitude = true;
        clusterLobes.faceTarget = false;
        clusterLobes.drawBody = false;
        clusterLobes.drawCell = false;
        clusterLobes.hidden = false;

        // ---------------- 光照模块 ----------------
        clusterLobes.lightRadius = 120f;
        clusterLobes.lightOpacity = 0.10f;

        // ---------------- 功能模块：载运 ----------------
        clusterLobes.payloadCapacity = 2f * Vars.tilePayload;

        // ---------------- 免疫模块 ----------------
        Seq<StatusEffect> all = Vars.content.statusEffects();
        for(StatusEffect s : all){
            if(s.speedMultiplier == 1f) continue;
            clusterLobes.immunities.add(s);
        }

        // ================= 武器模块 =================
        // ① 蓄力主炮 ×2（上下），组合射击模式 ShootMulti（NH 幽影同款）
        clusterLobes.weapons.add(chargeWeapon(90f));
        clusterLobes.weapons.add(chargeWeapon(270f));

        // ② 点防御武器（官方 mindustry.type.weapons.PointDefenseWeapon）
        clusterLobes.weapons.add(pointDefense());

        // ③ 轨道主炮（RailBulletType，EU 湮灭同款）
        clusterLobes.weapons.add(railWeapon());

        // ================= 视觉模块 =================
        HaloPart halo = new HaloPart();
        halo.mirror = false;
        halo.shapes = 4;
        halo.radius = 6f;
        halo.triLength = 4f;
        halo.haloRadius = 16f;
        halo.haloRotateSpeed = -1f;
        clusterLobes.parts.add(halo);

        // ================= 能力模块 =================
        // 战况信息条
        clusterLobes.abilities.add(new ClusterLobesBarAbility());

        // ① 锻炉：拦弹 → 蓄能 → 减伤 / 反打
        HIInterceptAbility forge = new HIInterceptAbility();
        forge.range = 175f;
        forge.chargePerDamage = 0.0022f;
        forge.decayPerSecond = 0.05f;
        forge.damageReductionMax = 0.55f;
        forge.absorbEffect = HIEffects.polyHit;
        clusterLobes.abilities.add(forge);

        // ② 镜盾：按角度反射
        HIMirrorShieldAbility mirror = new HIMirrorShieldAbility();
        mirror.sides = 6;
        mirror.radius = 62f;
        mirror.arc = 150f;
        mirror.reload = 7f;
        mirror.reflectSpeedScl = 1.3f;
        mirror.reflectDamageScl = 1.6f;
        mirror.spin = 0.35f;
        clusterLobes.abilities.add(mirror);

        // ③ 修复场（EU「神谕/海幻」同款思路）
        HIRepairFieldAbility repair = new HIRepairFieldAbility();
        repair.range = 150f;
        repair.amount = 55f;
        repair.reload = 30f;
        clusterLobes.abilities.add(repair);

        // ④ 光环场（EU「冥域」：增强己方 / 削弱敌方）
        HIAuraFieldAbility aura = new HIAuraFieldAbility();
        aura.range = 230f;
        aura.reload = 30f;
        aura.allyHealPercent = 0.02f;
        aura.allyStatus = StatusEffects.overdrive;
        aura.allyStatusDuration = 2f;
        aura.enemyStatus = HIStatus.electromagneticPulse;
        aura.enemyStatusDuration = 2f;
        aura.enemyDamage = 0f;
        aura.color = HIColors.b4;
        clusterLobes.abilities.add(aura);

        // ⑤ 护盾再生场（官方 ShieldRegenFieldAbility，EU「湮灭」同款）
        clusterLobes.abilities.add(new ShieldRegenFieldAbility(100f, 600f, 60f * 6f, 200f));

        // ⑥ 电磁第二血条
        HIEmpAbility emp = new HIEmpAbility();
        emp.empFraction = 0.3f;
        emp.empRepairPerSecond = 0.015f;
        clusterLobes.abilities.add(emp);

        // ⑦ 母舰：多点多周期孵化（EU「湮灭」4 个孵化点同款）
        clusterLobes.abilities.add(spawn(welder, 60f * 14f, 9.5f, 0f, 4));
        clusterLobes.abilities.add(spawn(welder, 60f * 14f, -9.5f, 0f, 4));
        clusterLobes.abilities.add(spawn(welder, 60f * 22f, 22f, 0f, 3));
        clusterLobes.abilities.add(spawn(welder, 60f * 22f, -22f, 0f, 3));

        // ⑧ 死亡爆发 + 瘫痪（EU「海幻」同款）
        HIDeathBlastAbility death = new HIDeathBlastAbility();
        death.range = 200f;
        death.damage = 2600f;
        death.paralyze = 60f * 4f;
        death.effect = HIEffects.polyHit;
        clusterLobes.abilities.add(death);

        // ⑨ 黑洞 / 能量吸引（外环拉伸）
        HIBlackHoleAbility blackHole = new HIBlackHoleAbility(8f * 28f, 8f * 27f);
        blackHole.pullAccel = 0.10f;
        blackHole.pullBonus = 0.22f;
        blackHole.maxPullSpeed = 4.5f;
        blackHole.healthPercentPerSecond = 0.03f;
        blackHole.status = HIStatus.electromagneticPulse;
        blackHole.stretchCount = 32;
        blackHole.stretchLength = 40f;
        blackHole.ellipse = 0.16f;
        blackHole.edgeColor = HIColors.b4;
        clusterLobes.abilities.add(blackHole);
    }

    // ==================================================================
    //  武器工厂
    // ==================================================================

    /** 蓄力主炮：锻炉蓄能 → 伤害 / 射速；组合射击模式。 */
    private static Weapon chargeWeapon(float baseRotation){
        HIChargeWeapon w = new HIChargeWeapon("clusterLobes-weapon");
        w.mirror = false;
        w.baseRotation = baseRotation;
        w.shake = 3f;
        w.shootCone = 360f;
        w.rotate = false;
        w.rotateSpeed = 0f;
        w.reload = 60f * 3f;
        w.inaccuracy = 10f;
        w.bullet = HIBullets.clusterBullet;
        w.damageBoost = 2.5f;
        w.reloadBoost = 1.2f;
        w.chargePerShot = 0.28f;

        // 组合射击：先 4 连发（间隔 6 帧），再补 2 连发
        ShootPattern main = new ShootPattern();
        main.shots = 4;
        main.shotDelay = 6f;
        ShootPattern extra = new ShootPattern();
        extra.shots = 2;
        w.shoot = new ShootMulti(main, extra);
        return w;
    }

    /** 点防御武器：自动打掉靠近的敌方子弹。 */
    private static Weapon pointDefense(){
        PointDefenseWeapon w = new PointDefenseWeapon("clusterLobes-point-defense");
        w.mirror = false;
        w.x = 0f;
        w.y = 1f;
        w.reload = 8f;
        w.targetInterval = 10f;
        w.targetSwitchInterval = 14f;
        w.shootSound = mindustry.gen.Sounds.shootForeshadow;
        w.bullet = new BulletType(){{
            shootEffect = Fx.sparkShoot;
            hitEffect = Fx.pointHit;
            maxRange = 288f;
            damage = 45f;
        }};
        return w;
    }

    /** 轨道主炮：高伤穿透，慢速。 */
    private static Weapon railWeapon(){
        Weapon w = new Weapon("clusterLobes-rail");
        w.mirror = false;
        w.top = false;
        w.rotate = true;
        w.rotateSpeed = 2f;
        w.x = 0f;
        w.y = 5f;
        w.shootY = 14f;
        w.reload = 60f * 2f;
        w.recoil = 5f;
        w.shake = 6f;
        w.ejectEffect = Fx.none;
        w.shootSound = mindustry.gen.Sounds.shootForeshadow;
        w.bullet = HIExtraBullets.railBullet;
        return w;
    }

    /** 孵化能力工厂。 */
    private static HIUnitSpawnAbility spawn(UnitType child, float time, float sx, float sy, int limit){
        HIUnitSpawnAbility a = new HIUnitSpawnAbility(child, time);
        a.sX = sx;
        a.sY = sy;
        a.limit = limit;
        a.amount = 1;
        a.spread = 18f;
        return a;
    }

    private HIUnits(){
    }
}
