#version 150

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 TextureMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec2 texCoord1;
out float part;

#define SPACING 512.0
#define MAXRANGE (0.5 * SPACING)
#define SKINRES 64
#define FACERES 8

const vec4[] subuvs = vec4[](
vec4(4.0,  0.0,  8.0,  4.0 ), // 4x4x12
vec4(8.0,  0.0,  12.0, 4.0 ),
vec4(0.0,  4.0,  4.0,  16.0),
vec4(4.0,  4.0,  8.0,  16.0),
vec4(8.0,  4.0,  12.0, 16.0),
vec4(12.0, 4.0,  16.0, 16.0),
vec4(4.0,  0.0,  7.0,  4.0 ), // 4x3x12
vec4(7.0,  0.0,  10.0, 4.0 ),
vec4(0.0,  4.0,  4.0,  16.0),
vec4(4.0,  4.0,  7.0,  16.0),
vec4(7.0,  4.0,  11.0, 16.0),
vec4(11.0, 4.0,  14.0, 16.0),
vec4(4.0,  0.0,  12.0, 4.0 ), // 4x8x12
vec4(12.0,  0.0, 20.0, 4.0 ),
vec4(0.0,  4.0,  4.0,  16.0),
vec4(4.0,  4.0,  12.0, 16.0),
vec4(12.0, 4.0,  16.0, 16.0),
vec4(16.0, 4.0,  24.0, 16.0)
);

const vec2[] origins = vec2[](
vec2(40.0, 16.0), // right arm
vec2(40.0, 32.0),

vec2(40.0, 16.0), // right arm
vec2(40.0, 32.0),

vec2(32.0, 48.0), // left arm
vec2(48.0, 48.0),

vec2(32.0, 48.0), // left arm
vec2(48.0, 48.0),

vec2(16.0, 16.0), // hip
vec2(16.0, 32.0),

vec2(16.0, 16.0), // hip
vec2(16.0, 32.0),

vec2(16.0, 16.0), // hip
vec2(16.0, 32.0),

vec2(0.0,  16.0), // right leg
vec2(0.0,  32.0),

vec2(0.0,  16.0), // right leg
vec2(0.0,  32.0),

vec2(16.0, 48.0), // left leg
vec2(0.0,  48.0),

vec2(16.0, 48.0), // left leg
vec2(0.0,  48.0)
);

const int[] faceremap = int[](0, 0, 1, 1, 2, 3, 4, 5);

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexDistance = fog_distance(Position, FogShape);
#ifdef NO_CARDINAL_LIGHTING
    vertexColor = Color;
#else
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
#endif
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);

    texCoord0 = UV0;
#ifdef APPLY_TEXTURE_MATRIX
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
#endif


//Start player display - https://github.com/bradleyq/stable_player_display
    part = 0.0;
    texCoord1 = vec2(0.0);
#if defined(ALPHA_CUTOUT) && !defined(EMISSIVE) && !defined(NO_OVERLAY)
    vec3 wpos = Position;
    ivec2 dim = textureSize(Sampler0, 0);

    if (ProjMat[2][3] == 0.0 || dim.x != 64 || dim.y != 64) { // short circuit if cannot be player
        texCoord0 = UV0;
        vertexDistance = fog_distance(Position, FogShape);
    }
    else if (length(inverse(ModelViewMat)[3].xyz - wpos) < 0.75) {
        texCoord0 = UV0;
        texCoord1 = UV1;
        vertexDistance = 0;
        vertexColor = vec4(0);
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 0.0);
    }
    else {
        vec2 UVout = UV0;
        vec2 UVout2 = vec2(0.0);
        int partId = -int((wpos.y - MAXRANGE) / SPACING);

        part = float(partId);

        if (partId > 0) {
            vec4 samp1 = texture(Sampler0, vec2(54.0 / 64.0, 20.0 / 64.0));
            vec4 samp2 = texture(Sampler0, vec2(55.0 / 64.0, 20.0 / 64.0));
            bool slim = samp1.a == 0.0 || (((samp1.r + samp1.g + samp1.b) == 0.0) && ((samp2.r + samp2.g + samp2.b) == 0.0) && samp1.a == 1.0 && samp2.a == 1.0);
            int outerLayer = (gl_VertexID / 24) % 2;
            int vertexId = gl_VertexID % 4;
            int faceId = (gl_VertexID % 24) / 4;
            ivec2 faceIdTmp = ivec2(round(UV0 * SKINRES));
            if ((faceId != 1 && vertexId >= 2) || (faceId == 1 && vertexId <= 1)) {
                faceIdTmp.y -= FACERES;
            }
            if (vertexId == 0 || vertexId == 3) {
                faceIdTmp.x -= FACERES;
            }
            faceIdTmp /= FACERES;
            faceId = (faceIdTmp.x % 4) + 4 * faceIdTmp.y;
            faceId = faceremap[faceId];
            int subuvIndex = faceId;

            wpos.y += SPACING * partId;
            gl_Position = ProjMat * ModelViewMat * vec4(wpos, 1.0);

            UVout = origins[2 * (partId - 1) + outerLayer];
            UVout2 = origins[2 * (partId - 1)];

            bool isBody = (partId >= 5 && partId <= 7);
            bool isLeg = (partId >= 8);

            if (slim && (partId >= 1 && partId <= 4)) {
                subuvIndex += 6;
            }
            else if (isBody) {
                subuvIndex += 12;
            }

            vec4 subuv = vec4(subuvs[subuvIndex]);
            if (isBody) {
                float t = (subuv.w - subuv.y) / 3.0;
                int partIdKey = (partId - 5) % 3;
                subuv.y += t * (2 - partIdKey);
                subuv.w -= t * partIdKey;
            } else {
                if (((partId + (isLeg ? 1 : 0)) % 2) == 0) {
                    subuv.y = (subuv.w + subuv.y) / 2;
                } else {
                    subuv.w = (subuv.w + subuv.y) / 2;
                }
            }

            vec2 offset = vec2(0.0);
            if (faceId == 1) {
                if (vertexId == 0) {
                    offset += subuv.zw;
                }
                else if (vertexId == 1) {
                    offset += subuv.xw;
                }
                else if (vertexId == 2) {
                    offset += subuv.xy;
                }
                else {
                    offset += subuv.zy;
                }
            }
            else {
                if (vertexId == 0) {
                    offset += subuv.zy;
                }
                else if (vertexId == 1) {
                    offset += subuv.xy;
                }
                else if (vertexId == 2) {
                    offset += subuv.xw;
                }
                else {
                    offset += subuv.zw;
                }
            }

            UVout += offset;
            UVout2 += offset;
            UVout /= float(SKINRES);
            UVout2 /= float(SKINRES);
        }

        vertexDistance = fog_distance(wpos, FogShape);
        texCoord0 = UVout;
        texCoord1 = UVout2;
    }
#endif
//End player display
}
