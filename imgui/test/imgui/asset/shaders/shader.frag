#version 330

// Attributes
#define FRAG_COLOR  0

layout (location = FRAG_COLOR) out vec4 outputColor;

uniform sampler2D texture_;

in vec2 uv_;
in vec4 color_;

void main()
{
    outputColor = color_ * texture(texture_, uv);
}
