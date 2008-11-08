//
// $Id$

package com.threerings.opengl.renderer.util;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.SoftCache;

import com.threerings.util.ArrayKey;

import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.CullState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TextureState;

/**
 * Contains methods to create snippets of GLSL shader code.
 */
public class SnippetUtil
{
    /**
     * Creates a fog parameter snippet.
     */
    public static void getFogParam (
        String name, String eyeVertex, String fogParam, RenderState[] states,
        ArrayList<String> defs)
    {
        FogState state = (FogState)states[RenderState.FOG_STATE];
        int mode = (state == null) ? -1 : state.getFogMode();
        if (mode == -1) {
            defs.add("DECLARE_" + name);
            defs.add("SET_" + name);
            return;
        }
        defs.add("DECLARE_" + name + " varying float " + fogParam + ";");
        ArrayKey key = new ArrayKey(name, eyeVertex, fogParam, mode);
        String def = _fogParams.get(key);
        if (def == null) {
            _fogParams.put(key, def = createFogParamDef(name, eyeVertex, fogParam, mode));
        }
        defs.add(def);
    }

    /**
     * Creates a fog blend snippet.
     */
    public static void getFogBlend (
        String name, String fogParam, RenderState[] states, ArrayList<String> defs)
    {
        FogState state = (FogState)states[RenderState.FOG_STATE];
        int mode = (state == null) ? -1 : state.getFogMode();
        if (mode == -1) {
            defs.add("DECLARE_" + name);
            defs.add("BLEND_" + name);
            return;
        }
        defs.add("DECLARE_" + name + " varying float " + fogParam + ";");
        defs.add("BLEND_" + name + " gl_FragColor.rgb = mix(gl_Fog.color.rgb, gl_FragColor.rgb, " +
            fogParam + ");");
    }

    /**
     * Retrieves a tex coord snippet.
     */
    public static void getTexCoord (
        String name, String eyeVertex, String eyeNormal, RenderState[] states,
        ArrayList<String> defs)
    {
        TextureState state = (TextureState)states[RenderState.TEXTURE_STATE];
        TextureUnit[] units = (state == null) ? null : state.getUnits();
        ArrayKey key = createTexCoordKey(name, eyeVertex, eyeNormal, units);
        String def = _texCoords.get(key);
        if (def == null) {
            _texCoords.put(key, def = createTexCoordDef(name, eyeVertex, eyeNormal, units));
        }
        defs.add(def);
    }

    /**
     * Creates a vertex lighting snippet.
     */
    public static void getVertexLighting (
        String name, String eyeVertex, String eyeNormal, RenderState[] states,
        ArrayList<String> defs)
    {
        CullState cstate = (CullState)states[RenderState.CULL_STATE];
        LightState lstate = (LightState)states[RenderState.LIGHT_STATE];
        int cullFace = (cstate == null) ? -1 : cstate.getCullFace();
        Light[] lights = (lstate == null) ? null : lstate.getLights();
        ArrayKey key = createVertexLightingKey(name, eyeVertex, eyeNormal, cullFace, lights);
        String def = _vertexLighting.get(key);
        if (def == null) {
            _vertexLighting.put(key, def = createVertexLightingDef(
                name, eyeVertex, eyeNormal, cullFace, lights));
        }
        defs.add(def);
    }

    /**
     * Creates and returns the definition for the supplied fog parameters.
     */
    protected static String createFogParamDef (
        String name, String eyeVertex, String fogParam, int mode)
    {
        StringBuilder buf = new StringBuilder();
        switch(mode) {
            case GL11.GL_LINEAR:
                buf.append(fogParam + " = clamp((gl_Fog.end + " + eyeVertex + ".z) * gl_Fog.scale");
                break;
            case GL11.GL_EXP:
                buf.append(fogParam + " = clamp(exp(gl_Fog.density * " + eyeVertex + ".z)");
                break;
            case GL11.GL_EXP2:
                buf.append("float f = gl_Fog.density * " + eyeVertex + ".z; ");
                buf.append(fogParam + " = clamp(exp(-f*f)");
                break;
        }
        buf.append(", 0.0, 1.0); ");
        return "SET_" + name + " { " + buf + "}";
    }

