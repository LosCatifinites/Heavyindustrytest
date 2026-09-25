package hiy;

import mindustry.type.StatusEffect;

/**
 * 自定义状态效果。
 * 目前只有从 DeepSpace 的 ice.content.IStatus 复刻过来的「电磁脉冲」。
 *
 * 注意：内容名会被 Vars.content.transformName() 自动加上「重工业-」前缀，
 * 所以实际名字是 重工业-electromagneticPulse，bundle key 也按这个写。
 */
public class HIStatus{

    /** 电磁脉冲：降低目标移动速度与生命上限。 */
    public static StatusEffect electromagneticPulse;

    public static void load(){
        electromagneticPulse = new StatusEffect("electromagneticPulse");
        electromagneticPulse.speedMultiplier = 0.7f;
        electromagneticPulse.healthMultiplier = 0.9f;
        electromagneticPulse.color = HIColors.b4;
    }

    private HIStatus(){
    }
}
