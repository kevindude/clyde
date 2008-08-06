//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.ARBTextureEnvDot3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.TextureUnit;

/**
 * Contains the configuration of a texture unit's environment.
 */
@EditorTypes({
    TextureEnvironmentConfig.Modulate.class, TextureEnvironmentConfig.Decal.class,
    TextureEnvironmentConfig.Blend.class, TextureEnvironmentConfig.Replace.class,
    TextureEnvironmentConfig.Add.class, TextureEnvironmentConfig.Combine.class })
public abstract class TextureEnvironmentConfig extends DeepObject
    implements Exportable
{
    /** RGB combination modes. */
    public enum RGBCombine
    {
        REPLACE(GL11.GL_REPLACE),
        MODULATE(GL11.GL_MODULATE),
        ADD(GL11.GL_ADD),
        ADD_SIGNED(ARBTextureEnvCombine.GL_ADD_SIGNED_ARB),
        INTERPOLATE(ARBTextureEnvCombine.GL_INTERPOLATE_ARB),
        SUBTRACT(ARBTextureEnvCombine.GL_SUBTRACT_ARB),
        DOT3_RGB(ARBTextureEnvDot3.GL_DOT3_RGB_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_env_dot3;
            }
        },
        DOT3_RGBA(ARBTextureEnvDot3.GL_DOT3_RGBA_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_env_dot3;
            }
        };

        public int getConstant ()
        {
            return _constant;
        }

        public boolean isSupported ()
        {
            return true;
        }

        RGBCombine (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** Alpha combination modes. */
    public enum AlphaCombine
    {
        REPLACE(GL11.GL_REPLACE),
        MODULATE(GL11.GL_MODULATE),
        ADD(GL11.GL_ADD),
        ADD_SIGNED(ARBTextureEnvCombine.GL_ADD_SIGNED_ARB),
        INTERPOLATE(ARBTextureEnvCombine.GL_INTERPOLATE_ARB),
        SUBTRACT(ARBTextureEnvCombine.GL_SUBTRACT_ARB);

        public int getConstant ()
        {
            return _constant;
        }

        AlphaCombine (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** Combine sources. */
    public enum Source
    {
        TEXTURE(GL11.GL_TEXTURE),
        CONSTANT(ARBTextureEnvCombine.GL_CONSTANT_ARB),
        PRIMARY_COLOR(ARBTextureEnvCombine.GL_PRIMARY_COLOR_ARB),
        PREVIOUS(ARBTextureEnvCombine.GL_PREVIOUS_ARB);

        public int getConstant ()
        {
            return _constant;
        }

        Source (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** RGB combination operands. */
    public enum RGBOperand
    {
        SRC_COLOR(GL11.GL_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(GL11.GL_ONE_MINUS_SRC_COLOR),
        SRC_ALPHA(GL11.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA);

        public int getConstant ()
        {
            return _constant;
        }

        RGBOperand (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** Alpha combination operands. */
    public enum AlphaOperand
    {
        SRC_ALPHA(GL11.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA);

        public int getConstant ()
        {
            return _constant;
        }

        AlphaOperand (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** The combination scale values. */
    public enum Scale
    {
        ONE(1f),
        TWO(2f),
        FOUR(4f);

        public float getValue ()
        {
            return _value;
        }

        Scale (float value)
        {
            _value = value;
        }

        protected float _value;
    }

    /**
     * The modulate environment mode.
     */
    public static class Modulate extends TextureEnvironmentConfig
    {
        @Override // documentation inherited
        public int getMode ()
        {
            return GL11.GL_MODULATE;
        }
    }

    /**
     * The decal environment mode.
     */
    public static class Decal extends TextureEnvironmentConfig
    {
        @Override // documentation inherited
        public int getMode ()
        {
            return GL11.GL_DECAL;
        }
    }

    /**
     * The blend environment mode.
     */
    public static class Blend extends TextureEnvironmentConfig
    {
        @Override // documentation inherited
        public int getMode ()
        {
            return GL11.GL_BLEND;
        }
    }

    /**
     * The replace environment mode.
     */
    public static class Replace extends TextureEnvironmentConfig
    {
        @Override // documentation inherited
        public int getMode ()
        {
            return GL11.GL_REPLACE;
        }
    }

    /**
     * The add environment mode.
     */
    public static class Add extends TextureEnvironmentConfig
    {
        @Override // documentation inherited
        public boolean isSupported ()
        {
            return GLContext.getCapabilities().GL_ARB_texture_env_add;
        }

        @Override // documentation inherited
        public int getMode ()
        {
            return GL11.GL_ADD;
        }
    }

    /**
     * The combine environment mode.
     */
    public static class Combine extends TextureEnvironmentConfig
    {
        /** The RGB combine mode. */
        @Editable(category="rgb")
        public RGBCombine rgbCombine = RGBCombine.MODULATE;

        /** The first RGB parameter. */
        @Editable(category="rgb")
        public RGBParam rgbParam0 = new RGBParam(Source.TEXTURE, RGBOperand.SRC_COLOR);

        /** The second RGB parameter. */
        @Editable(category="rgb")
        public RGBParam rgbParam1 = new RGBParam(Source.PREVIOUS, RGBOperand.SRC_COLOR);

        /** The third RGB parameter. */
        @Editable(category="rgb")
        public RGBParam rgbParam2 = new RGBParam(Source.CONSTANT, RGBOperand.SRC_ALPHA);

        /** The RGB combine scale. */
        @Editable(category="rgb")
        public Scale rgbScale = Scale.ONE;

        /** The alpha combine mode. */
        @Editable(category="alpha")
        public AlphaCombine alphaCombine = AlphaCombine.MODULATE;

        /** The first alpha parameter. */
        @Editable(category="alpha")
        public AlphaParam alphaParam0 = new AlphaParam(Source.TEXTURE, AlphaOperand.SRC_ALPHA);

        /** The first alpha parameter. */
        @Editable(category="alpha")
        public AlphaParam alphaParam1 = new AlphaParam(Source.PREVIOUS, AlphaOperand.SRC_ALPHA);

        /** The first alpha parameter. */
        @Editable(category="alpha")
        public AlphaParam alphaParam2 = new AlphaParam(Source.CONSTANT, AlphaOperand.SRC_ALPHA);

        /** The alpha combine scale. */
        @Editable(category="alpha")
        public Scale alphaScale = Scale.ONE;

        @Override // documentation inherited
        public boolean isSupported ()
        {
            return GLContext.getCapabilities().GL_ARB_texture_env_combine;
        }

        @Override // documentation inherited
        public void configure (TextureUnit unit)
        {
            super.configure(unit);
            unit.rgbCombine = rgbCombine.getConstant();
            unit.alphaCombine = alphaCombine.getConstant();
            unit.rgbSource0 = rgbParam0.source.getConstant();
            unit.rgbSource1 = rgbParam1.source.getConstant();
            unit.rgbSource2 = rgbParam2.source.getConstant();
            unit.alphaSource0 = alphaParam0.source.getConstant();
            unit.alphaSource1 = alphaParam1.source.getConstant();
            unit.alphaSource2 = alphaParam2.source.getConstant();
            unit.rgbOperand0 = rgbParam0.operand.getConstant();
            unit.rgbOperand1 = rgbParam1.operand.getConstant();
            unit.rgbOperand2 = rgbParam2.operand.getConstant();
            unit.alphaOperand0 = alphaParam0.operand.getConstant();
            unit.alphaOperand1 = alphaParam1.operand.getConstant();
            unit.alphaOperand2 = alphaParam2.operand.getConstant();
            unit.rgbScale = rgbScale.getValue();
            unit.alphaScale = alphaScale.getValue();
        }

        @Override // documentation inherited
        public int getMode ()
        {
            return ARBTextureEnvCombine.GL_COMBINE_ARB;
        }
    }

    /**
     * A single rgb combine parameter.
     */
    public static class RGBParam extends DeepObject
        implements Exportable
    {
        /** The source of the operand. */
        @Editable(hgroup="p")
        public Source source = Source.TEXTURE;

        /** The operand itself. */
        @Editable(hgroup="p")
        public RGBOperand operand = RGBOperand.SRC_COLOR;

        public RGBParam (Source source, RGBOperand operand)
        {
            this.source = source;
            this.operand = operand;
        }

        public RGBParam ()
        {
        }
    }

    /**
     * A single alpha source/operand pair.
     */
    public static class AlphaParam extends DeepObject
        implements Exportable
    {
        /** The source of the operand. */
        @Editable(hgroup="p")
        public Source source = Source.TEXTURE;

        /** The operand itself. */
        @Editable(hgroup="p")
        public AlphaOperand operand = AlphaOperand.SRC_ALPHA;

        public AlphaParam (Source source, AlphaOperand operand)
        {
            this.source = source;
            this.operand = operand;
        }

        public AlphaParam ()
        {
        }
    }

    /** The environment color. */
    @Editable(mode="alpha", hgroup="cl")
    public Color4f color = new Color4f(0f, 0f, 0f, 0f);

    /** The texture level of detail bias. */
    @Editable(step=0.01, hgroup="cl")
    public float lodBias;

    /**
     * Checks whether the mode is supported.
     */
    public boolean isSupported ()
    {
        return true;
    }

    /**
     * Configures the supplied texture unit with this environment's parameters.
     */
    public void configure (TextureUnit unit)
    {
        unit.envMode = getMode();
        unit.envColor.set(color);
        unit.lodBias = lodBias;
    }

    /**
     * Returns the OpenGL constant for the mode.
     */
    public abstract int getMode ();
}
