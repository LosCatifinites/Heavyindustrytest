// 空间撕裂 / 黑洞透镜扭曲
// 来源：EU（ExtraUtilities）assets/shaders/TearingSpace.frag
//       原作者 MEEPofFaith；本副本仅做 GLES 兼容改写（循环边界 + 去掉 --a/continue）
// 用法：由 HIBlackHoles.BlackHoleShader 加载，在 Trigger.postDraw 阶段整屏 blit。
// 每个黑洞是一个 vec4： (中心x, 中心y, 内半径, 外半径)，坐标都是"世界坐标"。

#define MAX_COUNT 8
#define HALFPI 1.5707963267948966

varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_campos;
uniform vec2 u_resolution;
uniform int u_blackholecount;
uniform vec4 u_blackholes[MAX_COUNT];

float interp(float a){
    float f = a - 1.0;
    float t = a;

    // Interp.pow5Out
    a = f * f * f * f * f + 1.0;

    // Interp.circle
    if(a <= 0.5){
        a *= 2.0;
        a = (1.0 - sqrt(1.0 - a * a)) / 2.0;
    }else{
        a = a - 1.0;
        a *= 2.0;
        a = (sqrt(1.0 - a * a) + 1.0) / 2.0;
    }

    // 在两个线性函数之间插值
    return f + (t - f) * a;
}

void main(){
    vec2 coords = (v_texCoords * u_resolution) + u_campos;
    vec2 offset = vec2(0.0);

    for(int i = 0; i < MAX_COUNT; i++){
        if(i >= u_blackholecount) break;

        vec4 blackhole = u_blackholes[i];
        float cX = blackhole.r;
        float cY = blackhole.g;
        float iR = blackhole.b;
        float oR = blackhole.a;

        float dst = distance(blackhole.xy, coords);

        if(dst <= oR){
            float p = (dst - iR) / (oR - iR);
            p = interp(p);
            float a = atan(coords.x - cX, coords.y - cY) + HALFPI;
            vec2 pos = vec2(cX - oR * cos(a) * p, cY + oR * sin(a) * p);
            offset += pos - coords;
        }
    }

    coords += offset;
    gl_FragColor = texture2D(u_texture, (coords - u_campos) / u_resolution);
}
