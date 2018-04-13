package net.masonapps.clayvr.environment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;

/**
 * Created by Bob Mason on 4/5/2018.
 */
public class ModelUtils {
    public static Model createSphereModel(ModelBuilder builder, Color color) {
        builder.begin();
        final MeshPartBuilder part = builder.part("s", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(color), ColorAttribute.createSpecular(Color.WHITE), new BlendingAttribute(true, 0.25f)));
        SphereShapeBuilder.build(part, 2f, 2f, 2f, 24, 12);
        return builder.end();
    }

    public static Model createTransparentRect(ModelBuilder modelBuilder, Color color) {
        final Material material = new Material(ColorAttribute.createDiffuse(color), new BlendingAttribute(true, 0.25f), IntAttribute.createCullFace(0));
        final float r = 1f;
        return modelBuilder.createRect(
                0, -r, r,
                0, r, r,
                0, r, -r,
                0, -r, -r,
                0f, 0f, 1f,
                material, VertexAttributes.Usage.Position);
    }

    public static Model createFloorRect(ModelBuilder modelBuilder, float r, Material material) {
        return modelBuilder.createRect(
                -r, 0, r,
                r, 0, r,
                r, 0, -r,
                -r, 0, -r,
                0f, 1f, 0f,
                material, VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates);
    }
}
