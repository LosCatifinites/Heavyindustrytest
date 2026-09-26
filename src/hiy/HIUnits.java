package hiy;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.part.HaloPart;
import mindustry.entities.pattern.ShootBarrel;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;

/**
 * 单位内容注册。
 *
 * 现在走的是重工业自己的单位框架（{@link HIUnitType} / {@link HIUnitEntity} / {@link HIEntities}），
 * 所以每个单位只需要一行 {@code HIUnitType.create(...)} 就完成「实体注册 + 构造器绑定」，
 * 单位自己的存档字段走 {@code initUnit/readUnit/writeUnit} 钩子。
 *
 * 注册顺序：<b>被生成的单位必须先建</b>（母舰的 HIUnitSpawnAbility 直接引用 welder）。
 */
public class HIUnits{

    /** 工蜂：装配母舰生成的小型维修单位（暂无专属贴图，兜底用原版 flare）。 */
    public static UnitType welder;

    /** 裂片集群：重工业的主力单位，整合了锻炉/镜盾/修复场/母舰/电磁五套机制。 */
    public static UnitType clusterLobes;

    public static void load(){
        loadWelder();
        loadClusterLobes();
    }

    // ==================================================================
    //  工蜂
    // ==================================================================
    private static void loadWelder(){
        welder = HIUnitType.create("welder", HIUnitEntity::new);
        welder.fallbackRegion = "flare";      // 贴图缺失时兜底（UnitType.load 之后生效）

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
        welder.itemCapacity = 0;
        welder.hidden = false;

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

        // ---------- 基础数值 ----------
        clusterLobes.health = 96000f;
        clusterLobes.armor = 0f;
        clusterLobes.hitSize = 32f;
        clusterLobes.speed = 18f / 7.5f;
        clusterLobes.rotateSpeed = 4f;
        clusterLobes.drag = 0.05f;
        clusterLobes.range = 8f * 60f;
        clusterLobes.engineSize = 0f;
        clusterLobes.itemCapacity = 0;

        // ---------- 形态 ----------
        clusterLobes.flying = true;
        clusterLobes.lowAltitude = true;
        clusterLobes.faceTarget = false;
        clusterLobes.drawBody = false;
        clusterLobes.drawCell = false;
        clusterLobes.hidden = false;

        // ---------- 免疫一切削弱移动速度的状态 ----------
        Seq<StatusEffect> all = Vars.content.statusEffects();
        for(StatusEffect s : all){
            if(s.speedMultiplier == 1f) continue;
            clusterLobes.immunities.add(s);
        }

        // ---------- 上下两门主武器（原 baseRotation 90 / 270）----------
        clusterLobes.weapons.add(weapon(90f));
        clusterLobes.weapons.add(weapon(270f));

        // ---------- 光环部件 ----------
        HaloPart halo = new HaloPart();
        halo.mirror = false;
        halo.shapes = 4;
        halo.radius = 6f;
        halo.triLength = 4f;
        halo.haloRadius = 16f;
        halo.haloRotateSpeed = -1f;
        clusterLobes.parts.add(halo);

        // ==============================================================
        //  机制整合（1 锻炉 / 2 母舰 / 3 镜盾 / 6 电磁）
        //  整段删掉即可回到「纯裂片集群」状态。
        // ==============================================================

        // ① 锻炉：拦弹 → 蓄能 →（减伤 + 反打）
        HIInterceptAbility forge = new HIInterceptAbility();
        forge.range = 175f;
        forge.chargePerDamage = 0.0022f;
        forge.decayPerSecond = 0.05f;
        forge.damageReductionMax = 0.55f;
        forge.absorbEffect = HIEffects.polyHit;
        clusterLobes.abilities.add(forge);

        // ③ 镜盾：正面多边形镜面，按角度反射子弹
        HIMirrorShieldAbility mirror = new HIMirrorShieldAbility();
        mirror.sides = 6;
        mirror.radius = 62f;
        mirror.arc = 150f;
        mirror.reload = 7f;
        mirror.reflectSpeedScl = 1.3f;
        mirror.reflectDamageScl = 1.6f;
        mirror.spin = 0.35f;
        clusterLobes.abilities.add(mirror);

        // ② 母舰：修复场 + 周期生成工蜂
        HIRepairFieldAbility repair = new HIRepairFieldAbility();
        repair.range = 150f;
        repair.amount = 55f;
        repair.reload = 30f;
        clusterLobes.abilities.add(repair);

        HIUnitSpawnAbility spawn = new HIUnitSpawnAbility(welder, 60f * 14f);
        spawn.amount = 1;
        spawn.limit = 4;
        spawn.spread = 26f;
        clusterLobes.abilities.add(spawn);

        // ⑥ 电磁：第二血条，打空即瘫痪
        HIEmpAbility emp = new HIEmpAbility();
        emp.empFraction = 0.3f;
        emp.empRepairPerSecond = 0.015f;
        clusterLobes.abilities.add(emp);

        // ---------- 信息面板附加条（原：格挡数量 / 护甲 / 伤害减免）----------
        clusterLobes.abilities.add(new ClusterLobesBarAbility());

        // ---------- 黑洞 / 能量吸引（外环拉伸；不想要就删掉下面这段）----------
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
    //  武器
    // ==================================================================

    /**
     * 主武器：蓄力炮。
     * 用 {@link HIChargeWeapon} 而不是普通 Weapon —— 它会把「锻炉蓄能」换成
     * 伤害倍率与射速倍率，开火时逐发消耗蓄能（①②两个机制因此连成闭环）。
     */
    private static Weapon weapon(float baseRotation){
        HIChargeWeapon w = new HIChargeWeapon("clusterLobes-weapon");
        w.mirror = false;
        w.baseRotation = baseRotation;
        w.shake = 3f;
        w.shootCone = 360f;
        w.rotate = false;
        w.rotateSpeed = 0f;
        w.reload = 60f * 3f;
        w.inaccuracy = 60f;
        w.bullet = HIBullets.clusterBullet;
        w.damageBoost = 2.5f;
        w.reloadBoost = 1.2f;
        w.chargePerShot = 0.28f;

        ShootBarrel barrel = new ShootBarrel();
        barrel.shots = 4;
        barrel.shotDelay = 6f;
        w.shoot = barrel;
        return w;
    }

    private HIUnits(){
    }
}
