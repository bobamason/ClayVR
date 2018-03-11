package net.masonapps.clayvr.screens;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.google.vr.sdk.controller.Controller;

import net.masonapps.clayvr.Constants;
import net.masonapps.clayvr.R;
import net.masonapps.clayvr.SculptingVrGame;
import net.masonapps.clayvr.Style;
import net.masonapps.clayvr.bvh.BVH;
import net.masonapps.clayvr.math.UnitConversion;
import net.masonapps.clayvr.sculpt.SculptMesh;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.gfx.Entity;
import org.masonapps.libgdxgooglevr.input.DaydreamButtonEvent;
import org.masonapps.libgdxgooglevr.math.CylindricalCoordinate;
import org.masonapps.libgdxgooglevr.math.PlaneUtils;
import org.masonapps.libgdxgooglevr.ui.CylindricalWindowUiContainer;
import org.masonapps.libgdxgooglevr.ui.LabelVR;
import org.masonapps.libgdxgooglevr.ui.LoadingSpinnerVR;
import org.masonapps.libgdxgooglevr.ui.WindowTableVR;
import org.masonapps.libgdxgooglevr.ui.WindowVR;

import java.text.DecimalFormat;

import static net.masonapps.clayvr.screens.SculptingScreen.TransformAction.ACTION_NONE;
import static net.masonapps.clayvr.screens.SculptingScreen.TransformAction.ROTATE;
import static net.masonapps.clayvr.screens.SculptingScreen.TransformAction.ZOOM;

/**
 * Created by Bob on 9/25/2017.
 */

public class ExportScreen extends RoomScreen {

    private static final float PADDING = 8f;
    private final Vector3 modelPosition = new Vector3(0, -1.5f, -2.2f);
    private final Vector3 position = new Vector3();
    private final LabelVR xLabel;
    private final LabelVR yLabel;
    private final LabelVR zLabel;
    private final SculptMesh sculptMesh;
    private final BVH bvh;
    private final String projectName;
    private final ExportListener listener;
    private final Entity sculptEntity;
    private final Entity box;
    private final LoadingSpinnerVR loadingSpinnerVR;
    private WindowTableVR fileTable;
    private WindowTableVR unitsTable;
    private Quaternion startRotation = new Quaternion();
    private Quaternion rotation = new Quaternion();
    private Quaternion lastRotation = new Quaternion();
    private float scale = 1f;
    private float targetSizeMeters = 1f;
    private Vector3 hitPoint = new Vector3();
    private Plane hitPlane = new Plane();
    private SculptingScreen.TransformAction transformAction = ACTION_NONE;
    private CylindricalWindowUiContainer ui;
    private UnitConversion.Unit units = UnitConversion.Unit.meter;
    private DecimalFormat df = new DecimalFormat("#.##");
    private ShapeRenderer shapeRenderer;
    private float offsetLabels = 0.1f;
    private Slider sizeSlider;

    public ExportScreen(SculptingVrGame game, SculptMesh sculptMesh, BVH bvh, String projectName, ExportListener listener) {
        super(game);
        this.sculptMesh = sculptMesh;
        this.bvh = bvh;
        this.projectName = projectName;
        this.listener = listener;
        ui = new CylindricalWindowUiContainer(2f, 4f);
//        getWorld().add(Style.newGradientBackground(getVrCamera().far - 1f));
//        getWorld().add(Grid.newInstance(2f, 0.25f, 0.025f, Color.WHITE, Color.GRAY)).setPosition(modelPosition);
        final Batch spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        manageDisposable(spriteBatch, shapeRenderer);
        final Skin skin = game.getSkin();
        final WindowVR.WindowVrStyle windowVrStyle = Style.createWindowVrStyle(skin);
        initFileTable(spriteBatch, skin, windowVrStyle);
        initUnitsTable(spriteBatch, skin, windowVrStyle);
        xLabel = new LabelVR("   0.00m", spriteBatch, skin, Style.DEFAULT_FONT, Color.RED);
        yLabel = new LabelVR("   0.00m", spriteBatch, skin, Style.DEFAULT_FONT, Color.BLUE);
        zLabel = new LabelVR("   0.00m", spriteBatch, skin, Style.DEFAULT_FONT, Color.GREEN);
        final float s = 0.75f;
        xLabel.scale(s, s);
        yLabel.scale(s, s);
        zLabel.scale(s, s);
        ui.addProcessor(xLabel);
        ui.addProcessor(yLabel);
        ui.addProcessor(zLabel);

        final ModelBuilder builder = new ModelBuilder();
        sculptEntity = SculptMesh.createSculptEntity(builder, sculptMesh, bvh.root.bb, Style.createSculptMaterial());
        sculptEntity.setPosition(modelPosition);
        getWorld().add(sculptEntity);

        box = getWorld().add(new Entity(new ModelInstance(createBoxModel(builder, Color.WHITE, sculptEntity.getBounds()))));
        box.setPosition(modelPosition);

        loadingSpinnerVR = new LoadingSpinnerVR(spriteBatch, skin.newDrawable(Style.Drawables.loading_spinner, Style.COLOR_ACCENT));
        loadingSpinnerVR.setPosition(0, 0, -1.5f);
        loadingSpinnerVR.setVisible(false);
        ui.addProcessor(loadingSpinnerVR);

        updateTransform();
    }

