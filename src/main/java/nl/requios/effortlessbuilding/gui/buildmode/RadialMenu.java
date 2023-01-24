package nl.requios.effortlessbuilding.gui.buildmode;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Vector4f;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import nl.requios.effortlessbuilding.ClientEvents;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.buildmode.ModeOptions;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.opengl.GL11;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

import static nl.requios.effortlessbuilding.buildmode.ModeOptions.*;

import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.ModeOptions.ActionEnum;
import nl.requios.effortlessbuilding.buildmode.ModeOptions.OptionEnum;

/**
 * Initially from Chisels and Bits by AlgorithmX2
 * https://github.com/AlgorithmX2/Chisels-and-Bits/blob/1.12/src/main/java/mod/chiselsandbits/client/gui/ChiselsAndBitsMenu.java
 */

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RadialMenu extends Screen {

	public static final RadialMenu instance = new RadialMenu();

	private final Vector4f radialButtonColor = new Vector4f(0f, 0f, 0f, .5f);
	private final Vector4f sideButtonColor = new Vector4f(.5f, .5f, .5f, .5f);
	private final Vector4f highlightColor = new Vector4f(.6f, .8f, 1f, .6f);
	private final Vector4f selectedColor = new Vector4f(0f, .5f, 1f, .5f);
	private final Vector4f highlightSelectedColor = new Vector4f(0.2f, .7f, 1f, .7f);

	private final int whiteTextColor = 0xffffffff;
	private final int watermarkTextColor = 0x88888888;
	private final int descriptionTextColor = 0xdd888888;
	private final int optionTextColor = 0xeeeeeeff;

	private final double ringInnerEdge = 30;
	private final double ringOuterEdge = 65;
	private final double categoryLineWidth = 1;
	private final double textDistance = 75;
	private final double buttonDistance = 105;
	private final float fadeSpeed = 0.3f;
	private final int buildModeDescriptionHeight = 100;
	private final int actionDescriptionWidth = 200;

	public BuildModeEnum switchTo = null;
	public ActionEnum doAction = null;
	public boolean performedActionUsingMouse;

	private float visibility;

	public RadialMenu() {
		super(Component.translatable("effortlessbuilding.screen.radial_menu"));
	}

	public boolean isVisible() {
		return Minecraft.getInstance().screen instanceof RadialMenu;
	}

	@Override
	protected void init() {
		super.init();
		performedActionUsingMouse = false;
		visibility = 0f;
	}

	@Override
	public void tick() {
		super.tick();

		if (!ClientEvents.isKeybindDown(0)) {
			onClose();
		}
	}

	@Override
	public void render(PoseStack ms, final int mouseX, final int mouseY, final float partialTicks) {
		BuildModeEnum currentBuildMode = EffortlessBuildingClient.BUILD_MODES.getBuildMode();

		ms.pushPose();
		ms.translate(0, 0, 200);

		visibility += fadeSpeed * partialTicks;
		if (visibility > 1f) visibility = 1f;

		final int startColor = (int) (visibility * 98) << 24;
		final int endColor = (int) (visibility * 128) << 24;

		fillGradient(ms, 0, 0, width, height, startColor, endColor);

		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
		final Tesselator tessellator = Tesselator.getInstance();
		final BufferBuilder buffer = tessellator.getBuilder();

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		final double middleX = width / 2.0;
		final double middleY = height / 2.0;

		//Fix for high def (retina) displays: use custom mouse coordinates
		//Borrowed from GameRenderer::updateCameraAndRender
		int mouseXX = (int) (minecraft.mouseHandler.xpos() * (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth());
		int mouseYY = (int) (minecraft.mouseHandler.ypos() * (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight());

		final double mouseXCenter = mouseXX - middleX;
		final double mouseYCenter = mouseYY - middleY;
		double mouseRadians = Math.atan2(mouseYCenter, mouseXCenter);

		final double quarterCircle = Math.PI / 2.0;

		if (mouseRadians < -quarterCircle) {
			mouseRadians = mouseRadians + Math.PI * 2;
		}

		final ArrayList<MenuRegion> modes = new ArrayList<MenuRegion>();
		final ArrayList<MenuButton> buttons = new ArrayList<MenuButton>();

		//Add build modes
		for (final BuildModeEnum mode : BuildModeEnum.values()) {
			modes.add(new MenuRegion(mode));
		}

		//Add actions
//		buttons.add(new MenuButton(ActionEnum.OPEN_PLAYER_SETTINGS, -buttonDistance - 65, -13, Direction.UP));
		buttons.add(new MenuButton(ActionEnum.TOGGLE_PROTECT_TILE_ENTITIES, -buttonDistance - 78, -13, Direction.UP));
		buttons.add(new MenuButton(ActionEnum.OPEN_MODIFIER_SETTINGS, -buttonDistance - 52, -13, Direction.UP));
		buttons.add(new MenuButton(ActionEnum.UNDO, -buttonDistance - 26, -13, Direction.UP));
		buttons.add(new MenuButton(ActionEnum.REDO, -buttonDistance, -13, Direction.UP));

		buttons.add(new MenuButton(ActionEnum.REPLACE_ONLY_AIR, -buttonDistance - 78, 13, Direction.DOWN));
		buttons.add(new MenuButton(ActionEnum.REPLACE_BLOCKS_AND_AIR, -buttonDistance - 52, 13, Direction.DOWN));
		buttons.add(new MenuButton(ActionEnum.REPLACE_ONLY_BLOCKS, -buttonDistance - 26, 13, Direction.DOWN));
		buttons.add(new MenuButton(ActionEnum.REPLACE_FILTERED_BY_OFFHAND, -buttonDistance, 13, Direction.DOWN));

		//Add buildmode dependent options
		OptionEnum[] options = currentBuildMode.options;
		for (int i = 0; i < options.length; i++) {
			for (int j = 0; j < options[i].actions.length; j++) {
				ActionEnum action = options[i].actions[j];
				buttons.add(new MenuButton(action, buttonDistance + j * 26, -13 + i * 39, Direction.DOWN));
			}
		}

		switchTo = null;
		doAction = null;

		//Draw buildmode backgrounds
		drawRadialButtonBackgrounds(currentBuildMode, buffer, middleX, middleY, mouseXCenter, mouseYCenter, mouseRadians,
				quarterCircle, modes);

		//Draw action backgrounds
		drawSideButtonBackgrounds(buffer, middleX, middleY, mouseXCenter, mouseYCenter, buttons);

		tessellator.end();
		RenderSystem.disableBlend();
		RenderSystem.enableTexture();

		drawIcons(ms, middleX, middleY, modes, buttons);

		drawTexts(ms, currentBuildMode, middleX, middleY, modes, buttons, options);

		ms.popPose();
	}

	private void drawRadialButtonBackgrounds(BuildModeEnum currentBuildMode, BufferBuilder buffer, double middleX, double middleY,
											 double mouseXCenter, double mouseYCenter, double mouseRadians, double quarterCircle, ArrayList<MenuRegion> modes) {
		if (!modes.isEmpty()) {
			final int totalModes = Math.max(3, modes.size());
			final double fragment = Math.PI * 0.005; //gap between buttons in radians at inner edge
			final double fragment2 = Math.PI * 0.0025; //gap between buttons in radians at outer edge
			final double radiansPerObject = 2.0 * Math.PI / totalModes;

			for (int i = 0; i < modes.size(); i++) {
				MenuRegion menuRegion = modes.get(i);
				final double beginRadians = i * radiansPerObject - quarterCircle;
				final double endRadians = (i + 1) * radiansPerObject - quarterCircle;

				menuRegion.x1 = Math.cos(beginRadians);
				menuRegion.x2 = Math.cos(endRadians);
				menuRegion.y1 = Math.sin(beginRadians);
				menuRegion.y2 = Math.sin(endRadians);

				final double x1m1 = Math.cos(beginRadians + fragment) * ringInnerEdge;
				final double x2m1 = Math.cos(endRadians - fragment) * ringInnerEdge;
				final double y1m1 = Math.sin(beginRadians + fragment) * ringInnerEdge;
				final double y2m1 = Math.sin(endRadians - fragment) * ringInnerEdge;

				final double x1m2 = Math.cos(beginRadians + fragment2) * ringOuterEdge;
				final double x2m2 = Math.cos(endRadians - fragment2) * ringOuterEdge;
				final double y1m2 = Math.sin(beginRadians + fragment2) * ringOuterEdge;
				final double y2m2 = Math.sin(endRadians - fragment2) * ringOuterEdge;

				final boolean isSelected = currentBuildMode.ordinal() == i;
				final boolean isMouseInQuad = inTriangle(x1m1, y1m1, x2m2, y2m2, x2m1, y2m1, mouseXCenter, mouseYCenter)
											  || inTriangle(x1m1, y1m1, x1m2, y1m2, x2m2, y2m2, mouseXCenter, mouseYCenter);
				final boolean isHighlighted = beginRadians <= mouseRadians && mouseRadians <= endRadians && isMouseInQuad;

				Vector4f color = radialButtonColor;
				if (isSelected) color = selectedColor;
				if (isHighlighted) color = highlightColor;
				if (isSelected && isHighlighted) color = highlightSelectedColor;

				if (isHighlighted) {
					menuRegion.highlighted = true;
					switchTo = menuRegion.mode;
				}

				buffer.vertex(middleX + x1m1, middleY + y1m1, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
				buffer.vertex(middleX + x2m1, middleY + y2m1, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
				buffer.vertex(middleX + x2m2, middleY + y2m2, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
				buffer.vertex(middleX + x1m2, middleY + y1m2, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();

				//Category line
				color = menuRegion.mode.category.color;
				final double categoryLineOuterEdge = ringInnerEdge + categoryLineWidth;

				final double x1m3 = Math.cos(beginRadians + fragment) * categoryLineOuterEdge;
				final double x2m3 = Math.cos(endRadians - fragment) * categoryLineOuterEdge;
				final double y1m3 = Math.sin(beginRadians + fragment) * categoryLineOuterEdge;
				final double y2m3 = Math.sin(endRadians - fragment) * categoryLineOuterEdge;

				buffer.vertex(middleX + x1m1, middleY + y1m1, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
				buffer.vertex(middleX + x2m1, middleY + y2m1, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
				buffer.vertex(middleX + x2m3, middleY + y2m3, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
				buffer.vertex(middleX + x1m3, middleY + y1m3, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
			}
		}
	}

	private void drawSideButtonBackgrounds(BufferBuilder buffer, double middleX, double middleY, double mouseXCenter, double mouseYCenter, ArrayList<MenuButton> buttons) {
		for (final MenuButton btn : buttons) {

			final boolean isHighlighted = btn.x1 <= mouseXCenter && btn.x2 >= mouseXCenter && btn.y1 <= mouseYCenter && btn.y2 >= mouseYCenter;

			boolean isSelected =
					btn.action == getBuildSpeed() ||
					btn.action == getFill() ||
					btn.action == getCubeFill() ||
					btn.action == getRaisedEdge() ||
					btn.action == getLineThickness() ||
					btn.action == getCircleStart() ||
					btn.action == EffortlessBuildingClient.BUILD_SETTINGS.getReplaceModeActionEnum() ||
					btn.action == ActionEnum.TOGGLE_PROTECT_TILE_ENTITIES && EffortlessBuildingClient.BUILD_SETTINGS.shouldProtectTileEntities();

			Vector4f color = sideButtonColor;
			if (isSelected) color = selectedColor;
			if (isHighlighted) color = highlightColor;
			if (isSelected && isHighlighted) color = highlightSelectedColor;

			if (isHighlighted) {
				btn.highlighted = true;
				doAction = btn.action;
			}

			buffer.vertex(middleX + btn.x1, middleY + btn.y1, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
			buffer.vertex(middleX + btn.x1, middleY + btn.y2, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
			buffer.vertex(middleX + btn.x2, middleY + btn.y2, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
			buffer.vertex(middleX + btn.x2, middleY + btn.y1, getBlitOffset()).color(color.x(), color.y(), color.z(), color.w()).endVertex();
		}
	}

	private void drawIcons(PoseStack ms, double middleX, double middleY,
						   ArrayList<MenuRegion> modes, ArrayList<MenuButton> buttons) {
		ms.pushPose();
		RenderSystem.enableTexture();
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		//Draw buildmode icons
		for (final MenuRegion menuRegion : modes) {

			final double x = (menuRegion.x1 + menuRegion.x2) * 0.5 * (ringOuterEdge * 0.55 + 0.45 * ringInnerEdge);
			final double y = (menuRegion.y1 + menuRegion.y2) * 0.5 * (ringOuterEdge * 0.55 + 0.45 * ringInnerEdge);

			RenderSystem.setShaderTexture(0, new ResourceLocation(EffortlessBuilding.MODID, "textures/icons/" + menuRegion.mode.name().toLowerCase() + ".png"));
			blit(ms, (int) (middleX + x - 8), (int) (middleY + y - 8), 16, 16, 0, 0, 18, 18, 18, 18);
		}

		//Draw action icons
		for (final MenuButton button : buttons) {

			final double x = (button.x1 + button.x2) / 2 + 0.01;
			final double y = (button.y1 + button.y2) / 2 + 0.01;

			RenderSystem.setShaderTexture(0, new ResourceLocation(EffortlessBuilding.MODID, "textures/icons/" + button.action.name().toLowerCase() + ".png"));
			blit(ms, (int) (middleX + x - 8), (int) (middleY + y - 8), 16, 16, 0, 0, 18, 18, 18, 18);
		}

		ms.popPose();
	}

	private void drawTexts(PoseStack ms, BuildModeEnum currentBuildMode, double middleX, double middleY, ArrayList<MenuRegion> modes, ArrayList<MenuButton> buttons, OptionEnum[] options) {
		//font.drawStringWithShadow("Actions", (int) (middleX - buttonDistance - 13) - font.getStringWidth("Actions") * 0.5f, (int) middleY - 38, 0xffffffff);

		//Draw option strings
		for (int i = 0; i < currentBuildMode.options.length; i++) {
			OptionEnum option = options[i];
			font.drawShadow(ms, I18n.get(option.name), (int) (middleX + buttonDistance - 9), (int) middleY - 37 + i * 39, optionTextColor);
		}

		String credits = "Effortless Building";
		font.drawShadow(ms, credits, width - font.width(credits) - 4, height - 10, watermarkTextColor);

		//Draw buildmode text
		for (final MenuRegion menuRegion : modes) {

			if (menuRegion.highlighted) {
				final double x = (menuRegion.x1 + menuRegion.x2) * 0.5;
				final double y = (menuRegion.y1 + menuRegion.y2) * 0.5;

				int fixed_x = (int) (x * textDistance);
				int fixed_y = (int) (y * textDistance) - font.lineHeight / 2;
				String text = I18n.get(menuRegion.mode.getNameKey());

				if (x <= -0.2) {
					fixed_x -= font.width(text);
				} else if (-0.2 <= x && x <= 0.2) {
					fixed_x -= font.width(text) / 2;
				}

				font.drawShadow(ms, text, (int) middleX + fixed_x, (int) middleY + fixed_y, whiteTextColor);

				//Draw description
				text = I18n.get(menuRegion.mode.getDescriptionKey());
				font.drawShadow(ms, text, (int) middleX - font.width(text) / 2f, (int) middleY + buildModeDescriptionHeight, descriptionTextColor);
			}
		}

		//Draw action text
		for (final MenuButton button : buttons) {
			if (button.highlighted) {

				String text = ChatFormatting.AQUA + button.name;
				String description = ChatFormatting.WHITE + button.description;

				//Add keybind in brackets
				String keybind = findKeybind(button, currentBuildMode);
				boolean hasKeybind = !keybind.isEmpty();
				keybind = ChatFormatting.GRAY + "(" + WordUtils.capitalizeFully(keybind) + ")";

				if (button.textSide == Direction.WEST) {

					font.draw(ms, text, (int) (middleX + button.x1 - 8) - font.width(text),
							(int) (middleY + button.y1 + 6), whiteTextColor);

				} else if (button.textSide == Direction.EAST) {

					font.draw(ms, text, (int) (middleX + button.x2 + 8),
							(int) (middleY + button.y1 + 6), whiteTextColor);

				} else if (button.textSide == Direction.UP || button.textSide == Direction.NORTH) {

					int y = (int) (middleY + button.y1 - 14);
					font.draw(ms, text, (int) (middleX + (button.x1 + button.x2) * 0.5 - font.width(text) * 0.5), y, whiteTextColor);

					y -= 12;
					if (hasKeybind) {
						font.draw(ms, keybind, (int) (middleX + (button.x1 + button.x2) * 0.5 - font.width(keybind) * 0.5), y, whiteTextColor);
						y -= 12;
					}

					if (!description.isEmpty())
						font.drawWordWrap(FormattedText.of(description), (int) (middleX + (button.x1 + button.x2) * 0.5 - actionDescriptionWidth * 0.5), y, actionDescriptionWidth, whiteTextColor);

				} else if (button.textSide == Direction.DOWN || button.textSide == Direction.SOUTH) {

					int y = (int) (middleY + button.y1 + 26);
					font.draw(ms, text, (int) (middleX + (button.x1 + button.x2) * 0.5 - font.width(text) * 0.5), y, whiteTextColor);

					y += 12;
					if (hasKeybind) {
						font.draw(ms, keybind, (int) (middleX + (button.x1 + button.x2) * 0.5 - font.width(keybind) * 0.5), y, whiteTextColor);
						y += 12;
					}

					if (!description.isEmpty())
						font.drawWordWrap(FormattedText.of(description), (int) (middleX + (button.x1 + button.x2) * 0.5 - actionDescriptionWidth * 0.5), y, actionDescriptionWidth, whiteTextColor);

				}

			}
		}
	}

	private String findKeybind(MenuButton button, BuildModeEnum currentBuildMode){
		String result = "";
		int keybindingIndex = -1;
		if (button.action == ActionEnum.OPEN_MODIFIER_SETTINGS) keybindingIndex = 1;
		if (button.action == ActionEnum.UNDO) keybindingIndex = 2;
		if (button.action == ActionEnum.REDO) keybindingIndex = 3;

		if (keybindingIndex != -1) {
			KeyMapping keyMap = ClientEvents.keyBindings[keybindingIndex];

			if (!keyMap.getKeyModifier().name().equals("None")) {
				result = keyMap.getKeyModifier().name() + " ";
			}
			result += I18n.get(keyMap.getKey().getName());
		}

		if (currentBuildMode.options.length > 0) {
			//Add (ctrl) to first two actions of first option
			if (button.action == currentBuildMode.options[0].actions[0]
				|| button.action == currentBuildMode.options[0].actions[1]) {
				result = I18n.get(ClientEvents.keyBindings[4].getKey().getDisplayName().getString());
				if (result.equals("Left Control")) result = "Ctrl";
			}
		}

		result = result.replace("Key.keyboard.", "");
		return result;
	}

	private boolean inTriangle(final double x1, final double y1, final double x2, final double y2,
							   final double x3, final double y3, final double x, final double y) {
		final double ab = (x1 - x) * (y2 - y) - (x2 - x) * (y1 - y);
		final double bc = (x2 - x) * (y3 - y) - (x3 - x) * (y2 - y);
		final double ca = (x3 - x) * (y1 - y) - (x1 - x) * (y3 - y);
		return sign(ab) == sign(bc) && sign(bc) == sign(ca);
	}

	private int sign(final double n) {
		return n > 0 ? 1 : -1;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		performAction(true);

		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void onClose() {
		super.onClose();
		//After onClose so it can open another screen
		if (!performedActionUsingMouse) performAction(false);
	}

	private void performAction(boolean fromMouseClick) {
		LocalPlayer player = Minecraft.getInstance().player;

		if (switchTo != null) {
			playRadialMenuSound();

			EffortlessBuildingClient.BUILD_MODES.setBuildMode(switchTo);

			EffortlessBuilding.log(player, I18n.get(switchTo.getNameKey()), true);

			if (fromMouseClick) performedActionUsingMouse = true;
		}

		//Perform button action
		ModeOptions.ActionEnum action = doAction;
		if (action != null) {
			playRadialMenuSound();

			ModeOptions.performAction(player, action);

			if (fromMouseClick) performedActionUsingMouse = true;
		}
	}

	public static void playRadialMenuSound() {
		final float volume = 0.1f;
		if (volume >= 0.0001f) {
			SimpleSoundInstance sound = new SimpleSoundInstance(SoundEvents.UI_BUTTON_CLICK, SoundSource.MASTER, volume,
					1.0f, RandomSource.create(), Minecraft.getInstance().player.blockPosition());
			Minecraft.getInstance().getSoundManager().play(sound);
		}
	}

	private static class MenuButton {

		public final ActionEnum action;
		public double x1, x2;
		public double y1, y2;
		public boolean highlighted;
		public String name;
		public String description = "";
		public Direction textSide;

		public MenuButton(final ActionEnum action, final double x, final double y,
						  final Direction textSide) {
			this.name = I18n.get(action.name);

			if (I18n.exists(action.name + ".description")) {
				this.description = I18n.get(action.name + ".description");
			}

			this.action = action;
			x1 = x - 10;
			x2 = x + 10;
			y1 = y - 10;
			y2 = y + 10;
			this.textSide = textSide;
		}

	}

	static class MenuRegion {

		public final BuildModeEnum mode;
		public double x1, x2;
		public double y1, y2;
		public boolean highlighted;

		public MenuRegion(final BuildModeEnum mode) {
			this.mode = mode;
		}

	}

}