    /**
     * Creates and returns a key for the supplied tex coord parameters.
     */
    protected static ArrayKey createTexCoordKey (
        String name, String eyeVertex, String eyeNormal, TextureUnit[] units)
    {
        int[][] genModes = new int[units == null ? 0 : units.length][];
        for (int ii = 0; ii < genModes.length; ii++) {
            TextureUnit unit = units[ii];
            genModes[ii] = (unit == null) ? null :
                new int[] { unit.genModeS, unit.genModeT, unit.genModeR, unit.genModeQ };
        }
        return new ArrayKey(name, eyeVertex, eyeNormal, genModes);
    }

    /**
     * Creates and returns the definition for the supplied tex coord parameters.
     */
    protected static String createTexCoordDef (
        String name, String eyeVertex, String eyeNormal, TextureUnit[] units)
    {
        StringBuilder buf = new StringBuilder();
        if (units != null) {
            if (anySphereMapped(units)) {
                buf.append("vec3 f = reflect(normalize(" + eyeVertex + ".xyz), " +
                    eyeNormal + ".xyz); ");
                buf.append("float z1 = f.z + 1.0; ");
                buf.append("float rm = 0.5 / sqrt(f.x*f.x + f.y*f.y + (z1*z1)); ");
                buf.append("vec4 sphereTexCoord = vec4(f.x*rm + 0.5, f.y*rm + 0.5, 0.0, 1.0); ");
            }
            for (int ii = 0; ii < units.length; ii++) {
                createTexCoordUnit(ii, units[ii], eyeVertex, buf);
            }
        }
        return name + " { " + buf + "}";
    }

