//
// $Id$

package com.threerings.tudey.client;

import com.samskivert.util.HashIntMap;

import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.math.Ray;
import com.threerings.math.Vector3f;

import com.threerings.opengl.GlView;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.scene.HashScene;
import com.threerings.opengl.scene.SceneElement;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

import com.samskivert.util.Predicate;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;

import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.Sprite;

import static com.threerings.tudey.Log.*;

/**
 * Displays a view of a Tudey scene.
 */
public class TudeySceneView extends SimpleScope
{
    /**
     * Creates a new scene view.
     */
    public TudeySceneView (GlContext ctx, Scope parentScope)
    {
        super(parentScope);
        _ctx = ctx;
        _scene = new HashScene(ctx, 64f, 6);
        _scene.setParentScope(this);
    }

    /**
     * Sets the scene model for this view.
     */
    public void setSceneModel (TudeySceneModel model)
    {
        // clear out the existing sprites
        for (EntrySprite sprite : _entrySprites.values()) {
            sprite.dispose();
        }
        _entrySprites.clear();

        // create the new sprites
         _sceneModel = model;
        for (Entry entry : _sceneModel.getEntries()) {
            addEntrySprite(entry);
        }
    }

    /**
     * Notes that an entry has been added to the scene.
     */
    public void entryAdded (Entry entry)
    {
        addEntrySprite(entry);
    }

    /**
     * Notes that an entry has been updated within the scene.
     */
    public void entryUpdated (Entry entry)
    {
        EntrySprite sprite = _entrySprites.get(entry.getId());
        if (sprite != null) {
            sprite.update(entry);
        } else {
            log.warning("Missing sprite to update.", "entry", entry);
        }
    }

    /**
     * Notes that an entry has been removed from the scene.
     */
    public void entryRemoved (int id)
    {
        EntrySprite sprite = _entrySprites.remove(id);
        if (sprite != null) {
            sprite.dispose();
        } else {
            log.warning("Missing entry sprite to remove.", "id", id);
        }
    }

    /**
     * Checks for an intersection between the provided ray and the sprites in the scene.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first sprite intersected by the ray, or <code>null</code> for
     * none.
     */
    public Sprite getIntersection (Ray ray, Vector3f location)
    {
        Predicate<Sprite> filter = Predicate.trueInstance();
        return getIntersection(ray, location, filter);
    }

    /**
     * Checks for an intersection between the provided ray and the sprites in the scene.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first sprite intersected by the ray, or <code>null</code> for
     * none.
     */
    public Sprite getIntersection (Ray ray, Vector3f location, final Predicate<Sprite> filter)
    {
        Model model = (Model)_scene.getIntersection(ray, location, new Predicate<SceneElement>() {
            public boolean isMatch (SceneElement element) {
                Object userObject = (element instanceof Model) ?
                    ((Model)element).getUserObject() : null;
                return userObject instanceof Sprite && filter.isMatch((Sprite)userObject);
            }
        });
        return (model == null) ? null : (Sprite)model.getUserObject();
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "view";
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        _scene.tick(elapsed);
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        _scene.enqueue();
    }

    /**
     * Adds a sprite for the specified entry.
     */
    protected void addEntrySprite (Entry entry)
    {
        _entrySprites.put(entry.getId(), entry.createSprite(_ctx, this));
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The OpenGL scene. */
    @Scoped
    protected HashScene _scene;

    /** The scene model. */
    protected TudeySceneModel _sceneModel;

    /** Sprites corresponding to the scene entries. */
    protected HashIntMap<EntrySprite> _entrySprites = new HashIntMap<EntrySprite>();
}