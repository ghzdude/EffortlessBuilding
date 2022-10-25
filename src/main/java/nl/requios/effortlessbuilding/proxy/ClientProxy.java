package nl.requios.effortlessbuilding.proxy;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeOptions;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.gui.DiamondRandomizerBagScreen;
import nl.requios.effortlessbuilding.gui.GoldenRandomizerBagScreen;
import nl.requios.effortlessbuilding.gui.RandomizerBagScreen;
import nl.requios.effortlessbuilding.gui.buildmode.PlayerSettingsGui;
import nl.requios.effortlessbuilding.gui.buildmode.RadialMenu;
import nl.requios.effortlessbuilding.gui.buildmodifier.ModifierSettingsGui;
import nl.requios.effortlessbuilding.helper.ReachHelper;
import nl.requios.effortlessbuilding.network.*;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = {Dist.CLIENT})
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ClientProxy implements IProxy {
	public static KeyMapping[] keyBindings;
	public static HitResult previousLookAt;
	public static HitResult currentLookAt;
	public static int ticksInGame = 0;
	private static int placeCooldown = 0;
	private static int breakCooldown = 0;

	@Override
	public void setup(FMLCommonSetupEvent event) {
	}

	@Override
	public void clientSetup(FMLClientSetupEvent event) {
		//Keybindings are setup and registered in ModClientEventHandler

		MenuScreens.register(EffortlessBuilding.RANDOMIZER_BAG_CONTAINER.get(), RandomizerBagScreen::new);
		MenuScreens.register(EffortlessBuilding.GOLDEN_RANDOMIZER_BAG_CONTAINER.get(), GoldenRandomizerBagScreen::new);
		MenuScreens.register(EffortlessBuilding.DIAMOND_RANDOMIZER_BAG_CONTAINER.get(), DiamondRandomizerBagScreen::new);
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {

		if (event.phase == TickEvent.Phase.START) {
			onMouseInput();

			//Update previousLookAt
			HitResult objectMouseOver = Minecraft.getInstance().hitResult;
			//Checking for null is necessary! Even in vanilla when looking down ladders it is occasionally null (instead of Type MISS)
			if (objectMouseOver == null) return;

			if (currentLookAt == null) {
				currentLookAt = objectMouseOver;
				previousLookAt = objectMouseOver;
				return;
			}

			if (objectMouseOver.getType() == HitResult.Type.BLOCK) {
				if (currentLookAt.getType() != HitResult.Type.BLOCK) {
					currentLookAt = objectMouseOver;
					previousLookAt = objectMouseOver;
				} else {
					if (((BlockHitResult) currentLookAt).getBlockPos() != ((BlockHitResult) objectMouseOver).getBlockPos()) {
						previousLookAt = currentLookAt;
						currentLookAt = objectMouseOver;
					}
				}
			}
		} else if (event.phase == TickEvent.Phase.END) {
			Screen gui = Minecraft.getInstance().screen;
			if (gui == null || !gui.isPauseScreen()) {
				ticksInGame++;
			}
		}

	}

	private static void onMouseInput() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) return;
		BuildModes.BuildModeEnum buildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();

		if (mc.screen != null ||
			buildMode == BuildModes.BuildModeEnum.NORMAL ||
			RadialMenu.instance.isVisible()) {
			return;
		}

		if (mc.options.keyUse.isDown()) {

			//KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);

			if (placeCooldown <= 0) {
				placeCooldown = 4;

				ItemStack currentItemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
				if (currentItemStack.getItem() instanceof BlockItem ||
					(CompatHelper.isItemBlockProxy(currentItemStack) && !player.isShiftKeyDown())) {

					ItemStack itemStack = CompatHelper.getItemBlockFromStack(currentItemStack);

					//find position in distance
					HitResult lookingAt = getLookingAt(player);
					if (lookingAt != null && lookingAt.getType() == HitResult.Type.BLOCK) {
						BlockHitResult blockLookingAt = (BlockHitResult) lookingAt;

						BuildModes.onBlockPlacedMessage(player, new BlockPlacedMessage(blockLookingAt, true));
						PacketHandler.INSTANCE.sendToServer(new BlockPlacedMessage(blockLookingAt, true));

						//play sound if further than normal
						if ((blockLookingAt.getLocation().subtract(player.getEyePosition(1f))).lengthSqr() > 25f &&
							itemStack.getItem() instanceof BlockItem) {

							BlockState state = ((BlockItem) itemStack.getItem()).getBlock().defaultBlockState();
							BlockPos blockPos = blockLookingAt.getBlockPos();
							SoundType soundType = state.getBlock().getSoundType(state, player.level, blockPos, player);
							player.level.playSound(player, player.blockPosition(), soundType.getPlaceSound(), SoundSource.BLOCKS,
								0.4f, soundType.getPitch());
							player.swing(InteractionHand.MAIN_HAND);
						}
					} else {
						BuildModes.onBlockPlacedMessage(player, new BlockPlacedMessage());
						PacketHandler.INSTANCE.sendToServer(new BlockPlacedMessage());
					}
				}
			} else if (buildMode == BuildModes.BuildModeEnum.NORMAL_PLUS) {
				placeCooldown--;
				if (ModeOptions.getBuildSpeed() == ModeOptions.ActionEnum.FAST_SPEED) placeCooldown = 0;
			}
		} else {
			placeCooldown = 0;
		}

		if (mc.options.keyAttack.isDown()) {

			//Break block in distance in creative (or survival if enabled in config)
			if (breakCooldown <= 0) {
				breakCooldown = 4;

				HitResult lookingAt = getLookingAt(player);
				if (lookingAt != null && lookingAt.getType() == HitResult.Type.BLOCK) {
					BlockHitResult blockLookingAt = (BlockHitResult) lookingAt;

					BuildModes.onBlockBrokenMessage(player, new BlockBrokenMessage(blockLookingAt));
					PacketHandler.INSTANCE.sendToServer(new BlockBrokenMessage(blockLookingAt));

					//play sound if further than normal
					if ((blockLookingAt.getLocation().subtract(player.getEyePosition(1f))).lengthSqr() > 25f) {

						BlockPos blockPos = blockLookingAt.getBlockPos();
						BlockState state = player.level.getBlockState(blockPos);
						SoundType soundtype = state.getBlock().getSoundType(state, player.level, blockPos, player);
						player.level.playSound(player, player.blockPosition(), soundtype.getBreakSound(), SoundSource.BLOCKS,
							0.4f, soundtype.getPitch());
						player.swing(InteractionHand.MAIN_HAND);
					}
				} else {
					BuildModes.onBlockBrokenMessage(player, new BlockBrokenMessage());
					PacketHandler.INSTANCE.sendToServer(new BlockBrokenMessage());
				}
			} else if (buildMode == BuildModes.BuildModeEnum.NORMAL_PLUS) {
				breakCooldown--;
				if (ModeOptions.getBuildSpeed() == ModeOptions.ActionEnum.FAST_SPEED) breakCooldown = 0;
			}

			//EffortlessBuilding.packetHandler.sendToServer(new CancelModeMessage());

		} else {
			breakCooldown = 0;
		}
	}

	@SubscribeEvent(receiveCanceled = true)
	public static void onKeyPress(InputEvent.Key event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;

		//Remember to send packet to server if necessary
		//Show Modifier Settings GUI
		if (keyBindings[0].consumeClick()) {
			openModifierSettings();
		}

		//QuickReplace toggle
		if (keyBindings[1].consumeClick()) {
			ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
			modifierSettings.setQuickReplace(!modifierSettings.doQuickReplace());
			EffortlessBuilding.log(player, "Set " + ChatFormatting.GOLD + "Quick Replace " + ChatFormatting.RESET + (
				modifierSettings.doQuickReplace() ? "on" : "off"));
			PacketHandler.INSTANCE.sendToServer(new ModifierSettingsMessage(modifierSettings));
		}

		//Radial menu
		if (keyBindings[2].isDown()) {
			if (ReachHelper.getMaxReach(player) > 0) {
				if (!RadialMenu.instance.isVisible()) {
					Minecraft.getInstance().setScreen(RadialMenu.instance);
				}
			} else {
				EffortlessBuilding.log(player, "Build modes are disabled until your reach has increased. Increase your reach with craftable reach upgrades.");
			}
		}

		//Undo (Ctrl+Z)
		if (keyBindings[3].consumeClick()) {
			ModeOptions.ActionEnum action = ModeOptions.ActionEnum.UNDO;
			ModeOptions.performAction(player, action);
			PacketHandler.INSTANCE.sendToServer(new ModeActionMessage(action));
		}

		//Redo (Ctrl+Y)
		if (keyBindings[4].consumeClick()) {
			ModeOptions.ActionEnum action = ModeOptions.ActionEnum.REDO;
			ModeOptions.performAction(player, action);
			PacketHandler.INSTANCE.sendToServer(new ModeActionMessage(action));
		}

		//Change placement mode
		if (keyBindings[5].consumeClick()) {
			//Toggle between first two actions of the first option of the current build mode
			BuildModes.BuildModeEnum currentBuildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();
			if (currentBuildMode.options.length > 0) {
				ModeOptions.OptionEnum option = currentBuildMode.options[0];
				if (option.actions.length >= 2) {
					if (ModeOptions.getOptionSetting(option) == option.actions[0]) {
						ModeOptions.performAction(player, option.actions[1]);
						PacketHandler.INSTANCE.sendToServer(new ModeActionMessage(option.actions[1]));
					} else {
						ModeOptions.performAction(player, option.actions[0]);
						PacketHandler.INSTANCE.sendToServer(new ModeActionMessage(option.actions[0]));
					}
				}
			}
		}

	}

	public static void openModifierSettings() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) return;

		//Disabled if max reach is 0, might be set in the config that way.
		if (ReachHelper.getMaxReach(player) == 0) {
			EffortlessBuilding.log(player, "Build modifiers are disabled until your reach has increased. Increase your reach with craftable reach upgrades.");
		} else {
			mc.setScreen(new ModifierSettingsGui());
		}
	}

	public static void openPlayerSettings() {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new PlayerSettingsGui());
	}

	@SubscribeEvent
	public static void onGuiOpen(ScreenEvent event) {
		Player player = Minecraft.getInstance().player;
		if (player != null) {
			BuildModes.initializeMode(player);
		}
	}

	public static boolean isKeybindDown(int keybindIndex) {
		return InputConstants.isKeyDown(
				Minecraft.getInstance().getWindow().getWindow(),
				ClientProxy.keyBindings[2].getKey().getValue());
	}

	public static HitResult getLookingAt(Player player) {
		Level world = player.level;

		//base distance off of player ability (config)
		float raytraceRange = ReachHelper.getPlacementReach(player);

		Vec3 look = player.getLookAngle();
		Vec3 start = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
		Vec3 end = new Vec3(player.getX() + look.x * raytraceRange, player.getY() + player.getEyeHeight() + look.y * raytraceRange, player.getZ() + look.z * raytraceRange);
//        return player.rayTrace(raytraceRange, 1f, RayTraceFluidMode.NEVER);
		//TODO 1.14 check if correct
		return world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
	}

	public Player getPlayerEntityFromContext(Supplier<NetworkEvent.Context> ctx) {
		return (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT ? Minecraft.getInstance().player : ctx.get().getSender());
	}

	@Override
	public void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar) {
		EffortlessBuilding.log(Minecraft.getInstance().player, prefix + I18n.get(translationKey) + suffix, actionBar);
	}
}
