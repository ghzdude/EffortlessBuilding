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
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.item.AbstractRandomizerBagItem;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.ServerBreakBlocksPacket;
import nl.requios.effortlessbuilding.network.ServerPlaceBlocksPacket;
import nl.requios.effortlessbuilding.utilities.*;

import java.util.HashSet;

// Receives block placed events, then finds additional blocks we want to place through various systems,
// and then sends them to the server to be placed
// Uses chain of responsibility pattern
@OnlyIn(Dist.CLIENT)
public class BuilderChain {

    private final BlockSet blocks = new BlockSet();
    private Item previousHeldItem;
    private int soundTime = 0;
    private BlockEntry startPosForPlacing;
    private BlockPos startPosForBreaking;
    private BlockHitResult lookingAtNear;

    public enum BuildingState {
        IDLE,
        PLACING,
        BREAKING
    }

    //What we are currently doing
    private BuildingState buildingState = BuildingState.IDLE;

    public enum AbilitiesState {
        CAN_PLACE_AND_BREAK,
        CAN_BREAK,
        NONE
    }

    //Whether we can place or break blocks, determined by what we are looking at and what we are holding
    private AbilitiesState abilitiesState = AbilitiesState.CAN_PLACE_AND_BREAK;

    public void onRightClick() {
        if (abilitiesState != AbilitiesState.CAN_PLACE_AND_BREAK || buildingState == BuildingState.BREAKING) {
            cancel();
            return;
        }

        if (buildingState == BuildingState.IDLE) {
            buildingState = BuildingState.PLACING;
        }

        var player = Minecraft.getInstance().player;
        var buildMode = EffortlessBuildingClient.BUILD_MODES.getBuildMode();

        //Find out if we should place blocks now
        if (buildMode.instance.onClick(blocks)) {
            buildingState = BuildingState.IDLE;

            if (!blocks.isEmpty()) {
                EffortlessBuildingClient.BLOCK_PREVIEWS.onBlocksPlaced(blocks);
                BlockUtilities.playSoundIfFurtherThanNormal(player, blocks.getLastBlockEntry(), false);
                player.swing(InteractionHand.MAIN_HAND);
                PacketHandler.INSTANCE.sendToServer(new ServerPlaceBlocksPacket(blocks));
            }
        }
    }

