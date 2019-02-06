package nl.requios.effortlessbuilding.helper;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import nl.requios.effortlessbuilding.BuildConfig;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.IBuildMode;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.BuildModifiers;
import nl.requios.effortlessbuilding.buildmodifier.Mirror;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.RadialMirror;
import nl.requios.effortlessbuilding.item.ItemRandomizerBag;
import nl.requios.effortlessbuilding.proxy.ClientProxy;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.Color;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(Side.CLIENT)
public class RenderHelper implements IWorldEventListener {

    private static final Color colorX = new Color(255, 72, 52);
    private static final Color colorY = new Color(67, 204, 51);
    private static final Color colorZ = new Color(52, 247, 255);
    private static final Color colorRadial = new Color(52, 247, 255);
    private static final int lineAlpha = 200;
    private static final int planeAlpha = 75;
    private static final Vec3d epsilon = new Vec3d(0.001, 0.001, 0.001); //prevents z-fighting

    private static List<BlockPos> previousCoordinates;

    private static final int secondaryTextureUnit = 7;

    private static void begin(float partialTicks) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        double playerX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double playerY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        Vec3d playerPos = new Vec3d(playerX, playerY, playerZ);

        GL11.glPushMatrix();
        GL11.glTranslated(-playerPos.x, -playerPos.y, -playerPos.z);

