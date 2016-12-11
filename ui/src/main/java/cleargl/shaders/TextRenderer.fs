#version 400 core

in VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn;

layout( location = 0) out vec4 FragColor;

uniform sampler2D uitex;
uniform mat4 ModelViewMatrix;
uniform mat4 MVP;

void main() {
    FragColor = texture(uitex, VertexIn.TexCoord);
}