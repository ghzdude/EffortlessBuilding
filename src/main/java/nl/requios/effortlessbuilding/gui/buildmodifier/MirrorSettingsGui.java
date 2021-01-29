package nl.requios.effortlessbuilding.gui.buildmodifier;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.Mirror;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.gui.elements.*;
import nl.requios.effortlessbuilding.helper.ReachHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("Duplicates")
@OnlyIn(Dist.CLIENT)
public class MirrorSettingsGui extends GuiCollapsibleScrollEntry {

	protected static final ResourceLocation BUILDING_ICONS = new ResourceLocation(EffortlessBuilding.MODID, "textures/gui/building_icons.png");

	protected List<Button> mirrorButtonList = new ArrayList<>();
	protected List<GuiIconButton> mirrorIconButtonList = new ArrayList<>();
	protected List<GuiNumberField> mirrorNumberFieldList = new ArrayList<>();

	private GuiNumberField textMirrorPosX, textMirrorPosY, textMirrorPosZ, textMirrorRadius;
	private GuiCheckBoxFixed buttonMirrorEnabled, buttonMirrorX, buttonMirrorY, buttonMirrorZ;
	private GuiIconButton buttonCurrentPosition, buttonToggleOdd, buttonDrawPlanes, buttonDrawLines;
	private boolean drawPlanes, drawLines, toggleOdd;

	public MirrorSettingsGui(GuiScrollPane scrollPane) {
		super(scrollPane);
	}

