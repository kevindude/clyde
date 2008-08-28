//
// $Id$

package com.threerings.opengl.eff;

import java.lang.ref.SoftReference;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.samskivert.util.HashIntMap;

import com.threerings.expr.Bound;
import com.threerings.expr.MutableInteger;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;

import com.threerings.opengl.effect.Particle;
import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.geom.DynamicGeometry;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.geometry.config.PassSummary;
import com.threerings.opengl.geometry.util.GeometryUtil;
import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.SimpleBatch.DrawElements;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;

/**
 * Represents a particle geometry instance.
 */
public abstract class ParticleGeometry extends DynamicGeometry
{
    /**
     * Renders particles as points.
     */
    public static class Points extends ParticleGeometry
    {
        public Points (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            super(scope);
            init(ctx, passes);
        }

        @Override // documentation inherited
        protected int getMode ()
        {
            return GL11.GL_POINTS;
        }

        @Override // documentation inherited
        protected int getParticleVertexCount ()
        {
            return 1;
        }

        @Override // documentation inherited
        protected int getParticleIndexCount ()
        {
            return 1;
        }

        @Override // documentation inherited
        protected int[] getPrototypeIndices ()
        {
            return new int[] { 0 };
        }

        @Override // documentation inherited
        protected void updateData ()
        {
            // get everything into local variables
            Particle[] particles = _particles;
            float[] data = _data;

            // figure out the texture coordinate parameters
            int udivs = _config.textureDivisionsS;
            float uscale = 1f / udivs;
            float vscale = 1f / _config.textureDivisionsT;

            // update the living particles
            for (int ii = 0, nn = _living.value; ii < nn; ii++) {
                Particle particle = particles[ii];

                // determine the texture coordinate offsets
                int frame = Math.round(particle.getFrame());
                float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;


            }
        }
    }

    /**
     * Renders particles as single line segments.
     */
    public static class Lines extends ParticleGeometry
    {
        public Lines (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            super(scope);
            init(ctx, passes);
        }

        @Override // documentation inherited
        protected int getMode ()
        {
            return GL11.GL_LINES;
        }

        @Override // documentation inherited
        protected int getParticleVertexCount ()
        {
            return 2;
        }

        @Override // documentation inherited
        protected int getParticleIndexCount ()
        {
            return 2;
        }

        @Override // documentation inherited
        protected int[] getPrototypeIndices ()
        {
            return new int[] { 0, 1 };
        }

        @Override // documentation inherited
        protected void updateData ()
        {
        }
    }

    /**
     * Renders particles as multi-segment line trails.
     */
    public static class LineTrails extends ParticleGeometry
    {
        public LineTrails (GlContext ctx, Scope scope, PassDescriptor[] passes, int segments)
        {
            super(scope);
            _segments = segments;
            init(ctx, passes);
        }

        @Override // documentation inherited
        protected int getMode ()
        {
            return GL11.GL_LINES;
        }

        @Override // documentation inherited
        protected int getParticleVertexCount ()
        {
            return _segments + 1;
        }

        @Override // documentation inherited
        protected int getParticleIndexCount ()
        {
            return _segments * 2;
        }

        @Override // documentation inherited
        protected int[] getPrototypeIndices ()
        {
            int[] prototype = new int[_segments * 2];
            for (int ii = 0, idx = 0; ii < _segments; ii++) {
                prototype[idx++] = ii;
                prototype[idx++] = ii + 1;
            }
            return prototype;
        }

        @Override // documentation inherited
        protected void updateData ()
        {
        }

        /** The number of segments in each particle. */
        protected int _segments;
    }

    /**
     * Renders particles as single quads.
     */
    public static class Quads extends ParticleGeometry
    {
        public Quads (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            super(scope);
            init(ctx, passes);
        }

        @Override // documentation inherited
        protected int getMode ()
        {
            return GL11.GL_TRIANGLES;
        }

