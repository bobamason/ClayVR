package net.masonapps.clayvr.screens;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.google.vr.sdk.controller.Controller;

import net.masonapps.clayvr.Style;
import net.masonapps.clayvr.bvh.BVH;
import net.masonapps.clayvr.math.Animator;
import net.masonapps.clayvr.math.RotationUtil;
import net.masonapps.clayvr.math.Segment;
import net.masonapps.clayvr.math.Side;
import net.masonapps.clayvr.mesh.Vertex;
import net.masonapps.clayvr.sculpt.Brush;
import net.masonapps.clayvr.sculpt.SculptMesh;
import net.masonapps.clayvr.sculpt.SculptingInterface;
import net.masonapps.clayvr.sculpt.UndoRedoCache;
import net.masonapps.clayvr.ui.ViewControls;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.gfx.Entity;
import org.masonapps.libgdxgooglevr.gfx.VrGame;
import org.masonapps.libgdxgooglevr.input.DaydreamButtonEvent;
import org.masonapps.libgdxgooglevr.math.PlaneUtils;
import org.masonapps.libgdxgooglevr.utils.ElapsedTimer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    private final BVH bvh;
    private final SculptMesh sculptMesh;
    //    private final SculptControlsVirtualStage buttonControls;
    private final UndoRedoCache undoRedoCache;
    private final Vector2 startPan = new Vector2();
    private final Vector2 pan = new Vector2();
    //    private final ExecutorService executor;
    private final Entity sculptEntity;
    private final Ray tmpRay = new Ray();
    private final Animator rotationAnimator;
    private final Animator positionAnimator;
    //    private boolean isModelLoaded = false;
    private BVH.IntersectionInfo intersection = new BVH.IntersectionInfo();
    private Vector3 transformedHitPoint = new Vector3();
    private Entity sphere;
    private boolean isTouchPadClicked = false;
    private Quaternion rotation = new Quaternion();
    private Quaternion lastRotation = new Quaternion();
    private Quaternion startRotation = new Quaternion();
    private Vector3 startHitPoint = new Vector3();
    private Plane hitPlane = new Plane();
    private Vector3 lastHitPoint = new Vector3();
    private Vector3 rawHitPoint = new Vector3();
    private Vector3 hitPoint = new Vector3();
    private List<Vertex> vertices = Collections.synchronizedList(new ArrayList<Vertex>());
    private Brush brush = new Brush();
    private Entity symmetryPlane;
    private String projectName;
    private TransformAction transformAction = ACTION_NONE;
    private float zoom = 1f;
    private float startZoom = 1f;
    private InputMode currentInputMode = InputMode.VIEW;
    private State currentState = STATE_NONE;
    private volatile boolean isMeshUpdating = false;
    //    private Stroke stroke = new Stroke();
    private float rayLength;
    private Segment segment = new Segment();
    private BoundingBox searchBB = new BoundingBox();
    private boolean shouldDoDropper = false;
    private Quaternion snappedRotation = new Quaternion();
    private Vector3 snappedPosition = new Vector3();

    public SculptingScreen(VrGame game, BVH bvh, String projectName) {
        super(game);
        this.bvh = bvh;
        sculptMesh = SculptMesh.newInstance(bvh.getMeshData());
//        sculptMesh.clipY = -1.6f;
        brush.setUseSymmetry(bvh.getMeshData().isSymmetryEnabled());
        this.projectName = projectName;
//        executor = Executors.newSingleThreadExecutor();
        final ShapeRenderer shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
        final SpriteBatch spriteBatch = new SpriteBatch();
        manageDisposable(shapeRenderer, spriteBatch);
//        getWorld().add(Style.newGradientBackground(getVrCamera().far - 1f));
//        getWorld().add(Grid.newInstance(20f, 0.5f, 0.02f, Color.WHITE, Color.DARK_GRAY)).setPosition(0, -1.3f, 0);

        final SculptingInterface.SculptUiEventListener sculptUiEventListener = new SculptingInterface.SculptUiEventListener() {

            @Override
            public void onDropperButtonClicked() {
                shouldDoDropper = true;
            }

            @Override
            public void onUndoClicked() {
//                isMeshUpdating = true;
//                CompletableFuture.runAsync(() ->
                UndoRedoCache.applySaveData(sculptMesh, undoRedoCache.undo(), bvh);
//                        .thenRun(() -> isMeshUpdating = false);
            }

            @Override
            public void onRedoClicked() {
//                isMeshUpdating = true;
//                CompletableFuture.runAsync(() ->
                UndoRedoCache.applySaveData(sculptMesh, undoRedoCache.redo(), bvh);
//                        .thenRun(() -> isMeshUpdating = false);
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

        final ModelBuilder builder = new ModelBuilder();
        sculptEntity = SculptMesh.createSculptEntity(builder, sculptMesh, bvh.root.bb, Style.createSculptMaterial());
        // TODO: 12/7/2017 remove 
//        sculptEntity.setVisible(false);
        getWorld().add(sculptEntity);

        //        final ModelInstance bvhBounds = new ModelInstance(BVHUtils.createSphereModel(bvh, Color.SKY));
//        instances.add(bvhBounds);

        sphere = getWorld().add(new Entity(new ModelInstance(createSphereModel(builder, Color.GRAY))));
        sphere.setLightingEnabled(true);
        sphere.setVisible(false);

        symmetryPlane = getWorld().add(new Entity(new ModelInstance(createRect(builder, Color.SKY))));
        symmetryPlane.setLightingEnabled(false);
        symmetryPlane.setVisible(brush.useSymmetry());

        updateSculptEntityTransform();

        undoRedoCache = new UndoRedoCache();

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
                updateSculptEntityTransform();
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
                sculptEntity.getPosition().set(position).slerp(snappedPosition, value);
                sculptEntity.invalidate();
            }

            @Override
            public void finished() {
                position.set(snappedPosition);
            }
        });
        positionAnimator.setInterpolation(Interpolation.linear);
        
        undoRedoCache.save(sculptMesh.getVertexArray());
    }

    private static Model createSphereModel(ModelBuilder builder, Color color) {
        builder.begin();
        final MeshPartBuilder part = builder.part("s", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(color), ColorAttribute.createSpecular(Color.WHITE), new BlendingAttribute(true, 0.25f)));
        SphereShapeBuilder.build(part, 2f, 2f, 2f, 24, 12);
        return builder.end();
    }

    private static Model createRect(ModelBuilder modelBuilder, Color color) {
        final Material material = new Material(ColorAttribute.createDiffuse(color), new BlendingAttribute(true, 0.25f), IntAttribute.createCullFace(0));
        final float r = 1f;
        return modelBuilder.createRect(
                0, -r, r,
                0, r, r,
                0, r, -r,
                0, -r, -r,
                0, 0, r,
                material, VertexAttributes.Usage.Position);
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
        symmetryPlane.setVisible(brush.useSymmetry());
//        buttonControls.act();
        if (isTouchPadClicked && currentState == STATE_SCULPTING) {
            ElapsedTimer.getInstance().start("sculpt");
            sculpt();
            ElapsedTimer.getInstance().print("sculpt");
        }
        
        rotationAnimator.update(GdxVr.graphics.getDeltaTime());
        positionAnimator.update(GdxVr.graphics.getDeltaTime());
        
//        if (currentState == STATE_SCULPTING) {
//            sculptMesh.clipRadius = brush.getRadius();
//            sculptMesh.clipCenter.set(transformedHitPoint);
//        } else {
//            sculptMesh.clipRadius = 0f;
//        }
//        ElapsedTimer.getInstance().start("update");
    }

    @Override
    public void render(Camera camera, int whichEye) {
//        ElapsedTimer.getInstance().print("render");
        super.render(camera, whichEye);
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

    private void saveVertexPositions() {
        vertices.forEach(vertex -> {
            if (!vertex.isSavedPositionUpdated()) {
                vertex.savePosition();
                if (brush.useSymmetry() && vertex.symmetricPair != null)
                    vertex.symmetricPair.savePosition();
            }
        });
    }

    private void updateBrush(Ray ray) {
        brush.update(ray, startHitPoint, hitPoint, segment);
        if (brush.getType() != Brush.Type.VERTEX_PAINT && !isBrushGrab())
            brush.updateSculptPlane(vertices);
    }

    private void rotate() {
        final Quaternion rotDiff = Pools.obtain(Quaternion.class);
        rotDiff.set(lastRotation).conjugate().mulLeft(GdxVr.input.getControllerOrientation());
        rotation.mulLeft(rotDiff);
        Pools.free(rotDiff);
        updateSculptEntityTransform();
        lastRotation.set(GdxVr.input.getControllerOrientation());
    }

    private void pan() {
        if (Intersector.intersectRayPlane(GdxVr.input.getInputRay(), hitPlane, hitPoint)) {
            final Vector2 tmp = Pools.obtain(Vector2.class);
            PlaneUtils.toSubSpace(hitPlane, hitPoint, tmp);
            pan.set(startPan).add(tmp.limit(2f));
            updateSculptEntityTransform();
            Pools.free(tmp);
        }
    }

    private void zoom() {
        if (Intersector.intersectRayPlane(GdxVr.input.getInputRay(), hitPlane, hitPoint)) {
            final Vector2 tmp = Pools.obtain(Vector2.class);
            PlaneUtils.toSubSpace(hitPlane, hitPoint, tmp);
            zoom = startZoom * (tmp.limit(2f).y + 2f) / 2f;
            zoom = MathUtils.clamp(zoom, 0.2f, 10f);
            Pools.free(tmp);
            updateSculptEntityTransform();
        }
    }

    private void updateSculptEntityTransform() {
        final Vector3 tmp = Pools.obtain(Vector3.class);
        bvh.root.bb.getDimensions(tmp);
        float r = tmp.len() / 2f * zoom;
        position.set(pan.x, pan.y, MODEL_Z - r / SQRT2);
        sculptEntity.setPosition(position).setRotation(rotation).setScale(zoom);
        final float s = r * zoom;
        final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
        sculptEntity.recalculateTransform();
        sculptEntity.getTransform(tmpMat);
        symmetryPlane.setTransform(tmpMat.scale(s, s, s));
        Pools.free(tmp);
        Pools.free(tmpMat);
    }

    private void updateVertices(final List<Vertex> vertices) {
        vertices.forEach(vertex -> brush.applyBrushToVertex(vertex));

        bvh.refit();

        Arrays.stream(sculptMesh.getVertexArray())
                .filter(Vertex::needsUpdate)
                .forEach(vertex -> {
                    if (brush.getType() != Brush.Type.VERTEX_PAINT)
                        vertex.recalculateNormal();
                    sculptMesh.setVertex(vertex);
                    vertex.clearUpdateFlag();
                });
        sculptMesh.update();
    }

//    private void drawBrushStroke(Camera camera) {
//        final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
//        shapeRenderer.begin();
//        shapeRenderer.setColor(Color.BLUE);
//        shapeRenderer.setProjectionMatrix(camera.combined);
//        shapeRenderer.setTransformMatrix(sculptEntity.getTransform(tmpMat));
//        if (stroke.getPointCount() >= 2) {
//            for (int i = 1; i < stroke.getPointCount(); i++) {
//                shapeRenderer.line(stroke.getPoint(i - 1), stroke.getPoint(i));
//            }
//        }
//        shapeRenderer.end();
//        Pools.free(tmpMat);
//    }

//    @Override
//    public void renderAfterCursor(Camera camera) {
//        buttonControls.draw(camera);
//    }

    @Override
    public void onControllerBackButtonClicked() {
        if (!sculptingInterface.onControllerBackButtonClicked()) {
            getSculptingVrGame().closeSculptScreen();
            getSculptingVrGame().switchToStartupScreen();
        }
    }

    public SculptMesh getSculptMesh() {
        return sculptMesh;
    }

    @Override
    public void dispose() {
        super.dispose();
        undoRedoCache.clear();
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    public void onDaydreamControllerUpdate(Controller controller, int connectionState) {
        updateCurrentInputMode();
//        buttonControls.setVisible(currentInputMode == InputMode.VIEW);
        sphere.setVisible(currentInputMode == InputMode.SCULPT && !shouldDoDropper);
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
            sphere.setPosition(transformedHitPoint).setScale(sculptEntity.getScaleX(), sculptEntity.getScaleY(), sculptEntity.getScaleZ()).scale(brush.getRadius());
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
//                stroke.addPoint(hitPoint);
                startHitPoint.set(hitPoint);
                lastHitPoint.set(hitPoint);
                if (isBrushGrab()) {
                    bvh.sphereSearch(vertices, hitPoint, brush.getRadius());
                    saveVertexPositions();
                }
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
                shouldDoDropper = false;
//                updateVerticesByStroke(vertices, stroke);
                Arrays.stream(sculptMesh.getVertexArray()).forEach(vertex -> {
                    vertex.clearFlagSkipSphereTest();
                    vertex.clearSavedFlag();
                });
//                isMeshUpdating = true;
//                CompletableFuture.runAsync(() -> {
                undoRedoCache.save(sculptMesh.getVertexArray());
//                    isMeshUpdating = false;
//                });
                vertices.clear();
//                stroke.clear();
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

//                    snappedPosition.set(center).scl(-1).mul(snappedRotation).add(projectPosition);
                    positionAnimator.setDuration(duration);
                    positionAnimator.start();
                }
                transformAction = ACTION_NONE;
                currentState = STATE_NONE;
                break;
        }
        currentState = State.STATE_NONE;
        sculptingInterface.setAlpha(1f);
        sculptingInterface.setVisible(true);
    }

