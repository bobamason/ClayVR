package net.masonapps.clayvr.ui;

import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMesh;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Pools;
import com.google.vr.sdk.controller.Controller;

import net.masonapps.clayvr.R;
import net.masonapps.clayvr.Style;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.gfx.Entity;
import org.masonapps.libgdxgooglevr.gfx.World;
import org.masonapps.libgdxgooglevr.input.DaydreamButtonEvent;
import org.masonapps.libgdxgooglevr.input.DaydreamTouchEvent;
import org.masonapps.libgdxgooglevr.math.CylindricalCoordinate;
import org.masonapps.libgdxgooglevr.ui.LabelVR;
import org.masonapps.libgdxgooglevr.ui.LoadingSpinnerVR;
import org.masonapps.libgdxgooglevr.ui.TableVR;
import org.masonapps.libgdxgooglevr.ui.VirtualStage;
import org.masonapps.libgdxgooglevr.ui.VrUiContainer;
import org.masonapps.libgdxgooglevr.utils.Logger;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Created by Bob on 8/15/2017.
 */
public class ModelSelectionUI<T> extends VrUiContainer {
    public static final float TOUCHPAD_SCALE = 150f;
    private static final float MODEL_RADIUS = 0.8f;
    private static final Vector3 MODEL_POSITION = new CylindricalCoordinate(2.5f, 90f, 0.25f, CylindricalCoordinate.AngleMode.degrees).toCartesian();
    private static final Vector3 PREVIOUS_POSITION = new CylindricalCoordinate(2.5f, 150f, 0f, CylindricalCoordinate.AngleMode.degrees).toCartesian();
    private static final Vector3 NEXT_POSITION = new CylindricalCoordinate(2.5f, 30f, 0f, CylindricalCoordinate.AngleMode.degrees).toCartesian();
    private final Entity sphere;
    private final GestureDetector gestureDetector;
    private final ModelAdapter<T> adapter;
    private final LoadingSpinnerVR previousSpinner;
    private final LoadingSpinnerVR currentSpinner;
    private final LoadingSpinnerVR nextSpinner;
    private NumberFormat nf = NumberFormat.getIntegerInstance();
    private boolean modelTest = false;
    private List<T> list;
    private int itemCount;
    private int currentIndex = 0;
    private int focusedIndex = -1;
    private LabelVR emptyLabel;
    private float scrollX = 0f;
    private boolean scrolling = false;
    private World world;
    @Nullable
    private ModelItem<T> previousItem = null;
    @Nullable
    private ModelItem<T> nextItem = null;
    private ModelItem<T> currentItem = null;
    private float animValue = 0f;
    private float animDuration = 0.5f;
    private boolean animating = false;
    @Nullable
    private Runnable animCompleteListener = null;
    private float targetValue = 0f;
    private final GestureDetector.GestureAdapter gestureAdapter = new GestureDetector.GestureAdapter() {

        public float startX = 0f;

        @Override
        public boolean touchDown(float x, float y, int pointer, int button) {
            startX = x;
            return super.touchDown(x, y, pointer, button);
        }

        @Override
        public boolean tap(float x, float y, int count, int button) {
            if (animating) return false;
            return false;
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY) {
            if (animating) return false;
            scrolling = true;
            scrollX = MathUtils.clamp((x - startX) / TOUCHPAD_SCALE, -1f, 1f);
            return true;
        }

        @Override
        public boolean panStop(float x, float y, int pointer, int button) {
            if (animating) return false;
            scrolling = false;
            scrollX = 0f;
            return true;
        }

        @Override
        public boolean fling(float velocityX, float velocityY, int button) {
            if (animating) return false;
            final float vX = velocityX / TOUCHPAD_SCALE;
            if (vX < -0.85f && currentIndex < itemCount - 1) {
                animateToNextModel();
            } else if (vX > 0.85f && currentIndex > 0) {
                animateToPreviousModel();
            }
            return true;
        }
    };
    private Interpolation interpolation = Interpolation.linear;

