#version 330

#define COLOR   0

uniform sampler2D Texture;

in vec2 uv;
in vec4 color;

layout (location = COLOR) out vec4 outColor;

void main()
{
    outColor = color * texture(Texture, uv);
}