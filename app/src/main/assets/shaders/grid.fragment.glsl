#ifdef GL_ES 
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision highp float;
#else
#define MED
#define LOWP
#define HIGH
#endif

varying vec3 v_pos;
uniform vec4 u_color1;
uniform vec4 u_color2;
uniform float u_spacing;
uniform float u_thickness;

void main(){
    float halfThickness = u_thickness * 0.5;
    vec2 p = fract(abs(v_pos.xz) / u_spacing);
    vec4 c;
    if(p.x < halfThickness || p.y < halfThickness)
        c = u_color1;
    else
        c = u_color2;
        
    gl_FragColor = c;
    
    if(gl_FragColor.a < 0.1) discard;
}