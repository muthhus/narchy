#version 400 core

//layout(location = 0) in vec4 position;
//in vec3 vPosition;
layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec3 vertexNormal;
layout(location = 2) in vec2 vertexTexCoord;

uniform vec3 offset = vec3(0.0, 0.0, 0.0);

out VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexOut;

uniform mat4 ModelViewMatrix;
uniform mat3 NormalMatrix;
uniform mat4 ProjectionMatrix;

uniform int renderStage = 0;
uniform float zScaling = 1.0;

void main()
{
//    gl_PointSize = vertexNormal.y;
    VertexOut.Normal = vertexNormal;
    VertexOut.Position = vec3( ModelViewMatrix * vec4(vertexPosition, 1.0));
    VertexOut.Position.z = zScaling * VertexOut.Position.z;
    VertexOut.TexCoord = vertexTexCoord;

    gl_Position = ProjectionMatrix * ModelViewMatrix * vec4(vertexPosition + offset, 1.0);
}