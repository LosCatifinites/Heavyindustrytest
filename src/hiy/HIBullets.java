package hiy;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BombBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.effect.WaveEffect;
import mindustry.gen.Bullet;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;

/**
 * 裂片集群用到的弹种。
 *
 * 由 DeepSpace 的 ice.entities.bullet.base.BasicBulletType /
 * ice.entities.bullet.BombBulletType 以及「裂片集群」内联弹种复刻而来。
 * 这里直接用官方 BasicBulletType / BombBulletType，无需搬运 DeepSpace 的框架。
 */
public class HIBullets{

    /** 主武器：追踪弹，消失时炸成 15 枚小炸弹。 */
    public static BasicBulletType clusterBullet;

    /** 分裂出的小炸弹（速度 0，纯范围伤害 + 演出）。 */
    public static BombBulletType clusterFrag;

    /** 单位本体环状激光。 */
    public static LaserBulletType ringLaser;

    public static void load(){
        // ---------- 分裂炸弹 ----------
        clusterFrag = new BombBulletType(15f, 40f){
            {
                collidesGround = true;
                collides = true;
                splashDamage = 15f;
                collidesTiles = true;
                speed = 0f;
                collidesAir = true;
                drag = 0f;
                lifetime = 15f;
                despawnEffect = new WaveEffect(){
                    {
                        lifetime = 15f;
                        sizeTo = 15f;
                        strokeFrom = 4f;
                        colorFrom = HIColors.b4;
                        colorTo = HIColors.b4;
                    }
                };
                hitEffect = despawnEffect;
            }

            @Override
            public void draw(Bullet b){
                Draw.color(HIColors.b4);
                b.vel.setZero();
                float rot = b.data instanceof Number ? ((Number)b.data).floatValue() : 0f;
                Drawf.tri(b.x, b.y, 8f, 8f, rot);
            }
        };

        // ---------- 环状激光 ----------
        ringLaser = new LaserBulletType(100f){
            {
                status = StatusEffects.shocked;
                width = 19f;
                hitEffect = Fx.hitLancer;
                sideAngle = 175f;
                sideWidth = 1f;
                sideLength = 40f;
                lifetime = 22f;
                drawSize = 400f;
                length = 180f;
                pierceBuilding = true;
                pierce = true;
            }
        };

        // ---------- 主武器追踪弹 ----------
        clusterBullet = new BasicBulletType(8f, 40f){
            {
                smokeEffect = Fx.none;
                shootSound = Sounds.shootMalign;
                shootEffect = Fx.none;
                // 原式：lifetime = 60 * 8f / speed + 60
                lifetime = 60f * 8f / speed + 60f;
                trailLength = 24;
                trailWidth = 3f;
                trailColor = HIColors.b4;
                homingRange = 100f * 8f;
                homingPower = 0.2f;
                homingDelay = 10f;
                hitEffect = HIEffects.polyHit;
                despawnEffect = HIEffects.polyHit;
                despawnHit = true;
            }

            @Override
            public void removed(Bullet b){
                for(int i = 0; i < 15; i++){
                    float x = Mathf.range(36f);
                    float y = Mathf.range(36f);
                    float ang = Mathf.random(360f);
                    float data = Mathf.random(360f);
                    clusterFrag.create(b, b.team, b.x + x, b.y + y, ang, -1f, 1f, 1f, data);
                }
                super.removed(b);
            }

            @Override
            public void update(Bullet b){
                super.update(b);
                if(Mathf.chanceDelta(1f)){
                    float x = Mathf.range(8f);
                    float y = Mathf.range(8f);
                    HIEffects.layerBullet.at(b.x + x, b.y + y, 0f, HIColors.b4, Mathf.random(360f));
                }
            }

            @Override
            public void draw(Bullet b){
                drawTrail(b);
                drawParts(b);

                float shrink = shrinkInterp.apply(b.fout());
                float dh = height * ((1f - shrinkY) + shrinkY * shrink);
                float dw = width * ((1f - shrinkX) + shrinkX * shrink);

                Tmp.c1.set(mixColorFrom).lerp(mixColorTo, b.fin());

                Draw.mixcol(Tmp.c1, Tmp.c1.a);
                Draw.color(trailColor);
                Drawf.tri(b.x, b.y, dw + 2f, dh, b.rotation());

                Draw.reset();
            }
        };
    }

    private HIBullets(){
    }
}
