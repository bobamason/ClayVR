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
uniform vec4 u_colorTop;
uniform vec4 u_colorMiddle;
uniform vec4 u_colorBottom;
uniform float u_gradient;

void main(){
    if(v_pos.y > 0.0){
        float t = clamp(v_pos.y  / u_gradient, 0.0, 1.0);
        gl_FragColor = mix(u_colorMiddle, u_colorTop, t * t);
    } else {
        float t = clamp(-v_pos.y  / u_gradient, 0.0, 1.0);
        gl_FragColor = mix(u_colorMiddle, u_colorBottom, t * t);
    }
}