package nl.requios.effortlessbuilding.systems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.ClientEvents;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.buildmodifier.BuildModifiers;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.item.AbstractRandomizerBagItem;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.ServerBreakBlocksPacket;
import nl.requios.effortlessbuilding.network.ServerPlaceBlocksPacket;
import nl.requios.effortlessbuilding.render.BlockPreviews;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.ReachHelper;
import nl.requios.effortlessbuilding.utilities.SurvivalHelper;

import java.util.ArrayList;
import java.util.List;

// Receives block placed events, then finds additional blocks we want to place through various systems,
// and then sends them to the server to be placed
// Uses chain of responsibility pattern
@OnlyIn(Dist.CLIENT)
public class BuilderChain {

    private final List<BlockEntry> blocks = new ArrayList<>();
    private int soundTime = 0;

    public enum State {
        IDLE,
        PLACING,
        BREAKING
    }

    private State state = State.IDLE;

    public void onRightClick() {
        var player = Minecraft.getInstance().player;

        if (state == State.BREAKING) {
            cancel();
            return;
        }

        //Check if we have a BlockItem in hand
        var itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean blockInHand = CompatHelper.isItemBlockProxy(itemStack);
        if (!blockInHand) {
            if (state == State.PLACING) cancel();
            return;
        }

        if (state == State.IDLE) {
            state = State.PLACING;
        }

        var buildMode = EffortlessBuildingClient.BUILD_MODES.getBuildMode();

        //Find out if we should place blocks now
        if (buildMode.instance.onClick(blocks)) {
            state = State.IDLE;

            if (!blocks.isEmpty()) {
                EffortlessBuildingClient.BLOCK_PREVIEWS.onBlocksPlaced(blocks);
                PacketHandler.INSTANCE.sendToServer(new ServerPlaceBlocksPacket(blocks));
            }
        }
    }

    public void onLeftClick() {
        var player = Minecraft.getInstance().player;

        if (state == State.PLACING) {
            cancel();
            return;
        }

        if (!ReachHelper.canBreakFar(player)) return;

        if (state == State.IDLE){
            state = State.BREAKING;

            //Recalculate block positions, because start position has changed
            onTick();
        }

        var buildMode = EffortlessBuildingClient.BUILD_MODES.getBuildMode();

        //Find out if we should break blocks now
        if (buildMode.instance.onClick(blocks)) {
            state = State.IDLE;

            if (!blocks.isEmpty()) {
                EffortlessBuildingClient.BLOCK_PREVIEWS.onBlocksBroken(blocks);
                PacketHandler.INSTANCE.sendToServer(new ServerBreakBlocksPacket(blocks));
            }
        }
    }

    public void onTick() {
        blocks.clear();

        var mc = Minecraft.getInstance();
        var player = mc.player;
        var level = mc.level;

        //Check if we have a BlockItem in hand
        var itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean blockInHand = CompatHelper.isItemBlockProxy(itemStack);

//        if (!blockInHand && state == State.PLACING) {
//            state = State.IDLE;
//        }

        var modifierSettings = ModifierSettingsManager.getModifierSettings(player);
        var buildMode = EffortlessBuildingClient.BUILD_MODES.getBuildMode();


        BlockHitResult lookingAt = ClientEvents.getLookingAt(player);
        BlockEntry startEntry = findStartPosition(player, lookingAt);
        if (startEntry != null) {
            blocks.add(startEntry);
        }

        EffortlessBuildingClient.BUILD_MODES.findCoordinates(blocks, player, buildMode);
        EffortlessBuildingClient.BUILD_MODIFIERS.findCoordinates(blocks, player, modifierSettings);

        if (blockInHand && state != State.BREAKING)
            findBlockStates(player, itemStack);

        //Check if they are different
//        if (!BuildModifiers.compareCoordinates(previousCoordinates, newCoordinates)) {
//
//            //Renew randomness of randomizer bag
//            AbstractRandomizerBagItem.renewRandomness();
//
//            //Play sound (max once every tick)
//            if (blocks.size() > 1 && blockStates.size() > 1 && soundTime < ClientEvents.ticksInGame) {
//                soundTime = ClientEvents.ticksInGame;
//
//                var firstBlockState = blocks.get(0).blockState;
//                if (firstBlockState != null) {
//                    SoundType soundType = firstBlockState.getBlock().getSoundType(firstBlockState, player.level,
//                            blocks.get(0).blockPos, player);
//                    SoundEvent soundEvent = state == BuilderChain.State.BREAKING ? soundType.getBreakSound() : soundType.getPlaceSound();
//                    player.level.playSound(player, player.blockPosition(), soundEvent, SoundSource.BLOCKS, 0.3f, 0.8f);
//                }
//            }
//        }
    }

