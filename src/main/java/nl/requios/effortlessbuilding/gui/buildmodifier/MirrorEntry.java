package nl.requios.effortlessbuilding.gui.buildmodifier;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.AllGuiTextures;
import nl.requios.effortlessbuilding.AllIcons;
import nl.requios.effortlessbuilding.buildmodifier.BaseModifier;
import nl.requios.effortlessbuilding.buildmodifier.Mirror;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.IconButton;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.ScrollInput;
import nl.requios.effortlessbuilding.create.foundation.utility.Components;
import nl.requios.effortlessbuilding.gui.elements.LabeledScrollInput;
import nl.requios.effortlessbuilding.utilities.MathHelper;
import nl.requios.effortlessbuilding.utilities.ReachHelper;

import java.text.DecimalFormat;
import java.util.Vector;

@OnlyIn(Dist.CLIENT)
public class MirrorEntry extends BaseModifierEntry<Mirror> {
	
	protected Vector<ScrollInput> positionInputs;
	protected IconButton playerPositionButton;
	protected IconButton toggleOffsetButton;
	protected Vector<IconButton> axisButtons;
	protected ScrollInput radiusInput;
	protected IconButton showLinesButton;
	protected IconButton showAreasButton;
	protected DecimalFormat df = new DecimalFormat("#.#");
	
	public MirrorEntry(ModifiersScreen screen, BaseModifier mirror) {
		super(screen, (Mirror) mirror, Component.literal("Mirror"), AllGuiTextures.MIRROR_ENTRY);
		
		positionInputs = new Vector<>();
		axisButtons = new Vector<>();
		
		//Position
		//ScrollInput works with double the value, so we can have 0.5 increments
		for (int i = 0; i < 3; i++) {
			final int index = i;
			var scrollInput = new LabeledScrollInput(0, 0, 27, 18)
				.showControlScrollsSlowerTooltip()
				.titled(Component.literal(i == 0 ? "X Position" : i == 1 ? "Y Position" : "Z Position"))
				.format(integer -> Component.literal(df.format(integer / 2.0)))
				.withStepFunction(stepContext -> stepContext.shift ? 20 : stepContext.control ? 1 : 2)
				.calling(value -> {
					modifier.position = MathHelper.with(modifier.position, index, value / 2.0);
					onValueChanged();
				});
			scrollInput.setState((int) (MathHelper.get(modifier.position, index) * 2.0));
			positionInputs.add(scrollInput);
		}
		listeners.addAll(positionInputs);
		
		//Player position button
		playerPositionButton = new IconButton(0, 0, AllIcons.I_PLAYER)
			.withCallback(() -> {
				modifier.position = Vec3.atLowerCornerOf(Minecraft.getInstance().player.blockPosition());
				onValueChanged();
			});
		playerPositionButton.setToolTip(Components.literal("Set to player position"));
		listeners.add(playerPositionButton);
		
		//Toggle offset button
		toggleOffsetButton = new IconButton(0, 0, AllIcons.I_BLOCK_CENTER)
			.withCallback(() -> {
				if (modifier.position.x == Math.floor(modifier.position.x)) {
					modifier.position = new Vec3(
						Math.floor(modifier.position.x) + 0.5,
						Math.floor(modifier.position.y) + 0.5,
						Math.floor(modifier.position.z) + 0.5
					);
				}
				else {
					modifier.position = new Vec3(
						Math.floor(modifier.position.x),
						Math.floor(modifier.position.y),
						Math.floor(modifier.position.z)
					);
				}
				onValueChanged();
			});
		listeners.add(toggleOffsetButton);
		
		//Axis buttons
		for (int i = 0; i < 3; i++) {
			final int index = i;
			IconButton button = new IconButton(0, 0, AllIcons.I_SHOW_LINES)
				.withCallback(() -> {
					modifier.toggleMirrorAxis(index);
					onValueChanged();
				});
			button.setToolTip(Components.literal(i == 0 ? "X Axis" : i == 1 ? "Y Axis" : "Z Axis"));
			axisButtons.add(button);
		}
		listeners.addAll(axisButtons);
		
		//Radius
		radiusInput = new LabeledScrollInput(0, 0, 27, 18)
			.withRange(0, ReachHelper.getMaxMirrorRadius(Minecraft.getInstance().player))
			.titled(Component.literal("Radius. Use Reach Upgrade items to increase maximum."))
			.calling(value -> {
				modifier.radius = value;
				onValueChanged();
			});
		radiusInput.setState(modifier.radius);
		listeners.add(radiusInput);
		
		//Show lines button
		showLinesButton = new IconButton(0, 0, AllIcons.I_SHOW_LINES)
			.withCallback(() -> {
				modifier.drawLines = !modifier.drawLines;
				onValueChanged();
			});
		listeners.add(showLinesButton);
		
		//Show areas button
		showAreasButton = new IconButton(0, 0, AllIcons.I_SHOW_AREAS)
			.withCallback(() -> {
				modifier.drawPlanes = !modifier.drawPlanes;
				onValueChanged();
			});
		listeners.add(showAreasButton);
		
		for (ScrollInput positionInput : positionInputs) {
			positionInput.onChanged();
		}
		radiusInput.onChanged();
	}
	
