package hiy;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.RailBulletType;

/**
 * 额外弹种（模块化新增，不动原 HIBullets）。
 *
 * 来源：
 *   - {@link #tracerBullet}  NewHorizon「幽影」的 TracerBulletType
 *   - {@link #railBullet}    EU「湮灭」的主炮 RailBulletType
 *   - {@link #airburstBullet} EU「湮灭」的空中爆点弹（PointBulletType 的等价物）
 */
public class HIExtraBullets{

    /** 追踪曳光弹：先直飞、后拐弯，命中带 shock 状态。 */
    public static HITracerBulletType tracerBullet;

    /** 轨道主炮：穿透，伤害随穿透递减。 */
    public static RailBulletType railBullet;

    /** 空中爆点弹：高速 + 大范围溅射。 */
    public static BasicBulletType airburstBullet;

    public static void load(){
        // ---------- 追踪曳光弹 ----------
        tracerBullet = new HITracerBulletType(8.5f, 78f);
        tracerBullet.lifetime = 48f;
        tracerBullet.inaccuracy = 12f;
        tracerBullet.width = 12f;
        tracerBullet.height = 20f;
        tracerBullet.trailWidth = 1.4f;
        tracerBullet.trailLength = 8;
        tracerBullet.trailColor = HIColors.b4;
        tracerBullet.frontColor = Color.white;
        tracerBullet.backColor = HIColors.b4;
        tracerBullet.tracerHoming = 0.3f;
        tracerBullet.tracerRange = 64f;
        tracerBullet.tracerDelay = 18f;
        tracerBullet.followAimSpeed = 8f;
        tracerBullet.knockback = 0.75f;
        tracerBullet.buildingDamageMultiplier = 0.05f;
        tracerBullet.status = StatusEffects.shocked;
        tracerBullet.statusDuration = 30f;
        tracerBullet.hitEffect = Fx.hitLancer;
        tracerBullet.despawnEffect = Fx.hitLancer;
        tracerBullet.shootEffect = Fx.sparkShoot;
        tracerBullet.smokeEffect = Fx.shootSmallSmoke;

        // ---------- 轨道主炮 ----------
        railBullet = new RailBulletType();
        railBullet.shootEffect = Fx.railShoot;
        railBullet.length = 420f;
        railBullet.pointEffectSpace = 60f;
        railBullet.pierceEffect = Fx.railHit;
        railBullet.pointEffect = Fx.railTrail;
        railBullet.hitEffect = Fx.massiveExplosion;
        railBullet.smokeEffect = Fx.shootBig2;
        railBullet.damage = 1350f;
        railBullet.pierceDamageFactor = 0.6f;
        railBullet.pierceBuilding = true;

        // ---------- 空中爆点弹 ----------
        airburstBullet = new BasicBulletType(320f, 100f);
        airburstBullet.collidesGround = false;
        airburstBullet.collidesAir = true;
        airburstBullet.collidesTiles = false;
        airburstBullet.lifetime = 42f;
        airburstBullet.splashDamage = 520f;
        airburstBullet.splashDamageRadius = 88f;
        airburstBullet.status = StatusEffects.shocked;
        airburstBullet.statusDuration = 30f;
        airburstBullet.hitShake = 6f;
        airburstBullet.hitEffect = Fx.none;
        airburstBullet.despawnEffect = Fx.none;
        airburstBullet.shootEffect = Fx.none;
        airburstBullet.smokeEffect = Fx.none;
    }

    private HIExtraBullets(){
    }
}