//    private void updateVerticesByStroke(List<Vertex> vertices, Stroke stroke) {
//        if (stroke.getPointCount() == 0) return;
//        stroke.simplifyBySegmentLength(brush.getRadius() / 2f);
//        brush.update(stroke);
//        brush.updateSculptPlane(vertices);
//        final Vector3 n = new Vector3(getForwardVector()).scl(-1);
////        Vector3 p = new Vector3();
////        for (int i = 0; i < stroke.getPointCount(); i++) {
////            p.add(stroke.getPoint(i));
////        }
////        p.scl(1f / stroke.getPointCount());
//        vertices.forEach(vertex -> brush.applyBrushToVertexUsingStroke(vertex));
//
//        bvh.refit();
//
//        vertices.stream()
//                .filter(Vertex::needsUpdate)
//                .forEach(vertex -> {
//                    if (brush.getType() != Brush.Type.VERTEX_PAINT)
//                        vertex.recalculateNormal();
//                    sculptMesh.setVertex(vertex);
//                    vertex.clearUpdateFlag();
//                });
//        sculptMesh.update();
//    }

    private boolean testBVHIntersection(Ray ray, boolean limitMovement) {
        boolean hasIntersection;
        final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
        hasIntersection = bvh.closestIntersection(ray, intersection);
        if (hasIntersection) {
            if (limitMovement) {
                rawHitPoint.set(intersection.hitPoint);
                hitPoint.set(rawHitPoint).sub(lastHitPoint).limit(brush.getRadius() * 0.75f).add(lastHitPoint);
            } else
                hitPoint.set(intersection.hitPoint);
            transformedHitPoint.set(hitPoint).mul(sculptEntity.getTransform(tmpMat));
            getSculptingVrGame().setCursorVisible(true);
            getSculptingVrGame().getCursor().position.set(transformedHitPoint);
            getSculptingVrGame().getCursor().setVisible(true);
        }
        Pools.free(tmpMat);
        rayLength = intersection.t;
        return hasIntersection;
    }

    public void sculpt() {
        sphere.setVisible(!shouldDoDropper);
        if (shouldDoDropper
                && testBVHIntersection(getTransformedRay(new Ray()), false)
                && intersection.triangle != null) {
            final Vertex v1 = intersection.triangle.v1;
            final Vertex v2 = intersection.triangle.v2;
            final Vertex v3 = intersection.triangle.v3;
            Vertex closest;
            if (v1.position.dst2(hitPoint) < v2.position.dst2(hitPoint)) {
                if (v1.position.dst2(hitPoint) < v3.position.dst2(hitPoint))
                    closest = v1;
                else
                    closest = v3;
            } else {
                if (v2.position.dst2(hitPoint) < v3.position.dst2(hitPoint))
                    closest = v2;
                else
                    closest = v3;
            }

            sculptingInterface.setDropperColor(closest.color);
            return;
        }
        final Ray ray = Pools.obtain(Ray.class);
        getTransformedRay(ray);
        if (isBrushGrab()) {
            updateHitPointUsingRayLength(ray);
            updateBrush(ray);
            updateVertices(vertices);
            lastHitPoint.set(hitPoint);
        } else {
            if (!testBVHIntersection(ray, true)) {
                updateHitPointUsingRayLength(ray);
            }
            segment.set(lastHitPoint, hitPoint);
//            searchBB.inf();
//            searchBB.ext(lastHitPoint);
//            searchBB.ext(hitPoint);
//            final float r = brush.getRadius();
//            searchBB.min.sub(r, r, r);
//            searchBB.max.add(r, r, r);
//            searchBB.set(searchBB.min, searchBB.max);
            bvh.sphereSearch(vertices, hitPoint, brush.getRadius() + 0.125f);
            saveVertexPositions();
            updateBrush(ray);
            updateVertices(vertices);
            lastHitPoint.set(hitPoint);
        }
        Pools.free(ray);
    }

    protected void updateHitPointUsingRayLength(Ray ray) {
        hitPoint.set(ray.direction).scl(rayLength).add(ray.origin);
        final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
        transformedHitPoint.set(hitPoint).mul(sculptEntity.getTransform(tmpMat));
        getSculptingVrGame().setCursorVisible(true);
        getSculptingVrGame().getCursor().position.set(transformedHitPoint);
        getSculptingVrGame().getCursor().setVisible(true);
        Pools.free(tmpMat);
    }

    private boolean isBrushGrab() {
        return brush.getType() == Brush.Type.GRAB;
    }

    private void updateCurrentInputMode() {
        switch (currentState) {
            case STATE_NONE:
                if (sculptingInterface.isCursorOver())
                    currentInputMode = InputMode.UI;
                else if (!isMeshUpdating) {
                    if (testBVHIntersection(getTransformedRay(), false))
                        currentInputMode = InputMode.SCULPT;
                    else
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

    private Ray getTransformedRay() {
        return getTransformedRay(tmpRay);
    }

    private Ray getTransformedRay(Ray ray) {
        final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
        ray.set(GdxVr.input.getInputRay()).mul(sculptEntity.getInverseTransform(tmpMat));
        Pools.free(tmpMat);
        return ray;
    }

    public BVH getBVH() {
        return bvh;
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