	@Override
	public void init(List<Widget> buttonList) {
		super.init(buttonList);

		int y = top - 2;
		buttonMirrorEnabled = new GuiCheckBoxFixed(left - 15 + 8, y, "", false) {
			@Override
			public void onClick(double mouseX, double mouseY) {
				super.onClick(mouseX, mouseY);
				setCollapsed(!buttonMirrorEnabled.isChecked());
			}
		};
		buttonList.add(buttonMirrorEnabled);

		y = top + 18;
		textMirrorPosX = new GuiNumberField(font, buttonList, left + 58, y, 62, 18);
		textMirrorPosX.setNumber(0);
		textMirrorPosX.setTooltip(
			Arrays.asList(new StringTextComponent("The position of the mirror."), new StringTextComponent("For odd numbered builds add 0.5.").mergeStyle(TextFormatting.GRAY)));
		mirrorNumberFieldList.add(textMirrorPosX);

		textMirrorPosY = new GuiNumberField(font, buttonList, left + 138, y, 62, 18);
		textMirrorPosY.setNumber(64);
		textMirrorPosY.setTooltip(Arrays.asList(new StringTextComponent("The position of the mirror."), new StringTextComponent("For odd numbered builds add 0.5.").mergeStyle(TextFormatting.GRAY)));
		mirrorNumberFieldList.add(textMirrorPosY);

		textMirrorPosZ = new GuiNumberField(font, buttonList, left + 218, y, 62, 18);
		textMirrorPosZ.setNumber(0);
		textMirrorPosZ.setTooltip(Arrays.asList(new StringTextComponent("The position of the mirror."), new StringTextComponent("For odd numbered builds add 0.5.").mergeStyle(TextFormatting.GRAY)));
		mirrorNumberFieldList.add(textMirrorPosZ);

		y = top + 50;
		buttonMirrorX = new GuiCheckBoxFixed(left + 60, y, " X", true);
		mirrorButtonList.add(buttonMirrorX);

		buttonMirrorY = new GuiCheckBoxFixed(left + 100, y, " Y", false);
		mirrorButtonList.add(buttonMirrorY);

		buttonMirrorZ = new GuiCheckBoxFixed(left + 140, y, " Z", false);
		mirrorButtonList.add(buttonMirrorZ);

		y = top + 47;
		textMirrorRadius = new GuiNumberField(font, buttonList, left + 218, y, 62, 18);
		textMirrorRadius.setNumber(50);
		//TODO change to diameter (remove /2)
		textMirrorRadius.setTooltip(Arrays.asList(new StringTextComponent("How far the mirror reaches in any direction."),
			new StringTextComponent("Max: ").mergeStyle(TextFormatting.GRAY).append(new StringTextComponent(String.valueOf(ReachHelper.getMaxReach(mc.player) / 2)).mergeStyle(TextFormatting.GOLD)),
			new StringTextComponent("Upgradeable in survival with reach upgrades.").mergeStyle(TextFormatting.GRAY)));
		mirrorNumberFieldList.add(textMirrorRadius);

		y = top + 72;
		buttonCurrentPosition = new GuiIconButton(left + 5, y, 0, 0, BUILDING_ICONS, button -> {
			Vector3d pos = new Vector3d(Math.floor(mc.player.getPosX()) + 0.5, Math.floor(mc.player.getPosY()) + 0.5, Math.floor(mc.player.getPosZ()) + 0.5);
			textMirrorPosX.setNumber(pos.x);
			textMirrorPosY.setNumber(pos.y);
			textMirrorPosZ.setNumber(pos.z);
		});
		buttonCurrentPosition.setTooltip(new StringTextComponent("Set mirror position to current player position"));
		mirrorIconButtonList.add(buttonCurrentPosition);

		buttonToggleOdd = new GuiIconButton(left + 35, y, 0, 20, BUILDING_ICONS, button -> {
			toggleOdd = !toggleOdd;
			buttonToggleOdd.setUseAlternateIcon(toggleOdd);
			if (toggleOdd) {
				buttonToggleOdd.setTooltip(Arrays.asList(new StringTextComponent("Set mirror position to corner of block"), new StringTextComponent("for even numbered builds")));
				textMirrorPosX.setNumber(textMirrorPosX.getNumber() + 0.5);
				textMirrorPosY.setNumber(textMirrorPosY.getNumber() + 0.5);
				textMirrorPosZ.setNumber(textMirrorPosZ.getNumber() + 0.5);
			} else {
				buttonToggleOdd.setTooltip(Arrays.asList(new StringTextComponent("Set mirror position to middle of block"), new StringTextComponent("for odd numbered builds")));
				textMirrorPosX.setNumber(Math.floor(textMirrorPosX.getNumber()));
				textMirrorPosY.setNumber(Math.floor(textMirrorPosY.getNumber()));
				textMirrorPosZ.setNumber(Math.floor(textMirrorPosZ.getNumber()));
			}
		});
		buttonToggleOdd.setTooltip(Arrays.asList(new StringTextComponent("Set mirror position to middle of block"), new StringTextComponent("for odd numbered builds")));
		mirrorIconButtonList.add(buttonToggleOdd);

		buttonDrawLines = new GuiIconButton(left + 65, y, 0, 40, BUILDING_ICONS, button -> {
			drawLines = !drawLines;
			buttonDrawLines.setUseAlternateIcon(drawLines);
			buttonDrawLines.setTooltip(new StringTextComponent(drawLines ? "Hide lines" : "Show lines"));
		});
		buttonDrawLines.setTooltip(new StringTextComponent("Show lines"));
		mirrorIconButtonList.add(buttonDrawLines);

		buttonDrawPlanes = new GuiIconButton(left + 95, y, 0, 60, BUILDING_ICONS, button -> {
			drawPlanes = !drawPlanes;
			buttonDrawPlanes.setUseAlternateIcon(drawPlanes);
			buttonDrawPlanes.setTooltip(new StringTextComponent(drawPlanes ? "Hide area" : "Show area"));
		});
		buttonDrawPlanes.setTooltip(new StringTextComponent("Show area"));
		mirrorIconButtonList.add(buttonDrawPlanes);

		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(mc.player);
		if (modifierSettings != null) {
			Mirror.MirrorSettings m = modifierSettings.getMirrorSettings();
			buttonMirrorEnabled.setIsChecked(m.enabled);
			textMirrorPosX.setNumber(m.position.x);
			textMirrorPosY.setNumber(m.position.y);
			textMirrorPosZ.setNumber(m.position.z);
			buttonMirrorX.setIsChecked(m.mirrorX);
			buttonMirrorY.setIsChecked(m.mirrorY);
			buttonMirrorZ.setIsChecked(m.mirrorZ);
			textMirrorRadius.setNumber(m.radius);
			drawLines = m.drawLines;
			drawPlanes = m.drawPlanes;
			buttonDrawLines.setUseAlternateIcon(drawLines);
			buttonDrawPlanes.setUseAlternateIcon(drawPlanes);
			buttonDrawLines.setTooltip(new StringTextComponent(drawLines ? "Hide lines" : "Show lines"));
			buttonDrawPlanes.setTooltip(new StringTextComponent(drawPlanes ? "Hide area" : "Show area"));
			if (textMirrorPosX.getNumber() == Math.floor(textMirrorPosX.getNumber())) {
				toggleOdd = false;
				buttonToggleOdd.setTooltip(Arrays.asList(new StringTextComponent("Set mirror position to middle of block"), new StringTextComponent("for odd numbered builds")));
			} else {
				toggleOdd = true;
				buttonToggleOdd.setTooltip(Arrays.asList(new StringTextComponent("Set mirror position to corner of block"), new StringTextComponent("for even numbered builds")));
			}
			buttonToggleOdd.setUseAlternateIcon(toggleOdd);
		}

		buttonList.addAll(mirrorButtonList);
		buttonList.addAll(mirrorIconButtonList);

		setCollapsed(!buttonMirrorEnabled.isChecked());
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		mirrorNumberFieldList.forEach(GuiNumberField::update);
	}

