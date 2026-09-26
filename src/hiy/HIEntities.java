package hiy;

import arc.func.Prov;
import arc.struct.ObjectMap;
import mindustry.gen.EntityMapping;
import mindustry.gen.Unit;

/**
 * 重工业的实体注册中心。
 *
 * 对应 DeepSpace 的 {@code ice.entities.EntityRegistry}，但按「单位内容名」而不是
 * 「实体类」索引 —— 因为多个单位可以共用同一个实体类（例如 HIUnitEntity）。
 *
 * 关键时序（官方 UnitType.java:532 证实）：
 *   {@code UnitType} 的**构造器**里就会执行 {@code constructor = EntityMapping.map(this.name)}
 *   所以 {@link #register} 必须在 {@code new UnitType(...)} **之前**调用。
 *   {@link HIUnitType#create} 已经帮你保证了这个顺序。
 */
public class HIEntities{

    /** full name（重工业-xxx） -> entity id */
    private static final ObjectMap<String, Integer> ids = new ObjectMap<>();

    /** 登记实体构造器，返回 EntityMapping 分配的 id。必须在 new UnitType 之前调用。 */
    public static <T extends Unit> int register(String contentName, Prov<T> prov){
        String full = HeavyIndustry.MOD_NAME + "-" + contentName;
        Integer old = ids.get(full);
        if(old != null) return old;

        int id = EntityMapping.register(full, prov);
        ids.put(full, id);
        return id;
    }

    /** 查已登记的 id，未登记返回 -1。 */
    public static int id(String contentName){
        return ids.get(HeavyIndustry.MOD_NAME + "-" + contentName, -1);
    }

    private HIEntities(){
    }
}
