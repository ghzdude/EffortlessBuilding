package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.opengl.*;

import java.util.OptionalDouble;
import java.util.function.Consumer;

public class BuildRenderTypes {
	public static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY;
	public static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY;

	//TODO 1.17
//	public static final RenderStateShard.DiffuseLightingStateShard DIFFUSE_LIGHTING_ENABLED;
//	public static final RenderStateShard.DiffuseLightingStateShard DIFFUSE_LIGHTING_DISABLED;

	public static final RenderStateShard.LayeringStateShard PROJECTION_LAYERING;

	public static final RenderStateShard.CullStateShard CULL_DISABLED;

	//TODO 1.17
//	public static final RenderStateShard.AlphaStateShard DEFAULT_ALPHA;

	public static final RenderStateShard.WriteMaskStateShard WRITE_TO_DEPTH_AND_COLOR;
	public static final RenderStateShard.WriteMaskStateShard COLOR_WRITE;

	public static final RenderType LINES;
	public static final RenderType PLANES;

	private static final int primaryTextureUnit = 0;
	private static final int secondaryTextureUnit = 2;

	static {
		TRANSLUCENT_TRANSPARENCY = ObfuscationReflectionHelper.getPrivateValue(RenderStateShard.class, null, "TRANSLUCENT_TRANSPARENCY");
		NO_TRANSPARENCY = ObfuscationReflectionHelper.getPrivateValue(RenderStateShard.class, null, "NO_TRANSPARENCY");

		PROJECTION_LAYERING = ObfuscationReflectionHelper.getPrivateValue(RenderStateShard.class, null, "VIEW_OFFSET_Z_LAYERING");

		CULL_DISABLED = new RenderStateShard.CullStateShard(false);

		//TODO 1.17
//		DEFAULT_ALPHA = new RenderStateShard.AlphaStateShard(0.003921569F);

		final boolean ENABLE_DEPTH_WRITING = true;
		final boolean ENABLE_COLOUR_COMPONENTS_WRITING = true;
		WRITE_TO_DEPTH_AND_COLOR = new RenderStateShard.WriteMaskStateShard(ENABLE_COLOUR_COMPONENTS_WRITING, ENABLE_DEPTH_WRITING);
		COLOR_WRITE = new RenderStateShard.WriteMaskStateShard(true, false);

		final int INITIAL_BUFFER_SIZE = 128;
		RenderType.CompositeState renderState;

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
		renderState = RenderType.CompositeState.builder()
			.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
			.setLayeringState(PROJECTION_LAYERING)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setWriteMaskState(WRITE_TO_DEPTH_AND_COLOR)
			.setCullState(CULL_DISABLED)
			.createCompositeState(false);
		LINES = RenderType.create("eb_lines",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.LINES, INITIAL_BUFFER_SIZE, false, false, renderState);

		renderState = RenderType.CompositeState.builder()
			.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
			.setLayeringState(PROJECTION_LAYERING)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setWriteMaskState(COLOR_WRITE)
			.setCullState(CULL_DISABLED)
			.createCompositeState(false);
		PLANES = RenderType.create("eb_planes",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP, INITIAL_BUFFER_SIZE, false, false, renderState);

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
			//TODO 1.17
//			RenderSystem.blendColor(1f, 1f, 1f, 0.8f);
		}, ShaderHandler::releaseShader);

		RenderType.CompositeState renderState = RenderType.CompositeState.builder()
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