    private static Model createBoxModel(ModelBuilder builder, Color color, BoundingBox bounds) {
        builder.begin();
        final MeshPartBuilder part = builder.part("s", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(color), new BlendingAttribute(true, 0.125f)));
        BoxShapeBuilder.build(part, bounds);
        return builder.end();
    }

    private static float getTargetSizeForUnit(UnitConversion.Unit unit) {
        switch (unit) {
            case millimeter:
                return 300f;
            case centimeter:
                return 30f;
            case inch:
                return 12f;
            case foot:
                return 3f;
            default:
                return 1f;
        }
    }

    protected void initFileTable(Batch spriteBatch, Skin skin, WindowVR.WindowVrStyle windowVrStyle) {
        fileTable = new WindowTableVR(spriteBatch, skin, 420, 500, Style.getStringResource(R.string.export_title, "Export as"), windowVrStyle);
        fileTable.setPosition(new CylindricalCoordinate(ui.getRadius(), 120f, 0f, CylindricalCoordinate.AngleMode.degrees).toCartesian());
        fileTable.lookAt(new Vector3(0, fileTable.getPosition().y, 0), Vector3.Y);
        final Table table = fileTable.getTable();

        final TextButton stlBtn = new TextButton(Style.getStringResource(R.string.stl, "Stereolithography (.stl)"), skin);
        final TextButton plyBtn = new TextButton(Style.getStringResource(R.string.ply, "Polygon File Format (.ply)"), skin);
        final TextButton objBtn = new TextButton(Style.getStringResource(R.string.obj, "Wavefront OBJ (.obj)"), skin);

        stlBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exportClicked(Constants.FILE_TYPE_STL);
            }
        });

        objBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exportClicked(Constants.FILE_TYPE_OBJ);
            }
        });

        plyBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exportClicked(Constants.FILE_TYPE_PLY);
            }
        });

        table.add(stlBtn)
                .growX()
                .center()
                .padTop(PADDING)
                .padBottom(PADDING)
                .padLeft(PADDING)
                .padRight(PADDING)
                .row();

        table.add(objBtn)
                .growX()
                .center()
                .padBottom(PADDING)
                .padLeft(PADDING)
                .padRight(PADDING)
                .row();

        table.add(plyBtn)
                .growX()
                .center()
                .padBottom(PADDING)
                .padLeft(PADDING)
                .padRight(PADDING)
                .row();

