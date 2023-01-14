package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.opengl.*;

import java.util.OptionalDouble;
import java.util.function.Consumer;

public class BuildRenderTypes extends RenderType {
	public static final RenderType LINES;
	public static final RenderType PLANES;

	private static final int primaryTextureUnit = 0;
	private static final int secondaryTextureUnit = 2;

	static {
		final LineState LINE = new LineState(OptionalDouble.of(2.0));
		final int INITIAL_BUFFER_SIZE = 128;
		RenderType.State renderState;

		//LINES
		renderState = State.builder()
				.setLineState(LINE)
				.setLayeringState(VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setTextureState(NO_TEXTURE)
				.setDepthTestState(NO_DEPTH_TEST)
				.setLightmapState(NO_LIGHTMAP)
				.setWriteMaskState(COLOR_DEPTH_WRITE)
				.setCullState(NO_CULL)
				.createCompositeState(false);
		LINES = RenderType.create("eb_lines",
				DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, INITIAL_BUFFER_SIZE, false, false, renderState);

		//PLANES
		renderState = State.builder()
				.setLineState(LINE)
				.setLayeringState(VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setTextureState(NO_TEXTURE)
				.setDepthTestState(NO_DEPTH_TEST)
				.setLightmapState(NO_LIGHTMAP)
				.setWriteMaskState(COLOR_WRITE)
				.setCullState(NO_CULL)
				.createCompositeState(false);
		PLANES = RenderType.create("eb_planes",
				DefaultVertexFormats.POSITION_COLOR, GL11.GL_TRIANGLE_STRIP, INITIAL_BUFFER_SIZE, false, false, renderState);
	}

	// Dummy constructor needed to make java happy
	public BuildRenderTypes(String p_i225992_1_, VertexFormat p_i225992_2_, int p_i225992_3_, int p_i225992_4_, boolean p_i225992_5_, boolean p_i225992_6_, Runnable p_i225992_7_, Runnable p_i225992_8_) {
		super(p_i225992_1_, p_i225992_2_, p_i225992_3_, p_i225992_4_, p_i225992_5_, p_i225992_6_, p_i225992_7_, p_i225992_8_);
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
		TexturingState MY_TEXTURING = new TexturingState(stateName, () -> {
//            RenderSystem.pushLightingAttributes();
//            RenderSystem.pushTextureAttributes();

			ShaderHandler.useShader(ShaderHandler.dissolve, generateShaderCallback(dissolve, Vector3d.atLowerCornerOf(blockPos), Vector3d.atLowerCornerOf(firstPos), Vector3d.atLowerCornerOf(secondPos), blockPos == secondPos, red));
			RenderSystem.blendColor(1f, 1f, 1f, 0.8f);
		}, ShaderHandler::releaseShader);

		RenderType.State renderState = RenderType.State.builder()
			.setTextureState(new TextureState(ShaderHandler.shaderMaskTextureLocation, false, false))
			.setTexturingState(MY_TEXTURING)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
			.setAlphaState(DEFAULT_ALPHA)
			.setCullState(new CullState(true))
			.setLightmapState(new LightmapState(false))
			.setOverlayState(new OverlayState(false))
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
}