    public ModelSelectionUI(SpriteBatch spriteBatch, Skin skin, List<T> list, World world, ModelAdapter<T> adapter, OnFileButtonClicked<T> listener) {
        super();
        this.world = world;
        this.adapter = adapter;
        gestureDetector = new GestureDetector(gestureAdapter);
        this.list = Collections.synchronizedList(list);

        emptyLabel = new LabelVR(Style.getStringResource(R.string.no_saved_projects, "no projects found"), spriteBatch, skin);
        emptyLabel.setVisible(false);
        emptyLabel.setPosition(MODEL_POSITION);
        addProcessor(emptyLabel);

        final ModelBuilder builder = new ModelBuilder();
        sphere = this.world.add(new Entity(new ModelInstance(createSphereModel(builder, Color.WHITE))));
        sphere.setLightingEnabled(true);
        sphere.setVisible(false);

        final Drawable loadingSpinnerDrawable = skin.newDrawable(Style.Drawables.loading_spinner, Style.COLOR_ACCENT);
        previousSpinner = new LoadingSpinnerVR(spriteBatch, loadingSpinnerDrawable);
        previousSpinner.setVisible(false);
        addProcessor(previousSpinner);
        currentSpinner = new LoadingSpinnerVR(spriteBatch, loadingSpinnerDrawable);
        currentSpinner.setVisible(false);
        addProcessor(currentSpinner);
        nextSpinner = new LoadingSpinnerVR(spriteBatch, loadingSpinnerDrawable);
        nextSpinner.setVisible(false);
        addProcessor(nextSpinner);

        final FileButtonBar buttonBar = new FileButtonBar(spriteBatch, skin, 560, 112, listener);
        buttonBar.setPosition(MODEL_POSITION.x, MODEL_POSITION.y - MODEL_RADIUS, MODEL_POSITION.z + MODEL_RADIUS);
        buttonBar.lookAt(new Vector3(0, 0, 0), Vector3.Y);
        buttonBar.setVisible(true);
        addProcessor(buttonBar);
        itemCount = list.size();
        if (itemCount > 0) {
            loadAllModels(currentIndex);
            focusedIndex = -1;
        } else {
            emptyLabel.setVisible(true);
        }
    }

    private static BoundingBox createBoundingBox(ModelData modelData) {
        final BoundingBox bb = new BoundingBox();
        bb.inf();
        for (ModelMesh mesh : modelData.meshes) {
            int vertexSize = 0;
            int offset = 0;
            for (VertexAttribute attribute : mesh.attributes) {
                vertexSize += attribute.type == GL20.GL_UNSIGNED_BYTE ? (attribute.numComponents / 4) : attribute.numComponents;
                if (attribute.usage == VertexAttributes.Usage.Position)
                    offset = attribute.offset;
            }
            for (int i = 0; i < mesh.vertices.length; i += vertexSize) {
                bb.ext(mesh.vertices[i + offset], mesh.vertices[i + offset + 1], mesh.vertices[i + offset + 2]);
            }
        }
        return bb;
    }

    private static Model createSphereModel(ModelBuilder builder, Color color) {
        builder.begin();
        final MeshPartBuilder part = builder.part("s", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(color), ColorAttribute.createSpecular(Color.WHITE), new BlendingAttribute(true, 0.1f)));
        SphereShapeBuilder.build(part, 2f, 2f, 2f, 24, 12);
        return builder.end();
    }

    @Override
    public boolean performRayTest(Ray ray) {
        modelTest = false;
        sphere.setVisible(false);
        if (super.performRayTest(ray)) return true;
        if (rayTest(currentItem, ray)) {
            focusedIndex = currentItem.index;
        }
        if (rayTest(previousItem, ray)) {
            focusedIndex = previousItem.index;
        }
        if (rayTest(nextItem, ray)) {
            focusedIndex = nextItem.index;
        }
        return modelTest;
    }

    //        public void setVertexCount(int numVertices) {
