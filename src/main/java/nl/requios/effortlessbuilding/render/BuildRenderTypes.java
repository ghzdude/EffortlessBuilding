package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.opengl.*;

import java.util.OptionalDouble;
import java.util.function.Consumer;

public class BuildRenderTypes {
	public static final RenderState.TransparencyState TRANSLUCENT_TRANSPARENCY;
	public static final RenderState.TransparencyState NO_TRANSPARENCY;

	public static final RenderState.DiffuseLightingState DIFFUSE_LIGHTING_ENABLED;
	public static final RenderState.DiffuseLightingState DIFFUSE_LIGHTING_DISABLED;

	public static final RenderState.LayerState PROJECTION_LAYERING;

	public static final RenderState.CullState CULL_DISABLED;

	public static final RenderState.AlphaState DEFAULT_ALPHA;

	public static final RenderState.WriteMaskState WRITE_TO_DEPTH_AND_COLOR;
	public static final RenderState.WriteMaskState COLOR_WRITE;

	public static final RenderType LINES;
	public static final RenderType PLANES;

	private static final int primaryTextureUnit = 0;
	private static final int secondaryTextureUnit = 2;

	static {
		TRANSLUCENT_TRANSPARENCY = ObfuscationReflectionHelper.getPrivateValue(RenderState.class, null, "TRANSLUCENT_TRANSPARENCY");
		NO_TRANSPARENCY = ObfuscationReflectionHelper.getPrivateValue(RenderState.class, null, "NO_TRANSPARENCY");

		DIFFUSE_LIGHTING_ENABLED = new RenderState.DiffuseLightingState(true);
		DIFFUSE_LIGHTING_DISABLED = new RenderState.DiffuseLightingState(false);

		PROJECTION_LAYERING = ObfuscationReflectionHelper.getPrivateValue(RenderState.class, null, "VIEW_OFFSET_Z_LAYERING");

		CULL_DISABLED = new RenderState.CullState(false);

		DEFAULT_ALPHA = new RenderState.AlphaState(0.003921569F);

		final boolean ENABLE_DEPTH_WRITING = true;
		final boolean ENABLE_COLOUR_COMPONENTS_WRITING = true;
		WRITE_TO_DEPTH_AND_COLOR = new RenderState.WriteMaskState(ENABLE_COLOUR_COMPONENTS_WRITING, ENABLE_DEPTH_WRITING);
		COLOR_WRITE = new RenderState.WriteMaskState(true, false);

		final int INITIAL_BUFFER_SIZE = 128;
		RenderType.State renderState;

		//LINES
//        RenderSystem.pushLightingAttributes();
//        RenderSystem.pushTextureAttributes();
//        RenderSystem.disableCull();
//        RenderSystem.disableLighting();
//        RenderSystem.disableTexture();
//
//        RenderSystem.enableBlend();
//        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//
//        RenderSystem.lineWidth(2);
		renderState = RenderType.State.builder()
			.setLineState(new RenderState.LineState(OptionalDouble.of(2)))
			.setLayeringState(PROJECTION_LAYERING)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setWriteMaskState(WRITE_TO_DEPTH_AND_COLOR)
			.setCullState(CULL_DISABLED)
			.createCompositeState(false);
		LINES = RenderType.create("eb_lines",
			DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, INITIAL_BUFFER_SIZE, renderState);

		renderState = RenderType.State.builder()
			.setLineState(new RenderState.LineState(OptionalDouble.of(2)))
			.setLayeringState(PROJECTION_LAYERING)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setWriteMaskState(COLOR_WRITE)
			.setCullState(CULL_DISABLED)
			.createCompositeState(false);
		PLANES = RenderType.create("eb_planes",
			DefaultVertexFormats.POSITION_COLOR, GL11.GL_TRIANGLE_STRIP, INITIAL_BUFFER_SIZE, renderState);

	}

	public static RenderType getBlockPreviewRenderType(float dissolve, BlockPos blockPos, BlockPos firstPos,
													   BlockPos secondPos, boolean red) {
//        RenderSystem.pushLightingAttributes();
//        RenderSystem.pushTextureAttributes();
//        RenderSystem.enableCull();
//        RenderSystem.enableTexture();
//        Minecraft.getInstance().textureManager.bindTexture(ShaderHandler.shaderMaskTextureLocation);
//
//        RenderSystem.enableBlend();
//        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//        RenderSystem.blendColor(1f, 1f, 1f, 0.8f);
		//end
//        ShaderHandler.releaseShader();

		//highjacking texturing state (which does nothing by default) to do my own things

		String stateName = "eb_texturing_" + dissolve + "_" + blockPos + "_" + firstPos + "_" + secondPos + "_" + red;
		RenderState.TexturingState MY_TEXTURING = new RenderState.TexturingState(stateName, () -> {
//            RenderSystem.pushLightingAttributes();
//            RenderSystem.pushTextureAttributes();

			ShaderHandler.useShader(ShaderHandler.dissolve, generateShaderCallback(dissolve, Vector3d.atLowerCornerOf(blockPos), Vector3d.atLowerCornerOf(firstPos), Vector3d.atLowerCornerOf(secondPos), blockPos == secondPos, red));
			RenderSystem.blendColor(1f, 1f, 1f, 0.8f);
		}, ShaderHandler::releaseShader);

		RenderType.State renderState = RenderType.State.builder()
			.setTextureState(new RenderState.TextureState(ShaderHandler.shaderMaskTextureLocation, false, false))
			.setTexturingState(MY_TEXTURING)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setDiffuseLightingState(DIFFUSE_LIGHTING_DISABLED)
			.setAlphaState(DEFAULT_ALPHA)
			.setCullState(new RenderState.CullState(true))
			.setLightmapState(new RenderState.LightmapState(false))
			.setOverlayState(new RenderState.OverlayState(false))
			.createCompositeState(true);
		//Unique name for every combination, otherwise it will reuse the previous one
		String name = "eb_block_previews_" + dissolve + "_" + blockPos + "_" + firstPos + "_" + secondPos + "_" + red;
		return RenderType.create(name,
			DefaultVertexFormats.BLOCK, GL11.GL_QUADS, 256, true, true, renderState);
	}

