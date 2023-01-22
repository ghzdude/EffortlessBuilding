package nl.requios.effortlessbuilding.systems;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.ClientEvents;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.item.AbstractRandomizerBagItem;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.ServerBreakBlocksPacket;
import nl.requios.effortlessbuilding.network.ServerPlaceBlocksPacket;
import nl.requios.effortlessbuilding.utilities.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

// Receives block placed events, then finds additional blocks we want to place through various systems,
// and then sends them to the server to be placed
// Uses chain of responsibility pattern
@OnlyIn(Dist.CLIENT)
public class BuilderChain {

    private final BlockSet blocks = new BlockSet();
    private boolean blockInHand;
    private boolean lookingAtInteractiveObject;
    private Item previousHeldItem;
    private int soundTime = 0;

    public enum State {
        IDLE,
        PLACING,
        BREAKING
    }

    private State state = State.IDLE;

    public void onRightClick() {
        if (lookingAtInteractiveObject) return;
        var player = Minecraft.getInstance().player;

        if (state == State.BREAKING) {
            cancel();
            return;
        }

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
                BlockUtilities.playSoundIfFurtherThanNormal(player, blocks.getLastBlockEntry(), false);
                player.swing(InteractionHand.MAIN_HAND);
                PacketHandler.INSTANCE.sendToServer(new ServerPlaceBlocksPacket(blocks));
            }
        }
    }

    public void onLeftClick() {
        if (lookingAtInteractiveObject) return;
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
                BlockUtilities.playSoundIfFurtherThanNormal(player, blocks.getLastBlockEntry(), true);
                player.swing(InteractionHand.MAIN_HAND);
                PacketHandler.INSTANCE.sendToServer(new ServerBreakBlocksPacket(blocks));
            }
        }
    }

    public void onTick() {
        var previousCoordinates = new HashSet<>(blocks.getCoordinates());
        blocks.clear();

        var mc = Minecraft.getInstance();
        var player = mc.player;
        var world = mc.level;

        //Check if we have a BlockItem in hand
        var itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        blockInHand = CompatHelper.isItemBlockProxy(itemStack);

        lookingAtInteractiveObject = BlockUtilities.determineIfLookingAtInteractiveObject(mc, world);
        if (lookingAtInteractiveObject) return;

        var buildMode = EffortlessBuildingClient.BUILD_MODES.getBuildMode();
        var modifierSettings = ModifierSettingsManager.getModifierSettings(player);

        if (state == State.IDLE) {
            //Find start position
            BlockHitResult lookingAt = ClientEvents.getLookingAtFar(player);
            BlockEntry startEntry = findStartPosition(player, lookingAt);
            if (startEntry != null) {
                blocks.add(startEntry);
                blocks.firstPos = startEntry.blockPos;
                blocks.lastPos = startEntry.blockPos;
            }
        }

        EffortlessBuildingClient.BUILD_MODES.findCoordinates(blocks, player, buildMode);
        EffortlessBuildingClient.BUILD_MODIFIERS.findCoordinates(blocks, player, modifierSettings);

        BuilderFilter.filterOnCoordinates(blocks, player);

        findExistingBlockStates(world);
        BuilderFilter.filterOnExistingBlockStates(blocks, player);

        findNewBlockStates(player, itemStack);
        BuilderFilter.filterOnNewBlockStates(blocks, player);

        //Check if any changes are made
        if (previousHeldItem != itemStack.getItem() || !previousCoordinates.equals(blocks.getCoordinates())) {
            onBlocksChanged(player);
        }

        previousHeldItem = itemStack.getItem();
    }

    private void onBlocksChanged(Player player) {

        //Renew randomness of randomizer bag
        AbstractRandomizerBagItem.renewRandomness();

        //Play sound (max once every tick)
        if (blocks.size() > 1 && soundTime < ClientEvents.ticksInGame) {
            soundTime = ClientEvents.ticksInGame;

            if (blocks.getLastBlockEntry() != null && blocks.getLastBlockEntry().newBlockState != null) {
                var lastBlockState = blocks.getLastBlockEntry().newBlockState;
                SoundType soundType = lastBlockState.getBlock().getSoundType(lastBlockState, player.level, blocks.lastPos, player);
                SoundEvent soundEvent = state == BuilderChain.State.BREAKING ? soundType.getBreakSound() : soundType.getPlaceSound();
                player.level.playSound(player, player.blockPosition(), soundEvent, SoundSource.BLOCKS, 0.3f, 0.8f);
            }
        }
    }

    public void cancel() {
        if (state == State.IDLE) return;
        state = State.IDLE;
        EffortlessBuildingClient.BUILD_MODES.onCancel();
        Minecraft.getInstance().player.playSound(SoundEvents.UI_TOAST_OUT, 4, 1);
    }

    private BlockEntry findStartPosition(Player player, BlockHitResult lookingAtFar) {
        if (lookingAtFar == null || lookingAtFar.getType() == HitResult.Type.MISS) return null;

        var startPos = lookingAtFar.getBlockPos();

        //Check if out of reach
        int maxReach = ReachHelper.getMaxReach(player);
        if (player.blockPosition().distSqr(startPos) > maxReach * maxReach) return null;

        //TODO we are always at IDLE state here, find another way to check if we are breaking
        if (state != State.BREAKING) {
            //Offset in direction of sidehit if not quickreplace and not replaceable
            boolean shouldOffsetStartPosition = EffortlessBuildingClient.BUILD_SETTINGS.shouldOffsetStartPosition();
            boolean replaceable = player.level.getBlockState(startPos).getMaterial().isReplaceable();
            boolean becomesDoubleSlab = SurvivalHelper.doesBecomeDoubleSlab(player, startPos);
            if (!shouldOffsetStartPosition && !replaceable && !becomesDoubleSlab) {
                startPos = startPos.relative(lookingAtFar.getDirection());
            }

            //Get under tall grass and other replaceable blocks
            if (shouldOffsetStartPosition && replaceable) {
                startPos = startPos.below();
            }
        } else {
            //Do not break far if we are not allowed to
            if (!ReachHelper.canBreakFar(player)) {
                boolean startPosIsNear = false;
                var lookingAtNear = Minecraft.getInstance().hitResult;
                if (lookingAtNear != null && lookingAtNear.getType() == HitResult.Type.BLOCK) {
                    startPosIsNear = ((BlockHitResult) lookingAtNear).getBlockPos().equals(startPos);
                }
                if (!startPosIsNear) return null;
            }
        }

        var blockEntry = new BlockEntry(startPos);

        //Place upside-down stairs if we aim high at block
        var hitVec = lookingAtFar.getLocation();
        //Format hitvec to 0.x
        hitVec = new Vec3(Math.abs(hitVec.x - ((int) hitVec.x)), Math.abs(hitVec.y - ((int) hitVec.y)), Math.abs(hitVec.z - ((int) hitVec.z)));
        if (hitVec.y > 0.5) {
            blockEntry.mirrorY = true;
        }

        return blockEntry;
    }

    private void findExistingBlockStates(Level world) {
        for (BlockEntry blockEntry : blocks) {
            blockEntry.existingBlockState = world.getBlockState(blockEntry.blockPos);
        }
    }

    private void findNewBlockStates(Player player, ItemStack itemStack) {
        if (state == State.BREAKING) return;

        if (itemStack.getItem() instanceof BlockItem) {

            for (BlockEntry blockEntry : blocks) {
                blockEntry.newBlockState = BlockUtilities.getBlockState(player, InteractionHand.MAIN_HAND, itemStack, blockEntry);
            }

        } else if (CompatHelper.isItemBlockProxy(itemStack, false)) {

            AbstractRandomizerBagItem.resetRandomness();
            for (BlockEntry blockEntry : blocks) {
                ItemStack itemBlockStack = CompatHelper.getItemBlockFromStack(itemStack);
                if (itemBlockStack == null || itemBlockStack.isEmpty()) continue;
                blockEntry.newBlockState = BlockUtilities.getBlockState(player, InteractionHand.MAIN_HAND, itemBlockStack, blockEntry);
            }
        }
    }


    public State getState() {
        return state;
    }

    public BlockSet getBlocks() {
        return blocks;
    }

    public boolean isBlockInHand() {
        return blockInHand;
    }

    public boolean isLookingAtInteractiveObject() {
        return lookingAtInteractiveObject;
    }
}