    public void cancel() {
        state = State.IDLE;
        EffortlessBuildingClient.BUILD_MODES.onCancel();
    }

    private BlockEntry findStartPosition(Player player, BlockHitResult lookingAt) {
        if (lookingAt == null || lookingAt.getType() == HitResult.Type.MISS) return null;

        var startPos = lookingAt.getBlockPos();

        //Check if out of reach
        int maxReach = ReachHelper.getMaxReach(player);
        if (player.blockPosition().distSqr(startPos) > maxReach * maxReach) return null;

        if (state != State.BREAKING) {
            //Offset in direction of sidehit if not quickreplace and not replaceable
            boolean isQuickReplacing = EffortlessBuildingClient.QUICK_REPLACE.isQuickReplacing();
            boolean replaceable = player.level.getBlockState(startPos).getMaterial().isReplaceable();
            boolean becomesDoubleSlab = SurvivalHelper.doesBecomeDoubleSlab(player, startPos);
            if (!isQuickReplacing && !replaceable && !becomesDoubleSlab) {
                startPos = startPos.relative(lookingAt.getDirection());
            }

            //Get under tall grass and other replaceable blocks
            if (isQuickReplacing && replaceable) {
                startPos = startPos.below();
            }
        }

        var blockEntry = new BlockEntry(startPos);

        //Place upside-down stairs if we aim high at block
        var hitVec = lookingAt.getLocation();
        //Format hitvec to 0.x
        hitVec = new Vec3(Math.abs(hitVec.x - ((int) hitVec.x)), Math.abs(hitVec.y - ((int) hitVec.y)), Math.abs(hitVec.z - ((int) hitVec.z)));
        if (hitVec.y > 0.5) {
            blockEntry.mirrorY = true;
        }

        return blockEntry;
    }

    private void findBlockStates(LocalPlayer player, ItemStack itemStack) {
        if (itemStack.getItem() instanceof BlockItem) {

            for (BlockEntry blockEntry : blocks) {
                blockEntry.blockState = getBlockState(player, InteractionHand.MAIN_HAND, itemStack, blockEntry);
            }

        } else if (CompatHelper.isItemBlockProxy(itemStack, false)) {

            for (BlockEntry blockEntry : blocks) {
                ItemStack itemBlockStack = CompatHelper.getItemBlockFromStack(itemStack);
                if (itemBlockStack == null || itemBlockStack.isEmpty()) continue;
                blockEntry.blockState = getBlockState(player, InteractionHand.MAIN_HAND, itemBlockStack, blockEntry);
            }
        }
    }

    public BlockState getBlockState(Player player, InteractionHand hand, ItemStack blockItemStack, BlockEntry blockEntry) {
        Block block = Block.byItem(blockItemStack.getItem());
        //TODO convert lookingAt hitvec to relative hitvec
        var blockHitResult = new BlockHitResult(Vec3.ZERO, Direction.UP, blockEntry.blockPos, false);
        return block.getStateForPlacement(new BlockPlaceContext(player, hand, blockItemStack, blockHitResult));
    }

    private void playPlacingSoundIfFurtherThanNormal(Player player, Vec3 location, BlockItem blockItem) {

        if ((location.subtract(player.getEyePosition(1f))).lengthSqr() > 25f) {
            BlockPos blockPos = new BlockPos(location);
            BlockState state = blockItem.getBlock().defaultBlockState();
            SoundType soundType = state.getBlock().getSoundType(state, player.level, blockPos, player);
            player.level.playSound(player, player.blockPosition(), soundType.getPlaceSound(), SoundSource.BLOCKS,
                    0.4f, soundType.getPitch());
        }
    }

    private void playBreakingSoundIfFurtherThanNormal(Player player, Vec3 location) {

        if ((location.subtract(player.getEyePosition(1f))).lengthSqr() > 25f) {
            BlockPos blockPos = new BlockPos(location);
            BlockState state = player.level.getBlockState(blockPos);
            SoundType soundtype = state.getBlock().getSoundType(state, player.level, blockPos, player);
            player.level.playSound(player, player.blockPosition(), soundtype.getBreakSound(), SoundSource.BLOCKS,
                    0.4f, soundtype.getPitch());
        }
    }

    private void swingHand(Player player, InteractionHand hand) {
        player.swing(hand);
    }

    public State getState() {
        return state;
    }

    public List<BlockEntry> getBlocks() {
        return blocks;
    }
}
