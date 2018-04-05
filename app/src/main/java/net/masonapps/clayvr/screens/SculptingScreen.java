package net.masonapps.clayvr.screens;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.google.vr.sdk.controller.Controller;

import net.masonapps.clayvr.bvh.BVH;
import net.masonapps.clayvr.environment.ModelUtils;
import net.masonapps.clayvr.math.Animator;
import net.masonapps.clayvr.math.RotationUtil;
import net.masonapps.clayvr.math.Side;
import net.masonapps.clayvr.sculpt.Brush;
import net.masonapps.clayvr.sculpt.SculptHandler;
import net.masonapps.clayvr.sculpt.SculptMesh;
import net.masonapps.clayvr.sculpt.SculptingInterface;
import net.masonapps.clayvr.ui.ViewControls;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.gfx.Entity;
import org.masonapps.libgdxgooglevr.gfx.VrGame;
import org.masonapps.libgdxgooglevr.input.DaydreamButtonEvent;
import org.masonapps.libgdxgooglevr.math.PlaneUtils;
import org.masonapps.libgdxgooglevr.utils.Logger;

import static net.masonapps.clayvr.screens.SculptingScreen.State.STATE_NONE;
import static net.masonapps.clayvr.screens.SculptingScreen.State.STATE_SCULPTING;
import static net.masonapps.clayvr.screens.SculptingScreen.State.STATE_VIEW_TRANSFORM;
import static net.masonapps.clayvr.screens.SculptingScreen.TransformAction.ACTION_NONE;
import static net.masonapps.clayvr.screens.SculptingScreen.TransformAction.PAN;
import static net.masonapps.clayvr.screens.SculptingScreen.TransformAction.ROTATE;
import static net.masonapps.clayvr.screens.SculptingScreen.TransformAction.ZOOM;

/**
 * Created by Bob Mason on 7/7/2017.
 */

public class SculptingScreen extends RoomScreen {

    private static final float SQRT2 = (float) Math.sqrt(2);
    private static final String TAG = SculptingScreen.class.getSimpleName();
    private static final float MODEL_Z = -2.2f;
    private static final float UI_ALPHA = 0.25f;
    private final SculptingInterface sculptingInterface;
    private final Vector3 position = new Vector3(0, 0, MODEL_Z);
    //    private final SculptControlsVirtualStage buttonControls;
    private final Vector2 startPan = new Vector2();
    private final Vector2 pan = new Vector2();
    private final Ray tmpRay = new Ray();
    private final Entity sculptEntity;
    private final Animator rotationAnimator;
    private final Animator positionAnimator;
    //    private boolean isModelLoaded = false;
    private Entity sphere;
    private boolean isTouchPadClicked = false;
    private Quaternion rotation = new Quaternion();
    private Quaternion lastRotation = new Quaternion();
    private Quaternion startRotation = new Quaternion();
    private Plane hitPlane = new Plane();
    private Entity symmetryPlane;
    private String projectName;
    private TransformAction transformAction = ACTION_NONE;
    private float zoom = 1f;
    private float startZoom = 1f;
    private InputMode currentInputMode = InputMode.VIEW;
    private State currentState = STATE_NONE;
    private volatile boolean isMeshUpdating = false;
    private Quaternion snappedRotation = new Quaternion();
    private Vector3 snappedPosition = new Vector3();
    private final SculptHandler sculptHandler;
    private Vector3 hitPoint = new Vector3();
    private ShapeRenderer shapeRenderer;

