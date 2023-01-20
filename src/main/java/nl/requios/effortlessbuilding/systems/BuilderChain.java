package nl.requios.effortlessbuilding.systems;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.ClientEvents;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.ServerPlaceBlocksMessage;
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

    public enum State {
        IDLE,
        PLACING,
        BREAKING
    }

    private State state = State.IDLE;


    public void onRightClick() {
        if (state == State.BREAKING) {
            cancel();
            return;
        }

        if (state == State.IDLE) {
            state = State.PLACING;
        }

        var player = Minecraft.getInstance().player;
        var buildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();

        //Find out if we should place blocks now
        if (buildMode.instance.onClick()) {
            state = State.IDLE;
            PacketHandler.INSTANCE.sendToServer(new ServerPlaceBlocksMessage(blocks));
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
        }

        var buildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();

        //Find out if we should break blocks now
        if (buildMode.instance.onClick()) {
            state = State.IDLE;
            PacketHandler.INSTANCE.sendToServer(new ServerPlaceBlocksMessage(blocks));
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
        var buildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();


        BlockHitResult lookingAt = ClientEvents.getLookingAt(player);
        BlockEntry startEntry = findStartPosition(player, lookingAt, modifierSettings.doQuickReplace());
        if (startEntry != null) {
            blocks.add(startEntry);
        }

        EffortlessBuildingClient.BUILD_MODES.findCoordinates(blocks, player, buildMode);
        EffortlessBuildingClient.BUILD_MODIFIERS.findCoordinates(blocks, player, modifierSettings);

        if (state == State.PLACING) {
            //TODO find block states
        }
    }

    public void cancel() {
        var player = Minecraft.getInstance().player;

        state = State.IDLE;
        EffortlessBuildingClient.BUILD_MODES.onCancel(player);
    }

    private BlockEntry findStartPosition(Player player, BlockHitResult lookingAt, boolean doingQuickReplace) {
        if (lookingAt == null || lookingAt.getType() == HitResult.Type.MISS) return null;

        var startPos = lookingAt.getBlockPos();

        //Check if out of reach
        int maxReach = ReachHelper.getMaxReach(player);
        if (player.blockPosition().distSqr(startPos) > maxReach * maxReach) return null;

        //Offset in direction of sidehit if not quickreplace and not replaceable
        boolean replaceable = player.level.getBlockState(startPos).getMaterial().isReplaceable();
        boolean becomesDoubleSlab = SurvivalHelper.doesBecomeDoubleSlab(player, startPos);
        if (!doingQuickReplace && !replaceable && !becomesDoubleSlab) {
            startPos = startPos.relative(lookingAt.getDirection());
        }

        //Get under tall grass and other replaceable blocks
        if (doingQuickReplace && replaceable) {
            startPos = startPos.below();
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
}