//        fileTable.resizeToFitTable();
        ui.addProcessor(fileTable);

        fileTable.resizeToFitTable();
    }

    protected void initUnitsTable(Batch spriteBatch, Skin skin, WindowVR.WindowVrStyle windowVrStyle) {
        unitsTable = new WindowTableVR(spriteBatch, skin, 420, 500, Style.getStringResource(R.string.size, "Size"), windowVrStyle);
        unitsTable.setPosition(new CylindricalCoordinate(ui.getRadius(), 60f, 0f, CylindricalCoordinate.AngleMode.degrees).toCartesian());
        unitsTable.lookAt(new Vector3(0, unitsTable.getPosition().y, 0), Vector3.Y);

        final Table table = unitsTable.getTable();

        sizeSlider = new Slider(1f, 300f, 1f, false, skin);

        sizeSlider.setValue(100f);
        sizeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                final float s = getTargetSizeForUnit(units) * sizeSlider.getValue() * 0.01f;
                targetSizeMeters = UnitConversion.convertToMeters(s, units);
                updateTransform();
            }
        });
        table.add(sizeSlider)
                .colspan(5)
                .growX()
                .padTop(PADDING)
                .padLeft(PADDING)
                .padRight(PADDING)
                .row();
        
        final ButtonGroup<TextButton> buttonGroup = new ButtonGroup<>();

        final TextButton millimeters = new TextButton("mm", skin, Style.TOGGLE);
        millimeters.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                units = UnitConversion.Unit.millimeter;
                buttonGroup.uncheckAll();
                buttonGroup.setChecked(millimeters.getText().toString());
                onUnitsChanged();
            }
        });
        table.add(millimeters)
                .center()
                .padTop(PADDING)
                .padBottom(PADDING)
                .padLeft(PADDING)
                .padRight(PADDING);
        buttonGroup.add(millimeters);

        final TextButton centimeters = new TextButton("cm", skin, Style.TOGGLE);
        centimeters.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                units = UnitConversion.Unit.centimeter;
                buttonGroup.uncheckAll();
                buttonGroup.setChecked(centimeters.getText().toString());
                onUnitsChanged();
            }
        });
        table.add(centimeters)
                .center()
                .padTop(PADDING)
                .padBottom(PADDING)
                .padRight(PADDING);
        buttonGroup.add(centimeters);

        final TextButton meters = new TextButton("m", skin, Style.TOGGLE);
        meters.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                units = UnitConversion.Unit.meter;
                buttonGroup.uncheckAll();
                buttonGroup.setChecked(meters.getText().toString());
                onUnitsChanged();
            }
        });
        table.add(meters)
                .center()
                .padTop(PADDING)
                .padBottom(PADDING)
                .padRight(PADDING);
        buttonGroup.add(meters);

        final TextButton inches = new TextButton("in", skin, Style.TOGGLE);
        inches.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                units = UnitConversion.Unit.inch;
                buttonGroup.uncheckAll();
                buttonGroup.setChecked(inches.getText().toString());
                onUnitsChanged();
            }
        });
        table.add(inches)
                .center()
                .padTop(PADDING)
                .padBottom(PADDING)
                .padRight(PADDING);
        buttonGroup.add(inches);

        final TextButton feet = new TextButton("ft", skin, Style.TOGGLE);
        feet.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                units = UnitConversion.Unit.foot;
                buttonGroup.uncheckAll();
                buttonGroup.setChecked(feet.getText().toString());
                onUnitsChanged();
            }
        });
        table.add(feet)
                .center()
                .padTop(PADDING)
                .padBottom(PADDING)
                .padRight(PADDING);
        buttonGroup.add(feet);

        buttonGroup.setMinCheckCount(1);
        buttonGroup.setMaxCheckCount(1);
        buttonGroup.setChecked("m");

        ui.addProcessor(unitsTable);

        unitsTable.resizeToFitTable();
    }

    @Override
    protected void addLights(Array<BaseLight> lights) {
        final DirectionalLight light = new DirectionalLight();
        light.setColor(Color.WHITE);
        light.setDirection(new Vector3(1, -1, -1).nor());
        lights.add(light);
    }

    private void exportClicked(String fileType) {
        loadingSpinnerVR.setVisible(true);
        fileTable.getTable().setTouchable(Touchable.disabled);
        listener.onExportFile(projectName, fileType, sculptEntity.getTransform(new Matrix4()));
    }

    public void onExportComplete() {
        loadingSpinnerVR.setVisible(false);
        fileTable.getTable().setTouchable(Touchable.enabled);
    }

    @Override
    public void show() {
        super.show();
        GdxVr.input.setInputProcessor(ui);
    }

    @Override
    public void hide() {
        super.hide();
        GdxVr.input.setInputProcessor(null);
    }

    @Override
    public void update() {
        super.update();
        ui.act();
        if (transformAction == ROTATE)
            rotate();
//        else if (transformAction == PAN)
//            pan();
        else if (transformAction == ZOOM)
            zoom();
    }

    @Override
    public void render(Camera camera, int whichEye) {
        super.render(camera, whichEye);
        final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
        final Vector3 tmp1 = Pools.obtain(Vector3.class);
        final Vector3 tmp2 = Pools.obtain(Vector3.class);
        final BoundingBox bounds = sculptEntity.getBounds();

        final float offset = offsetLabels * 0.5f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.setTransformMatrix(sculptEntity.getTransform(tmpMat));

        shapeRenderer.setColor(Color.RED);
        shapeRenderer.line(bounds.getCorner001(tmp1).add(0, -offset, 0), bounds.getCorner101(tmp2).add(0, -offset, 0));
        shapeRenderer.line(bounds.getCorner001(tmp1), bounds.getCorner001(tmp2).add(0, -offset, 0));
        shapeRenderer.line(bounds.getCorner101(tmp1), bounds.getCorner101(tmp2).add(0, -offset, 0));

        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.line(bounds.getCorner111(tmp1).add(offset, 0, 0), bounds.getCorner110(tmp2).add(offset, 0, 0));
        shapeRenderer.line(bounds.getCorner111(tmp1), bounds.getCorner111(tmp2).add(offset, 0, 0));
        shapeRenderer.line(bounds.getCorner110(tmp1), bounds.getCorner110(tmp2).add(offset, 0, 0));

        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.line(bounds.getCorner101(tmp1).add(offset, 0, 0), bounds.getCorner111(tmp2).add(offset, 0, 0));
        shapeRenderer.line(bounds.getCorner101(tmp1), bounds.getCorner101(tmp2).add(offset, 0, 0));
        shapeRenderer.line(bounds.getCorner111(tmp1), bounds.getCorner111(tmp2).add(offset, 0, 0));

        shapeRenderer.end();
        
        ui.draw(camera);

        Pools.free(tmpMat);
        Pools.free(tmp1);
        Pools.free(tmp2);
    }

    @Override
    public void onControllerButtonEvent(Controller controller, DaydreamButtonEvent event) {
        if (!ui.isCursorOver() && controller.clickButtonState) {
            if (transformAction == ACTION_NONE)
                lastRotation.set(GdxVr.input.getControllerOrientation());
            transformAction = ROTATE;
        } else
            transformAction = ACTION_NONE;
    }

    private void rotate() {
        final Quaternion rotDiff = Pools.obtain(Quaternion.class);
        rotDiff.set(lastRotation).conjugate().mulLeft(GdxVr.input.getControllerOrientation());
        rotation.mulLeft(rotDiff);
        Pools.free(rotDiff);
        updateTransform();
        lastRotation.set(GdxVr.input.getControllerOrientation());
    }

    private void zoom() {
        if (Intersector.intersectRayPlane(GdxVr.input.getInputRay(), hitPlane, hitPoint)) {
            final Vector2 tmp = Pools.obtain(Vector2.class);
            PlaneUtils.toSubSpace(hitPlane, hitPoint, tmp);
            scale = (tmp.limit(2f).y + 2f) / 2f;
            scale = MathUtils.clamp(scale, 0.2f, 10f);
            Pools.free(tmp);
            updateTransform();
        }
    }

    private void onUnitsChanged() {
        targetSizeMeters = UnitConversion.convertToMeters(getTargetSizeForUnit(units), units);
        sizeSlider.setValue(100f);
        switch (units) {
            case millimeter:
            case centimeter:
            case inch:
                modelPosition.set(0, -1f, -1.5f);
                break;
            case foot:
            case meter:
                modelPosition.set(0, -1.5f, -2.2f);
                break;
            default:
                break;
        }
        updateTransform();
    }

    private void updateTransform() {
        final Vector3 tmpV = Pools.obtain(Vector3.class);
        final Vector3 testPoint = Pools.obtain(Vector3.class);
        final Matrix4 tmpMat = Pools.obtain(Matrix4.class);
        final Matrix4 tmpInvMat = Pools.obtain(Matrix4.class);

        final Vector3 dimensions = sculptEntity.getDimensions();
        final BoundingBox bounds = sculptEntity.getBounds();

        final float maxDimen = Math.max(dimensions.x, Math.max(dimensions.y, dimensions.z));
        scale = targetSizeMeters / maxDimen;
        tmpMat.set(rotation).scale(scale, scale, scale);
        tmpInvMat.set(tmpMat).inv();

        testPoint.set(0, -1000, 0).mul(tmpInvMat);
        final Vector3 closestPoint = bvh.closestPoint(testPoint).mul(tmpMat);
        position.set(modelPosition).sub(0, closestPoint.y, 0);

//        position.set(modelPosition).sub(0, -bounds.min.y * scale, 0);
        sculptEntity.setPosition(position).setRotation(rotation).setScale(scale);
        sculptEntity.recalculateTransform();
        sculptEntity.getTransform(tmpMat);
//        Logger.d("transform = " + tmpMat);
        box.setTransform(tmpMat);

        final float offset = offsetLabels + xLabel.getHeightWorld();
        String u = UnitConversion.getUnitString(units);
        tmpV.set(0, bounds.min.y * scale - offset, bounds.max.z * scale).mul(rotation).add(position);
        final float x = UnitConversion.convertMeterToUnit(dimensions.x * scale, units);
        xLabel.setText(df.format(x) + u);
        xLabel.setPosition(tmpV);

        tmpV.set(bounds.max.x * scale + offset, 0, bounds.max.z * scale).mul(rotation).add(position);
        final float y = UnitConversion.convertMeterToUnit(dimensions.y * scale, units);
        yLabel.setText(df.format(y) + u);
        yLabel.setPosition(tmpV);

        tmpV.set(bounds.min.x * scale - offset, bounds.max.y * scale + offset, 0).mul(rotation).add(position);
        final float z = UnitConversion.convertMeterToUnit(dimensions.z * scale, units);
        zLabel.setText(df.format(z) + u);
        zLabel.setPosition(tmpV);

        Pools.free(tmpV);
        Pools.free(tmpMat);
    }

    @Override
    public void onControllerBackButtonClicked() {
        final SculptingScreen sculptingScreen = getSculptingVrGame().getSculptingScreen();
        if (sculptingScreen != null)
            getSculptingVrGame().setScreen(sculptingScreen);
        else
            getSculptingVrGame().switchToStartupScreen();
    }

    public interface ExportListener {
        void onExportFile(String projectName, String fileType, Matrix4 transform);
    }
}
