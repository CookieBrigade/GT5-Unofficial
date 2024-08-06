#version 120

uniform mat4 u_TranslationMatrix;

attribute vec2 a_TexCoord;
varying vec2 v_TexCoord;

void main() {
    v_TexCoord = a_TexCoord;
    gl_Position = gl_ModelViewProjectionMatrix * u_TranslationMatrix * gl_Vertex;
}
