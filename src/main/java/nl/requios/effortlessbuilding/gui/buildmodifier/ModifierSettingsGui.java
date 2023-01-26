package nl.requios.effortlessbuilding.gui.buildmodifier;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.ClientEvents;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.buildmodifier.Array;
import nl.requios.effortlessbuilding.buildmodifier.BaseModifier;
import nl.requios.effortlessbuilding.buildmodifier.Mirror;
import nl.requios.effortlessbuilding.buildmodifier.RadialMirror;
import nl.requios.effortlessbuilding.gui.elements.GuiCollapsibleScrollEntry;
import nl.requios.effortlessbuilding.gui.elements.GuiScrollPane;
import nl.requios.effortlessbuilding.network.PacketHandler;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ModifierSettingsGui extends Screen {
	private final Map<Class, Class> modifierPanelMap = new HashMap<Class, Class>() {{
		put(Mirror.class, MirrorPanel.class);
		put(Array.class, ArrayPanel.class);
		put(RadialMirror.class, RadialMirrorPanel.class);
	}};

	private GuiScrollPane scrollPane;
	private List<BaseModifierPanel> modifierPanels;
	private Button buttonClose;

	public ModifierSettingsGui() {
		super(Component.translatable("effortlessbuilding.screen.modifier_settings"));
	}

	@Override
	//Create buttons and labels and add them to buttonList/labelList
	protected void init() {

		scrollPane = new GuiScrollPane(this, font, 8, height - 30);

		initScrollEntries();

		scrollPane.init(renderables);

		//Close button
		int y = height - 26;
		buttonClose = new Button(width / 2 - 100, y, 200, 20, Component.literal("Close"), (button) -> {
			Minecraft.getInstance().player.closeContainer();
		});
		addRenderableOnly(buttonClose);
	}

	private void initScrollEntries() {

		var modifierSettingsList = EffortlessBuildingClient.BUILD_MODIFIERS.getModifierSettingsList();
		for (BaseModifier modifier : modifierSettingsList) {
			BaseModifierPanel modifierPanel = createModifierPanel(modifier.getClass().getSimpleName());
			if (modifierPanel != null) {
				modifierPanel.setModifier(modifier);
				scrollPane.AddListEntry(modifierPanel);
			}
		}
	}

	private BaseModifierPanel createModifierPanel(String type) {
		switch (type) {
			case "Mirror": return new MirrorPanel(scrollPane);
			case "Array": return new ArrayPanel(scrollPane);
			case "RadialMirror": return new RadialMirrorPanel(scrollPane);
			default: return null;
		}
	}

	@Override
	//Process general logic, i.e. hide buttons
	public void tick() {
		scrollPane.updateScreen();

		handleMouseInput();
	}

	@Override
	//Set colors using GL11, use the fontObj field to display text
	//Use drawTexturedModalRect() to transfers areas of a texture resource to the screen
	public void render(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(ms);

		scrollPane.render(ms, mouseX, mouseY, partialTicks);

		buttonClose.render(ms, mouseX, mouseY, partialTicks);

		scrollPane.drawTooltip(ms, this, mouseX, mouseY);
	}


	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		super.charTyped(typedChar, keyCode);
		scrollPane.charTyped(typedChar, keyCode);
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int p_96553_, int p_96554_) {
		if (keyCode == ClientEvents.keyBindings[1].getKey().getValue()) {
			minecraft.player.closeContainer();
			return true;
		}

		return super.keyPressed(keyCode, p_96553_, p_96554_);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		renderables.forEach(renderable -> {
			if (renderable instanceof Button) {
				Button button = (Button) renderable;
				button.mouseClicked(mouseX, mouseY, mouseButton);
			}
		});
		return scrollPane.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int state) {
		if (state != 0 || !scrollPane.mouseReleased(mouseX, mouseY, state)) {
			return super.mouseReleased(mouseX, mouseY, state);
		}
		return false;
	}

	public void handleMouseInput() {
		//super.handleMouseInput();
		scrollPane.handleMouseInput();

		//Scrolling numbers
//        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
//        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
//        numberFieldList.forEach(numberField -> numberField.handleMouseInput(mouseX, mouseY));
	}

	@Override
	public void removed() {
		scrollPane.onGuiClosed();

		//save everything
		Mirror.MirrorSettings m = mirrorPanel.getMirrorSettings();
		Array.ArraySettings a = arrayPanel.getArraySettings();
		RadialMirror.RadialMirrorSettings r = radialMirrorPanel.getRadialMirrorSettings();

		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(minecraft.player);
		if (modifierSettings == null) modifierSettings = new ModifierSettingsManager.ModifierSettings();
		modifierSettings.setMirrorSettings(m);
		modifierSettings.setArraySettings(a);
		modifierSettings.setRadialMirrorSettings(r);

		//Sanitize
		String error = ModifierSettingsManager.sanitize(modifierSettings, minecraft.player);
		if (!error.isEmpty()) EffortlessBuilding.log(minecraft.player, error);

		ModifierSettingsManager.setModifierSettings(minecraft.player, modifierSettings);

		//Send to server
		PacketHandler.INSTANCE.sendToServer(new ModifierSettingsMessage(modifierSettings));

		Minecraft.getInstance().mouseHandler.grabMouse();
	}

}
