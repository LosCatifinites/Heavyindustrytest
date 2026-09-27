package hiy;

import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.effect.MultiEffect;

/**
 * 特效挂载中心。
 *
 * 集中管理「哪些特效挂在哪个攻击/防御环节」，这样调特效只改这一个文件。
 *
 * 全部取自你挑选的 17 个官方预设（已逐个在 Mindustry v160.5 的 Fx.class 常量池里核对存在），
 * 外加一个复刻 NewHorizon 的「空间撕裂」（{@link HIEffects#spaceTear}，来自
 * {@code NHFx.collapserBulletExplode}）。
 *
 * 挂载对照表
 * ─────────────────────────────────────────────────────────────
 *  攻击环节            特效
 * ─────────────────────────────────────────────────────────────
 *  主炮弹 · 开火       Fx.shootQuellPulse
 *  主炮弹 · 飞行拖尾   Fx.missileTrailShort
 *  主炮弹 · 命中       Fx.sparkExplosion（射速快，用轻特效）
 *  主炮弹 · 消散       Fx.squareWaveEffect
 *  分裂弹 · 命中/消散  Fx.dynamicSpikes
 *  环状激光 · 发射     Fx.lancerLaserShoot
 *  环状激光 · 命中     Fx.lightningCharge
 *  轨道炮 · 开火       Fx.railShoot + HIEffects.muzzleHeavy
 *  轨道炮 · 穿透       Fx.scatheSlash
 *  轨道炮 · 命中       ★ 空间撕裂（NH 坍缩爆炸复刻，缩小版）
 *  曳光弹 · 命中       Fx.sparkExplosion
 *  点防御 · 命中       Fx.sparkExplosion
 *  空中爆点 · 命中     Fx.scatheExplosion
 * ─────────────────────────────────────────────────────────────
 *  防御环节            特效
 * ─────────────────────────────────────────────────────────────
 *  锻炉 · 吸收敌弹     Fx.teleportOut
 *  镜盾 · 反射成功     Fx.scatheSlash
 *  修复场 · 每次结算   Fx.overdriveBlockFull
 *  光环场 · 每次结算   Fx.overdriveBlockFull
 *  （孵化模块已随 welder 一并移除；spawnCombo 仍保留备用）
 *  死亡 · 爆发         Fx.coreExplosion
 *  死亡 · 附加         Fx.breakProp + Fx.coreBuildShockwave
 * ─────────────────────────────────────────────────────────────
 */
public class HIFx{

    // ---- 你挑选的 17 个官方预设（原样引用，方便统一替换）----
    public static final Effect
        squareWave        = Fx.squareWaveEffect,
        lancerLaserShoot  = Fx.lancerLaserShoot,
        coreExplosion     = Fx.coreExplosion,
        rotateBlock       = Fx.rotateBlock,
        breakProp         = Fx.breakProp,
        overdriveBlockFull = Fx.overdriveBlockFull,
        shootQuellPulse   = Fx.shootQuellPulse,
        missileTrailShort = Fx.missileTrailShort,
        coreBuildShockwave = Fx.coreBuildShockwave,
        dynamicSpikes     = Fx.dynamicSpikes,
        railShoot         = Fx.railShoot,
        scatheSlash       = Fx.scatheSlash,
        coreLaunchConstruct = Fx.coreLaunchConstruct,
        teleportOut       = Fx.teleportOut,
        scatheExplosion   = Fx.scatheExplosion,
        lightningCharge   = Fx.lightningCharge,
        sparkExplosion    = Fx.sparkExplosion;

    /** 死亡时的附加组合：碎片飞溅 + 核心冲击波。 */
    public static Effect deathExtra;

    /** 孵化时的组合：建造光束 + 旋转方块。 */
    public static Effect spawnCombo;

    /** 把特效挂到底层弹种上（必须在 HIBullets / HIExtraBullets 之后调用）。 */
    public static void apply(){
        deathExtra = new MultiEffect(breakProp, coreBuildShockwave);
        spawnCombo = new MultiEffect(coreLaunchConstruct, rotateBlock);

        // ---- 主炮弹 ----
        HIBullets.clusterBullet.shootEffect = shootQuellPulse;
        HIBullets.clusterBullet.trailEffect = missileTrailShort;
        HIBullets.clusterBullet.trailChance = 0.35f;
        HIBullets.clusterBullet.hitEffect = sparkExplosion;       // 主炮弹射速快，用轻特效
        HIBullets.clusterBullet.despawnEffect = squareWave;

        // ---- 分裂弹 ----
        HIBullets.clusterFrag.hitEffect = dynamicSpikes;
        HIBullets.clusterFrag.despawnEffect = dynamicSpikes;

        // ---- 环状激光 ----
        HIBullets.ringLaser.shootEffect = lancerLaserShoot;
        HIBullets.ringLaser.hitEffect = lightningCharge;

        // ---- 轨道炮 ----
        HIExtraBullets.railBullet.hitEffect = HIEffects.spaceTear;   // ★ 空间撕裂（唯一展示位，约 2 秒一发）
        HIExtraBullets.railBullet.pierceEffect = scatheSlash;

        // ---- 曳光弹 ----
        HIExtraBullets.tracerBullet.hitEffect = sparkExplosion;

        // ---- 空中爆点 ----
        HIExtraBullets.airburstBullet.hitEffect = scatheExplosion;
    }

    private HIFx(){
    }
}
