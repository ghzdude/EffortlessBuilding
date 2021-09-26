package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.BuildConfig;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import org.lwjgl.opengl.*;

import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.function.Function;

public class BuildRenderTypes extends RenderType {
	public static ResourceLocation shaderMaskTextureLocation = new ResourceLocation(EffortlessBuilding.MODID, "textures/shader_mask.png");

	public static final RenderType LINES;
	public static final RenderType PLANES;

	public static ShaderInstance dissolveShaderInstance;
	private static final ShaderStateShard RENDERTYPE_DISSOLVE_SHADER = new ShaderStateShard(() -> dissolveShaderInstance);

	//Between 0 and 7, but dont override vanilla textures
	//Also update dissolve.fsh SamplerX
	private static final int maskTextureIndex = 7;

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


	// Dummy constructor needed to make java happy
	public BuildRenderTypes(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
		super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
	}

	public static RenderType getBlockPreviewRenderType(float dissolve, BlockPos blockPos, BlockPos firstPos, BlockPos secondPos, boolean red) {

		Boolean useShaders = BuildConfig.visuals.useShaders.get();
		//TODO 1.17 don't use shaders if config says no

		String stateName = "eb_texturing_" + dissolve + "_" + blockPos + "_" + firstPos + "_" + secondPos + "_" + red;
		TexturingStateShard MY_TEXTURING = new TexturingStateShard(stateName, () -> {
			setShaderParameters(dissolveShaderInstance, dissolve, Vec3.atLowerCornerOf(blockPos), Vec3.atLowerCornerOf(firstPos), Vec3.atLowerCornerOf(secondPos), blockPos == secondPos, red);
			RenderSystem.setShaderColor(1f, 1f, 1f, 0.8f);
		}, () -> {});

		RenderType.CompositeState renderState = RenderType.CompositeState.builder()
				.setShaderState(RENDERTYPE_DISSOLVE_SHADER)
				.setTexturingState(MY_TEXTURING)
				.setTextureState(new RenderStateShard.TextureStateShard(shaderMaskTextureLocation, false, false))
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

	private static void setShaderParameters(ShaderInstance shader, final float dissolve, final Vec3 blockpos,
															final Vec3 firstpos, final Vec3 secondpos,
															final boolean highlight, final boolean red) {
		Uniform percentileUniform = shader.getUniform("dissolve");
		Uniform highlightUniform = shader.getUniform("highlight");
		Uniform redUniform = shader.getUniform("red");
		Uniform blockposUniform = shader.getUniform("blockpos");
		Uniform firstposUniform = shader.getUniform("firstpos");
		Uniform secondposUniform = shader.getUniform("secondpos");

		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
		RenderSystem.setShaderTexture(maskTextureIndex, shaderMaskTextureLocation);

		percentileUniform.set(dissolve);
		highlightUniform.set(highlight ? 1 : 0);
		redUniform.set(red ? 1 : 0);

		blockposUniform.set((float) blockpos.x, (float) blockpos.y, (float) blockpos.z);
		firstposUniform.set((float) firstpos.x, (float) firstpos.y, (float) firstpos.z);
		secondposUniform.set((float) secondpos.x, (float) secondpos.y, (float) secondpos.z);
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
