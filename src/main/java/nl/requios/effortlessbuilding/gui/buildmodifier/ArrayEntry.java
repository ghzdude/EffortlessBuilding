package nl.requios.effortlessbuilding.gui.buildmodifier;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.Array;
import nl.requios.effortlessbuilding.buildmodifier.BaseModifier;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.Label;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.ScrollInput;
import nl.requios.effortlessbuilding.create.foundation.utility.Components;
import nl.requios.effortlessbuilding.gui.elements.GuiCheckBoxFixed;
import nl.requios.effortlessbuilding.gui.elements.GuiNumberField;
import nl.requios.effortlessbuilding.gui.elements.GuiScrollPane;
import nl.requios.effortlessbuilding.utilities.MathHelper;
import nl.requios.effortlessbuilding.utilities.ReachHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

@OnlyIn(Dist.CLIENT)
public class ArrayEntry extends BaseModifierEntry<Array> {

	protected Vector<ScrollInput> offsetInputs = new Vector<>(3);
	protected Vector<Label> offsetLabels = new Vector<>(3);
	protected ScrollInput countInput;
	protected Label reachLabel;

	public ArrayEntry(BaseModifier array) {
		super((Array) array);

		offsetInputs.clear();
		offsetLabels.clear();

		for (int i = 0; i < 3; i++) {
			var label = new Label(0, 0, Components.immutableEmpty()).withShadow();

			final int index = i;
			var scrollInput = new ScrollInput(0, 0, 20, 20)
				.withRange(0, 100)
				.writingTo(label)
				.titled(Component.literal("Offset"))
				.calling(value -> {
					modifier.offset = MathHelper.with(modifier.offset, index, value);
//						label.x = x + 65 + 20 * index - font.width(label.text) / 2;
					onValueChanged();
				});
			scrollInput.setState(MathHelper.get(modifier.offset, index));
			scrollInput.onChanged();

			offsetInputs.add(scrollInput);
			offsetLabels.add(label);
		}

		listeners.addAll(offsetInputs);
		listeners.addAll(offsetLabels);

		countInput = new ScrollInput(0, 0, 20, 20)
			.withRange(1, 100)
			.titled(Component.literal("Count"))
			.calling(value -> {
				modifier.count = value;
				onValueChanged();
			});

		countInput.setState(modifier.count);
		countInput.onChanged();

		listeners.add(countInput);

		reachLabel = new Label(100, 100, Components.immutableEmpty()).withShadow();

		onValueChanged();
	}

	@Override
	public void tick() {
		super.tick();
		offsetInputs.forEach(ScrollInput::tick);
		offsetLabels.forEach(Label::tick);
		countInput.tick();

		int currentReach = Math.max(-1, getArrayReach());
		int maxReach = ReachHelper.getMaxReach(Minecraft.getInstance().player);
		ChatFormatting reachColor = isCurrentReachValid(currentReach, maxReach) ? ChatFormatting.GRAY : ChatFormatting.RED;
		var reachText = "Reach: " + reachColor + currentReach + ChatFormatting.GRAY + "/" + ChatFormatting.GRAY + maxReach;
		reachLabel.text = Component.literal(reachText);
	}

	@Override
	public void render(PoseStack ms, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
		super.render(ms, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);

	}

	@Override
	public void onValueChanged() {
		super.onValueChanged();

	}

	private int getArrayReach() {
		try {
			//find largest offset
			double x = Math.abs(modifier.offset.getX());
			double y = Math.abs(modifier.offset.getY());
			double z = Math.abs(modifier.offset.getZ());
			double largestOffset = Math.max(Math.max(x, y), z);
			return (int) (largestOffset * modifier.count);
		} catch (NumberFormatException | NullPointerException ex) {
			return -1;
		}
	}

	private boolean isCurrentReachValid(int currentReach, int maxReach) {
		return currentReach <= maxReach && currentReach > -1;
	}
}