    public SculptingScreen(VrGame game, BVH bvh, String projectName) {
        super(game);
        final SculptMesh sculptMesh = SculptMesh.newInstance(bvh.getMeshData());
        final Brush brush = new Brush();
        brush.setUseSymmetry(bvh.getMeshData().isSymmetryEnabled());
        this.projectName = projectName;
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
        final SpriteBatch spriteBatch = new SpriteBatch();
        manageDisposable(shapeRenderer, spriteBatch);
//        getWorld().add(Style.newGradientBackground(getVrCamera().far - 1f));
//        getWorld().add(Grid.newInstance(20f, 0.5f, 0.02f, Color.WHITE, Color.DARK_GRAY)).setPosition(0, -1.3f, 0);

        final SculptingInterface.SculptUiEventListener sculptUiEventListener = new SculptingInterface.SculptUiEventListener() {

            @Override
            public void onDropperButtonClicked() {
                sculptHandler.setShouldDoDropper(true);
            }

            @Override
            public void onUndoClicked() {
                sculptHandler.undo();
            }

            @Override
            public void onRedoClicked() {
                sculptHandler.redo();
            }

            @Override
            public void onExportClicked() {
                getSculptingVrGame().switchToExportScreen();
            }

            @Override
            public void onSymmetryChanged(boolean enabled) {
                brush.setUseSymmetry(enabled);
                sculptMesh.getMeshData().setSymmetryEnabled(enabled);
            }
        };
        sculptingInterface = new SculptingInterface(brush, spriteBatch, getSculptingVrGame().getSkin(), sculptUiEventListener);
        sculptingInterface.loadWindowPositions(PreferenceManager.getDefaultSharedPreferences(GdxVr.app.getContext()));

        sculptHandler = new SculptHandler(bvh, brush, sculptMesh, sculptingInterface::setDropperColor);

        sculptEntity = sculptHandler.getSculptEntity();
        getWorld().add(sculptEntity);

        final ModelBuilder builder = new ModelBuilder();
        sphere = getWorld().add(new Entity(new ModelInstance(ModelUtils.createSphereModel(builder, Color.GRAY))));
        sphere.setLightingEnabled(true);
        sphere.setVisible(false);

        symmetryPlane = getWorld().add(new Entity(new ModelInstance(ModelUtils.createRect(builder, Color.SKY))));
        symmetryPlane.setLightingEnabled(false);
        symmetryPlane.setVisible(brush.useSymmetry());


        updateSculptEntityPosition();

//        final Skin skin = getSculptingVrGame().getSkin();
//        buttonControls = new SculptControlsVirtualStage(spriteBatch, skin, 0.065f,
//                skin.newDrawable(Style.Drawables.ic_pan),
//                Style.getStringResource(R.string.pan, "pan"),
//                skin.newDrawable(Style.Drawables.ic_undo),
//                Style.getStringResource(R.string.undo, "undo"),
//                skin.newDrawable(Style.Drawables.ic_zoom),
//                Style.getStringResource(R.string.zoom, "zoom"),
//                skin.newDrawable(Style.Drawables.ic_rotate),
//                Style.getStringResource(R.string.rotate, "rotate"));
//        buttonControls.setListener(new SculptControlsVirtualStage.SculptControlsListener()
//
//        {
//            @Override
//            public void onButtonDown(int focusedButton) {
//                if (currentState == STATE_SCULPTING) return;
//                currentState = STATE_VIEW_TRANSFORM;
//                startRotation.set(GdxVr.input.getControllerOrientation());
//                lastRotation.set(GdxVr.input.getControllerOrientation());
//                final Vector3 tmp = Pools.obtain(Vector3.class);
//                final Vector3 tmp2 = Pools.obtain(Vector3.class);
//                final Ray ray = GdxVr.input.getInputRay();
//                hitPlane.set(tmp.set(ray.direction).scl(2f).add(ray.origin), tmp2.set(ray.direction).scl(-1));
//                switch (focusedButton) {
//                    case QuadButtonListener.TOP:
//                        startPan.set(pan);
//                        transformAction = PAN;
//                        break;
//                    case QuadButtonListener.BOTTOM:
//                        break;
//                    case QuadButtonListener.LEFT:
//                        startZoom = zoom;
//                        transformAction = ZOOM;
//                        break;
//                    case QuadButtonListener.RIGHT:
//                        transformAction = ROTATE;
//                        break;
//                }
//                Pools.free(tmp);
//                Pools.free(tmp2);
//            }
//
//            @Override
//            public void onButtonUp() {
//                if (currentState == STATE_VIEW_TRANSFORM) {
//                    transformAction = ACTION_NONE;
//                    currentState = STATE_NONE;
//                }
//            }
//        });

        sculptingInterface.setViewControlsListener(new ViewControls.ViewControlListener() {
            @Override
            public void onViewSelected(Side side) {
                final Quaternion tmpQ = Pools.obtain(Quaternion.class);
                RotationUtil.rotateToViewSide(tmpQ, side);
                rotation.set(tmpQ);
                lastRotation.set(tmpQ);
                updateSymmetryPlaneTransform();
                Pools.free(tmpQ);
            }
        });

        rotationAnimator = new Animator(new Animator.AnimationListener() {
            @Override
            public void apply(float value) {
                final Quaternion rot = sculptEntity.getRotation();
                rot.set(rotation).slerp(snappedRotation, value);
                lastRotation.set(rot);
                sculptEntity.invalidate();
                updateSymmetryPlaneTransform();
            }

            @Override
            public void finished() {
                rotation.set(snappedRotation);
                lastRotation.set(rotation);
            }
        });
        rotationAnimator.setInterpolation(Interpolation.linear);

        positionAnimator = new Animator(new Animator.AnimationListener() {
            @Override
            public void apply(float value) {
                final Vector3 pos = sculptEntity.getPosition();
                pos.set(SculptingScreen.this.position).slerp(snappedPosition, value);
                pan.set(pos.x, pos.y);
                updateSculptEntityPosition();
            }

            @Override
            public void finished() {
                position.set(snappedPosition);
            }
        });
        positionAnimator.setInterpolation(Interpolation.linear);

    }

