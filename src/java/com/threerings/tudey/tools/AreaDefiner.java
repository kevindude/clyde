//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Predicate;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.scene.SceneElement;

import com.threerings.tudey.client.sprite.AreaSprite;
import com.threerings.tudey.config.AreaConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.AreaEntry;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.Vertex;

/**
 * The area definer tool.
 */
public class AreaDefiner extends ConfigTool<AreaConfig>
{
    /**
     * Creates the area definer tool.
     */
    public AreaDefiner (SceneEditor editor)
    {
        super(editor, AreaConfig.class, new AreaReference());
    }

    @Override // documentation inherited
    public void deactivate ()
    {
        // release any vertex being moved
        super.deactivate();
        if (_entry != null) {
            release();
        }
    }

    @Override // documentation inherited
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _entry = null;
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent event)
    {
        if (_editor.isControlDown()) {
            return;
        }
        int button = event.getButton();
        if (_entry != null) {
            if (button == MouseEvent.BUTTON1) { // continue placing
                insertVertex(_entry, _idx + 1);
            } else if (button == MouseEvent.BUTTON3) { // remove the vertex
                release();
            }
            return;
        }
        if (_editor.getMouseRay(_pick)) {
            SceneElement element = _editor.getView().getScene().getIntersection(
                _pick, _isect, AREA_FILTER);
            if (element instanceof Model) {
                Model model = (Model)element;
                AreaSprite sprite = (AreaSprite)model.getUserObject();
                AreaEntry entry = (AreaEntry)sprite.getEntry();
                int idx = sprite.getVertexIndex(model);
                if (idx != -1) {
                    if (button == MouseEvent.BUTTON1) { // start moving the vertex
                        _entry = entry;
                        _idx = idx;
                    } else if (button == MouseEvent.BUTTON3) {
                        removeVertices(entry, idx, 1);
                    }
                    return;
                }
                idx = sprite.getEdgeIndex(model);

                if (button == MouseEvent.BUTTON1) { // insert in between
                    insertVertex(entry, idx + 1);
                } else if (button == MouseEvent.BUTTON3) {
                    if (idx == entry.vertices.length - 1) { // last edge
                        removeVertices(entry, idx, 1);
                        removeVertices(entry, 0, 1);
                    } else { // middle edge
                        removeVertices(entry, idx, 2);
                    }
                }
                return;

            } else if (element != null && button == MouseEvent.BUTTON3) {
                // delete the entire area
                AreaSprite sprite = (AreaSprite)element.getUserObject();
                _editor.removeEntry(sprite.getEntry().getKey());
            }
        }
        ConfigReference<AreaConfig> area = _eref.getReference();
        if (button == MouseEvent.BUTTON1 && area != null && getMousePlaneIntersection(_isect)) {
            // start a new area
            _entry = new AreaEntry();
            _idx = 1;
            _entry.area = area;
            _entry.vertices = new Vertex[] { new Vertex(), new Vertex() };
            setMouseLocation(_entry.vertices[0]);
            _editor.addEntry(_entry);
        }
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        if (_entry == null || !getMousePlaneIntersection(_isect) || _editor.isControlDown()) {
            return;
        }
        _entry = (AreaEntry)_entry.clone();
        setMouseLocation(_entry.vertices[_idx]);
        _editor.updateEntry(_entry);
    }

    @Override // documentation inherited
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        if (_entry != null && _entry.getKey().equals(oentry.getKey()) && _entry != nentry) {
            _entry = null;
        }
    }

    @Override // documentation inherited
    public void entryRemoved (Entry oentry)
    {
        if (_entry != null && _entry.getKey().equals(oentry.getKey())) {
            _entry = null;
        }
    }

    /**
     * Sets the location of the specified vertex to the one indicated by the mouse cursor.
     */
    protected void setMouseLocation (Vertex vertex)
    {
        // snap to tile grid if shift not held down
        if (!_editor.isShiftDown()) {
            _isect.x = FloatMath.floor(_isect.x) + 0.5f;
            _isect.y = FloatMath.floor(_isect.y) + 0.5f;
        }
        vertex.set(_isect.x, _isect.y, _editor.getGrid().getZ());
    }

    /**
     * Releases the vertex being moved.
     */
    protected void release ()
    {
        removeVertices(_entry, _idx, 1);
        _entry = null;
    }

    /**
     * Inserts a new vertex at the specified location and starts moving it.
     */
    protected void insertVertex (AreaEntry entry, int idx)
    {
        _entry = (AreaEntry)entry.clone();
        _idx = idx;
        _entry.vertices = ArrayUtil.insert(_entry.vertices, new Vertex(), idx);
        _editor.updateEntry(_entry);
    }

    /**
     * Removes the indexed vertices from the supplied entry (removing the entry itself if it has no
     * more vertices).
     */
    protected void removeVertices (AreaEntry entry, int idx, int count)
    {
        if (entry.vertices.length <= count) {
            _editor.removeEntry(entry.getKey());
        } else {
            AreaEntry nentry = (AreaEntry)entry.clone();
            nentry.vertices = ArrayUtil.splice(nentry.vertices, idx, count);
            _editor.updateEntry(nentry);
        }
    }

    /**
     * Allows us to edit the area reference.
     */
    protected static class AreaReference extends EditableReference<AreaConfig>
    {
        /** The area reference. */
        @Editable(nullable=true)
        public ConfigReference<AreaConfig> area;

        @Override // documentation inherited
        public ConfigReference<AreaConfig> getReference ()
        {
            return area;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<AreaConfig> ref)
        {
            area = ref;
        }
    }

    /** The entry containing the vertex we're moving, if any. */
    protected AreaEntry _entry;

    /** The index of the vertex we're moving. */
    protected int _idx;

    /** Holds the result of an intersection test. */
    protected Vector3f _isect = new Vector3f();

    /** A filter that only passes area models. */
    protected static final Predicate<SceneElement> AREA_FILTER = new Predicate<SceneElement>() {
        public boolean isMatch (SceneElement element) {
            return element.getUserObject() instanceof AreaSprite;
        }
    };
}