package net.masonapps.clayvr.sculpt;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;

import net.masonapps.clayvr.bvh.BVH;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Bob on 7/6/2017.
 */

public class DebugUtils {

    public static Model createBoundsModel(BVH bvh, Color color) {
        final ModelBuilder builder = new ModelBuilder();
        builder.begin();
        final Queue<BVH.Node> queue = new LinkedList<>();
        queue.add(bvh.root);
        while (!queue.isEmpty()) {
            final BVH.Node node = queue.poll();
            if (node instanceof BVH.Group) {
                queue.add(((BVH.Group) node).child1);
                queue.add(((BVH.Group) node).child2);
            }
            final MeshPartBuilder part = builder.part("", GL20.GL_LINES, VertexAttributes.Usage.Position, new Material(ColorAttribute.createDiffuse(color)));
            BoxShapeBuilder.build(part, node.bb);
        }
        return builder.end();
    }
}
