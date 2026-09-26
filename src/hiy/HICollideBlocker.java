package hiy;

import mindustry.gen.Hitboxc;
import mindustry.gen.Unit;

/**
 * 让单位在某些条件下忽略碰撞。
 * 对应 DeepSpace 的 {@code universecore.world.ability.ICollideBlockerAbility}，
 * 由 {@link HIUnitEntity#collides} 统一检查。
 */
public interface HICollideBlocker{
    boolean blockedCollides(Unit unit, Hitboxc other);
}
