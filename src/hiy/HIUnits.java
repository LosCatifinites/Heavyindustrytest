package hiy;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.part.HaloPart;
import mindustry.entities.pattern.ShootBarrel;
import mindustry.gen.EntityMapping;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;

/**
 * 内容注册入口：把「裂片集群」注册成官方 UnitType。
 *
 * 数值与武器全部取自 DeepSpace 的 src/ice/content/unit/裂片集群.kt。
 */
public class HIUnits{

    /** 裂片集群。 */
    public static UnitType clusterLobes;

    public static void load(){
        // ★ 必须在创建 UnitType 之前注册实体。
        //   UnitType 的构造函数会执行 EntityMapping.map(this.name)，
        //   而 this.name 此时已被 transformName 加上「重工业-」前缀，
        //   名称一致时构造器会被自动替换成我们的实体类。
        ClusterLobesUnit.registeredId =
            EntityMapping.register(HeavyIndustry.MOD_NAME + "-clusterLobes", ClusterLobesUnit::new);

        clusterLobes = new UnitType("clusterLobes");
        clusterLobes.constructor = ClusterLobesUnit::new;

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

        // ---------- 信息面板附加条 ----------
        clusterLobes.abilities.add(new ClusterLobesBarAbility());

        // ---------- 黑洞 / 能量吸引（演示；不想要就删掉下面这段） ----------
        // 移植自 EU sucker.js（牵引）+ DeepSpace BlockHoleBulletType（距离衰减/百分比伤害）。
        // 视觉是纯矢量的「外环拉伸」，不吞本体、不扭曲像素。
        // 想启用 EU 那种整屏像素扭曲：blackHole.shader = true;（会用到 HIBlackHoles + TearingSpace.frag）
        HIBlackHoleAbility blackHole = new HIBlackHoleAbility(8f * 28f, 8f * 27f);
        blackHole.pullAccel = 0.10f;
        blackHole.pullBonus = 0.22f;
        blackHole.maxPullSpeed = 4.5f;
        blackHole.healthPercentPerSecond = 0.03f;
        blackHole.status = HIStatus.electromagneticPulse;
        blackHole.stretchCount = 32;        // 外环上拉伸线的数量
        blackHole.stretchLength = 40f;      // 拉伸线长度
        blackHole.ellipse = 0.16f;          // 外环被拉成椭圆的程度
        blackHole.edgeColor = HIColors.b4;
        clusterLobes.abilities.add(blackHole);
    }

    private static Weapon weapon(float baseRotation){
        Weapon w = new Weapon("clusterLobes-weapon");
        w.mirror = false;
        w.baseRotation = baseRotation;
        w.shake = 3f;
        w.shootCone = 360f;
        w.rotate = false;
        w.rotateSpeed = 0f;
        w.reload = 60f * 3f;
        w.inaccuracy = 60f;
        w.bullet = HIBullets.clusterBullet;

        ShootBarrel barrel = new ShootBarrel();
        barrel.shots = 4;
        barrel.shotDelay = 6f;
        w.shoot = barrel;
        return w;
    }

    private HIUnits(){
    }
}