//            vertexCountLabel.setText(nf.format(numVertices) + " vertices");
//        }
//
//        public void setTriangleCount(int numTriangles) {
//            triangleCountLabel.setText(nf.format(numTriangles) + " triangles");
//        }

    private boolean rayTest(ModelItem modelItem, Ray ray) {
        if (modelItem == null) return false;
        final Entity entity = modelItem.entity;
        modelItem.targetRotation.idt();
        if (entity != null && entity.intersectsRaySphere(ray, hitPoint3D)) {

            final Vector3 dir = Pools.obtain(Vector3.class);
            final Vector3 tmp = Pools.obtain(Vector3.class);
            final Vector3 tmp2 = Pools.obtain(Vector3.class);

            dir.set(hitPoint3D).sub(entity.getPosition()).nor();
            tmp.set(Vector3.Y).crs(dir).nor();
            tmp2.set(dir).crs(tmp).nor();
            modelItem.targetRotation.setFromAxes(tmp.x, tmp2.x, dir.x, tmp.y, tmp2.y, dir.y, tmp.z, tmp2.z, dir.z);
            entity.invalidate();
            Pools.free(tmp);
            Pools.free(tmp2);
            Pools.free(dir);


            hitPoint2DPixels.set(-1, -1);
            modelTest = true;
            isCursorOver = true;

            return true;
        }
        return false;
    }

    @Override
    public void act() {
        super.act();
        final float deltaTime = GdxVr.graphics.getDeltaTime();
        if (animating) {
            if (animValue < targetValue) {
                animValue += deltaTime / animDuration;
                if (animValue > targetValue) {
                    animValue = targetValue;
                    animating = false;
                    if (animCompleteListener != null)
                        animCompleteListener.run();
                }
            } else if (animValue > targetValue) {
                animValue -= deltaTime / animDuration;
                if (animValue < targetValue) {
                    animValue = targetValue;
                    animating = false;
                    if (animCompleteListener != null)
                        animCompleteListener.run();
                }
            }
        } else if (scrolling) {
            animValue = scrollX * 0.5f;
        } else {
            if (animValue < 0f) {
                animValue += deltaTime / animDuration;
                if (animValue > 0f)
                    animValue = 0f;
            } else if (animValue > 0f) {
                animValue -= deltaTime / animDuration;
                if (animValue < 0f)
                    animValue = 0f;
            }
        }
        sphere.setVisible(focusedIndex != -1);
        final Vector3 pPos = Pools.obtain(Vector3.class);
        final Vector3 cPos = Pools.obtain(Vector3.class);
        final Vector3 nPos = Pools.obtain(Vector3.class);
        final float scaleSmall = 0.75f;
        final float scaleLarge = 1.0f;
        final float cS = MathUtils.lerp(scaleLarge, scaleSmall, Math.abs(scrollX));
        float pS;
        float nS;
        if (animValue < 0f) {
            final float t = interpolation.apply(-animValue);
            pPos.set(PREVIOUS_POSITION);
            cPos.set(MODEL_POSITION).slerp(PREVIOUS_POSITION, t);
            nPos.set(NEXT_POSITION).slerp(MODEL_POSITION, t);
            pS = scaleSmall;
            nS = MathUtils.lerp(scaleSmall, scaleLarge, t);
        } else {
            final float t = interpolation.apply(animValue);
            pPos.set(PREVIOUS_POSITION).slerp(MODEL_POSITION, t);
            cPos.set(MODEL_POSITION).slerp(NEXT_POSITION, t);
            nPos.set(NEXT_POSITION);
            pS = MathUtils.lerp(scaleSmall, scaleLarge, t);
            nS = scaleSmall;
        }
        updateTransform(previousItem, previousSpinner, pPos, pS);
        updateTransform(currentItem, currentSpinner, cPos, cS);
        updateTransform(nextItem, nextSpinner, nPos, nS);
        Pools.free(pPos);
        Pools.free(cPos);
        Pools.free(nPos);
    }

    protected void updateTransform(@Nullable ModelItem<T> modelItem, VirtualStage stage, Vector3 position, float scale) {
        if (modelItem == null) return;
        final Entity currentModel = modelItem.entity;
        if (currentModel != null) {
            stage.setVisible(false);
            float r = modelItem.entity.getRadius();
            if (r != 0)
                modelItem.entity.setScale(MODEL_RADIUS / r * scale);
            modelItem.entity.setPosition(position);
            currentModel.getRotation().slerp(modelItem.targetRotation, 0.25f);
            currentModel.invalidate();

            if (modelItem.index == focusedIndex) {
                sphere.setPosition(currentModel.getPosition());
                sphere.setScale(MODEL_RADIUS * scale);
            }
        } else {
            stage.setPosition(position);
            stage.setVisible(true);
        }
    }

    private void dataSetChanged() {
        destroyAllItems();
        itemCount = list.size();
        currentIndex = MathUtils.clamp(currentIndex, 0, itemCount);
        if (itemCount > 0) {
            showItem(currentIndex);
        } else {
            emptyLabel.setVisible(true);
        }
    }

    private void showItem(int index) {
        Logger.d("showItem: currentIndex: " + currentIndex + " -> index:" + index);
        if (index < 0 || index >= itemCount) return;
        loadAllModels(index);
        focusedIndex = -1;
    }

    private void showPreviousModel() {
        currentIndex--;
        ModelItem<T> tmp = currentItem;
        currentItem = previousItem;
        destroyModelItem(nextItem);
        nextItem = tmp;
        final int i = currentIndex - 1;
        if (i >= 0) {
            previousItem = new ModelItem<>();
            previousItem.targetRotation.idt();
            previousItem.index = i;
            previousItem.t = list.get(i);
            loadModel(previousItem);
        } else {
            previousItem = null;
        }
    }

    private void showNextModel() {
        currentIndex++;
        ModelItem<T> tmp = currentItem;
        currentItem = nextItem;
        destroyModelItem(previousItem);
        previousItem = tmp;
        final int i = currentIndex + 1;
        if (i < itemCount) {
            nextItem = new ModelItem<>();
            nextItem.targetRotation.idt();
            nextItem.index = i;
            nextItem.t = list.get(i);
            loadModel(nextItem);
        } else {
            nextItem = null;
        }
    }

    private void loadAllModels(int index) {
        final int startIndex = index - 1;
        final int endIndex = index + 1;
        currentIndex = index;
        Logger.d("loadAllModels: currentIndex: " + currentIndex + " startIndex:" + startIndex + " endIndex:" + endIndex);

        if (startIndex >= 0) {
            previousItem = new ModelItem<>();
            previousItem.targetRotation.idt();
            previousItem.index = startIndex;
            previousItem.t = list.get(startIndex);
            loadModel(previousItem);
        } else {
            destroyModelItem(previousItem);
            previousItem = null;
        }

        destroyModelItem(currentItem);
        currentItem = new ModelItem<>();
        currentItem.targetRotation.idt();
        currentItem.index = currentIndex;
        currentItem.t = list.get(currentIndex);
        loadModel(currentItem);

        if (endIndex < itemCount) {
            nextItem = new ModelItem<>();
            nextItem.targetRotation.idt();
            nextItem.index = endIndex;
            nextItem.t = list.get(endIndex);
            loadModel(nextItem);
        } else {
            destroyModelItem(nextItem);
            nextItem = null;
        }
    }

    private void destroyAllItems() {
        destroyModelItem(previousItem);
        destroyModelItem(currentItem);
        destroyModelItem(nextItem);
    }
    
    private void destroyModelItem(@Nullable ModelItem modelItem) {
        if (modelItem == null) return;
        if (modelItem.loadModelFuture != null) {
            modelItem.loadModelFuture.cancel(true);
        }
        if (modelItem.entity != null) {
            world.remove(modelItem.entity);
            modelItem.entity.dispose();
            modelItem.entity = null;
        }
    }

    private void loadModel(final ModelItem<T> modelItem) {
//        if (loadModelFuture != null && !loadModelFuture.isDone()) {
//            loadModelFuture.cancel(true);
//        }
        modelItem.loadModelFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return adapter.loadModelData(modelItem.t);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
        modelItem.loadModelFuture.exceptionally(e -> {
            runOnGLThread(() -> adapter.onLoadModelFailed(modelItem.t, e));
            return null;
        }).thenAccept(modelData -> {
            if (modelData != null) {
                final BoundingBox bb = createBoundingBox(modelData);
                runOnGLThread(() -> {
                    final Model model = new Model(modelData);
                    final ModelInstance modelInstance = new ModelInstance(model, 0, 4, -5);
                    modelInstance.materials.set(0, Style.createSculptMaterial());
                    modelItem.entity = world.add(new Entity(modelInstance, bb));
                    modelItem.loadModelFuture = null;
                });
            }
        });
    }

    private void onModelClicked(int index) {
        if (animating) return;
        if (previousItem != null && index == previousItem.index) {
            animateToPreviousModel();
        } else if (nextItem != null && index == nextItem.index) {
            animateToNextModel();
        }
    }

    private void animateToPreviousModel() {
        animating = true;
        animDuration = MathUtils.clamp(0.5f * 1f - Math.abs(scrollX), 0.025f, 0.5f);
        targetValue = 1f;
        animCompleteListener = () -> {
            showPreviousModel();
            animValue = 0f;
            targetValue = 0f;
        };
    }

    private void animateToNextModel() {
        animating = true;
        animDuration = MathUtils.clamp(0.5f * 1f - Math.abs(scrollX), 0.025f, 0.5f);
        targetValue = -1f;
        animCompleteListener = () -> {
            showNextModel();
            animValue = 0f;
            targetValue = 0f;
        };
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
        dataSetChanged();
    }

    public void addItem(int index, T t) {
        list.add(index, t);
        dataSetChanged();
    }

    public void addItem(T t) {
        list.add(t);
        dataSetChanged();
    }

    public void removeItem(int index) {
        list.remove(index);
        dataSetChanged();
    }

    public T getItem(int index) {
        return list.get(index);
    }

    public void removeItem(T t) {
        list.remove(t);
        dataSetChanged();
    }

    public void onControllerButtonEvent(Controller controller, DaydreamButtonEvent event) {
        if (event.button == DaydreamButtonEvent.BUTTON_TOUCHPAD
                && event.action == DaydreamButtonEvent.ACTION_DOWN
                && modelTest
                && focusedIndex >= 0)
            onModelClicked(focusedIndex);
    }

    public void onControllerTouchPadEvent(Controller controller, DaydreamTouchEvent event) {
        switch (event.action) {
            case DaydreamTouchEvent.ACTION_DOWN:
                gestureDetector.touchDown(event.x * TOUCHPAD_SCALE, event.y * TOUCHPAD_SCALE, 0, 0);
                break;
            case DaydreamTouchEvent.ACTION_MOVE:
                gestureDetector.touchDragged(event.x * TOUCHPAD_SCALE, event.y * TOUCHPAD_SCALE, 0);
                break;
            case DaydreamTouchEvent.ACTION_UP:
                gestureDetector.touchUp(event.x * TOUCHPAD_SCALE, event.y * TOUCHPAD_SCALE, 0, 0);
                break;
        }
    }

    private void runOnGLThread(Runnable runnable) {
        GdxVr.app.postRunnable(runnable);
    }

    public interface ModelAdapter<T> {

        ModelData loadModelData(T t) throws IOException;

        void onLoadModelFailed(T t, Throwable e);
    }

    public static class ModelItem<T> {
        @Nullable
        public CompletableFuture<ModelData> loadModelFuture = null;
        @Nullable
        public Entity entity = null;
        public Quaternion targetRotation = new Quaternion();
        public T t;
        public int index = -1;
    }

    public interface OnFileButtonClicked<T> {

        void onOpenClicked(final T t);

        void onCopyClicked(final T t);

        void onDeleteClicked(final T t);
    }

    private class FileButtonBar extends TableVR {

        public FileButtonBar(Batch batch, Skin skin, int tableWidth, int tableHeight, final OnFileButtonClicked<T> listener) {
            super(batch, skin, tableWidth, tableHeight);
            final Table buttonBarTable = getTable();
            setBackground(skin.newDrawable(Style.Drawables.window, Style.COLOR_WINDOW));

            final VerticalImageTextButton openBtn = new VerticalImageTextButton(Style.getStringResource(R.string.open, "open"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_open));
            openBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (currentItem != null && currentItem.t != null)
                        listener.onOpenClicked(currentItem.t);
                }
            });
            final float padding = 8f;
            buttonBarTable.add(openBtn).padTop(padding).padBottom(padding).padLeft(padding).padRight(padding);

            final VerticalImageTextButton copyBtn = new VerticalImageTextButton(Style.getStringResource(R.string.copy, "copy"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_copy));
            copyBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (currentItem != null && currentItem.t != null)
                        listener.onCopyClicked(currentItem.t);
                }
            });
            buttonBarTable.add(copyBtn).padTop(padding).padBottom(padding).padRight(padding);

            final VerticalImageTextButton deleteBtn = new VerticalImageTextButton(Style.getStringResource(R.string.delete, "export"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_delete));
            deleteBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (currentItem != null && currentItem.index != -1 && currentItem.t != null) {
                        removeItem(currentItem.index);
                        listener.onDeleteClicked(currentItem.t);
                    }
                }
            });
            buttonBarTable.add(deleteBtn).padTop(padding).padBottom(padding).padRight(padding);

            resizeToFitTable();
        }
    }
}