    public void onLeftClick() {
        if (abilitiesState == AbilitiesState.NONE || buildingState == BuildingState.PLACING) {
            cancel();
            return;
        }

        var player = Minecraft.getInstance().player;
        if (!ReachHelper.canBreakFar(player)) return;

        if (buildingState == BuildingState.IDLE){
            buildingState = BuildingState.BREAKING;

            //Use new start position for breaking, because we assumed the player was gonna place
            blocks.setStartPos(new BlockEntry(startPosForBreaking));
            BuilderFilter.filterOnCoordinates(blocks, player);
            findExistingBlockStates(player.level);
            BuilderFilter.filterOnExistingBlockStates(blocks, player);
        }

        var buildMode = EffortlessBuildingClient.BUILD_MODES.getBuildMode();

        //Find out if we should break blocks now
        if (buildMode.instance.onClick(blocks)) {
            buildingState = BuildingState.IDLE;

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
        startPosForPlacing = null;
        startPosForBreaking = null;
        lookingAtNear = null;

        var mc = Minecraft.getInstance();
        var player = mc.player;
        var world = mc.level;

        abilitiesState = determineAbilities(mc, player, world);
        if (abilitiesState == AbilitiesState.NONE) return;

        var buildMode = EffortlessBuildingClient.BUILD_MODES.getBuildMode();
        var modifierSettings = ModifierSettingsManager.getModifierSettings(player);

        if (buildingState == BuildingState.IDLE) {
            //Find start position
            BlockEntry startEntry = findStartPosition(player, buildMode);
            if (startEntry != null) {
                blocks.setStartPos(startEntry);
            } else {
                //We aren't placing or breaking blocks, and we have no start position
                abilitiesState = AbilitiesState.NONE;
                return;
            }
        }

        EffortlessBuildingClient.BUILD_MODES.findCoordinates(blocks, player, buildMode);
        EffortlessBuildingClient.BUILD_MODIFIERS.findCoordinates(blocks, player, modifierSettings);
        BuilderFilter.filterOnCoordinates(blocks, player);

        if (buildMode == BuildModeEnum.DISABLED && blocks.size() <= 1) {
            abilitiesState = AbilitiesState.NONE;
            return;
        }

        findExistingBlockStates(world);
        BuilderFilter.filterOnExistingBlockStates(blocks, player);

        var itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        findNewBlockStates(player, itemStack);
        BuilderFilter.filterOnNewBlockStates(blocks, player);

        //Check if any changes are made
        if (previousHeldItem != itemStack.getItem() || !previousCoordinates.equals(blocks.getCoordinates())) {
            onBlocksChanged(player);
        }

        previousHeldItem = itemStack.getItem();
    }

    //Whether we can place or break blocks, determined by what we are looking at and what we are holding
    private AbilitiesState determineAbilities(Minecraft mc, Player player, Level world) {

        var hitResult = Minecraft.getInstance().hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            lookingAtNear = (BlockHitResult) hitResult;
        }

        var itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean blockInHand = CompatHelper.isItemBlockProxy(itemStack);
        boolean lookingAtInteractiveObject = BlockUtilities.determineIfLookingAtInteractiveObject(mc, world);
        boolean isShiftKeyDown = player.isShiftKeyDown();

        if (lookingAtInteractiveObject && !isShiftKeyDown)
            return AbilitiesState.NONE;

        if (!blockInHand)
            return AbilitiesState.CAN_BREAK;

        return AbilitiesState.CAN_PLACE_AND_BREAK;
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
                SoundEvent soundEvent = buildingState == BuildingState.BREAKING ? soundType.getBreakSound() : soundType.getPlaceSound();
                player.level.playSound(player, player.blockPosition(), soundEvent, SoundSource.BLOCKS, 0.3f, 0.8f);
            }
        }
    }

    public void cancel() {
        if (buildingState == BuildingState.IDLE) return;
        buildingState = BuildingState.IDLE;
        EffortlessBuildingClient.BUILD_MODES.onCancel();
        Minecraft.getInstance().player.playSound(SoundEvents.UI_TOAST_OUT, 4, 1);
    }

    private BlockEntry findStartPosition(Player player, BuildModeEnum buildMode) {

        //Determine if we should look far or nearby
        boolean shouldLookAtNear = buildMode == BuildModeEnum.DISABLED;
        BlockHitResult lookingAt;
        if (shouldLookAtNear) {
            lookingAt = lookingAtNear;
        } else {
            lookingAt = ClientEvents.getLookingAtFar(player);
        }
        if (lookingAt == null || lookingAt.getType() == HitResult.Type.MISS) return null;

        var startPos = lookingAt.getBlockPos();

        //Check if out of reach
        int maxReach = ReachHelper.getMaxReach(player);
        if (player.blockPosition().distSqr(startPos) > maxReach * maxReach) return null;

        startPosForBreaking = startPos;
        if (!shouldLookAtNear && !ReachHelper.canBreakFar(player)) {
            startPosForBreaking = null;
        }

        if (abilitiesState == AbilitiesState.CAN_PLACE_AND_BREAK) {
            //Calculate start position for placing

            //Offset in direction of sidehit if not quickreplace and not replaceable
            boolean shouldOffsetStartPosition = EffortlessBuildingClient.BUILD_SETTINGS.shouldOffsetStartPosition();
            boolean replaceable = player.level.getBlockState(startPos).getMaterial().isReplaceable();
            boolean becomesDoubleSlab = SurvivalHelper.doesBecomeDoubleSlab(player, startPos);
            if (!shouldOffsetStartPosition && !replaceable && !becomesDoubleSlab) {
                startPos = startPos.relative(lookingAt.getDirection());
            }

            //Get under tall grass and other replaceable blocks
            if (shouldOffsetStartPosition && replaceable) {
                startPos = startPos.below();
            }

        } else {
            //We can only break

            //Do not break far if we are not allowed to
            if (startPosForBreaking == null) return null;
        }

        var blockEntry = new BlockEntry(startPos);

        //Place upside-down stairs if we aim high at block
        var hitVec = lookingAt.getLocation();
        //Format hitvec to 0.x
        hitVec = new Vec3(Math.abs(hitVec.x - ((int) hitVec.x)), Math.abs(hitVec.y - ((int) hitVec.y)), Math.abs(hitVec.z - ((int) hitVec.z)));
        if (hitVec.y > 0.5) {
            blockEntry.mirrorY = true;
        }

        startPosForPlacing = blockEntry;

        return blockEntry;
    }

    private void findExistingBlockStates(Level world) {
        for (BlockEntry blockEntry : blocks) {
            blockEntry.existingBlockState = world.getBlockState(blockEntry.blockPos);
        }
    }

    private void findNewBlockStates(Player player, ItemStack itemStack) {
        if (buildingState == BuildingState.BREAKING) return;

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

    public BlockSet getBlocks() {
        return blocks;
    }

    public BuildingState getBuildingState() {
        return buildingState;
    }

    public AbilitiesState getAbilitiesState() {
        return abilitiesState;
    }

    public BuildingState getPretendBuildingState() {
        if (buildingState != BuildingState.IDLE) return buildingState;
        if (abilitiesState == AbilitiesState.CAN_PLACE_AND_BREAK) return BuildingState.PLACING;
        if (abilitiesState == AbilitiesState.CAN_BREAK) return BuildingState.BREAKING;
        return BuildingState.IDLE;
    }

    public BlockEntry getStartPosForPlacing() {
        return startPosForPlacing;
    }

    public BlockPos getStartPosForBreaking() {
        return startPosForBreaking;
    }

    public BlockEntry getStartPos() {
        if (getPretendBuildingState() == BuildingState.BREAKING) return new BlockEntry(getStartPosForBreaking());
        return getStartPosForPlacing();
    }

    public BlockHitResult getLookingAtNear() {
        return lookingAtNear;
    }
}
