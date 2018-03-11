package net.masonapps.clayvr.environment;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ShortArray;

/**
 * Created by Bob on 6/6/2017.
 */

public class SkyDomeBuilder extends BaseShapeBuilder {
    private final static ShortArray tmpIndices = new ShortArray();

    public static Model build(ModelBuilder modelBuilder, float radius, int divisionsU, int divisionsV, Texture texture) {
        return build(modelBuilder, radius, divisionsU, divisionsV, new TextureRegion(texture));
    }

    public static Model build(ModelBuilder modelBuilder, float radius, int divisionsU, int divisionsV, TextureRegion region) {
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("sphere", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates, new Material(TextureAttribute.createDiffuse(region), IntAttribute.createCullFace(GL20.GL_FRONT)));
        build(builder, radius, divisionsU, divisionsV);
        return modelBuilder.end();
    }

    public static void build(MeshPartBuilder builder, float radius, int divisionsU, int divisionsV) {
        final float hw = radius;
        final float hh = radius;
        final float hd = radius;
        final float auo = 0f;
        final float stepU = MathUtils.PI2 / divisionsU;
        final float avo = 0f;
        final float stepV = MathUtils.PI / 2f / divisionsV;
        final float us = 1f / divisionsU;
        final float vs = 1f / divisionsV;
        float angleU = 0f;
        float angleV = 0f;
        MeshPartBuilder.VertexInfo curr1 = vertTmp3.set(null, null, null, null);
        curr1.hasUV = curr1.hasPosition = curr1.hasNormal = true;

        final int s = divisionsU + 3;
        tmpIndices.clear();
        tmpIndices.ensureCapacity(divisionsU * 2);
        tmpIndices.size = s;
        int tempOffset = 0;
        builder.ensureVertices((divisionsV + 1) * (divisionsU + 1));
        builder.ensureRectangleIndices(divisionsU);
        for (int iv = 0; iv <= divisionsV; iv++) {
            angleV = avo + stepV * iv;
            final float t = MathUtils.sin(angleV);
            final float h = MathUtils.cos(angleV) * hh;
            for (int iu = 0; iu <= divisionsU; iu++) {
                angleU = auo + stepU * iu;
                curr1.position.set(MathUtils.cos(angleU) * hw * t, h, MathUtils.sin(angleU) * hd * t);
                curr1.normal.set(curr1.position).scl(-1).nor();
                curr1.uv.set(curr1.position.x / radius / 2f + 0.5f, curr1.position.z / radius / 2f + 0.5f);
                tmpIndices.set(tempOffset, builder.vertex(curr1));
                final int o = tempOffset + s;
                if ((iv > 0) && (iu > 0))
                    builder.rect(tmpIndices.get(tempOffset), tmpIndices.get((o - 1) % s), tmpIndices.get((o - (divisionsU + 2)) % s),
                            tmpIndices.get((o - (divisionsU + 1)) % s));
                tempOffset = (tempOffset + 1) % tmpIndices.size;
            }
        }
    }
}