    /**
     * Determines whether any of the specified texture units use sphere-map texture coordinate
     * generation.
     */
    protected static boolean anySphereMapped (TextureUnit[] units)
    {
        for (TextureUnit unit : units) {
            if (unit != null && unit.anyGenModesEqual(GL11.GL_SPHERE_MAP)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Appends the code for a single texture coordinate unit.
     */
    protected static void createTexCoordUnit (
        int idx, TextureUnit unit, String eyeVertex, StringBuilder buf)
    {
        if (unit == null) {
            return;
        }
        if (unit.genModeS == GL11.GL_SPHERE_MAP && unit.genModeT == GL11.GL_SPHERE_MAP) {
            buf.append("gl_TexCoord[" + idx + "] = sphereTexCoord; ");
        } else if (unit.allGenModesEqual(-1)) {
            buf.append("gl_TexCoord[" + idx + "] = gl_TextureMatrix[" +
                idx + "] * gl_MultiTexCoord" + idx + "; ");
        } else {
            if (unit.anyGenModesEqual(-1)) {
                buf.append("vec4 texCoord" + idx + " = gl_TextureMatrix[" +
                    idx + "] * gl_MultiTexCoord" + idx + "; ");
            }
            buf.append("gl_TexCoord[" + idx + "] = vec4(");
            buf.append(createTexCoordElement(idx, 's', unit.genModeS, eyeVertex) + ", ");
            buf.append(createTexCoordElement(idx, 't', unit.genModeT, eyeVertex) + ", ");
            buf.append(createTexCoordElement(idx, 'r', unit.genModeR, eyeVertex) + ", ");
            buf.append(createTexCoordElement(idx, 'q', unit.genModeQ, eyeVertex) + "); ");
        }
    }

    /**
     * Returns the code for a single texture coordinate element.
     */
    protected static String createTexCoordElement (
        int idx, char element, int mode, String eyeVertex)
    {
        switch (mode) {
            case GL11.GL_SPHERE_MAP:
                return "sphereTexCoord." + element;
            case GL11.GL_OBJECT_LINEAR:
                return "dot(gl_ObjectPlane" + Character.toUpperCase(element) +
                    "[" + idx + "], gl_Vertex)";
            case GL11.GL_EYE_LINEAR:
                return "dot(gl_EyePlane" + Character.toUpperCase(element) +
                    "[" + idx + "], " + eyeVertex + ")";
            default:
                return "(gl_TextureMatrix[" + idx + "] * gl_MultiTexCoord" + idx + ")." + element;
        }
    }

    /**
     * Creates and returns a key for the supplied vertex lighting parameters.
     */
    protected static ArrayKey createVertexLightingKey (
        String name, String eyeVertex, String eyeNormal, int cullFace, Light[] lights)
    {
        Light.Type[] types;
        if (lights != null) {
            types = new Light.Type[lights.length];
            for (int ii = 0; ii < types.length; ii++) {
                Light light = lights[ii];
                types[ii] = (light == null) ? null : light.getType();
            }
        } else {
            types = null;
        }
        return new ArrayKey(name, eyeVertex, eyeNormal, cullFace, types);
    }

    /**
     * Creates and returns the definition for the supplied vertex lighting parameters.
     */
    protected static String createVertexLightingDef (
        String name, String eyeVertex, String eyeNormal, int cullFace, Light[] lights)
    {
        StringBuilder buf = new StringBuilder();
        if (cullFace == -1 || cullFace == GL11.GL_BACK) {
            buf.append(createVertexLightingSide("Front", eyeVertex, eyeNormal, lights));
        }
        if (cullFace == -1 || cullFace == GL11.GL_FRONT) {
            buf.append(createVertexLightingSide("Back", eyeVertex, eyeNormal, lights));
        }
        return name + " { " + buf + "}";
    }

    /**
     * Creates and returns the expression for a single vertex-lit side.
     */
    protected static String createVertexLightingSide (
        String side, String eyeVertex, String eyeNormal, Light[] lights)
    {
        String variable = "gl_" + side + "Color";
        if (lights == null) {
            return variable + " = gl_Color; ";
        }
        StringBuilder buf = new StringBuilder();
        buf.append(variable + " = gl_" + side + "LightModelProduct.sceneColor; ");
        for (int ii = 0; ii < lights.length; ii++) {
            Light light = lights[ii];
            if (light == null) {
                continue;
            }
            switch (light.getType()) {
                case DIRECTIONAL:
                    addDirectionalLight(ii, side, eyeNormal, buf);
                    break;
                case POINT:
                    addPointLight(ii, side, eyeVertex, eyeNormal, buf);
                    break;
                case SPOT:
                    addSpotLight(ii, side, eyeVertex, eyeNormal, buf);
                    break;
            }
        }
        return buf.toString();
    }

    /**
     * Adds the influence of a directional light.
     */
    protected static void addDirectionalLight (
        int idx, String side, String eyeNormal, StringBuilder buf)
    {
        buf.append("gl_" + side + "Color += gl_" + side + "LightProduct[" + idx +
            "].ambient + gl_" + side + "LightProduct[" + idx + "].diffuse * max(dot(" +
            eyeNormal + ", gl_LightSource[" + idx + "].position), 0.0); ");
    }

    /**
     * Adds the influence of a point light.
     */
    protected static void addPointLight (
        int idx, String side, String eyeVertex, String eyeNormal, StringBuilder buf)
    {
    }

    /**
     * Adds the influence of a spot light.
     */
    protected static void addSpotLight (
        int idx, String side, String eyeVertex, String eyeNormal, StringBuilder buf)
    {
    }

    /** Cached fog param snippets. */
    protected static SoftCache<ArrayKey, String> _fogParams = new SoftCache<ArrayKey, String>();

    /** Cached tex coord snippets. */
    protected static SoftCache<ArrayKey, String> _texCoords = new SoftCache<ArrayKey, String>();

    /** Cached vertex lighting snippets. */
    protected static SoftCache<ArrayKey, String> _vertexLighting =
        new SoftCache<ArrayKey, String>();
}