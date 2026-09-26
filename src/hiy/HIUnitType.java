package hiy;

import arc.Core;
import arc.func.Prov;
import arc.graphics.Color;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Unit;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.type.Weapon;

/**
 * 重工业单位内容基类。
 *
 * 由两部分合并而来：
 *   1. DeepSpace 的 {@code SglUnitType}（27 行）：实体注册 + init/read/write 钩子
 *   2. NewHorizon 的 {@code NHUnitType}：
 *      - 统一描边色 / 描边宽度
 *      - {@link #init()} 里按「最远武器射程」自动算 {@code fogRadius}
 *      - 工厂造价用的 {@link #setRequirements}
 *
 * 用法：
 * <pre>
 *   // ★ 必须先建「要生成的子单位」，再建引用它的单位
 *   welder = HIUnitType.create("welder", HIUnitEntity::new);
 *   clusterLobes = HIUnitType.create("clusterLobes", ClusterLobesUnit::new);
 * </pre>
 */
public class HIUnitType extends UnitType{

    /** 默认描边色（NH 的 grayOutline）。 */
    public static final Color defaultOutline = Color.valueOf("2e3039");

    /** EntityMapping 分配到的 id，由 {@link HIUnitEntity#classId()} 返回。 */
    public int entityId = -1;

    /** 贴图缺失时的兜底 region 名（例如 "flare"）。
     *  必须在 {@link #load()} 里设置，因为 {@code UnitType.load()} 会用
     *  {@code Core.atlas.find(name)} <b>无条件覆盖</b> region。 */
    public String fallbackRegion;

    /** 统一入口：先登记实体、再创建内容（顺序对了 constructor 才会自动绑定）。 */
    public static <T extends Unit> HIUnitType create(String contentName, Prov<T> prov){
        int id = HIEntities.register(contentName, prov);
        HIUnitType type = new HIUnitType(contentName);
        type.entityId = id;
        return type;
    }

    public HIUnitType(String contentName){
        super(contentName);

        // ---- 模块：统一描边（NHUnitType 同款）----
        outlineColor = defaultOutline;
        outlineRadius = 3;
    }

    /** 模块：按最远武器射程自动设置战争迷雾半径（NewHorizon 的做法）。 */
    @Override
    public void init(){
        super.init();

        float maxWeaponRange = 0f;
        for(Weapon weapon : weapons){
            if(weapon.range() > maxWeaponRange) maxWeaponRange = weapon.range();
        }
        // 覆盖掉 gameplay 里对 fogRadius 的手动赋值；想自定义请在子类 init() 里再改
        fogRadius = maxWeaponRange / 8f;
    }

    /** 模块：工厂造价（NHUnitType 同款）。 */
    public void setRequirements(ItemStack[] stacks){
        cachedRequirements = stacks;
        totalRequirements = firstRequirements = ItemStack.mult(stacks, 1f / 15f);
    }

    /** 单位被创建（add）后调用。 */
    public void initUnit(Unit unit){
    }

    /** 反序列化：自己追加的字段在这里读（必须与 writeUnit 顺序一致）。 */
    public void readUnit(Unit unit, Reads read){
    }

    /** 序列化：自己追加的字段在这里写。顺序一旦改动老存档会坏。 */
    public void writeUnit(Unit unit, Writes write){
    }

    /** 自检：没登记上就警告（不会崩，但会退化成 UnitEntity）。 */
    public void checkRegistered(){
        if(entityId < 0){
            Log.warn("[重工业] 单位 @ 没有登记实体类，将退化为 UnitEntity", name);
        }
    }

    @Override
    public void load(){
        super.load();

        // UnitType.load() 刚刚把 region 覆盖成 Core.atlas.find(name)，
        // 这里在贴图缺失时兜底到原版 region（避免满屏 error 图）。
        if(fallbackRegion != null && (region == null || !region.found())){
            region = Core.atlas.find(fallbackRegion);
        }
    }
}