	@Override
	public void drawEntry(MatrixStack ms, int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY,
						  boolean isSelected, float partialTicks) {

		int yy = y;
		int offset = 8;

		buttonMirrorEnabled.render(ms, mouseX, mouseY, partialTicks);
		if (buttonMirrorEnabled.isChecked()) {
			buttonMirrorEnabled.y = yy;
			font.drawString(ms, "Mirror enabled", left + offset, yy + 2, 0xFFFFFF);

			yy = y + 18;
			font.drawString(ms, "Position", left + offset, yy + 5, 0xFFFFFF);
			font.drawString(ms, "X", left + 40 + offset, yy + 5, 0xFFFFFF);
			textMirrorPosX.y = yy;
			font.drawString(ms, "Y", left + 120 + offset, yy + 5, 0xFFFFFF);
			textMirrorPosY.y = yy;
			font.drawString(ms, "Z", left + 200 + offset, yy + 5, 0xFFFFFF);
			textMirrorPosZ.y = yy;

			yy = y + 50;
			font.drawString(ms, "Direction", left + offset, yy + 2, 0xFFFFFF);
			buttonMirrorX.y = yy;
			buttonMirrorY.y = yy;
			buttonMirrorZ.y = yy;
			font.drawString(ms, "Radius", left + 176 + offset, yy + 2, 0xFFFFFF);
			textMirrorRadius.y = yy - 3;

			yy = y + 72;
			buttonCurrentPosition.y = yy;
			buttonToggleOdd.y = yy;
			buttonDrawLines.y = yy;
			buttonDrawPlanes.y = yy;

			mirrorButtonList.forEach(button -> button.render(ms, mouseX, mouseY, partialTicks));
			mirrorIconButtonList.forEach(button -> button.render(ms, mouseX, mouseY, partialTicks));
			mirrorNumberFieldList.forEach(numberField -> numberField.drawNumberField(ms, mouseX, mouseY, partialTicks));
		} else {
			buttonMirrorEnabled.y = yy;
			font.drawString(ms, "Mirror disabled", left + offset, yy + 2, 0x999999);
		}

	}

	public void drawTooltip(MatrixStack ms, Screen guiScreen, int mouseX, int mouseY) {
		//Draw tooltips last
		if (buttonMirrorEnabled.isChecked()) {
			mirrorIconButtonList.forEach(iconButton -> iconButton.drawTooltip(ms, scrollPane.parent, mouseX, mouseY));
			mirrorNumberFieldList.forEach(numberField -> numberField.drawTooltip(ms, scrollPane.parent, mouseX, mouseY));
		}
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		super.charTyped(typedChar, keyCode);
		for (GuiNumberField numberField : mirrorNumberFieldList) {
			numberField.charTyped(typedChar, keyCode);
		}
		return true;
	}

	@Override
	public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
		mirrorNumberFieldList.forEach(numberField -> numberField.mouseClicked(mouseX, mouseY, mouseEvent));

		boolean insideMirrorEnabledLabel = mouseX >= left && mouseX < right && relativeY >= -2 && relativeY < 12;

		if (insideMirrorEnabledLabel) {
			buttonMirrorEnabled.playDownSound(this.mc.getSoundHandler());
			buttonMirrorEnabled.onClick(mouseX, mouseY);
		}

		return true;
	}

	public Mirror.MirrorSettings getMirrorSettings() {
		boolean mirrorEnabled = buttonMirrorEnabled.isChecked();

		Vector3d mirrorPos = new Vector3d(0, 64, 0);
		try {
			mirrorPos = new Vector3d(textMirrorPosX.getNumber(), textMirrorPosY.getNumber(), textMirrorPosZ.getNumber());
		} catch (NumberFormatException | NullPointerException ex) {
			EffortlessBuilding.log(mc.player, "Mirror position not a valid number.");
		}

		boolean mirrorX = buttonMirrorX.isChecked();
		boolean mirrorY = buttonMirrorY.isChecked();
		boolean mirrorZ = buttonMirrorZ.isChecked();

		int mirrorRadius = 50;
		try {
			mirrorRadius = (int) textMirrorRadius.getNumber();
		} catch (NumberFormatException | NullPointerException ex) {
			EffortlessBuilding.log(mc.player, "Mirror radius not a valid number.");
		}

		return new Mirror.MirrorSettings(mirrorEnabled, mirrorPos, mirrorX, mirrorY, mirrorZ, mirrorRadius, drawLines, drawPlanes);
	}

	@Override
	protected String getName() {
		return "Mirror";
	}

	@Override
	protected int getExpandedHeight() {
		return 100;
	}
}
