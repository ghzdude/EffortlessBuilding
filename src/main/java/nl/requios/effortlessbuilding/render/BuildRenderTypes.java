package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.*;

import java.util.OptionalDouble;
import java.util.function.Consumer;

public class BuildRenderTypes extends RenderType {
	public static final RenderType LINES;
	public static final RenderType PLANES;

	private static final int primaryTextureUnit = 0;
	private static final int secondaryTextureUnit = 2;

	static {
		final LineStateShard LINE = new LineStateShard(OptionalDouble.of(2.0));
		final int INITIAL_BUFFER_SIZE = 128;
		RenderType.CompositeState renderState;

		//LINES
		renderState = CompositeState.builder()
				.setLineState(LINE)
				.setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
				.setLayeringState(VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setTextureState(RenderStateShard.NO_TEXTURE)
				.setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
				.setLightmapState(RenderStateShard.NO_LIGHTMAP)
				.setWriteMaskState(COLOR_DEPTH_WRITE)
				.setCullState(RenderStateShard.NO_CULL)
				.createCompositeState(false);
		LINES = RenderType.create("eb_lines",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.LINES, INITIAL_BUFFER_SIZE, false, false, renderState);

		//PLANES
		renderState = CompositeState.builder()
				.setLineState(LINE)
				.setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
				.setLayeringState(VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setTextureState(RenderStateShard.NO_TEXTURE)
				.setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
				.setLightmapState(RenderStateShard.NO_LIGHTMAP)
				.setWriteMaskState(COLOR_WRITE)
				.setCullState(RenderStateShard.NO_CULL)
				.createCompositeState(false);
		PLANES = RenderType.create("eb_planes",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP, INITIAL_BUFFER_SIZE, false, false, renderState);
	}

	public BuildRenderTypes(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
		super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
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
		RenderStateShard.TexturingStateShard MY_TEXTURING = new RenderStateShard.TexturingStateShard(stateName, () -> {
//            RenderSystem.pushLightingAttributes();
//            RenderSystem.pushTextureAttributes();

			ShaderHandler.useShader(ShaderHandler.dissolve, generateShaderCallback(dissolve, Vec3.atLowerCornerOf(blockPos), Vec3.atLowerCornerOf(firstPos), Vec3.atLowerCornerOf(secondPos), blockPos == secondPos, red));
			RenderSystem.setShaderColor(1f, 1f, 1f, 0.8f);
		}, ShaderHandler::releaseShader);

		RenderType.CompositeState renderState = RenderType.CompositeState.builder()
				.setShaderState(RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER)
				.setTextureState(new RenderStateShard.TextureStateShard(ShaderHandler.shaderMaskTextureLocation, false, false))
				.setTexturingState(MY_TEXTURING)
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				//TODO 1.17
	//			.setDiffuseLightingState(DIFFUSE_LIGHTING_DISABLED)
	//			.setAlphaState(DEFAULT_ALPHA)
				.setCullState(new RenderStateShard.CullStateShard(true))
				.setLightmapState(new RenderStateShard.LightmapStateShard(false))
				.setOverlayState(new RenderStateShard.OverlayStateShard(false))
				.createCompositeState(true);
		//Unique name for every combination, otherwise it will reuse the previous one
		String name = "eb_block_previews_" + dissolve + "_" + blockPos + "_" + firstPos + "_" + secondPos + "_" + red;
		return RenderType.create(name,
			DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, true, true, renderState);
	}

	private static Consumer<Integer> generateShaderCallback(final float dissolve, final Vec3 blockpos,
															final Vec3 firstpos, final Vec3 secondpos,
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
			RenderSystem.setShaderTexture(0, ShaderHandler.shaderMaskTextureLocation);
			//mc.getTextureManager().bindForSetup(ShaderHandler.shaderMaskTextureLocation);//getTexture(ShaderHandler.shaderMaskTextureLocation).bindTexture();
			//GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getTextureManager().getTexture(ShaderHandler.shaderMaskTextureLocation).getGlTextureId());

			//image
			ARBShaderObjects.glUniform1iARB(imageUniform, primaryTextureUnit);
			glActiveTexture(ARBMultitexture.GL_TEXTURE0_ARB + primaryTextureUnit);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
			//mc.getTextureManager().bindForSetup(TextureAtlas.LOCATION_BLOCKS);//.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).bindTexture();
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
		Vec3 blockPos;
		Vec3 firstPos;
		Vec3 secondPos;
		boolean red;

		public ShaderInfo(float dissolve, Vec3 blockPos, Vec3 firstPos, Vec3 secondPos, boolean red) {
			this.dissolve = dissolve;
			this.blockPos = blockPos;
			this.firstPos = firstPos;
			this.secondPos = secondPos;
			this.red = red;
		}
	}
}