    @Override
    protected void addLights(Array<BaseLight> lights) {
        final DirectionalLight light = new DirectionalLight();
        light.setColor(Color.WHITE);
        light.setDirection(new Vector3(1, -1, -1).nor());
        lights.add(light);
    }

    @Override
    protected void doneLoading(AssetManager assets) {
        super.doneLoading(assets);
        Log.d(TAG, "done loading");
//        if (!isModelLoaded) {
//            isModelLoaded = true;
//        }
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(GdxVr.app.getContext()).edit();
        sculptingInterface.saveWindowPositions(editor);
        editor.apply();
    }

    @Override
    public void show() {
        super.show();
        GdxVr.input.setInputProcessor(sculptingInterface);
//        buttonControls.attachListener();
    }

    @Override
    public void hide() {
        super.hide();
        GdxVr.input.setInputProcessor(null);
//        buttonControls.detachListener();
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(GdxVr.app.getContext()).edit();
        sculptingInterface.saveWindowPositions(editor);
        editor.apply();
    }

    @Override
    public void update() {
//        ElapsedTimer.getInstance().print("update");
        super.update();
        sculptingInterface.act();
        symmetryPlane.setVisible(sculptHandler.getBrush().useSymmetry());
//        buttonControls.act();
        if (isTouchPadClicked && currentState == STATE_SCULPTING) {
            sphere.setVisible(!sculptHandler.shouldDoDropper());
            sculptHandler.sculpt();
            getSculptingVrGame().getCursor().position.set(sculptHandler.getTransformedHitPoint());
        }

        rotationAnimator.update(GdxVr.graphics.getDeltaTime());
        positionAnimator.update(GdxVr.graphics.getDeltaTime());

//        Logger.d("fps: " + GdxVr.graphics.getFramesPerSecond());
    }

    @Override
    public void render(Camera camera, int whichEye) {
//        ElapsedTimer.getInstance().print("render");
        super.render(camera, whichEye);
//        shapeRenderer.begin();
//        shapeRenderer.setProjectionMatrix(camera.combined);
//        DebugUtils.debugBVH(shapeRenderer, sculptHandler.getBVH(), sculptEntity.getTransform(), Color.YELLOW);
//        shapeRenderer.end();
//        final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
//        sculptMesh.renderEdges(camera, sculptEntity.getTransform(tmpMat));
//        Pools.free(tmpMat);
//        drawBrushStroke(camera);

//        if (currentState == STATE_SCULPTING) {
//            final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
//            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
//            shapeRenderer.setProjectionMatrix(camera.combined);
//            shapeRenderer.setTransformMatrix(sculptEntity.getTransform(tmpMat));
//            shapeRenderer.setColor(Color.BLACK);
//            shapeRenderer.box(searchBB.getCenterX(), searchBB.getCenterY(), searchBB.getCenterZ(), searchBB.getWidth(), searchBB.getHeight(), searchBB.getDepth());
//            shapeRenderer.end();
//            Pools.free(tmpMat);
//        }

        sculptingInterface.draw(camera);

//        ElapsedTimer.getInstance().start("render");
    }