	private static Consumer<Integer> generateShaderCallback(final float dissolve, final Vector3d blockpos,
															final Vector3d firstpos, final Vector3d secondpos,
															final boolean highlight, final boolean red) {
		Minecraft mc = Minecraft.getInstance();
		return (Integer shader) -> {
			int percentileUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "dissolve");
			int highlightUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "highlight");
			int redUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "red");
			int blockposUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "blockpos");
			int firstposUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "firstpos");
			int secondposUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "secondpos");
			int imageUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "image");
			int maskUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "mask");

			RenderSystem.enableTexture();
			GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

			//mask
			ARBShaderObjects.glUniform1iARB(maskUniform, secondaryTextureUnit);
			glActiveTexture(ARBMultitexture.GL_TEXTURE0_ARB + secondaryTextureUnit);
			mc.getTextureManager().bind(ShaderHandler.shaderMaskTextureLocation);//getTexture(ShaderHandler.shaderMaskTextureLocation).bindTexture();
			//GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getTextureManager().getTexture(ShaderHandler.shaderMaskTextureLocation).getGlTextureId());

			//image
			ARBShaderObjects.glUniform1iARB(imageUniform, primaryTextureUnit);
			glActiveTexture(ARBMultitexture.GL_TEXTURE0_ARB + primaryTextureUnit);
			mc.getTextureManager().bind(AtlasTexture.LOCATION_BLOCKS);//.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).bindTexture();
			//GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getTextureManager().getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).getGlTextureId());

			//blockpos
			ARBShaderObjects.glUniform3fARB(blockposUniform, (float) blockpos.x, (float) blockpos.y, (float) blockpos.z);
			ARBShaderObjects.glUniform3fARB(firstposUniform, (float) firstpos.x, (float) firstpos.y, (float) firstpos.z);
			ARBShaderObjects.glUniform3fARB(secondposUniform, (float) secondpos.x, (float) secondpos.y, (float) secondpos.z);

			//dissolve
			ARBShaderObjects.glUniform1fARB(percentileUniform, dissolve);
			//highlight
			ARBShaderObjects.glUniform1iARB(highlightUniform, highlight ? 1 : 0);
			//red
			ARBShaderObjects.glUniform1iARB(redUniform, red ? 1 : 0);
		};
	}

	public static void glActiveTexture(int texture) {
		if (GL.getCapabilities().GL_ARB_multitexture && !GL.getCapabilities().OpenGL13) {
			ARBMultitexture.glActiveTextureARB(texture);
		} else {
			GL13.glActiveTexture(texture);
		}

	}

	private class ShaderInfo {
		float dissolve;
		Vector3d blockPos;
		Vector3d firstPos;
		Vector3d secondPos;
		boolean red;

		public ShaderInfo(float dissolve, Vector3d blockPos, Vector3d firstPos, Vector3d secondPos, boolean red) {
			this.dissolve = dissolve;
			this.blockPos = blockPos;
			this.firstPos = firstPos;
			this.secondPos = secondPos;
			this.red = red;
		}
	}

//    public static class MyTexturingState extends RenderState.TexturingState {
//
//        public float dissolve;
//        public Vector3d blockPos;
//        public Vector3d firstPos;
//        public Vector3d secondPos;
//        public boolean highlight;
//        public boolean red;
//
//        public MyTexturingState(String p_i225989_1_, float dissolve, Vector3d blockPos, Vector3d firstPos,
//                                Vector3d secondPos, boolean highlight, boolean red, Runnable p_i225989_2_, Runnable p_i225989_3_) {
//            super(p_i225989_1_, p_i225989_2_, p_i225989_3_);
//            this.dissolve = dissolve;
//            this.blockPos = blockPos;
//            this.firstPos = firstPos;
//            this.secondPos = secondPos;
//            this.highlight = highlight;
//            this.red = red;
//        }
//
//        @Override
//        public boolean equals(Object p_equals_1_) {
//            if (this == p_equals_1_) {
//                return true;
//            } else if (p_equals_1_ != null && this.getClass() == p_equals_1_.getClass()) {
//                MyTexturingState other = (MyTexturingState)p_equals_1_;
//                return this.dissolve == other.dissolve && this.blockPos == other.blockPos && this.firstPos == other.firstPos
//                       && this.secondPos == other.secondPos && this.highlight == other.highlight && this.red == other.red;
//            } else {
//                return false;
//            }
//        }
//
//        @Override
//        public int hashCode() {
//
//        }
//    }
}