        GL11.glDepthMask(false);
    }

    private static void beginLines() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glLineWidth(2);
    }

    private static void endLines() {
        GL11.glPopAttrib();
    }

    private static void beginBlockPreviews() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(EffortlessBuilding.MODID, "textures/shader_color.png"));
        Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(EffortlessBuilding.MODID, "textures/shader_mask.png"));

        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL14.glBlendColor(1F, 1F, 1F, 0.8f);
    }

    private static void endBlockPreviews() {
        ShaderHelper.releaseShader();
        GlStateManager.disableBlend();
        GL11.glPopAttrib();
    }

    private static void end() {
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }

    private static void renderBlockPreview(BlockRendererDispatcher dispatcher, BlockPos blockPos, IBlockState blockState) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.005f, -0.005f, 0.005f);
        GlStateManager.scale(1.01f, 1.01f, 1.01f);
        dispatcher.renderBlockBrightness(blockState, 0.85f);
        GlStateManager.popMatrix();
    }

    public static void renderBlockOutline(BlockPos pos) {
        renderBlockOutline(pos, pos);
    }

    //Renders outline. Pos1 has to be minimal x,y,z and pos2 maximal x,y,z
    public static void renderBlockOutline(BlockPos pos1, BlockPos pos2) {
        GL11.glLineWidth(2);

        AxisAlignedBB aabb = new AxisAlignedBB(pos1, pos2.add(1, 1, 1)).grow(0.0020000000949949026);

        RenderGlobal.drawSelectionBoundingBox(aabb, 0f, 0f, 0f, 0.4f);
    }

    @SubscribeEvent
    public static void onRender(RenderWorldLastEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
        ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);

        begin(event.getPartialTicks());

        beginLines();
        //Mirror lines and areas
        Mirror.MirrorSettings m = modifierSettings.getMirrorSettings();
        if (m != null && m.enabled && (m.mirrorX || m.mirrorY || m.mirrorZ))
        {
            Vec3d pos = m.position.add(epsilon);
            int radius = m.radius;

            if (m.mirrorX)
            {
                Vec3d posA = new Vec3d(pos.x, pos.y - radius, pos.z - radius);
                Vec3d posB = new Vec3d(pos.x, pos.y + radius, pos.z + radius);

                drawMirrorPlane(posA, posB, colorX, m.drawLines, m.drawPlanes, true);
            }
            if (m.mirrorY)
            {
                Vec3d posA = new Vec3d(pos.x - radius, pos.y, pos.z - radius);
                Vec3d posB = new Vec3d(pos.x + radius, pos.y, pos.z + radius);

                drawMirrorPlaneY(posA, posB, colorY, m.drawLines, m.drawPlanes);
            }
            if (m.mirrorZ)
            {
                Vec3d posA = new Vec3d(pos.x - radius, pos.y - radius, pos.z);
                Vec3d posB = new Vec3d(pos.x + radius, pos.y + radius, pos.z);

                drawMirrorPlane(posA, posB, colorZ, m.drawLines, m.drawPlanes, true);
            }

            //Draw axis coordinated colors if two or more axes are enabled
            //(If only one is enabled the lines are that planes color)
            if (m.drawLines && ((m.mirrorX && m.mirrorY) || (m.mirrorX && m.mirrorZ) || (m.mirrorY && m.mirrorZ)))
            {
                drawMirrorLines(m);
            }
        }

        //Radial mirror lines and areas
        RadialMirror.RadialMirrorSettings r = modifierSettings.getRadialMirrorSettings();
        if (r != null && r.enabled)
        {
            Vec3d pos = r.position.add(epsilon);
            int radius = r.radius;

            float angle = 2f * ((float) Math.PI) / r.slices;
            Vec3d relStartVec = new Vec3d(radius, 0, 0);
            if (r.slices%4 == 2) relStartVec = relStartVec.rotateYaw(angle / 2f);

            for (int i = 0; i < r.slices; i++) {
                Vec3d relNewVec = relStartVec.rotateYaw(angle * i);
                Vec3d newVec = pos.add(relNewVec);

                Vec3d posA = new Vec3d(pos.x, pos.y - radius, pos.z);
                Vec3d posB = new Vec3d(newVec.x, pos.y + radius, newVec.z);
                drawMirrorPlane(posA, posB, colorRadial, r.drawLines, r.drawPlanes, false);
            }
        }
        endLines();

        //Render block previews
        RayTraceResult lookingAt = ClientProxy.getLookingAt(player);
        if (modeSettings.getBuildMode() == BuildModes.BuildModeEnum.Normal) lookingAt = Minecraft.getMinecraft().objectMouseOver;

        ItemStack mainhand = player.getHeldItemMainhand();
        boolean toolInHand = !(!mainhand.isEmpty() && (mainhand.getItem() instanceof ItemBlock || mainhand.getItem() instanceof ItemRandomizerBag));

        BlockPos startPos = null;
        EnumFacing sideHit = null;
        Vec3d hitVec = null;

        //Checking for null is necessary! Even in vanilla when looking down ladders it is occasionally null (instead of Type MISS)
        if (lookingAt != null && lookingAt.typeOfHit == RayTraceResult.Type.BLOCK) {
            startPos = lookingAt.getBlockPos();

            //Check if tool (or none) in hand
            boolean replaceable = player.world.getBlockState(startPos).getBlock().isReplaceable(player.world, startPos);
            if (!modifierSettings.doQuickReplace() && !toolInHand && !replaceable) {
                startPos = startPos.offset(lookingAt.sideHit);
            }

            //Get under tall grass and other replaceable blocks
            if (modifierSettings.doQuickReplace() && !toolInHand && replaceable) {
                startPos = startPos.down();
            }

            sideHit = lookingAt.sideHit;
            hitVec = lookingAt.hitVec;
        }

        //Dont render if in normal mode and modifiers are disabled
        //Unless alwaysShowBlockPreview is true in config
        if (modeSettings.getBuildMode() != BuildModes.BuildModeEnum.Normal ||
            (startPos != null && BuildModifiers.isEnabled(modifierSettings, startPos)) ||
            BuildConfig.visuals.alwaysShowBlockPreview) {

            beginBlockPreviews();

            IBuildMode buildModeInstance = modeSettings.getBuildMode().instance;
            if (buildModeInstance.getSideHit(player) != null) sideHit = buildModeInstance.getSideHit(player);
            if (buildModeInstance.getHitVec(player) != null) hitVec = buildModeInstance.getHitVec(player);

            if (sideHit != null) {
                BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
                float percentile = ClientProxy.ticksInGame / 100f % 1f;
                //TODO test
                percentile = 1f;

                //get coordinates
                List<BlockPos> startCoordinates = BuildModes.findCoordinates(player, startPos);

                BlockPos firstPos = BlockPos.ORIGIN, secondPos = BlockPos.ORIGIN;
                //remember first and last pos for highlighting
                if (!startCoordinates.isEmpty()) {
                    firstPos = startCoordinates.get(0);
                    secondPos = startCoordinates.get(startCoordinates.size() - 1);
                }

                //Limit number of blocks you can place
                int limit = ReachHelper.getMaxBlocksPlacedAtOnce(player);
                if (startCoordinates.size() > limit) {
                    startCoordinates = startCoordinates.subList(0, limit);
                }

                List<BlockPos> newCoordinates = BuildModifiers.findCoordinates(player, startCoordinates);

                Collections.sort(newCoordinates, (lhs, rhs) -> {
                    // -1 - less than, 1 - greater than, 0 - equal
                    double lhsDistanceToPlayer = new Vec3d(lhs).subtract(player.getPositionEyes(1f)).lengthSquared();
                    double rhsDistanceToPlayer = new Vec3d(rhs).subtract(player.getPositionEyes(1f)).lengthSquared();
                    return (int) Math.signum(lhsDistanceToPlayer - rhsDistanceToPlayer);
                });

                //check if they are different from previous
                if (!BuildModifiers.compareCoordinates(previousCoordinates, newCoordinates)) {
                    previousCoordinates = newCoordinates;
                    //if so, renew randomness of randomizer bag
                    ItemRandomizerBag.renewRandomness();
                }

                hitVec = new Vec3d(Math.abs(hitVec.x - ((int) hitVec.x)), Math.abs(hitVec.y - ((int) hitVec.y)),
                        Math.abs(hitVec.z - ((int) hitVec.z)));
                List<ItemStack> itemStacks = new ArrayList<>();
                List<IBlockState> blockStates = BuildModifiers.findBlockStates(player, startCoordinates, hitVec, lookingAt.sideHit, itemStacks);

                //check if valid blockstates
                if (blockStates.size() != 0 && newCoordinates.size() == blockStates.size()) {
                    for (int i = newCoordinates.size() - 1; i >= 0; i--) {
                        BlockPos blockPos = newCoordinates.get(i);
                        IBlockState blockState = blockStates.get(i);
                        ItemStack itemstack = itemStacks.get(i);
                        //Check if can place
                        if (!itemstack.isEmpty() && SurvivalHelper.canPlayerEdit(player, player.world, blockPos, itemstack) &&
                            SurvivalHelper.mayPlace(player.world, Block.getBlockFromItem(itemstack.getItem()), blockState, blockPos, true, EnumFacing.UP, player) &&
                            SurvivalHelper.canReplace(player.world, player, blockPos)) {

                            ShaderHelper.useShader(ShaderHelper.dissolve, generateShaderCallback(percentile,
                                    new Vec3d(blockPos), new Vec3d(firstPos), new Vec3d(secondPos),
                                    /*blockPos == firstPos ||*/ blockPos == secondPos));
                            renderBlockPreview(dispatcher, blockPos, blockState);
                        }
                    }
                }
            }
            endBlockPreviews();

            beginLines();
            //Draw outlines if tool in hand
            //Find proper raytrace: either normal range or increased range depending on canBreakFar
            RayTraceResult objectMouseOver = Minecraft.getMinecraft().objectMouseOver;
            RayTraceResult breakingRaytrace = ReachHelper.canBreakFar(player) ? lookingAt : objectMouseOver;
            if (toolInHand && breakingRaytrace != null && breakingRaytrace.typeOfHit == RayTraceResult.Type.BLOCK) {
                List<BlockPos> breakCoordinates = BuildModifiers.findCoordinates(player, breakingRaytrace.getBlockPos());

                //Only render first outline if further than normal reach
                boolean excludeFirst = objectMouseOver != null && objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK;
                for (int i = excludeFirst ? 1 : 0; i < breakCoordinates.size(); i++) {
                    BlockPos coordinate = breakCoordinates.get(i);

                    IBlockState blockState = player.world.getBlockState(coordinate);
                    if (!blockState.getBlock().isAir(blockState, player.world, coordinate)) {
                        if (SurvivalHelper.canBreak(player.world, player, coordinate) || i == 0) {
                            renderBlockOutline(coordinate);
                        }
                    }
                }
            }
            endLines();
        }

        end();
    }


    //----Mirror----

    public static void drawMirrorPlane(Vec3d posA, Vec3d posB, Color c, boolean drawLines, boolean drawPlanes, boolean drawVerticalLines) {

        GL11.glColor4d(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        if (drawPlanes) {
            bufferBuilder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);

            bufferBuilder.pos(posA.x, posA.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
            bufferBuilder.pos(posA.x, posB.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
            bufferBuilder.pos(posB.x, posA.y, posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
            bufferBuilder.pos(posB.x, posB.y, posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();

            tessellator.draw();
        }

        if (drawLines) {
            Vec3d middle = posA.add(posB).scale(0.5);
            bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

            bufferBuilder.pos(posA.x, middle.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
            bufferBuilder.pos(posB.x, middle.y, posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
            if (drawVerticalLines) {
                bufferBuilder.pos(middle.x, posA.y, middle.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
                bufferBuilder.pos(middle.x, posB.y, middle.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
            }

            tessellator.draw();
        }
    }

    public static void drawMirrorPlaneY(Vec3d posA, Vec3d posB, Color c, boolean drawLines, boolean drawPlanes) {

        GL11.glColor4d(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        if (drawPlanes) {
            bufferBuilder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);

            bufferBuilder.pos(posA.x, posA.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
            bufferBuilder.pos(posA.x, posA.y, posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
            bufferBuilder.pos(posB.x, posA.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
            bufferBuilder.pos(posB.x, posA.y, posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();

            tessellator.draw();
        }

        if (drawLines) {
            Vec3d middle = posA.add(posB).scale(0.5);
            bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

            bufferBuilder.pos(middle.x, middle.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
            bufferBuilder.pos(middle.x, middle.y, posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
            bufferBuilder.pos(posA.x, middle.y, middle.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
            bufferBuilder.pos(posB.x, middle.y, middle.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();

            tessellator.draw();
        }
    }

    public static void drawMirrorLines(Mirror.MirrorSettings m) {

        Vec3d pos = m.position.add(epsilon);

        GL11.glColor4d(100, 100, 100, 255);
        GL11.glLineWidth(2);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        bufferBuilder.pos(pos.x - m.radius, pos.y, pos.z).color(colorX.getRed(), colorX.getGreen(), colorX.getBlue(), lineAlpha).endVertex();
        bufferBuilder.pos(pos.x + m.radius, pos.y, pos.z).color(colorX.getRed(), colorX.getGreen(), colorX.getBlue(), lineAlpha).endVertex();
        bufferBuilder.pos(pos.x, pos.y - m.radius, pos.z).color(colorY.getRed(), colorY.getGreen(), colorY.getBlue(), lineAlpha).endVertex();
        bufferBuilder.pos(pos.x, pos.y + m.radius, pos.z).color(colorY.getRed(), colorY.getGreen(), colorY.getBlue(), lineAlpha).endVertex();
        bufferBuilder.pos(pos.x, pos.y, pos.z - m.radius).color(colorZ.getRed(), colorZ.getGreen(), colorZ.getBlue(), lineAlpha).endVertex();
        bufferBuilder.pos(pos.x, pos.y, pos.z + m.radius).color(colorZ.getRed(), colorZ.getGreen(), colorZ.getBlue(), lineAlpha).endVertex();

        tessellator.draw();
    }

    private static Consumer<Integer> generateShaderCallback(final float percentile, final Vec3d blockpos, final Vec3d firstpos, final Vec3d secondpos, final boolean highlight) {
        Minecraft mc = Minecraft.getMinecraft();
        return (Integer shader) -> {
            int percentileUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "percentile");
            int highlightUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "highlight");
            int blockposUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "blockpos");
            int firstposUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "firstpos");
            int secondposUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "secondpos");
            int imageUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "image");
            int maskUniform = ARBShaderObjects.glGetUniformLocationARB(shader, "mask");

            //image
            OpenGlHelper.setActiveTexture(ARBMultitexture.GL_TEXTURE0_ARB);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.renderEngine.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE/*new ResourceLocation(EffortlessBuilding.MODID, "textures/shader_color.png")*/).getGlTextureId());
            ARBShaderObjects.glUniform1iARB(imageUniform, 0);

            OpenGlHelper.setActiveTexture(ARBMultitexture.GL_TEXTURE0_ARB + secondaryTextureUnit);

            GlStateManager.enableTexture2D();
            GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

            //mask
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,
                    mc.renderEngine.getTexture(new ResourceLocation(EffortlessBuilding.MODID, "textures/shader_mask.png")).getGlTextureId());
            ARBShaderObjects.glUniform1iARB(maskUniform, secondaryTextureUnit);

            //blockpos
            ARBShaderObjects.glUniform3fARB(blockposUniform, (float) blockpos.x, (float) blockpos.y, (float) blockpos.z);
            ARBShaderObjects.glUniform3fARB(firstposUniform, (float) firstpos.x, (float) firstpos.y, (float) firstpos.z);
            ARBShaderObjects.glUniform3fARB(secondposUniform, (float) secondpos.x, (float) secondpos.y, (float) secondpos.z);

            //percentile
            ARBShaderObjects.glUniform1fARB(percentileUniform, percentile);
            //highlight
            ARBShaderObjects.glUniform1iARB(highlightUniform, highlight ? 1 : 0);
        };
    }

    //IWORLDEVENTLISTENER IMPLEMENTATION
    @Override
    public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {

    }

    @Override
    public void notifyLightSet(BlockPos pos) {

    }

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {

    }

    @Override
    public void playSoundToAllNearExcept(@Nullable EntityPlayer player, SoundEvent soundIn, SoundCategory category,
                                         double x, double y, double z, float volume, float pitch) {

    }

    @Override
    public void playRecord(SoundEvent soundIn, BlockPos pos) {

    }

    @Override
    public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord,
                              double xSpeed, double ySpeed, double zSpeed, int... parameters) {

    }

    @Override
    public void spawnParticle(int id, boolean ignoreRange, boolean p_190570_3_, double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed, int... parameters) {

    }

    @Override
    public void onEntityAdded(Entity entityIn) {

    }

    @Override
    public void onEntityRemoved(Entity entityIn) {

    }

    @Override
    public void broadcastSound(int soundID, BlockPos pos, int data) {

    }

    @Override
    public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data) {

    }

    //Sends breaking progress for all coordinates to renderglobal, so all blocks get visually broken
    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
        Minecraft mc = Minecraft.getMinecraft();

        ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(mc.player);
        if (!BuildModifiers.isEnabled(modifierSettings, pos)) return;

        List<BlockPos> coordinates = BuildModifiers.findCoordinates(mc.player, pos);
        for (int i = 1; i < coordinates.size(); i++) {
            BlockPos coordinate = coordinates.get(i);
            if (SurvivalHelper.canBreak(mc.world, mc.player, coordinate)) {
                //Send i as entity id because only one block can be broken per id
                //Unless i happens to be the player id, then take something else
                int fakeId = mc.player.getEntityId() != i ? i : coordinates.size();
                mc.renderGlobal.sendBlockBreakProgress(fakeId, coordinate, progress);
            }
        }
    }
}
