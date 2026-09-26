package hiy;

import arc.math.Angles;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Teamc;

/**
 * 模块：追踪曳光弹（NewHorizon「幽影」的 TracerBulletType 简化版）。
 *
 * 与官方的区别：官方的 {@code homingPower} 是「按目标位置连续转向」，
 * 这里额外提供 {@link #followAimSpeed} —— 没锁到目标时朝**射手的准星**慢慢摆正，
 * 于是弹道会先直飞再拐弯，视觉上像曳光。
 *
 * 实现方式：覆写官方的 {@code BulletType.updateHoming(Bullet)}（它在 update() 内部被调用）。
 * 注意不要把 homingPower 和 tracerHoming 同时开，否则会叠加两次转向。
 */
public class HITracerBulletType extends BasicBulletType{

    /** 锁定目标后的转向力（>0 才生效）。 */
    public float tracerHoming = 0.3f;
    /** 追踪半径。 */
    public float tracerRange = 60f;
    /** 开始追踪前的延迟帧。 */
    public float tracerDelay = 20f;
    /** 没锁到目标时，朝射手准星摆正的速度（0 = 不摆）。 */
    public float followAimSpeed = 6f;

    public HITracerBulletType(){
        super();
    }

    public HITracerBulletType(float speed, float damage){
        super(speed, damage);
    }

    @Override
    public void updateHoming(Bullet b){
        if(b.time < tracerDelay) return;

        if(tracerHoming > 0.0001f){
            Teamc target = Units.closestTarget(b.team, b.x, b.y, tracerRange);
            if(target != null){
                b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(target), tracerHoming * Time.delta * 50f));
                return;
            }
        }

        if(followAimSpeed > 0f && b.shooter instanceof mindustry.gen.Unit){
            mindustry.gen.Unit u = (mindustry.gen.Unit)b.shooter;
            float angle = b.angleTo(u.aimX, u.aimY);
            b.vel.setAngle(Angles.moveToward(b.vel.angle(), angle, followAimSpeed * Time.delta));
        }
    }
}
