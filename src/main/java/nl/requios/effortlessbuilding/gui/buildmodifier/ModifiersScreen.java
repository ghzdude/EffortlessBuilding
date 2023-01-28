package nl.requios.effortlessbuilding.gui.buildmodifier;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
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
import nl.requios.effortlessbuilding.create.foundation.gui.AbstractSimiScreen;
import nl.requios.effortlessbuilding.gui.elements.GuiScrollPane;
import nl.requios.effortlessbuilding.network.PacketHandler;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ModifiersScreen extends AbstractSimiScreen {
	protected ModifiersScreenList list;
	private Button buttonClose;

	public ModifiersScreen() {
		super(Component.translatable("effortlessbuilding.screen.modifier_settings"));
	}

	@Override
	//Create buttons and labels and add them to buttonList/labelList
	protected void init() {
		super.init();

		int listWidth = Math.min(width - 80, 300);
		int yCenter = height / 2;
		int listL = this.width / 2 - listWidth / 2;
		int listR = this.width / 2 + listWidth / 2;

		list = new ModifiersScreenList(minecraft, listWidth, height - 80, 35, height - 45, 40);
		list.setLeftPos(this.width / 2 - list.getWidth() / 2);

		addRenderableWidget(list);

		initScrollEntries();

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
			var entry = createModifierPanel(modifier);
			list.children().add(entry);
		}
	}

	private BaseModifierEntry createModifierPanel(BaseModifier modifier) {
		if (modifier instanceof Mirror) {
			return new MirrorEntry(modifier);
		} else if (modifier instanceof Array) {
			return new ArrayEntry(modifier);
		} else if (modifier instanceof RadialMirror) {
			return new RadialMirrorEntry(modifier);
		} else {
			return null;
		}
	}

	private void addModifier(BaseModifier modifier) {
		var entry = createModifierPanel(modifier);
		list.children().add(entry);
		EffortlessBuildingClient.BUILD_MODIFIERS.addModifierSettings(modifier);
	}

	private void removeModifier(BaseModifierEntry entry) {
		list.children().remove(entry);
		EffortlessBuildingClient.BUILD_MODIFIERS.removeModifierSettings(entry.modifier);
	}

	@Override
	public void resize(@Nonnull Minecraft client, int width, int height) {
		double scroll = list.getScrollAmount();
		init(client, width, height);
		list.setScrollAmount(scroll);
	}

	@Override
	protected void renderWindow(PoseStack ms, int mouseX, int mouseY, float partialTicks) {

	}

	@Override
	public void onClose() {
		super.onClose();
		EffortlessBuildingClient.BUILD_MODIFIERS.save(minecraft.player);
	}

	@Override
	public boolean keyPressed(int keyCode, int p_96553_, int p_96554_) {
		if (keyCode == ClientEvents.keyBindings[1].getKey().getValue()) {
			minecraft.player.closeContainer();
			return true;
		}

		return super.keyPressed(keyCode, p_96553_, p_96554_);
	}

}