    private void rotate() {
        final Quaternion rotDiff = Pools.obtain(Quaternion.class);
        rotDiff.set(lastRotation).conjugate().mulLeft(GdxVr.input.getControllerOrientation());
        rotation.mulLeft(rotDiff);
        Pools.free(rotDiff);
        sculptEntity.setRotation(rotation);
        lastRotation.set(GdxVr.input.getControllerOrientation());
        updateSymmetryPlaneTransform();
    }

    private void pan() {
        if (Intersector.intersectRayPlane(GdxVr.input.getInputRay(), hitPlane, hitPoint)) {
            final Vector2 tmp = Pools.obtain(Vector2.class);
            PlaneUtils.toSubSpace(hitPlane, hitPoint, tmp);
            pan.set(startPan).add(tmp.limit(2f));
            updateSculptEntityPosition();
            Pools.free(tmp);
        }
    }

    private void zoom() {
        if (Intersector.intersectRayPlane(GdxVr.input.getInputRay(), hitPlane, hitPoint)) {
            final Vector2 tmp = Pools.obtain(Vector2.class);
            PlaneUtils.toSubSpace(hitPlane, hitPoint, tmp);
            zoom = startZoom * (tmp.limit(2f).y + 2f) / 2f;
            zoom = MathUtils.clamp(zoom, 0.2f, 10f);
            sculptEntity.setScale(zoom);
            updateSculptEntityPosition();
            Pools.free(tmp);
            updateSymmetryPlaneTransform();
        }
    }

    private void updateSculptEntityPosition() {
        position.set(pan.x, pan.y, MODEL_Z - sculptEntity.getRadius() / SQRT2);
        sculptEntity.setPosition(position);
        updateSymmetryPlaneTransform();
    }

    private void updateSymmetryPlaneTransform() {
        if (!sculptHandler.getBrush().useSymmetry()) return;
        final float s = sculptEntity.getRadius() * zoom;
        final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
        sculptEntity.recalculateTransform();
        sculptEntity.getTransform(tmpMat);
        symmetryPlane.setTransform(tmpMat.scale(s, s, s));
        Pools.free(tmpMat);
    }

    public SculptMesh getSculptMesh() {
        return sculptHandler.getSculptMesh();
    }

    @Override
    public void onControllerBackButtonClicked() {
        if (!sculptingInterface.onControllerBackButtonClicked()) {
            getSculptingVrGame().closeSculptScreen();
            getSculptingVrGame().switchToStartupScreen();
        }
    }