        @Override // documentation inherited
        protected int getParticleVertexCount ()
        {
            return 4;
        }

        @Override // documentation inherited
        protected int getParticleIndexCount ()
        {
            return 6;
        }

        @Override // documentation inherited
        protected int[] getPrototypeIndices ()
        {
            return new int[] { 0, 1, 2, 2, 1, 3 };
        }

        @Override // documentation inherited
        protected void updateData ()
        {
        }
    }

    /**
     * Renders particles as multi-segment quad trails.
     */
    public static class QuadTrails extends ParticleGeometry
    {
        public QuadTrails (GlContext ctx, Scope scope, PassDescriptor[] passes, int segments)
        {
            super(scope);
            _segments = segments;
            init(ctx, passes);
        }

        @Override // documentation inherited
        protected int getMode ()
        {
            return GL11.GL_TRIANGLES;
        }

        @Override // documentation inherited
        protected int getParticleVertexCount ()
        {
            return (_segments + 1) * 2;
        }

        @Override // documentation inherited
        protected int getParticleIndexCount ()
        {
            return _segments * 6;
        }

        @Override // documentation inherited
        protected int[] getPrototypeIndices ()
        {
            int[] prototype = new int[_segments * 6];
            for (int ii = 0, idx = 0; ii < _segments; ii++) {
                int offset = ii*2;
                prototype[idx++] = offset;
                prototype[idx++] = offset + 1;
                prototype[idx++] = offset + 2;

                prototype[idx++] = offset + 2;
                prototype[idx++] = offset + 1;
                prototype[idx++] = offset + 3;
            }
            return prototype;
        }

        @Override // documentation inherited
        protected void updateData ()
        {
        }

        /** The number of segments in each particle. */
        protected int _segments;
    }

    /**
     * Renders particles as meshes.
     */
    public static class Meshes extends ParticleGeometry
    {
        public Meshes (
            GlContext ctx, Scope scope, PassDescriptor[] passes, GeometryConfig.IndexedStored geom)
        {
            super(scope);
            _geom = geom;
            init(ctx, passes);
        }

        @Override // documentation inherited
        protected int getMode ()
        {
            return _geom.mode.getConstant();
        }

        @Override // documentation inherited
        protected int getParticleVertexCount ()
        {
            return _geom.getVertexCount();
        }

        @Override // documentation inherited
        protected int getParticleIndexCount ()
        {
            return _geom.indices.capacity();
        }

        @Override // documentation inherited
        protected int[] getPrototypeIndices ()
        {
            int[] prototype = new int[_geom.indices.capacity()];
            for (int ii = 0; ii < prototype.length; ii++) {
                prototype[ii] = _geom.indices.get(ii);
            }
            return prototype;
        }

        @Override // documentation inherited
        protected void updateData ()
        {
        }

        /** The geometry to render. */
        protected GeometryConfig.IndexedStored _geom;
    }

    /**
     * Creates a new geometry object.
     */
    public ParticleGeometry (Scope scope)
    {
        ScopeUtil.updateBound(this, scope);
    }

    @Override // documentation inherited
    public ArrayState getArrayState (int pass)
    {
        return _arrayStates[pass];
    }

    @Override // documentation inherited
    public DrawCommand getDrawCommand (int pass)
    {
        return _drawCommand;
    }

    @Override // documentation inherited
    public void update ()
    {
        super.update();

        // modify the draw command based on the number of living particles
        _drawCommand.setLimits(0, _living.value * getParticleIndexCount());
        _drawCommand.setRange(0, _living.value * getParticleVertexCount() - 1);
    }

