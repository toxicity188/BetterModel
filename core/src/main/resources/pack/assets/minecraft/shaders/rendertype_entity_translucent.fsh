#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec4 normal;
in float part;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1 || abs(mod(part + 0.5, 1.0) - 0.5) > 0.001) {
        discard;
    }
    if (color.a < 1.0 && part > 0.5) {
        vec4 color2 = texture(Sampler0, texCoord1);
        if (color.a < 0.75 && int(gl_FragCoord.x + gl_FragCoord.y) % 2 == 0) {
            discard;
        }
        else {
            color.rgb = mix(color2.rgb, color.rgb, min(1.0, color.a * 2));
            color.a = 1.0;
        }
    }

    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
