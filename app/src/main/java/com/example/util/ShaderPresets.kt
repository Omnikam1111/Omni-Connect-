package com.example.util

data class ShaderPresetInfo(
    val id: String,
    val nameEn: String,
    val nameDe: String,
    val code: String
)

object ShaderPresets {
    const val PRESET_CUSTOM_DEFAULT = """precision highp float;
uniform vec3 iResolution;
uniform float iTime;
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    float t = iTime * 0.4;
    float r = sin(uv.x * 10.0 + t) * 0.5 + 0.5;
    float g = sin(uv.y * 10.0 - t * 1.2) * 0.5 + 0.5;
    float b = sin((uv.x + uv.y) * 8.0 + t * 0.5) * 0.5 + 0.5;
    fragColor = vec4(vec3(r, g, b) * 0.25 + vec3(0.03, 0.02, 0.05), 1.0);
}"""

    const val PRESET_STARS = """precision highp float;
uniform vec3 iResolution;
uniform float iTime;
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
    float t = iTime * 0.08;
    vec3 col = vec3(0.0);
    vec2 st = fragCoord / iResolution.xy;
    col += mix(vec3(0.03, 0.015, 0.06), vec3(0.008, 0.002, 0.015), st.y);
    for(float i=1.0; i<5.0; i++) {
        float speed = i * 0.12;
        float depth = fract(t * speed + i * 0.25);
        vec2 st_layer = uv * mix(24.0, 3.0, depth) + vec2(1.0 * 0.22 + i * 15.73, -1.0 * 0.11);
        vec2 id = floor(st_layer);
        vec2 gr = fract(st_layer) - 0.5;
        float r = sin(id.x * 12.9898 + id.y * 78.233) * 43758.5453;
        float star = step(0.985, fract(r));
        if (star > 0.0) {
            float brightness = sin(t * 8.0 + r) * 0.5 + 0.5;
            float dist = length(gr);
            float glow = 0.015 / (dist + 0.01) * brightness;
            float fade = depth * (1.0 - depth) * 4.0;
            col += vec3(0.95, 0.9, 1.0) * glow * fade;
        }
    }
    fragColor = vec4(col, 1.0);
}"""

    const val PRESET_CLOUDS = """precision highp float;
uniform vec3 iResolution;
uniform float iTime;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0,0.0)), hash(i + vec2(1.0,0.0)), u.x),
               mix(hash(i + vec2(0.0,1.0)), hash(i + vec2(1.0,1.0)), u.x), u.y);
}
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    mat2 rot = mat2(0.707, 0.707, -0.707, 0.707);
    for (int i = 0; i < 4; ++i) {
        v += a * noise(p);
        p = rot * p * 2.0 + vec2(100.0);
        a *= 0.5;
    }
    return v;
}
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    vec2 p = uv * 3.5;
    p.x += iTime * 0.04 + 1.0 * 0.17;
    p.y += sin(iTime * 0.01) * 0.15 + 1.0 * 0.23;
    float f = fbm(p + fbm(p + iTime * 0.02));
    vec3 sky = mix(vec3(0.04, 0.06, 0.15), vec3(0.12, 0.18, 0.35), uv.y);
    vec3 cloudColor = vec3(0.8, 0.65, 0.6) * 0.55 + vec3(0.15, 0.3, 0.45) * 0.45;
    fragColor = vec4(mix(sky, cloudColor, clamp(f * 1.4 - 0.2, 0.0, 1.0)), 1.0);
}"""

