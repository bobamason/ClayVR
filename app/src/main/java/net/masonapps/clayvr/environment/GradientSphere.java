package net.masonapps.clayvr.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

import org.masonapps.libgdxgooglevr.gfx.Entity;

/**
 * Created by Bob on 8/3/2017.
 */

public class GradientSphere extends Entity {

    private final GradientShader gradientShader;

    public GradientSphere(ModelInstance modelInstance) {
        super(modelInstance);
        setLightingEnabled(false);
        gradientShader = new GradientShader();
        setShader(gradientShader);
    }

    public static GradientSphere newInstance(float radius, int divisionsU, int divisionsV, Color cTop, Color cMiddle, Color cBottom) {
        final float d = radius * 2f;
        final Model sphere = new ModelBuilder().createSphere(d, d, d, divisionsU, divisionsV, new Material(), VertexAttributes.Usage.Position);
        final GradientSphere gradientSphere = new GradientSphere(new ModelInstance(sphere));
        gradientSphere.getGradientShader().setColorTop(cTop);
        gradientSphere.getGradientShader().setColorMiddle(cMiddle);
        gradientSphere.getGradientShader().setColorBottom(cBottom);
        gradientSphere.getGradientShader().setGradientHeight(radius);
        return gradientSphere;
    }

    public GradientShader getGradientShader() {
        return gradientShader;
    }

    private static class GradientShader extends BaseShader {

        private static String vertexShader = null;
        private static String fragmentShader = null;
        private final int u_projTrans = register(new Uniform("u_projTrans"));
        private final int u_worldTrans = register(new Uniform("u_worldTrans"));
        private final int u_colorTop = register(new Uniform("u_colorTop"));
        private final int u_colorMiddle = register(new Uniform("u_colorMiddle"));
        private final int u_colorBottom = register(new Uniform("u_colorBottom"));
        private final int u_gradient = register(new Uniform("u_gradient"));
        private final Color colorTop = new Color();
        private final Color colorMiddle = new Color();
        private final Color colorBottom = new Color();
        private final ShaderProgram program;
        private float gradientHeight = 10f;

        public GradientShader() {
            program = new ShaderProgram(getVertexShader(), getFragmentShader());

            if (!program.isCompiled())
                throw new GdxRuntimeException("Couldn't compile shader " + program.getLog());
            String log = program.getLog();
            if (log.length() > 0) Gdx.app.error("ShaderTest", "Shader compilation log: " + log);
            init();
        }

        public static String getVertexShader() {
            if (vertexShader == null)
                vertexShader = Gdx.files.internal("shaders/gradient.vertex.glsl").readString();
            return vertexShader;
        }

        public static String getFragmentShader() {
            if (fragmentShader == null)
                fragmentShader = Gdx.files.internal("shaders/gradient.fragment.glsl").readString();
            return fragmentShader;
        }

        @Override
        public void init() {
            super.init(program, null);
        }

        @Override
        public int compareTo(Shader other) {
            return 0;
        }

        @Override
        public boolean canRender(Renderable instance) {
            return true;
        }

        @Override
        public void begin(Camera camera, RenderContext context) {
            program.begin();
            context.setDepthTest(GL20.GL_LEQUAL, 0f, 1f);
            context.setDepthMask(true);
            set(u_projTrans, camera.combined);
        }

        @Override
        public void render(Renderable renderable) {
            set(u_worldTrans, renderable.worldTransform);
            set(u_colorTop, colorTop);
            set(u_colorMiddle, colorMiddle);
            set(u_colorBottom, colorBottom);
            set(u_gradient, gradientHeight);
            renderable.meshPart.render(program);
        }

        @Override
        public void end() {
            program.end();
        }

        @Override
        public void dispose() {
            super.dispose();
            program.dispose();
        }

        public void setGradientHeight(float gradientHeight) {
            this.gradientHeight = gradientHeight;
        }

        public void setColorTop(Color colorTop) {
            this.colorTop.set(colorTop);
        }

        public void setColorMiddle(Color colorMiddle) {
            this.colorMiddle.set(colorMiddle);
        }

        public void setColorBottom(Color colorBottom) {
            this.colorBottom.set(colorBottom);
        }
    }
}