    /**
     * Initializes the geometry.
     */
    protected void init (GlContext ctx, PassDescriptor[] passes)
    {
        // create the base arrays
        HashMap<String, ClientArray> vertexAttribArrays = new HashMap<String, ClientArray>();
        HashIntMap<ClientArray> texCoordArrays = new HashIntMap<ClientArray>();
        texCoordArrays.put(0, new ClientArray(2, (FloatBuffer)null));
        ClientArray colorArray = new ClientArray(4, (FloatBuffer)null);
        ClientArray normalArray = null;
        ClientArray vertexArray = new ClientArray(3, (FloatBuffer)null);

        // put them in a list and compute the offsets and stride
        ArrayList<ClientArray> arrays = GeometryUtil.createList(
            vertexAttribArrays, texCoordArrays, colorArray, normalArray, vertexArray);
        int stride = GeometryUtil.updateOffsetsAndStride(arrays);

        // (re)create the data array if necessary
        _data = (_config.data == null) ? null : _config.data.get();
        if (_data == null) {
            int size = _particles.length * getParticleVertexCount() * stride / 4;
            _config.data = new SoftReference<float[]>(_data = new float[size]);
        }

        // use a VBO if possible
        BufferObject elementArrayBuffer = null;
        if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
            _arrayBuffer = new BufferObject(ctx.getRenderer());
            _floatArray = getScratchBuffer(_data.length);

            // (re)create the shared element array buffer if necessary
            elementArrayBuffer = (_config.elementArrayBuffer == null) ?
                null : _config.elementArrayBuffer.get();
            if (elementArrayBuffer == null) {
                _config.elementArrayBuffer = new SoftReference<BufferObject>(
                    elementArrayBuffer = new BufferObject(ctx.getRenderer()));
                elementArrayBuffer.setData(createIndices());
            }
            _drawCommand = SimpleBatch.createDrawBufferElements(
                getMode(), 0, 0, 0, GL11.GL_UNSIGNED_SHORT, 0L);

        } else {
            _floatArray = BufferUtils.createFloatBuffer(_data.length);

            // (re)create the shared index buffer if necessary
            ShortBuffer indices = (_config.indices == null) ? null : _config.indices.get();
            if (indices == null) {
                _config.indices = new SoftReference<ShortBuffer>(
                    indices = createIndices());
            }
            _drawCommand = SimpleBatch.createDrawShortElements(getMode(), 0, 0, indices);
        }

        // set the array references
        for (ClientArray array : arrays) {
            array.arrayBuffer = _arrayBuffer;
            array.floatArray = (_arrayBuffer == null) ? _floatArray : null;
        }

        // create the array states
        _arrayStates = GeometryUtil.createArrayStates(
            vertexAttribArrays, texCoordArrays, colorArray,
            normalArray, vertexArray, elementArrayBuffer, passes);
    }

    /**
     * Creates and populates the index buffer.
     */
    protected ShortBuffer createIndices ()
    {
        int[] prototype = getPrototypeIndices();
        ShortBuffer indices = BufferUtils.createShortBuffer(_particles.length * prototype.length);
        int vpp = getParticleVertexCount();
        for (int ii = 0, offset = 0; ii < _particles.length; ii++, offset += vpp) {
            for (int index : prototype) {
                indices.put((short)(offset + index));
            }
        }
        indices.rewind();
        return indices;
    }

    /**
     * Returns the primitive mode.
     */
    protected abstract int getMode ();

    /**
     * Returns the number of vertices in each particle.
     */
    protected abstract int getParticleVertexCount ();

    /**
     * Returns the number of indices in each particle.
     */
    protected abstract int getParticleIndexCount ();

    /**
     * Returns the array of prototype indices that will be replicated at different offsets to
     * create the actual index buffer.
     */
    protected abstract int[] getPrototypeIndices ();

    /** The configuration of the layer. */
    @Bound
    protected ParticleSystemConfig.Layer _config;

    /** The particles to render. */
    @Bound
    protected Particle[] _particles;

    /** The number of particles currently active. */
    @Bound
    protected MutableInteger _living;

    /** The layer's transform state. */
    @Bound
    protected TransformState _transformState;

    /** The array states for each pass. */
    protected ArrayState[] _arrayStates;

    /** The draw command shared by all passes. */
    protected DrawElements _drawCommand;
}
