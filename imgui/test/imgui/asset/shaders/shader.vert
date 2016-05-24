#version 330

// Attributes
#define POSITION    0
#define UV          1
#define COLOR       2

layout (location = POSITION) in vec2 position;
layout (location = UV) in vec2 uv;
layout (location = COLOR) in vec4 color;

uniform mat4 proj;

out vec2 uv_;
out vec4 color_;

void main()
{
    uv_ = uv;
    color_ = color;
    gl_Position = vec4(position, 0, 1);
}
