//
// $Id$

package com.threerings.openal;

import java.io.File;
import java.io.IOException;

import org.lwjgl.openal.AL10;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.openal.SoundGroup;
import com.threerings.openal.Source;
import com.threerings.openal.config.SounderConfig;
import com.threerings.openal.config.SounderConfig.QueuedFile;
import com.threerings.openal.util.AlContext;

import static com.threerings.openal.Log.*;

/**
 * Plays a sound.
 */
public class Sounder extends SimpleScope
    implements ConfigUpdateListener<SounderConfig>
{
    /**
     * The actual sounder implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (AlContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
        }

        /**
         * Starts playing the sound.
         */
        public abstract void start ();

        /**
         * Stops the sound.
         */
        public abstract void stop ();

        /**
         * Updates the sound.
         */
        public void update ()
        {
            // nothing by default
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "impl";
        }

        /**
         * (Re)configures the implementation.
         */
        protected void setConfig (SounderConfig.Original config)
        {
            _config = config;
        }

        /** The application context. */
        protected AlContext _ctx;

        /** The implementation configuration. */
        protected SounderConfig.Original _config;

        /** The sound transform. */
        @Bound
        protected Transform3D _transform;
    }

    /**
     * Plays a sound clip.
     */
    public static class Clip extends Implementation
    {
        /**
         * Creates a new clip implementation.
         */
        public Clip (AlContext ctx, Scope parentScope, SounderConfig.Clip config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Clip config)
        {
            super.setConfig(_config = config);

            // resolve the group and use it to obtain a sound reference
            SoundGroup group = ScopeUtil.resolve(
                _parentScope, "soundGroup", null, SoundGroup.class);
            _sound = (group == null) ? null : group.getSound(config.file);
            if (_sound == null) {
                return;
            }

            // configure the sound
            _sound.setGain(config.gain);
            _sound.setSourceRelative(config.sourceRelative);
            _sound.setMinGain(config.minGain);
            _sound.setMaxGain(config.maxGain);
            _sound.setReferenceDistance(config.referenceDistance);
            _sound.setRolloffFactor(config.rolloffFactor);
            _sound.setMaxDistance(config.maxDistance);
            _sound.setPitch(config.pitch);
            _sound.setConeInnerAngle(config.coneInnerAngle);
            _sound.setConeOuterAngle(config.coneOuterAngle);
            _sound.setConeOuterGain(config.coneOuterGain);
        }

        @Override // documentation inherited
        public void start ()
        {
            if (_sound != null) {
                updateSoundTransform();
                _sound.play(null, _config.loop);
            }
        }

        @Override // documentation inherited
        public void stop ()
        {
            if (_sound != null) {
                _sound.stop();
            }
        }

        @Override // documentation inherited
        public void update ()
        {
            updateSoundTransform();
        }

        /**
         * Updates the position and direction of the sound.
         */
        protected void updateSoundTransform ()
        {
            _transform.update(Transform3D.RIGID);
            Vector3f translation = _transform.getTranslation();
            _sound.setPosition(translation.x, translation.y, translation.z);
            if (_config.directional) {
                _transform.getRotation().transformUnitX(_direction);
                _sound.setDirection(_direction.x, _direction.y, _direction.z);
            }
        }

        /** The implementation configuration. */
        protected SounderConfig.Clip _config;

        /** The sound. */
        protected Sound _sound;

        /** Holds the direction vector. */
        protected Vector3f _direction = new Vector3f();
    }

    /**
     * Plays a sound stream.
     */
    public static class Stream extends Implementation
    {
        /**
         * Creates a new clip implementation.
         */
        public Stream (AlContext ctx, Scope parentScope, SounderConfig.Stream config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Stream config)
        {
            super.setConfig(_config = config);
        }

        @Override // documentation inherited
        public void start ()
        {
            if (_stream != null) {
                _stream.dispose();
            }
            QueuedFile[] queue = _config.queue;
            try {
                _stream = createStream(queue[0]);
            } catch (IOException e) {
                log.warning("Error opening stream.", "file", queue[0].file, e);
                _stream = null;
                return;
            }
            ResourceManager rsrcmgr = _ctx.getResourceManager();
            for (int ii = 1; ii < queue.length; ii++) {
                QueuedFile queued = queue[ii];
                if (queued.file != null) {
                    _stream.queueFile(rsrcmgr.getResourceFile(queued.file), queued.loop);
                }
            }
            _stream.setGain(_config.gain);

            // configure the stream source
            Source source = _stream.getSource();
            source.setSourceRelative(_config.sourceRelative);
            source.setMinGain(_config.minGain);
            source.setMaxGain(_config.maxGain);
            source.setReferenceDistance(_config.referenceDistance);
            source.setRolloffFactor(_config.rolloffFactor);
            source.setMaxDistance(_config.maxDistance);
            source.setPitch(_config.pitch);
            source.setConeInnerAngle(_config.coneInnerAngle);
            source.setConeOuterAngle(_config.coneOuterAngle);
            source.setConeOuterGain(_config.coneOuterGain);

            // start playing
            if (_config.fadeIn > 0f) {
                _stream.fadeIn(_config.fadeIn);
            } else {
                _stream.play();
            }
        }

        @Override // documentation inherited
        public void stop ()
        {
            if (_stream != null) {
                if (_config.fadeOut > 0f) {
                    _stream.fadeOut(_config.fadeOut, true);
                } else {
                    _stream.dispose();
                }
                _stream = null;
            }
        }

        /**
         * Creates the file stream.
         */
        protected FileStream createStream (QueuedFile queued)
            throws IOException
        {
            return new FileStream(
                _ctx.getSoundManager(), _ctx.getResourceManager().getResourceFile(queued.file),
                    queued.loop) {
                protected void update (float time) {
                    super.update(time);
                    if (_state == AL10.AL_PLAYING) {
                        updateSoundTransform();
                    }
                }
                protected void updateSoundTransform () {
                    _transform.update(Transform3D.RIGID);
                    Vector3f translation = _transform.getTranslation();
                    _source.setPosition(translation.x, translation.y, translation.z);
                    if (_config.directional) {
                        _transform.getRotation().transformUnitX(_direction);
                        _source.setDirection(_direction.x, _direction.y, _direction.z);
                    }
                }
                protected Vector3f _direction = new Vector3f();
            };
        }

        /** The implementation configuration. */
        protected SounderConfig.Stream _config;

        /** The stream. */
        protected FileStream _stream;
    }

    /**
     * Creates a new sounder with a null configuration.
     *
     * @param transform a reference to the sound transform to use.
     */
    public Sounder (AlContext ctx, Scope parentScope, Transform3D transform)
    {
        this(ctx, parentScope, transform, (SounderConfig)null);
    }

    /**
     * Creates a new sounder with the referenced configuration.
     *
     * @param transform a reference to the sound transform to use.
     */
    public Sounder (
        AlContext ctx, Scope parentScope, Transform3D transform,
        ConfigReference<SounderConfig> ref)
    {
        this(ctx, parentScope, transform,
            ctx.getConfigManager().getConfig(SounderConfig.class, ref));
    }

    /**
     * Creates a new sounder with the given configuration.
     *
     * @param transform a reference to the sound transform to use.
     */
    public Sounder (AlContext ctx, Scope parentScope, Transform3D transform, SounderConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        _transform = transform;
        setConfig(config);
    }

    /**
     * Sets the configuration of this sounder.
     */
    public void setConfig (ConfigReference<SounderConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(SounderConfig.class, ref));
    }

    /**
     * Sets the configuration of this sounder.
     */
    public void setConfig (SounderConfig config)
    {
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Starts playing the sound.
     */
    public void start ()
    {
        resetEpoch();
        _impl.start();
    }

    /**
     * Stops playing the sound.
     */
    public void stop ()
    {
        _impl.stop();
    }

    /**
     * Updates the sound for the current frame.  Invocation of this method is not guaranteed;
     * in particular, while {@link com.threerings.opengl.scene.config.ViewerEffectConfig.Sound}
     * calls this method, {@link com.threerings.opengl.model.config.ActionConfig.PlaySound}
     * does not.
     */
    public void update ()
    {
        _impl.update();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<SounderConfig> event)
    {
        updateFromConfig();
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "sounder";
    }

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        resetEpoch();
    }

    @Override // documentation inherited
    public void dispose ()
    {
        super.dispose();
        if (_config != null) {
            _config.removeListener(this);
        }
    }

    /**
     * Updates the sounder to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getSounderImplementation(_ctx, this, _impl);
        _impl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
    }

    /**
     * Resets the epoch value to the current time.
     */
    protected void resetEpoch ()
    {
        _epoch.value = _now.value;
    }

    /** The application context. */
    protected AlContext _ctx;

    /** The sound transform reference. */
    @Scoped
    protected Transform3D _transform;

    /** The configuration of this sounder. */
    protected SounderConfig _config;

    /** The sounder implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** The container for the current time. */
    @Bound
    protected MutableLong _now = new MutableLong(System.currentTimeMillis());

    /** A container for the sound epoch. */
    @Scoped
    protected MutableLong _epoch = new MutableLong(System.currentTimeMillis());

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null, null) {
        public void start () { }
        public void stop () { }
    };
}