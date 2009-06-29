//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.client.sprite;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Lists;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.RandomUtil;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;

import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.ModelAdapter;
import com.threerings.opengl.model.config.AnimationConfig;
import com.threerings.opengl.model.config.ModelConfig;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.ActorSpriteConfig;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.actor.Active;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.EntryState;
import com.threerings.tudey.data.actor.HasActor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.ActorHistory;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents an active element of the scene.
 */
public class ActorSprite extends Sprite
    implements TudeySceneView.TickParticipant, ConfigUpdateListener<ActorConfig>, HasActor
{
    /**
     * The actual sprite implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Returns the sprite's floor flags.
         */
        public int getFloorFlags ()
        {
            return 0x0;
        }

        /**
         * Determines whether the implementation is hoverable.
         */
        public boolean isHoverable ()
        {
            return false;
        }

        /**
         * Determines whether the implementation is clickable.
         */
        public boolean isClickable ()
        {
            return false;
        }

        /**
         * Dispatches an event on the implementation.
         *
         * @return true if the implementation handled the event, false if it should be handled
         * elsewhere.
         */
        public boolean dispatchEvent (Event event)
        {
            return false;
        }

        /**
         * Updates the implementation with new interpolated state.
         */
        public void update (Actor actor)
        {
            // nothing by default
        }

        /**
         * Notes that the actor was just created (as opposed to just being added).
         */
        public void wasCreated ()
        {
            // nothing by default
        }

        /**
         * Notes that the actor is about to be destroyed (as opposed to just being removed).
         */
        public void willBeDestroyed ()
        {
            // nothing by default
        }

        /**
         * Notes that the occupant controlling this actor has entered.
         */
        public void occupantEntered (TudeyOccupantInfo info)
        {
            // nothing by default
        }

        /**
         * Notes that the occupant controlling this actor has left.
         */
        public void occupantLeft (TudeyOccupantInfo info)
        {
            // nothing by default
        }

        /**
         * Notes that the occupant controlling this actor has been updated.
         */
        public void occupantUpdated (TudeyOccupantInfo oinfo, TudeyOccupantInfo ninfo)
        {
            // nothing by default
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "impl";
        }
    }

    /**
     * Superclass of the original implementations.
     */
    public static class Original extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public Original (TudeyContext ctx, Scope parentScope, ActorSpriteConfig config)
        {
            this(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (ActorSpriteConfig config)
        {
            _config = config;
            _model.setConfig(getModelConfig());
        }

        @Override // documentation inherited
        public int getFloorFlags ()
        {
            return _config.floorFlags;
        }

        @Override // documentation inherited
        public void update (Actor actor)
        {
            // update the model transform
            Vector2f translation = actor.getTranslation();
            for (Model model : _attachedModels) {
                _view.getFloorTransform(
                    translation.x, translation.y, actor.getRotation(),
                    _config.floorMask, model.getLocalTransform());
                model.updateBounds();
            }
        }

        @Override // documentation inherited
        public void wasCreated ()
        {
            ((ActorSprite)getParentScope()).spawnTransientModel(_config.creationTransient);
        }

        @Override // documentation inherited
        public void willBeDestroyed ()
        {
            ((ActorSprite)getParentScope()).spawnTransientModel(_config.destructionTransient);
        }

        /**
         * Creates a new implementation without configuring it.
         */
        protected Original (TudeyContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
        }

        /**
         * Returns the configuration to use for the actor model (gives subclasses a chance to
         * adjust the result).
         */
        protected ConfigReference<ModelConfig> getModelConfig ()
        {
            return _config.model;
        }

        /** The renderer context. */
        protected TudeyContext _ctx;

        /** The sprite configuration. */
        protected ActorSpriteConfig _config;

        /** The model. */
        @Bound
        protected Model _model;

        /** The owning view. */
        @Bound
        protected TudeySceneView _view;

        /** Other models attached to this sprite. */
        @Bound
        protected HashSet<Model> _attachedModels;
    }

    /**
     * Depicts a mobile actor with optional movement animations.
     */
    public static class Moving extends Original
    {
        /**
         * Creates a new implementation.
         */
        public Moving (TudeyContext ctx, Scope parentScope, ActorSpriteConfig.Moving config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        @Override // documentation inherited
        public void setConfig (ActorSpriteConfig config)
        {
            super.setConfig(config);

            ActorSpriteConfig.Moving mconfig = (ActorSpriteConfig.Moving)config;
            _model.getLocalTransform().setScale(mconfig.scale);
            _idles = resolve(mconfig.idles);
            _idleWeights = mconfig.getIdleWeights();
            _movements = resolve(mconfig.movements);
        }

        @Override // documentation inherited
        public void update (Actor actor)
        {
            super.update(actor);

            // update the base animation
            Animation base = getBase((Mobile)actor);
            if (base != null && !base.isPlaying()) {
                base.start();
            }
        }

        /**
         * Creates a new implementation without configuring it.
         */
        protected Moving (TudeyContext ctx, Scope parentScope)
        {
            super(ctx, parentScope);
        }

        /**
         * Resolves an array of weighted animations.
         */
        protected Animation[] resolve (ActorSpriteConfig.WeightedAnimation[] weighted)
        {
            Animation[] anims = new Animation[weighted.length];
            for (int ii = 0; ii < anims.length; ii++) {
                anims[ii] = _model.createAnimation(weighted[ii].animation);
            }
            return anims;
        }

        /**
         * Resolves the animations from an array of movement sets.
         */
        protected Animation[][] resolve (ActorSpriteConfig.MovementSet[] sets)
        {
            Animation[][] anims = new Animation[sets.length][];
            for (int ii = 0; ii < anims.length; ii++) {
                anims[ii] = sets[ii].resolve(_model);
            }
            return anims;
        }

        /**
         * Returns the base animation for the actor.
         */
        protected Animation getBase (Mobile actor)
        {
            return actor.isSet(Mobile.MOVING) ? getMovement(actor) : getIdle();
        }

        /**
         * Returns a reference to the idle animation that the sprite should be playing.
         */
        protected Animation getIdle ()
        {
            return getWeightedAnimation(_idles, _idleWeights);
        }

        /**
         * Returns the movement animation appropriate to the actor's speed and direction, or
         * <code>null</code> for none.
         */
        protected Animation getMovement (Mobile actor)
        {
            ActorSpriteConfig.Moving config = (ActorSpriteConfig.Moving)_config;
            return getMovement(actor, config.scale, config.movements, _movements);
        }

        /**
         * Returns a weighted random animation (unless one of the animations is already playing,
         * in which case the method will return that animation).
         */
        protected static Animation getWeightedAnimation (Animation[] anims, float[] weights)
        {
            if (anims.length == 0) {
                return null;
            }
            for (Animation anim : anims) {
                if (anim.isPlaying()) {
                    return anim;
                }
            }
            return anims[RandomUtil.getWeightedIndex(weights)];
        }

        /**
         * Configures and returns the appropriate movement animation for the actor.
         *
         * @param scale the actor scale.
         * @param sets the movement set configs.
         * @param movements the resolved movement animations.
         */
        protected static Animation getMovement (
            Mobile actor, float scale, ActorSpriteConfig.MovementSet[] sets,
            Animation[][] movements)
        {
            // make sure we have movement animations
            int mlen = movements.length;
            if (mlen == 0) {
                return null;
            }
            float sspeed = actor.getSpeed() / scale;
            int idx = 0;
            if (mlen > 1) {
                float cdiff = Math.abs(sspeed - sets[0].speed);
                for (int ii = 1; ii < sets.length; ii++) {
                    float diff = Math.abs(sspeed - sets[ii].speed);
                    if (diff < cdiff) {
                        cdiff = diff;
                        idx = ii;
                    }
                }
            }
            float angle = FloatMath.getAngularDifference(
                actor.getDirection(), actor.getRotation()) + FloatMath.PI;
            Animation movement = movements[idx][Math.round(angle / FloatMath.HALF_PI) % 4];
            movement.setSpeed(sspeed / sets[idx].speed);
            return movement;
        }

        /** The resolved idle animations. */
        protected Animation[] _idles;

        /** The weights of the idle animations. */
        protected float[] _idleWeights;

        /** The movement animations. */
        protected Animation[][] _movements;

        /** The current idle animation. */
        protected Animation _currentIdle;
    }

    /**
     * Depicts an active actor with activity animations.
     */
    public static class Acting extends Moving
    {
        /**
         * Creates a new implementation.
         */
        public Acting (TudeyContext ctx, Scope parentScope, ActorSpriteConfig.Moving config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        @Override // documentation inherited
        public void update (Actor actor)
        {
            // update the activity
            Active active = (Active)actor;
            int started = active.getActivityStarted();
            if (started > _lastActivityStarted) {
                _lastActivityStarted = started;
                Activity previous = _activity;
                Activity next = _activities.get(active.getActivity());
                if (_activity != null) {
                    _activity.stop(next);
                }
                if ((_activity = next) != null) {
                    _activity.start(previous);
                }
            }
            if (_activity != null) {
                _activity.update();
            }

            super.update(actor);
        }

        @Override // documentation inherited
        protected Animation getBase (Mobile actor)
        {
            // activities at priority zero override default base animation
            return (_activity != null && _activity.getPriority() == 0) ?
                null : super.getBase(actor);
        }

        /**
         * Creates a new implementation without configuring it.
         */
        protected Acting (TudeyContext ctx, Scope parentScope)
        {
            super(ctx, parentScope);
        }

        /**
         * Handles an activity.
         */
        protected class Activity
        {
            /**
             * Creates a new activity.
             */
            public Activity (String... anims)
            {
                List<Animation> list = Lists.newArrayList();
                for (String anim : anims) {
                    Animation animation = _model.getAnimation(anim);
                    if (animation != null) {
                        list.add(animation);
                    }
                }
                _anims = list.toArray(new Animation[list.size()]);
            }

            /**
             * Creates a new activity.
             */
            public Activity (ConfigReference<AnimationConfig>... anims)
            {
                List<Animation> list = Lists.newArrayList();
                for (ConfigReference<AnimationConfig> anim : anims) {
                    Animation animation = (anim == null) ? null : _model.createAnimation(anim);
                    if (anim != null) {
                        list.add(animation);
                    }
                }
                _anims = list.toArray(new Animation[list.size()]);
            }

            /**
             * Creates a new activity.
             */
            public Activity (Animation... anims)
            {
                _anims = anims;
            }

            /**
             * Returns a reference to the array of animations.
             */
            public Animation[] getAnimations ()
            {
                return _anims;
            }

            /**
             * Returns the priority of the activity.
             */
            public int getPriority ()
            {
                return (_anims.length == 0) ? 0 : _anims[0].getPriority();
            }

            /**
             * Starts the activity.
             */
            public void start (Activity previous)
            {
                if (_anims.length > 0) {
                    _anims[_idx = 0].start();
                }
            }

            /**
             * Stops the activity.
             */
            public void stop (Activity next)
            {
                if (_idx < _anims.length) {
                    _anims[_idx].stop();
                }
            }

            /**
             * Updates the activity.
             */
            public void update ()
            {
                if (_idx < _anims.length - 1 && !_anims[_idx].isPlaying()) {
                    _anims[++_idx].start();
                }
            }

            /** The activity's component animations. */
            protected Animation[] _anims;

            /** The index of the current animation. */
            protected int _idx;
        }

        /** The time at which the last activity was started. */
        protected int _lastActivityStarted;

        /** The activity handlers. */
        protected IntMap<Activity> _activities = IntMaps.newHashIntMap();

        /** The current activity. */
        protected Activity _activity;
    }

    /**
     * Executes animations on the corresponding entry sprite.
     */
    public static class StatefulEntry extends Original
    {
        /**
         * Creates a new implementation.
         */
        public StatefulEntry (
            TudeyContext ctx, Scope parentScope, ActorSpriteConfig.StatefulEntry config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        @Override // documentation inherited
        public void setConfig (ActorSpriteConfig config)
        {
            super.setConfig(config);

            // find the corresponding entry sprite and its model
            EntryState estate = (EntryState)((ActorSprite)_parentScope).getActor();
            EntrySprite esprite = _view.getEntrySprite(estate.getKey());
            _entryModel = (esprite == null) ? null : esprite.getModel();
            if (_entryModel == null) {
                return;
            }

            // resolve the state animations
            ActorSpriteConfig.StatefulEntry sconfig = (ActorSpriteConfig.StatefulEntry)config;
            _states = new Animation[sconfig.states.length];
            for (int ii = 0; ii < _states.length; ii++) {
                _states[ii] = _entryModel.createAnimation(sconfig.states[ii].animation);
            }
        }

        @Override // documentation inherited
        public void update (Actor actor)
        {
            super.update(actor);

            // update the state animation
            EntryState estate = (EntryState)actor;
            int entered = estate.getStateEntered();
            if (entered > _lastStateEntered) {
                int state = estate.getState();
                Animation anim = (state < _states.length) ? _states[state] : null;
                if (anim != null) {
                    anim.start();
                    anim.tick((_view.getDelayedTime() - entered) / 1000f);
                }
                _lastStateEntered = entered;
            }
        }

        /** The model corresponding to the entry sprite. */
        protected Model _entryModel;

        /** Animations for the various states. */
        protected Animation[] _states;

        /** The time at which we entered the last state. */
        protected int _lastStateEntered;
    }

    /**
     * Creates a new actor sprite.
     */
    public ActorSprite (TudeyContext ctx, TudeySceneView view, int timestamp, Actor actor)
    {
        super(ctx, view);

        // create the advancer if the actor is client-controlled; otherwise, the history
        _actor = (Actor)actor.clone();
        if ((_advancer = _actor.maybeCreateAdvancer(ctx, view, timestamp)) == null) {
            _history = new ActorHistory(timestamp, actor, view.getBufferDelay() * 2);
        }

        // create the model and the shape
        _model = new Model(ctx);
        _model.setUserObject(this);
        _attachedModels = new HashSet<Model>();
        _attachedModels.add(_model);
        _shape = new ShapeElement(_actor.getOriginal().shape);
        _shape.setUserObject(this);

        // register as tick participant
        _view.addTickParticipant(this);

        // if the actor is created, add it immediately
        updateActor();
        if (isCreated()) {
            _view.getScene().add(_model);
            _view.getActorSpace().add(_shape);
            update();
        } else {
            _impl = null; // signifies that the actor has not yet been created
        }
    }

    @Override // documentation inherited
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Returns a reference to the advancer used to advance the state, if this is actor is
     * controlled by the client.
     */
    public ActorAdvancer getAdvancer ()
    {
        return _advancer;
    }

    /**
     * Returns a reference to the sprite's model.
     */
    public Model getModel ()
    {
        return _model;
    }

    /**
     * Returns a reference to all the sprites models.
     */
    public Set<Model> getModels ()
    {
        return _attachedModels;
    }

    /**
     * Attaches a model to this sprite.
     */
    public void attachModel (Model model)
    {
        if (_attachedModels.add(model) && isCreated()) {
            _view.getScene().add(model);
        }
    }

    /**
     * Gets and attaches a transient model to this sprite.
     */
    public void spawnAttachedTransientModel (ConfigReference<ModelConfig> ref)
    {
        if (isCreated()) {
            Model model = _view.getScene().getFromTransientPool(ref);
            model.addObserver(_transientObserver);
            _attachedModels.add(model);
            _view.getScene().add(model);
        }
    }

    /**
     * Spawns a transient model at the location of this sprite.
     */
    public void spawnTransientModel (ConfigReference<ModelConfig> ref)
    {
        if (ref != null) {
            Transform3D transform = new Transform3D(_model.getLocalTransform());
            transform.setScale(1f);
            _view.getScene().spawnTransient(ref, transform);
        }
    }

    /**
     * Detaches a model from this sprite.
     */
    public void detachModel (Model model)
    {
        if (model == _model) {
            return;
        }
        if (_attachedModels.remove(model) && isCreated()) {
            _view.getScene().remove(model);
        }
    }

    /**
     * Updates this sprite with new state.
     */
    public void update (int timestamp, Actor actor)
    {
        _history.record(timestamp, actor);
    }

    /**
     * Notes that the actor has been removed.
     */
    public void remove (int timestamp)
    {
        _removed = timestamp;
    }

    /**
     * Notes that the occupant controlling this actor has entered.
     */
    public void occupantEntered (TudeyOccupantInfo info)
    {
        if (_impl != null) {
            _impl.occupantEntered(info);
        }
    }

    /**
     * Notes that the occupant controlling this actor has left.
     */
    public void occupantLeft (TudeyOccupantInfo info)
    {
        if (_impl != null) {
            _impl.occupantLeft(info);
        }
    }

    /**
     * Notes that the occupant controlling this actor has been updated.
     */
    public void occupantUpdated (TudeyOccupantInfo oinfo, TudeyOccupantInfo ninfo)
    {
        if (_impl != null) {
            _impl.occupantUpdated(oinfo, ninfo);
        }
    }

    // documentation inherited from interface TudeySceneView.TickParticipant
    public boolean tick (int delayedTime)
    {
        // update the actor for the current time
        updateActor();

        // handle pre-creation state
        if (_impl == null) {
            if (isCreated()) {
                for (Model model : _attachedModels) {
                    _view.getScene().add(model);
                }
                _view.getActorSpace().add(_shape);

                // start off with a null implementation; that way nothing will break if updating
                // tries to access the objects we just added to the scene/actor space
                _impl = NULL_IMPLEMENTATION;
                update();
                _impl.wasCreated();
            } else {
                return true; // chill until actually created
            }
        } else {
            update();
        }

        // see if we're destroyed/removed
        if (isDestroyed()) {
            _impl.willBeDestroyed();
            dispose();
            return false;

        } else if (isRemoved()) {
            dispose();
            return false;

        } else {
            return true;
        }
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ActorConfig> event)
    {
        updateFromConfig();
        _impl.update(_actor);
    }

    @Override // documentation inherited
    public int getFloorFlags ()
    {
        return _impl.getFloorFlags();
    }

    @Override // documentation inherited
    public boolean isHoverable ()
    {
        return _impl.isHoverable();
    }

    @Override // documentation inherited
    public boolean isClickable ()
    {
        return _impl.isClickable();
    }

    @Override // documentation inherited
    public boolean dispatchEvent (Event event)
    {
        return _impl.dispatchEvent(event);
    }

    @Override // documentation inherited
    public void dispose ()
    {
        super.dispose();
        if (_impl != null) {
            _impl.dispose();
        }
        if (_config != null) {
            _config.removeListener(this);
        }
        for (Model model : _attachedModels) {
            _view.getScene().remove(model);
        }
        _view.getActorSpace().remove(_shape);
    }

    /**
     * Brings the state of the actor up-to-date with the current time.
     */
    protected void updateActor ()
    {
        if (_advancer == null) {
            _history.get(_view.getDelayedTime(), _actor);
        } else {
            _advancer.advance(_view.getAdvancedTime());
        }
    }

    /**
     * Determines whether the actor has been created.
     */
    protected boolean isCreated ()
    {
        return (_advancer == null) ? _history.isCreated(_view.getDelayedTime()) :
            _view.getAdvancedTime() >= _actor.getCreated();
    }

    /**
     * Determines whether the actor has been destroyed.
     */
    protected boolean isDestroyed ()
    {
        return (_advancer == null) ? _history.isDestroyed(_view.getDelayedTime()) :
            _view.getAdvancedTime() >= _actor.getDestroyed();
    }

    /**
     * Determines whether the actor has been removed.
     */
    protected boolean isRemoved ()
    {
        return getActorTime() >= _removed;
    }

    /**
     * Returns the time value for the actor.
     */
    protected int getActorTime ()
    {
        return (_advancer == null) ? _view.getDelayedTime() : _view.getAdvancedTime();
    }

    /**
     * Updates the configuration and implementation of the sprite.
     */
    protected void update ()
    {
        setConfig(_actor.getConfig());
        _impl.update(_actor);
        updateShape();
    }

    /**
     * Sets the configuration of this sprite.
     */
    protected void setConfig (ConfigReference<ActorConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(ActorConfig.class, ref));
    }

    /**
     * Sets the configuration of this sprite.
     */
    protected void setConfig (ActorConfig config)
    {
        if (_config == config) {
            return;
        }
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Updates the sprite to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getSpriteImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            if (_impl != null) {
                _impl.dispose();
            }
            _impl = nimpl;
        }
    }

    /**
     * Updates the shape according to the state of the actor.
     */
    protected void updateShape ()
    {
        _shape.getTransform().set(_actor.getTranslation(), _actor.getRotation(), 1f);
        _shape.setConfig(_actor.getOriginal().shape); // also updates the bounds
    }

    /** The history that we use to find interpolated actor state. */
    protected ActorHistory _history;

    /** The advancer, if this is a controlled actor. */
    protected ActorAdvancer _advancer;

    /** The "play head" actor with interpolated or advanced state. */
    @Scoped
    protected Actor _actor;

    /** The actor configuration. */
    protected ActorConfig _config;

    /** The timestamp at which the actor was removed, if any. */
    protected int _removed = Integer.MAX_VALUE;

    /** The actor model. */
    @Scoped
    protected Model _model;

    /** Other models attached to this sprite. */
    @Scoped
    protected HashSet<Model> _attachedModels;

    /** The actor's shape element. */
    protected ShapeElement _shape;

    /** The actor implementation (<code>null</code> until actually created). */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** Detaches transient models. */
    protected ModelAdapter _transientObserver = new ModelAdapter() {
        public boolean modelCompleted (Model model) {
            _attachedModels.remove(model);
            model.removeObserver(this);
            return true;
        }
    };

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