    const val PRESET_FIRE = """precision highp float;
uniform vec3 iResolution;
uniform float iTime;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0,0.0)), hash(i + vec2(1.0,0.0)), u.x),
               mix(hash(i + vec2(0.0,1.0)), hash(i + vec2(1.0,1.0)), u.x), u.y);
}
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    float pixelScale = max(iResolution.x / 110.0, 1.0);
    vec2 pc = floor(fragCoord.xy / pixelScale) * pixelScale;
    vec2 uv = pc / iResolution.xy;
    vec2 p = (pc - 0.5 * iResolution.xy) / iResolution.y;
    p.y += 0.5 + 1.0 * 0.03;
    p.x += 1.0 * 0.05;
    float n = noise(vec2(p.x * 6.0, p.y * 3.5 - iTime * 4.5));
    float n2 = noise(vec2(p.x * 12.0, p.y * 8.0 - iTime * 6.5));
    p.x += (n - 0.5) * 0.35 * (1.1 - uv.y) + (n2 - 0.5) * 0.15 * (1.1 - uv.y);
    float distToCenter = length(p * vec2(2.2, 0.75));
    float intensity = clamp((1.0 - distToCenter + (1.0 - uv.y) * 0.35) * 1.5, 0.0, 1.0);
    intensity = floor(intensity * 5.0) / 5.0;
    vec3 finalCol = vec3(0.02, 0.01, 0.03);
    if (intensity > 0.8) finalCol = vec3(1.0, 0.95, 0.4);
    else if (intensity > 0.6) finalCol = vec3(1.0, 0.55, 0.0);
    else if (intensity > 0.4) finalCol = vec3(0.9, 0.12, 0.05);
    else if (intensity > 0.2) finalCol = vec3(0.35, 0.08, 0.1);
    float sparkNoise = hash(floor((pc + vec2(0.0, iTime * 15.0 * pixelScale)) / (5.0 * pixelScale)));
    if (sparkNoise > 0.992 && uv.y > 0.3 && uv.y < 0.8) {
        finalCol = mix(finalCol, vec3(1.0, 0.8, 0.2), step(0.5, hash(pc)));
    }
    fragColor = vec4(finalCol, 1.0);
}"""