	@Override
	public void render(PoseStack ms, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
		super.render(ms, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);
		
		//draw position inputs
		for (int i = 0; i < 3; i++) {
			ScrollInput input = positionInputs.get(i);
			input.x = left + 47 + 29 * i;
			input.y = top + 18;
			input.render(ms, mouseX, mouseY, partialTicks);
		}
		
		//draw player position button
		playerPositionButton.x = left + 134;
		playerPositionButton.y = top + 18;
		playerPositionButton.render(ms, mouseX, mouseY, partialTicks);
		
		//draw toggle offset button
		toggleOffsetButton.x = left + 154;
		toggleOffsetButton.y = top + 18;
		toggleOffsetButton.render(ms, mouseX, mouseY, partialTicks);
		
		//draw axis buttons
		for (int i = 0; i < 3; i++) {
			IconButton button = axisButtons.get(i);
			button.x = left + 47 + 18 * i;
			button.y = top + 38;
			button.render(ms, mouseX, mouseY, partialTicks);
		}
		
		//draw radius input
		radiusInput.x = left + 145;
		radiusInput.y = top + 38;
		radiusInput.render(ms, mouseX, mouseY, partialTicks);
		
		//draw show lines button
		showLinesButton.x = right - 43;
		showLinesButton.y = top + 38;
		showLinesButton.render(ms, mouseX, mouseY, partialTicks);
		
		//draw show areas button
		showAreasButton.x = right - 23;
		showAreasButton.y = top + 38;
		showAreasButton.render(ms, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void onValueChanged() {
		super.onValueChanged();
		
		//Position
		for (int i = 0; i < 3; i++) {
			ScrollInput input = positionInputs.get(i);
			input.setState((int) (MathHelper.get(modifier.position, i) * 2.0));
		}
		
		//Toggle offset button
		if (modifier.position.x == Math.floor(modifier.position.x)) {
			toggleOffsetButton.setIcon(AllIcons.I_BLOCK_CENTER);
			toggleOffsetButton.setToolTip(Components.literal("Set mirror position to center of block, for uneven numbered builds."));
		}
		else {
			toggleOffsetButton.setIcon(AllIcons.I_BLOCK_CORNER);
			toggleOffsetButton.setToolTip(Components.literal("Set mirror position to corner of block, for even numbered builds."));
		}
		
		//Axis buttons
		for (int i = 0; i < 3; i++) {
			IconButton button = axisButtons.get(i);
			if (modifier.getMirrorAxis(i)) {
				button.setIcon(AllIcons.I_SHOW_LINES);
			}
			else {
				button.setIcon(AllIcons.I_HIDE_LINES);
			}
		}
		
		//Show lines button
		if (modifier.drawLines) {
			showLinesButton.setIcon(AllIcons.I_SHOW_LINES);
			showLinesButton.setToolTip(Components.literal("Hide mirror lines"));
		}
		else {
			showLinesButton.setIcon(AllIcons.I_HIDE_LINES);
			showLinesButton.setToolTip(Components.literal("Show mirror lines"));
		}
		
		//Show areas button
		if (modifier.drawPlanes) {
			showAreasButton.setIcon(AllIcons.I_SHOW_AREAS);
			showAreasButton.setToolTip(Components.literal("Hide mirror areas"));
		}
		else {
			showAreasButton.setIcon(AllIcons.I_HIDE_AREAS);
			showAreasButton.setToolTip(Components.literal("Show mirror areas"));
		}
	}
	
	//	protected static final ResourceLocation BUILDING_ICONS = new ResourceLocation(EffortlessBuilding.MODID, "textures/gui/building_icons.png");
//
//	protected List<Button> mirrorButtonList = new ArrayList<>();
//	protected List<GuiIconButton> mirrorIconButtonList = new ArrayList<>();
//	protected List<GuiNumberField> mirrorNumberFieldList = new ArrayList<>();
//
//	private GuiNumberField textMirrorPosX, textMirrorPosY, textMirrorPosZ, textMirrorRadius;
//	private GuiCheckBoxFixed buttonMirrorEnabled, buttonMirrorX, buttonMirrorY, buttonMirrorZ;
//	private GuiIconButton buttonCurrentPosition, buttonToggleOdd, buttonDrawPlanes, buttonDrawLines;
//	private boolean drawPlanes, drawLines, toggleOdd;
//
//	public MirrorEntry(GuiScrollPane scrollPane) {
//		super(scrollPane);
//	}
//
//	@Override
//	public void init(List<Widget> renderables) {
//		super.init(renderables);
//
//		int y = top - 2;
//		buttonMirrorEnabled = new GuiCheckBoxFixed(left - 15 + 8, y, "", false) {
//			@Override
//			public void onClick(double mouseX, double mouseY) {
//				super.onClick(mouseX, mouseY);
//				setCollapsed(!buttonMirrorEnabled.isChecked());
//			}
//		};
//		renderables.add(buttonMirrorEnabled);
//
//		y = top + 18;
//		textMirrorPosX = new GuiNumberField(font, renderables, left + 58, y, 62, 18);
//		textMirrorPosX.setNumber(0);
//		textMirrorPosX.setTooltip(
//			Arrays.asList(Component.literal("The position of the mirror."), Component.literal("For odd numbered builds add 0.5.").withStyle(ChatFormatting.GRAY)));
//		mirrorNumberFieldList.add(textMirrorPosX);
//
//		textMirrorPosY = new GuiNumberField(font, renderables, left + 138, y, 62, 18);
//		textMirrorPosY.setNumber(64);
//		textMirrorPosY.setTooltip(Arrays.asList(Component.literal("The position of the mirror."), Component.literal("For odd numbered builds add 0.5.").withStyle(ChatFormatting.GRAY)));
//		mirrorNumberFieldList.add(textMirrorPosY);
//
//		textMirrorPosZ = new GuiNumberField(font, renderables, left + 218, y, 62, 18);
//		textMirrorPosZ.setNumber(0);
//		textMirrorPosZ.setTooltip(Arrays.asList(Component.literal("The position of the mirror."), Component.literal("For odd numbered builds add 0.5.").withStyle(ChatFormatting.GRAY)));
//		mirrorNumberFieldList.add(textMirrorPosZ);
//
//		y = top + 50;
//		buttonMirrorX = new GuiCheckBoxFixed(left + 60, y, " X", true);
//		mirrorButtonList.add(buttonMirrorX);
//
//		buttonMirrorY = new GuiCheckBoxFixed(left + 100, y, " Y", false);
//		mirrorButtonList.add(buttonMirrorY);
//
//		buttonMirrorZ = new GuiCheckBoxFixed(left + 140, y, " Z", false);
//		mirrorButtonList.add(buttonMirrorZ);
//
//		y = top + 47;
//		textMirrorRadius = new GuiNumberField(font, renderables, left + 218, y, 62, 18);
//		textMirrorRadius.setNumber(50);
//		//TODO change to diameter (remove /2)
//		textMirrorRadius.setTooltip(Arrays.asList(Component.literal("How far the mirror reaches in any direction."),
//			Component.literal("Max: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(ReachHelper.getMaxReach(mc.player) / 2)).withStyle(ChatFormatting.GOLD)),
//			Component.literal("Upgradeable in survival with reach upgrades.").withStyle(ChatFormatting.GRAY)));
//		mirrorNumberFieldList.add(textMirrorRadius);
//
//		y = top + 72;
//		buttonCurrentPosition = new GuiIconButton(left + 5, y, 0, 0, BUILDING_ICONS, button -> {
//			Vec3 pos = new Vec3(Math.floor(mc.player.getX()) + 0.5, Math.floor(mc.player.getY()) + 0.5, Math.floor(mc.player.getZ()) + 0.5);
//			textMirrorPosX.setNumber(pos.x);
//			textMirrorPosY.setNumber(pos.y);
//			textMirrorPosZ.setNumber(pos.z);
//		});
//		buttonCurrentPosition.setTooltip(Component.literal("Set mirror position to current player position"));
//		mirrorIconButtonList.add(buttonCurrentPosition);
//
//		buttonToggleOdd = new GuiIconButton(left + 35, y, 0, 20, BUILDING_ICONS, button -> {
//			toggleOdd = !toggleOdd;
//			buttonToggleOdd.setUseAlternateIcon(toggleOdd);
//			if (toggleOdd) {
//				buttonToggleOdd.setTooltip(Arrays.asList(Component.literal("Set mirror position to corner of block"), Component.literal("for even numbered builds")));
//				textMirrorPosX.setNumber(textMirrorPosX.getNumber() + 0.5);
//				textMirrorPosY.setNumber(textMirrorPosY.getNumber() + 0.5);
//				textMirrorPosZ.setNumber(textMirrorPosZ.getNumber() + 0.5);
//			} else {
//				buttonToggleOdd.setTooltip(Arrays.asList(Component.literal("Set mirror position to middle of block"), Component.literal("for odd numbered builds")));
//				textMirrorPosX.setNumber(Math.floor(textMirrorPosX.getNumber()));
//				textMirrorPosY.setNumber(Math.floor(textMirrorPosY.getNumber()));
//				textMirrorPosZ.setNumber(Math.floor(textMirrorPosZ.getNumber()));
//			}
//		});
//		buttonToggleOdd.setTooltip(Arrays.asList(Component.literal("Set mirror position to middle of block"), Component.literal("for odd numbered builds")));
//		mirrorIconButtonList.add(buttonToggleOdd);
//
//		buttonDrawLines = new GuiIconButton(left + 65, y, 0, 40, BUILDING_ICONS, button -> {
//			drawLines = !drawLines;
//			buttonDrawLines.setUseAlternateIcon(drawLines);
//			buttonDrawLines.setTooltip(Component.literal(drawLines ? "Hide lines" : "Show lines"));
//		});
//		buttonDrawLines.setTooltip(Component.literal("Show lines"));
//		mirrorIconButtonList.add(buttonDrawLines);
//
//		buttonDrawPlanes = new GuiIconButton(left + 95, y, 0, 60, BUILDING_ICONS, button -> {
//			drawPlanes = !drawPlanes;
//			buttonDrawPlanes.setUseAlternateIcon(drawPlanes);
//			buttonDrawPlanes.setTooltip(Component.literal(drawPlanes ? "Hide area" : "Show area"));
//		});
//		buttonDrawPlanes.setTooltip(Component.literal("Show area"));
//		mirrorIconButtonList.add(buttonDrawPlanes);
//
//		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(mc.player);
//		if (modifierSettings != null) {
//			Mirror.MirrorSettings m = modifierSettings.getMirrorSettings();
//			buttonMirrorEnabled.setIsChecked(m.enabled);
//			textMirrorPosX.setNumber(m.position.x);
//			textMirrorPosY.setNumber(m.position.y);
//			textMirrorPosZ.setNumber(m.position.z);
//			buttonMirrorX.setIsChecked(m.mirrorX);
//			buttonMirrorY.setIsChecked(m.mirrorY);
//			buttonMirrorZ.setIsChecked(m.mirrorZ);
//			textMirrorRadius.setNumber(m.radius);
//			drawLines = m.drawLines;
//			drawPlanes = m.drawPlanes;
//			buttonDrawLines.setUseAlternateIcon(drawLines);
//			buttonDrawPlanes.setUseAlternateIcon(drawPlanes);
//			buttonDrawLines.setTooltip(Component.literal(drawLines ? "Hide lines" : "Show lines"));
//			buttonDrawPlanes.setTooltip(Component.literal(drawPlanes ? "Hide area" : "Show area"));
//			if (textMirrorPosX.getNumber() == Math.floor(textMirrorPosX.getNumber())) {
//				toggleOdd = false;
//				buttonToggleOdd.setTooltip(Arrays.asList(Component.literal("Set mirror position to middle of block"), Component.literal("for odd numbered builds")));
//			} else {
//				toggleOdd = true;
//				buttonToggleOdd.setTooltip(Arrays.asList(Component.literal("Set mirror position to corner of block"), Component.literal("for even numbered builds")));
//			}
//			buttonToggleOdd.setUseAlternateIcon(toggleOdd);
//		}
//
//		renderables.addAll(mirrorButtonList);
//		renderables.addAll(mirrorIconButtonList);
//
//		setCollapsed(!buttonMirrorEnabled.isChecked());
//	}
//
//	@Override
//	public void updateScreen() {
//		super.updateScreen();
//		mirrorNumberFieldList.forEach(GuiNumberField::update);
//	}
//
//	@Override
//	public void drawEntry(PoseStack ms, int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY,
//						  boolean isSelected, float partialTicks) {
//
//		int yy = y;
//		int offset = 8;
//
//		buttonMirrorEnabled.render(ms, mouseX, mouseY, partialTicks);
//		if (buttonMirrorEnabled.isChecked()) {
//			buttonMirrorEnabled.y = yy;
//			font.draw(ms, "Mirror enabled", left + offset, yy + 2, 0xFFFFFF);
//
//			yy = y + 18;
//			font.draw(ms, "Position", left + offset, yy + 5, 0xFFFFFF);
//			font.draw(ms, "X", left + 40 + offset, yy + 5, 0xFFFFFF);
//			textMirrorPosX.y = yy;
//			font.draw(ms, "Y", left + 120 + offset, yy + 5, 0xFFFFFF);
//			textMirrorPosY.y = yy;
//			font.draw(ms, "Z", left + 200 + offset, yy + 5, 0xFFFFFF);
//			textMirrorPosZ.y = yy;
//
//			yy = y + 50;
//			font.draw(ms, "Direction", left + offset, yy + 2, 0xFFFFFF);
//			buttonMirrorX.y = yy;
//			buttonMirrorY.y = yy;
//			buttonMirrorZ.y = yy;
//			font.draw(ms, "Radius", left + 176 + offset, yy + 2, 0xFFFFFF);
//			textMirrorRadius.y = yy - 3;
//
//			yy = y + 72;
//			buttonCurrentPosition.y = yy;
//			buttonToggleOdd.y = yy;
//			buttonDrawLines.y = yy;
//			buttonDrawPlanes.y = yy;
//
//			mirrorButtonList.forEach(button -> button.render(ms, mouseX, mouseY, partialTicks));
//			mirrorIconButtonList.forEach(button -> button.render(ms, mouseX, mouseY, partialTicks));
//			mirrorNumberFieldList.forEach(numberField -> numberField.drawNumberField(ms, mouseX, mouseY, partialTicks));
//		} else {
//			buttonMirrorEnabled.y = yy;
//			font.draw(ms, "Mirror disabled", left + offset, yy + 2, 0x999999);
//		}
//
//	}
//
//	public void drawTooltip(PoseStack ms, Screen guiScreen, int mouseX, int mouseY) {
//		//Draw tooltips last
//		if (buttonMirrorEnabled.isChecked()) {
//			mirrorIconButtonList.forEach(iconButton -> iconButton.drawTooltip(ms, scrollPane.parent, mouseX, mouseY));
//			mirrorNumberFieldList.forEach(numberField -> numberField.drawTooltip(ms, scrollPane.parent, mouseX, mouseY));
//		}
//	}
//
//	@Override
//	public boolean charTyped(char typedChar, int modifiers) {
//		super.charTyped(typedChar, modifiers);
//		for (GuiNumberField numberField : mirrorNumberFieldList) {
//			numberField.charTyped(typedChar, modifiers);
//		}
//		return true;
//	}
//
//	@Override
//	public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
//		mirrorNumberFieldList.forEach(numberField -> numberField.mouseClicked(mouseX, mouseY, mouseEvent));
//
//		boolean insideMirrorEnabledLabel = mouseX >= left && mouseX < right && relativeY >= -2 && relativeY < 12;
//
//		if (insideMirrorEnabledLabel) {
//			buttonMirrorEnabled.playDownSound(this.mc.getSoundManager());
//			buttonMirrorEnabled.onClick(mouseX, mouseY);
//		}
//
//		return true;
//	}
//
//	public Mirror.MirrorSettings getMirrorSettings() {
//		boolean mirrorEnabled = buttonMirrorEnabled.isChecked();
//
//		Vec3 mirrorPos = new Vec3(0, 64, 0);
//		try {
//			mirrorPos = new Vec3(textMirrorPosX.getNumber(), textMirrorPosY.getNumber(), textMirrorPosZ.getNumber());
//		} catch (NumberFormatException | NullPointerException ex) {
//			EffortlessBuilding.log(mc.player, "Mirror position not a valid number.");
//		}
//
//		boolean mirrorX = buttonMirrorX.isChecked();
//		boolean mirrorY = buttonMirrorY.isChecked();
//		boolean mirrorZ = buttonMirrorZ.isChecked();
//
//		int mirrorRadius = 50;
//		try {
//			mirrorRadius = (int) textMirrorRadius.getNumber();
//		} catch (NumberFormatException | NullPointerException ex) {
//			EffortlessBuilding.log(mc.player, "Mirror radius not a valid number.");
//		}
//
//		return new Mirror.MirrorSettings(mirrorEnabled, mirrorPos, mirrorX, mirrorY, mirrorZ, mirrorRadius, drawLines, drawPlanes);
//	}
//
//	@Override
//	protected String getName() {
//		return "Mirror";
//	}
//
//	@Override
//	protected int getExpandedHeight() {
//		return 100;
//	}
}