    @Override
    public void dispose() {
        sculptHandler.shutdown();
        super.dispose();
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    public void onDaydreamControllerUpdate(Controller controller, int connectionState) {
        updateCurrentInputMode();
//        buttonControls.setVisible(currentInputMode == InputMode.VIEW);
        sphere.setVisible(currentInputMode == InputMode.SCULPT && !sculptHandler.shouldDoDropper());
        if (currentState == STATE_VIEW_TRANSFORM) {
            getSculptingVrGame().setCursorVisible(false);
            sculptingInterface.setVisible(false);
            if (transformAction == ROTATE)
                rotate();
            else if (transformAction == PAN)
                pan();
            else if (transformAction == ZOOM)
                zoom();
        } else {
            getSculptingVrGame().setCursorVisible(true);
        }
        if (currentInputMode == InputMode.SCULPT) {
            sphere.setPosition(sculptHandler.getTransformedHitPoint()).setScale(sculptEntity.getScaleX(), sculptEntity.getScaleY(), sculptEntity.getScaleZ()).scale(sculptHandler.getBrush().getRadius());
        }
        if (controller.clickButtonState) {
            if (!isTouchPadClicked) {
                onTouchPadButtonDown();
                isTouchPadClicked = true;
            }
        } else {
            if (isTouchPadClicked) {
                onTouchPadButtonUp();
                isTouchPadClicked = false;
            }
        }
    }

    @Override
    public void onControllerButtonEvent(Controller controller, DaydreamButtonEvent event) {
    }

    private void onTouchPadButtonDown() {
        switch (currentInputMode) {
            case UI:
                currentState = STATE_NONE;
                break;
            case SCULPT:
                if (isMeshUpdating) break;
                currentState = STATE_SCULPTING;
                ((ColorAttribute) sphere.modelInstance.materials.get(0).get(ColorAttribute.Diffuse)).color.set(Color.YELLOW);
                sculptingInterface.setAlpha(UI_ALPHA);
                sculptHandler.onTouchPadButtonDown();
                break;
            case VIEW:
                currentState = STATE_VIEW_TRANSFORM;
                startRotation.set(GdxVr.input.getControllerOrientation());
                lastRotation.set(GdxVr.input.getControllerOrientation());
                final Vector3 tmp = Pools.obtain(Vector3.class);
                final Vector3 tmp2 = Pools.obtain(Vector3.class);
                final Ray ray = GdxVr.input.getInputRay();
                hitPlane.set(tmp.set(ray.direction).scl(2f).add(ray.origin), tmp2.set(ray.direction).scl(-1));
                transformAction = ROTATE;
                Pools.free(tmp);
                Pools.free(tmp2);
                break;
        }
    }

    private void onTouchPadButtonUp() {
        switch (currentState) {
            case STATE_NONE:
                break;
            case STATE_SCULPTING:
                ((ColorAttribute) sphere.modelInstance.materials.get(0).get(ColorAttribute.Diffuse)).color.set(Color.GRAY);
                sculptHandler.onTouchPadButtonUp();
                break;
            case STATE_VIEW_TRANSFORM:
                if (RotationUtil.snap(rotation, snappedRotation, 0.1f)) {
                    final Quaternion rotDiff = Pools.obtain(Quaternion.class);
                    rotDiff.set(rotation).conjugate().mulLeft(snappedRotation);
                    final float angleRad = rotDiff.getAngleRad();
                    final float duration = Math.abs(angleRad < MathUtils.PI ? angleRad : MathUtils.PI2 - angleRad) / MathUtils.PI;
                    Pools.free(rotDiff);
                    rotationAnimator.setDuration(duration);
                    rotationAnimator.start();

                    snappedPosition.set(position);
//                    positionAnimator.setDuration(duration);
//                    positionAnimator.start();
                }
                transformAction = ACTION_NONE;
                currentState = STATE_NONE;
                break;
        }
        currentState = State.STATE_NONE;
        sculptingInterface.setAlpha(1f);
        sculptingInterface.setVisible(true);
    }

    private void updateCurrentInputMode() {
        switch (currentState) {
            case STATE_NONE:
                if (sculptingInterface.isCursorOver())
                    currentInputMode = InputMode.UI;
                else if (!isMeshUpdating) {
                    if (sculptEntity.intersectsRayBounds(GdxVr.input.getInputRay(), hitPoint)) {
                        currentInputMode = InputMode.SCULPT;
                        sculptHandler.onControllerUpdate();
                        Logger.d("bounds hitPoint = " + hitPoint);
                    } else
                        currentInputMode = InputMode.VIEW;
                } else
                    currentInputMode = InputMode.VIEW;
                break;
            case STATE_SCULPTING:
                currentInputMode = InputMode.SCULPT;
                break;
            case STATE_VIEW_TRANSFORM:
                currentInputMode = InputMode.VIEW;
                break;
        }
    }

    public BVH getBVH() {
        return sculptHandler.getBVH();
    }

    enum TransformAction {
        ACTION_NONE, ROTATE, PAN, ZOOM
    }

    enum InputMode {
        SCULPT, UI, VIEW
    }

    enum State {
        STATE_SCULPTING, STATE_VIEW_TRANSFORM, STATE_NONE
    }
}
