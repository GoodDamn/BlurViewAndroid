#version 310 es
// openglES 3.0
precision mediump float;
out vec4 FragColor;
uniform vec2 uScreenSize;
uniform sampler2D uTexture;

void main() {
    FragColor = vec4(
        texture(
            uTexture,
            gl_FragCoord.xy / uScreenSize
        ),
        1.0
    );
}