package nl.requios.effortlessbuilding.proxy;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeOptions;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.gui.RandomizerBagScreen;
import nl.requios.effortlessbuilding.gui.buildmode.PlayerSettingsGui;
import nl.requios.effortlessbuilding.gui.buildmode.RadialMenu;
import nl.requios.effortlessbuilding.gui.buildmodifier.ModifierSettingsGui;
import nl.requios.effortlessbuilding.helper.ReachHelper;
import nl.requios.effortlessbuilding.network.*;
import nl.requios.effortlessbuilding.render.ShaderHandler;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = {Dist.CLIENT})
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ClientProxy implements IProxy {
	public static KeyBinding[] keyBindings;
	public static RayTraceResult previousLookAt;
	public static RayTraceResult currentLookAt;
	public static int ticksInGame = 0;
	private static int placeCooldown = 0;
	private static int breakCooldown = 0;
	private static boolean shadersInitialized = false;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {

		if (event.phase == TickEvent.Phase.START) {
			onMouseInput();

			//Update previousLookAt
			RayTraceResult objectMouseOver = Minecraft.getInstance().hitResult;
			//Checking for null is necessary! Even in vanilla when looking down ladders it is occasionally null (instead of Type MISS)
			if (objectMouseOver == null) return;

			if (currentLookAt == null) {
				currentLookAt = objectMouseOver;
				previousLookAt = objectMouseOver;
				return;
			}

			if (objectMouseOver.getType() == RayTraceResult.Type.BLOCK) {
				if (currentLookAt.getType() != RayTraceResult.Type.BLOCK) {
					currentLookAt = objectMouseOver;
					previousLookAt = objectMouseOver;
				} else {
					if (((BlockRayTraceResult) currentLookAt).getBlockPos() != ((BlockRayTraceResult) objectMouseOver).getBlockPos()) {
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

			//Init shaders in the first tick. Doing it anywhere before this seems to crash the game.
			if (!shadersInitialized) {
				ShaderHandler.init();
				shadersInitialized = true;
			}
		}

	}

	private static void onMouseInput() {
		Minecraft mc = Minecraft.getInstance();
		ClientPlayerEntity player = mc.player;
		if (player == null) return;
		BuildModes.BuildModeEnum buildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();

		if (Minecraft.getInstance().screen != null ||
			buildMode == BuildModes.BuildModeEnum.NORMAL ||
			RadialMenu.instance.isVisible()) {
			return;
		}

		if (mc.options.keyUse.isDown()) {

			//KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);

			if (placeCooldown <= 0) {
				placeCooldown = 4;

				ItemStack currentItemStack = player.getItemInHand(Hand.MAIN_HAND);
				if (currentItemStack.getItem() instanceof BlockItem ||
					(CompatHelper.isItemBlockProxy(currentItemStack) && !player.isShiftKeyDown())) {

					ItemStack itemStack = CompatHelper.getItemBlockFromStack(currentItemStack);

					//find position in distance
					RayTraceResult lookingAt = getLookingAt(player);
					if (lookingAt != null && lookingAt.getType() == RayTraceResult.Type.BLOCK) {
						BlockRayTraceResult blockLookingAt = (BlockRayTraceResult) lookingAt;

						BuildModes.onBlockPlacedMessage(player, new BlockPlacedMessage(blockLookingAt, true));
						PacketHandler.INSTANCE.sendToServer(new BlockPlacedMessage(blockLookingAt, true));

						//play sound if further than normal
						if ((blockLookingAt.getLocation().subtract(player.getEyePosition(1f))).lengthSqr() > 25f &&
							itemStack.getItem() instanceof BlockItem) {

							BlockState state = ((BlockItem) itemStack.getItem()).getBlock().defaultBlockState();
							BlockPos blockPos = blockLookingAt.getBlockPos();
							SoundType soundType = state.getBlock().getSoundType(state, player.level, blockPos, player);
							player.level.playSound(player, player.blockPosition(), soundType.getPlaceSound(), SoundCategory.BLOCKS,
								0.4f, soundType.getPitch());
							player.swing(Hand.MAIN_HAND);
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

				//Early out if cant break far, coming from own mouse event (not block broken event)
				//To make breaking blocks in survival possible like array
				//TODO this causes not being able to cancel placement in survival
				//  moving it to after buildmodes fixes that, but introduces this bug
				if (!ReachHelper.canBreakFar(player)) return;

				RayTraceResult lookingAt = getLookingAt(player);
				if (lookingAt != null && lookingAt.getType() == RayTraceResult.Type.BLOCK) {
					BlockRayTraceResult blockLookingAt = (BlockRayTraceResult) lookingAt;

					BuildModes.onBlockBrokenMessage(player, new BlockBrokenMessage(blockLookingAt));
					PacketHandler.INSTANCE.sendToServer(new BlockBrokenMessage(blockLookingAt));

					//play sound if further than normal
					if ((blockLookingAt.getLocation().subtract(player.getEyePosition(1f))).lengthSqr() > 25f) {

						BlockPos blockPos = blockLookingAt.getBlockPos();
						BlockState state = player.level.getBlockState(blockPos);
						SoundType soundtype = state.getBlock().getSoundType(state, player.level, blockPos, player);
						player.level.playSound(player, player.blockPosition(), soundtype.getBreakSound(), SoundCategory.BLOCKS,
							0.4f, soundtype.getPitch());
						player.swing(Hand.MAIN_HAND);
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
	public static void onKeyPress(InputEvent.KeyInputEvent event) {
		ClientPlayerEntity player = Minecraft.getInstance().player;
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
			EffortlessBuilding.log(player, "Set " + TextFormatting.GOLD + "Quick Replace " + TextFormatting.RESET + (
				modifierSettings.doQuickReplace() ? "on" : "off"));
			PacketHandler.INSTANCE.sendToServer(new ModifierSettingsMessage(modifierSettings));
		}

		//Creative/survival mode toggle
		if (keyBindings[2].consumeClick()) {
			if (player.isCreative()) {
				player.chat("/gamemode survival");
			} else {
				player.chat("/gamemode creative");
			}
		}

		//Undo (Ctrl+Z)
		if (keyBindings[4].consumeClick()) {
			ModeOptions.ActionEnum action = ModeOptions.ActionEnum.UNDO;
			ModeOptions.performAction(player, action);
			PacketHandler.INSTANCE.sendToServer(new ModeActionMessage(action));
		}

		//Redo (Ctrl+Y)
		if (keyBindings[5].consumeClick()) {
			ModeOptions.ActionEnum action = ModeOptions.ActionEnum.REDO;
			ModeOptions.performAction(player, action);
			PacketHandler.INSTANCE.sendToServer(new ModeActionMessage(action));
		}

		//Change placement mode
		if (keyBindings[6].consumeClick()) {
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

		//For shader development
		if (keyBindings.length >= 8 && keyBindings[7].consumeClick()) {
			ShaderHandler.init();
			EffortlessBuilding.log(player, "Reloaded shaders");
		}

	}

	public static void openModifierSettings() {
		Minecraft mc = Minecraft.getInstance();
		ClientPlayerEntity player = mc.player;
		if (player == null)
			return;

		RadialMenu.instance.setVisibility(0f);

		//Disabled if max reach is 0, might be set in the config that way.
		if (ReachHelper.getMaxReach(player) == 0) {
			EffortlessBuilding.log(player, "Build modifiers are disabled until your reach has increased. Increase your reach with craftable reach upgrades.");
		} else {
			if (mc.screen == null) {
				mc.setScreen(new ModifierSettingsGui());
			} else {
				player.closeContainer();
			}
		}
	}

	public static void openPlayerSettings() {
		Minecraft mc = Minecraft.getInstance();
		ClientPlayerEntity player = mc.player;
		if (player == null)
			return;

		RadialMenu.instance.setVisibility(0f);

		//Disabled if max reach is 0, might be set in the config that way.
		if (mc.screen == null) {
			mc.setScreen(new PlayerSettingsGui());
		} else {
			player.closeContainer();
		}
	}

	@SubscribeEvent
	public static void onGuiOpen(GuiOpenEvent event) {
		PlayerEntity player = Minecraft.getInstance().player;
		if (player != null) {
			BuildModes.initializeMode(player);
		}
	}

	public static RayTraceResult getLookingAt(PlayerEntity player) {
		World world = player.level;

		//base distance off of player ability (config)
		float raytraceRange = ReachHelper.getPlacementReach(player);

		Vector3d look = player.getLookAngle();
		Vector3d start = new Vector3d(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
		Vector3d end = new Vector3d(player.getX() + look.x * raytraceRange, player.getY() + player.getEyeHeight() + look.y * raytraceRange, player.getZ() + look.z * raytraceRange);
//        return player.rayTrace(raytraceRange, 1f, RayTraceFluidMode.NEVER);
		//TODO 1.14 check if correct
		return world.clip(new RayTraceContext(start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
	}

	@Override
	public void setup(FMLCommonSetupEvent event) {
	}

	@Override
	public void clientSetup(FMLClientSetupEvent event) {
		// register key bindings
		keyBindings = new KeyBinding[7];

		// instantiate the key bindings
		keyBindings[0] = new KeyBinding("key.effortlessbuilding.hud.desc", KeyConflictContext.UNIVERSAL, InputMappings.getKey(GLFW.GLFW_KEY_KP_ADD, 0), "key.effortlessbuilding.category");
		keyBindings[1] = new KeyBinding("key.effortlessbuilding.replace.desc", KeyConflictContext.IN_GAME, InputMappings.getKey(GLFW.GLFW_KEY_KP_SUBTRACT, 0), "key.effortlessbuilding.category");
		keyBindings[2] = new KeyBinding("key.effortlessbuilding.creative.desc", KeyConflictContext.IN_GAME, InputMappings.getKey(GLFW.GLFW_KEY_F4, 0), "key.effortlessbuilding.category");
		keyBindings[3] = new KeyBinding("key.effortlessbuilding.mode.desc", KeyConflictContext.IN_GAME, InputMappings.getKey(GLFW.GLFW_KEY_LEFT_ALT, 0), "key.effortlessbuilding.category") {
			@Override
			public boolean same(KeyBinding other) {
				//Does not conflict with Chisels and Bits radial menu
				if (other.getKey().getValue() == getKey().getValue() && other.getName().equals("mod.chiselsandbits.other.mode"))
					return false;
				return super.same(other);
			}
		};
		keyBindings[4] = new KeyBinding("key.effortlessbuilding.undo.desc", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputMappings.getKey(GLFW.GLFW_KEY_Z, 0), "key.effortlessbuilding.category");
		keyBindings[5] = new KeyBinding("key.effortlessbuilding.redo.desc", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputMappings.getKey(GLFW.GLFW_KEY_Y, 0), "key.effortlessbuilding.category");
		keyBindings[6] = new KeyBinding("key.effortlessbuilding.altplacement.desc", KeyConflictContext.IN_GAME, InputMappings.getKey(GLFW.GLFW_KEY_LEFT_CONTROL, 0), "key.effortlessbuilding.category");
		//keyBindings[7] = new KeyBinding("Reload shaders", KeyConflictContext.UNIVERSAL, InputMappings.getInputByCode(GLFW.GLFW_KEY_TAB, 0), "key.effortlessbuilding.category");

		// register all the key bindings
		for (KeyBinding keyBinding : keyBindings) {
			ClientRegistry.registerKeyBinding(keyBinding);
		}

		DeferredWorkQueue.runLater(() -> ScreenManager.register(EffortlessBuilding.RANDOMIZER_BAG_CONTAINER.get(), RandomizerBagScreen::new));
	}

	public PlayerEntity getPlayerEntityFromContext(Supplier<NetworkEvent.Context> ctx) {
		return (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT ? Minecraft.getInstance().player : ctx.get().getSender());
	}

	@Override
	public void logTranslate(PlayerEntity player, String prefix, String translationKey, String suffix, boolean actionBar) {
		EffortlessBuilding.log(Minecraft.getInstance().player, prefix + I18n.get(translationKey) + suffix, actionBar);
	}
}