    val PRESETS = listOf(
        ShaderPresetInfo(
            id = "stars",
            nameEn = "Celestial Starfield",
            nameDe = "Himmels-Sterne",
            code = PRESET_STARS
        ),
        ShaderPresetInfo(
            id = "human_shape",
            nameEn = "Human Shapeshifter",
            nameDe = "Menschlicher Gestaltwandler",
            code = """precision highp float;
#define NUMMB 16
uniform float time;
uniform vec2 mouse;
uniform vec2 resolution;
float rn(float xx){ return fract(sin(floor(xx) * 0.4686) * 3718.927 + floor(xx)); }
vec3 palette(float t){ return 0.55 + 0.45 * cos(6.28318 * (vec3(0.08, 0.18, 0.32) + t)); }
vec2 humanPos(int i, float aspect) {
    float breath = sin(time * 0.6) * 0.015;
    if (i == 0) return vec2(0.0, 0.65 + breath);
    if (i == 1) return vec2(0.0, 0.35 + breath);
    if (i == 2) return vec2(0.0, 0.10 + breath);
    if (i == 3) return vec2(0.0,-0.15 + breath);
    float armSwing = sin(time * 0.8) * 0.02;
    if (i == 4) return vec2(-0.35 + armSwing, 0.30 + breath);
    if (i == 5) return vec2(-0.55 + armSwing, 0.05 + breath);
    if (i == 6) return vec2(-0.40 + armSwing,-0.20 + breath);
    if (i == 7) return vec2( 0.35 - armSwing, 0.30 + breath);
    if (i == 8) return vec2( 0.55 - armSwing, 0.05 + breath);
    if (i == 9) return vec2( 0.40 - armSwing,-0.20 + breath);
    float legShift = sin(time * 0.5) * 0.015;
    if (i == 10) return vec2(-0.15 + legShift, -0.45);
    if (i == 11) return vec2(-0.25 + legShift, -0.75);
    if (i == 12) return vec2(-0.20 + legShift, -1.05);
    if (i == 13) return vec2( 0.15 - legShift, -0.45);
    if (i == 14) return vec2( 0.25 - legShift, -0.75);
    if (i == 15) return vec2( 0.20 - legShift, -1.05);
    return vec2(0.0);
}
float field(vec2 p) {
    float aspect = resolution.x / resolution.y;
    float f = 0.0;
    for(int i = 0; i < NUMMB; i++) {
        vec2 d = p - humanPos(i, aspect);
        f += 0.12 / (dot(d, d) + 0.02);
    }
    return f;
}
void main() {
    vec2 p = (gl_FragCoord.xy / resolution.xy) * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;
    float aspect = resolution.x / resolution.y, f = 0.0;
    vec3 col = vec3(0.0);
    for(int i = 0; i < NUMMB; i++) {
        vec2 d = p - humanPos(i, aspect);
        float dist = dot(d, d), influence = 0.12 / (dist + 0.02);
        f += influence;
        col += palette(float(i) * 0.08 + time * 0.05) * influence;
    }
    col /= max(f, 0.0001);
    float eps = 0.01;
    vec3 n = normalize(vec3(field(p + vec2(eps, 0.0)) - f, field(p + vec2(0.0, eps)) - f, 0.08));
    vec3 lightDir = normalize(vec3(-0.4, 0.6, 1.0)), viewDir = vec3(0.0, 0.0, 1.0);
    float diff = max(dot(n, lightDir), 0.0) * 0.7 + 0.3;
    float spec = pow(max(dot(n, normalize(lightDir + viewDir)), 0.0), 30.0);
    float fres = pow(1.0 - max(dot(n, viewDir), 0.0), 2.0);
    col = mix(vec3(0.02, 0.01, 0.03), col * diff + spec * vec3(1.0, 0.95, 0.9) + fres * vec3(0.3, 0.4, 0.6), smoothstep(0.5, 1.5, f));
    col *= 1.0 - dot(p, p) * 0.25;
    gl_FragColor = vec4(1.0 - exp(-col * 1.2), 1.0);
}"""
		),
		ShaderPresetInfo(
			id = "liquid_light",
			nameEn = "Liquid Light Spectrum",
			nameDe = "Flüssiges Lichtspektrum",
			code = """precision highp float;
uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}
float ripple(vec2 p, vec2 center, float freq, float speed) {
    float d = length(p - center);
    return sin(d * freq - time * speed) * exp(-d * 3.5);
}
float heightField(vec2 p) {
    float h = 0.0;
    h += ripple(p, vec2(sin(time * 0.03), cos(time * 0.02)) * 0.4, 18.0, 0.7);
    h += ripple(p, vec2(cos(time * 0.025), sin(time * 0.04)) * 0.5, 22.0, 0.5);
    h += ripple(p, vec2(sin(time * 0.018 + 2.0), cos(time * 0.03)) * 0.45, 14.0, 0.4);
    h += ripple(p, vec2(cos(time * 0.015 + 4.0), sin(time * 0.02)) * 0.6, 26.0, 0.6);
    h += ripple(p, mouse * 2.0 - 1.0, 30.0, 1.2) * 0.8;
    return h * 0.08;
}
void main() {
    vec2 p = (gl_FragCoord.xy / resolution.xy) * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;
    float h = heightField(p);
    float e = 0.003;
    vec3 n = normalize(vec3(h - heightField(p + vec2(e,0.0)), h - heightField(p + vec2(0.0,e)), e));
    float flow = h * 6.0 + time * 0.25;
    vec3 water = hsv2rgb(vec3(fract(flow), 0.95, 0.55 + 0.45 * smoothstep(-0.05, 0.05, h)));
    vec3 lightDir = normalize(vec3(-0.3, 0.4, 1.0)), viewDir = vec3(0.0, 0.0, 1.0);
    float diff = max(dot(n, lightDir), 0.0) * 0.6 + 0.4;
    float spec = pow(max(dot(n, normalize(lightDir + viewDir)), 0.0), 90.0);
    float fres = pow(1.0 - max(dot(n, viewDir), 0.0), 3.0);
    vec3 col = water * diff + spec * vec3(1.0) + hsv2rgb(vec3(fract(flow) + 0.2, 1.0, 1.0)) * fres * 0.5;
    col += sin(h * 120.0 + time * 2.0) * 0.02;
    col *= exp(-abs(h) * 8.0) + 0.4;
    col = mix(hsv2rgb(vec3(fract(time * 0.05), 0.4, 0.05)), col, 0.95) * (1.0 - dot(p, p) * 0.25);
    gl_FragColor = vec4(1.0 - exp(-col * 1.2), 1.0);
}"""
		),
		ShaderPresetInfo(
			id = "lava_lamp",
			nameEn = "Convection Lava Lamp",
			nameDe = "Thermo-Lavalampe",
			code = """precision highp float;
uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;
float hash(float n) { return fract(sin(n) * 43758.5453123); }
float blob(vec2 p, vec2 c, float r) {
    float d = length(p - c);
    return r / (d * d + 0.08);
}
vec3 lavaPalette(float t) {
    vec3 dark = vec3(0.02, 0.005, 0.01), red = vec3(0.6, 0.05, 0.02);
    vec3 orange = vec3(0.9, 0.25, 0.05), yellow = vec3(1.0, 0.6, 0.1);
    float a = 0.5 + 0.5 * sin(t), b = 0.5 + 0.5 * cos(t * 0.7);
    return mix(dark, red, a) + mix(vec3(0.0), orange, b * 0.7) + yellow * pow(0.5 + 0.5 * sin(t * 1.2), 6.0) * 0.2;
}
void main() {
    vec2 p = (gl_FragCoord.xy / resolution.xy) * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;
    float t = time * 0.25, field = 0.0;
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        vec2 c = vec2(cos(t * (0.25 + hash(fi * 27.7)) + hash(fi * 41.3) * 6.28), sin(t * (0.3 + hash(fi * 13.1)) + hash(fi * 27.7) * 6.28)) * 0.7;
        c += ((mouse * 2.0 - 1.0) - c) * 0.08;
        field += blob(p, c, 0.35 + 0.15 * sin(hash(fi * 41.3) * 6.28));
    }
    float heat = smoothstep(1.2, 2.4, field), eps = 0.01;
    vec3 n = normalize(vec3(blob(p + vec2(eps,0.0), vec2(0.0), 1.0) - field, blob(p + vec2(0.0,eps), vec2(0.0), 1.0) - field, 1.0));
    vec3 lightDir = normalize(vec3(-0.4, 0.6, 1.0)), viewDir = vec3(0.0, 0.0, 1.0);
    float diff = max(dot(n, lightDir), 0.0) * 0.6 + 0.4;
    float spec = pow(max(dot(n, normalize(lightDir + viewDir)), 0.0), 40.0);
    float fres = pow(1.0 - max(dot(n, viewDir), 0.0), 3.0);
    vec3 col = lavaPalette(field * 0.8 + t * 0.4) * diff + spec * vec3(1.0, 0.4, 0.1) + fres * vec3(0.8, 0.2, 0.05) * 0.6;
    col += sin(field * 10.0 + time) * 0.02;
    col = mix(vec3(0.01, 0.0, 0.005), col * (heat + 0.3), 0.95) * (1.0 - dot(p, p) * 0.35);
    gl_FragColor = vec4(1.0 - exp(-col * 1.2), 1.0);
}"""
		),
		ShaderPresetInfo(
			id = "ocean_blue",
			nameEn = "Ocean Gerstner Waves",
			nameDe = "Ozean-Gerstner-Wellen",
			code = """precision highp float;
uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;
#define NUM_WAVES 6
float amplitude[NUM_WAVES];
float wavelength[NUM_WAVES];
float speed[NUM_WAVES];
vec2 direction[NUM_WAVES];
void initWaves() {
    amplitude[0] = 0.060; amplitude[1] = 0.045; amplitude[2] = 0.030; amplitude[3] = 0.020; amplitude[4] = 0.015; amplitude[5] = 0.010;
    wavelength[0] = 2.50; wavelength[1] = 1.80; wavelength[2] = 1.20; wavelength[3] = 0.90; wavelength[4] = 0.60; wavelength[5] = 0.35;
    speed[0] = 0.18; speed[1] = 0.22; speed[2] = 0.28; speed[3] = 0.35; speed[4] = 0.45; speed[5] = 0.60;
    direction[0] = normalize(vec2(1.0, 0.2)); direction[1] = normalize(vec2(0.8, 0.5)); direction[2] = normalize(vec2(0.6, 0.8));
    direction[3] = normalize(vec2(-0.3, 1.0)); direction[4] = normalize(vec2(-0.8, 0.4)); direction[5] = normalize(vec2(-0.5, -0.6));
}
float waveHeight(vec2 p) {
    float h = 0.0;
    for(int i = 0; i < NUM_WAVES; i++) {
        float k = 2.0 * 3.14159 / wavelength[i];
        h += amplitude[i] * sin(dot(direction[i], p) * k + speed[i] * k * time * 0.25);
    }
    return h;
}
void main() {
    initWaves();
    vec2 p = (gl_FragCoord.xy / resolution.xy) * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;
    vec2 wp = p * 3.0 + (p - (mouse * 2.0 - 1.0)) * 0.05 / (length(p - (mouse * 2.0 - 1.0)) + 0.3);
    float h = waveHeight(wp);
    float e = 0.01;
    vec3 n = normalize(vec3(waveHeight(wp) - waveHeight(wp + vec2(e,0.0)), waveHeight(wp) - waveHeight(wp + vec2(0.0,e)), e));
    vec3 lightDir = normalize(vec3(-0.3, 0.6, 1.0)), viewDir = vec3(0.0, 0.0, 1.0);
    float diff = pow(max(dot(n, lightDir), 0.0), 0.8);
    float spec = pow(max(dot(n, normalize(lightDir + viewDir)), 0.0), 80.0);
    float fres = pow(1.0 - max(dot(n, viewDir), 0.0), 4.0);
    vec3 base = mix(vec3(0.01, 0.02, 0.05), vec3(0.05, 0.25, 0.45), smoothstep(-0.05, 0.05, h)) * (0.6 + diff * 0.7);
    base += spec * vec3(1.0) + fres * vec3(0.2, 0.4, 0.8) + smoothstep(0.05, 0.12, h) * vec3(0.6, 0.7, 0.8) * 0.25;
    base = mix(vec3(0.0, 0.02, 0.05), base, exp(-abs(p.y) * 1.2)) * (1.0 - dot(p, p) * 0.25);
    gl_FragColor = vec4(1.0 - exp(-base * 1.3), 1.0);
}"""
		),
		ShaderPresetInfo(
			id = "retro_fire",
			nameEn = "CRT Pixel Fire",
			nameDe = "CRT-Pixel-Feuer",
			code = PRESET_FIRE
		),
		ShaderPresetInfo(
			id = "electric_storm",
			nameEn = "Electric Nebula Storm",
			nameDe = "Elektrischer Nebelsturm",
			code = """precision highp float;
uniform float time;
uniform vec2 resolution;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) { v += noise(p) * a; p *= 2.0; a *= 0.5; }
    return v;
}
float lightning(vec2 p) {
    float t = time * 0.1, flash = 0.0;
    float strikeChance = hash(vec2(floor(t), 0.0));
    if (strikeChance > 0.98) {
        float fPos = hash(vec2(strikeChance, 1.0)) * 2.0 - 1.0;
        flash = smoothstep(0.1, 0.0, abs(p.x - fPos)) * (sin(t * 50.0) * 0.5 + 0.5);
    }
    return flash;
}
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / resolution.xy;
    vec2 p = uv * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;
    float t = time * 0.05;
    vec2 q = p * 1.5;
    float n = fbm(q + vec2(t, -t)), n2 = fbm(q * 2.0 - vec2(t * 1.5, t));
    p += vec2(cos(n + 0.5 * n2 * 3.1415), sin(n + 0.5 * n2 * 3.1415)) * 0.3;
    float stormClouds = smoothstep(0.1, 0.9, fbm(p * 1.2 + t) * 0.7 + fbm(p * 2.5 - t * 1.3) * 0.3);
    vec3 color = mix(mix(vec3(0.05, 0.05, 0.1), vec3(0.2, 0.1, 0.3), stormClouds), vec3(0.4, 0.2, 0.1), fbm(p * 3.0));
    color += vec3(lightning(p) * 2.0) * stormClouds;
    fragColor = vec4(1.0 - exp(-color * (1.0 - dot(p, p) * 0.4) * 1.5), 1.0);
}
void main() { mainImage(gl_FragColor, gl_FragCoord.xy); }"""
		),
		ShaderPresetInfo(
			id = "symmetric_plasma",
			nameEn = "Retro Plasma Core",
			nameDe = "Retro-Plasmakern",
			code = """precision highp float;
uniform vec3 iResolution;
uniform float iTime;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}
vec3 palette(float t) { return 0.5 + 0.5 * cos(6.28318 * (vec3(0.0, 0.33, 0.67) + t)); }
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 p = (fragCoord.xy / iResolution.xy) - 0.5;
    p.x *= iResolution.x / iResolution.y;
    float t = iTime * 0.6;
    float s = sin(t * 0.3), c = cos(t * 0.3);
    p = mat2(c, -s, s, c) * p;
    float r = length(p), a = atan(p.y, p.x);
    float plasma = sin(r * 10.0 - t * 2.0) + sin(p.x * 8.0 + t) + sin(p.y * 8.0 - t * 1.5) + noise(p * 3.0 + t) * 0.8 + sin(10.0 * r - t * 3.0);
    a = mod(a, 6.28318 / 6.0);
    plasma += sin(a * 6.0 + plasma * 2.0) * 0.6;
    float centerGlow = exp(-r * 3.0);
    vec3 col = palette((plasma + centerGlow * 1.5) * 0.25 + t * 0.2) * (0.6 + centerGlow);
    col -= sin(fragCoord.y * 1.5) * 0.04;
    col += (hash(vec2(floor(iTime * 8.0), floor((fragCoord.y / iResolution.y) * 10.0))) - 0.5) * 0.03;
    fragColor = vec4(col * (1.0 - dot(p, p) * 0.8), 1.0);
}
void main() { mainImage(gl_FragColor, gl_FragCoord.xy); }"""
		),
		ShaderPresetInfo(
			id = "underwater_rays",
			nameEn = "Sun Rays Underwater",
			nameDe = "Unterwasser-Sonnenstrahlen",
			code = """precision highp float;
uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;
float hash(float n) { return fract(sin(n) * 43758.5453); }
float caustics(vec2 p, float t) {
    return (sin(p.x * 3.0 + t) + sin(p.y * 4.0 - t * 0.7) + sin((p.x + p.y) * 2.5 + t * 0.4)) * 0.33;
}
float shaft(vec2 p, vec2 dir) {
    return exp(-abs(p.y - dot(p, dir)) * 3.0);
}
void main() {
    vec2 p = (gl_FragCoord.xy / resolution.xy) * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;
    float t = time * 0.15;
    vec3 col = vec3(0.0);
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float a = t * (0.3 + hash(fi) * 0.2) + fi * 1.7;
        float s = shaft(p, vec2(cos(a), sin(a))) * exp(-abs(p.y + 0.3) * 2.0);
        col += vec3(0.1 + 0.1 * hash(fi + 1.0), 0.3 + 0.2 * hash(fi + 2.0), 0.5 + 0.3 * hash(fi + 3.0)) * s;
    }
    col += vec3(0.2, 0.5, 0.7) * caustics(p * 2.0, t) * 0.2;
    col = mix(vec3(0.002, 0.01, 0.02), col, 0.9) * (1.0 - dot(p, p) * 0.35);
    gl_FragColor = vec4(col / (1.0 + col), 1.0);
}"""
		),
		ShaderPresetInfo(
			id = "molten_cocoa",
			nameEn = "Glossy Molten Cocoa",
			nameDe = "Flüssiger Kakao-Ripple",
			code = """precision highp float;
uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;
vec3 chocolatePalette(float t) {
    vec3 dark = vec3(0.035, 0.012, 0.006), bitter = vec3(0.12, 0.045, 0.018);
    vec3 cocoa = vec3(0.22, 0.09, 0.035), caramel = vec3(0.38, 0.18, 0.06);
    return mix(dark, bitter, 0.5 + 0.5 * sin(t)) + mix(vec3(0.0), cocoa, 0.5 + 0.5 * cos(t * 0.6)) + caramel * pow(0.5 + 0.5 * sin(t * 1.1), 4.0) * 0.12;
}
float ripple(vec2 p, vec2 center, float freq, float speed) {
    float d = length(p - center);
    return sin(d * freq - time * speed) * exp(-d * 3.0);
}
float heightField(vec2 p) {
    float h = 0.0;
    h += ripple(p, vec2(sin(time * 0.03) * 0.4, cos(time * 0.02) * 0.3), 18.0, 0.5) * 0.9;
    h += ripple(p, vec2(cos(time * 0.025) * 0.5, sin(time * 0.04) * 0.4), 22.0, 0.4) * 0.7;
    h += ripple(p, vec2(sin(time * 0.018 + 2.0) * 0.35, cos(time * 0.03 + 1.0) * 0.45), 14.0, 0.3) * 0.8;
    h += ripple(p, vec2(cos(time * 0.015 + 4.0) * 0.6, sin(time * 0.02 + 3.0) * 0.3), 26.0, 0.45) * 0.5;
    h += ripple(p, mouse * 2.0 - 1.0, 30.0, 0.8) * 0.6;
    return h * 0.08;
}
void main() {
    vec2 p = (gl_FragCoord.xy / resolution.xy) * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;
    float h = heightField(p);
    float eps = 0.003;
    vec3 n = normalize(vec3(h - heightField(p + vec2(eps,0.0)), h - heightField(p + vec2(0.0,eps)), eps));
    vec3 lightDir = normalize(vec3(-0.4, 0.5, 1.0)), viewDir = vec3(0.0,0.0,1.0);
    float diff = max(dot(n,lightDir),0.0) * 0.65 + 0.35;
    float spec = pow(max(dot(n, normalize(lightDir + viewDir)),0.0), 140.0);
    float fres = pow(1.0 - max(dot(n,viewDir),0.0), 2.5);
    vec3 col = chocolatePalette(h * 6.0 + time * 0.015) * diff + spec * vec3(0.65, 0.50, 0.35) * 0.7 + fres * vec3(0.65, 0.42, 0.18) * 0.4;
    col += sin(h * 120.0 + time * 0.3) * 0.01;
    col *= exp(-abs(h) * 9.0) + 0.4;
    col = mix(vec3(0.004, 0.0015, 0.0008), col, 0.96) * (1.0 - dot(p,p) * 0.25);
    gl_FragColor = vec4(1.0 - exp(-col * 1.4), 1.0);
}"""
		),
		ShaderPresetInfo(
			id = "cosmic_nebula",
			nameEn = "Cosmic Warp Nebula",
			nameDe = "Kosmischer Warpnebel",
			code = """precision highp float;
uniform float time;
uniform vec2 resolution;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) { v += noise(p) * a; p *= 2.0; a *= 0.5; }
    return v;
}
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / resolution.xy;
    vec2 p = uv * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;
    float t = time * 0.05;
    vec2 q = p * 1.5;
    float n = fbm(q + vec2(t, -t)), n2 = fbm(q * 2.0 - vec2(t * 1.5, t));
    p += vec2(cos(n + 0.5 * n2 * 3.1415), sin(n + 0.5 * n2 * 3.1415)) * 0.3;
    float nebula = smoothstep(0.2, 0.85, fbm(p * 1.2 + t) * 0.7 + fbm(p * 2.5 - t * 1.3) * 0.3);
    vec3 color = mix(mix(vec3(0.1, 0.2, 0.6), vec3(0.8, 0.2, 0.9), nebula), vec3(1.0, 0.6, 0.2), fbm(p * 3.0));
    float tS = time * 0.02;
    vec2 id = floor(uv * 200.0), gv = fract(uv * 200.0) - 0.5;
    float h = hash(id);
    float s = step(0.995, h) * smoothstep(0.5, 0.0, length(gv));
    color += mix(vec3(1.0, 0.8, 0.6), vec3(0.8, 0.9, 1.0), sin(tS * 10.0 + hash(id + 1000.0) * 10.0) * 0.5 + 0.5) * s * (1.0 - nebula);
    fragColor = vec4(1.0 - exp(-color * (1.0 - dot(p, p) * 0.4) * 1.5), 1.0);
}
void main() { mainImage(gl_FragColor, gl_FragCoord.xy); }"""
		),
		ShaderPresetInfo(
			id = "clouds",
			nameEn = "Twilight Dusk Clouds",
			nameDe = "Dämmerungs-Wolken",
			code = PRESET_CLOUDS
		)
	)

    fun getShaderCode(preset: String, seed: Float, customCode: String = ""): String {
        val head = "const float iSeed = ${seed};\n"
        if (preset == "custom") {
            return head + (if (customCode.isNotBlank()) customCode else PRESET_CUSTOM_DEFAULT)
        }
        val found = PRESETS.firstOrNull { it.id == preset }
        return head + (found?.code ?: PRESET_STARS)
    }
}
