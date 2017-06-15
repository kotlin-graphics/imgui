#version 330

#define POSITION    0
#define COLOR       3
#define TEX_COORD   4

uniform mat4 mat;

layout (location = POSITION) in vec2 Position;
layout (location = TEX_COORD) in vec2 UV;
layout (location = COLOR) in vec4 Color;

out vec2 uv;
out vec4 color;

void main()
{
    uv = UV;
    color = Color;
    gl_Position = mat * vec4(Position.xy, 0, 1);
}