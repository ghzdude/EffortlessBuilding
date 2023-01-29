package nl.requios.effortlessbuilding.gui.buildmodifier;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.AllGuiTextures;
import nl.requios.effortlessbuilding.buildmodifier.Array;
import nl.requios.effortlessbuilding.buildmodifier.BaseModifier;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.ScrollInput;
import nl.requios.effortlessbuilding.gui.elements.LabeledScrollInput;
import nl.requios.effortlessbuilding.utilities.MathHelper;
import nl.requios.effortlessbuilding.utilities.ReachHelper;

import java.util.Vector;

@OnlyIn(Dist.CLIENT)
public class ArrayEntry extends BaseModifierEntry<Array> {

	protected Vector<ScrollInput> offsetInputs = new Vector<>(3);
	protected ScrollInput countInput;

	public ArrayEntry(ModifiersScreen screen, BaseModifier array) {
		super(screen, (Array) array, Component.literal("Array"), AllGuiTextures.ARRAY_ENTRY);

		offsetInputs.clear();

		for (int i = 0; i < 3; i++) {
			final int index = i;
			var scrollInput = new LabeledScrollInput(0, 0, 18, 18)
				.titled(Component.literal(i == 0 ? "X Offset" : i == 1 ? "Y Offset" : "Z Offset"))
				.calling(value -> {
					modifier.offset = MathHelper.with(modifier.offset, index, value);
					onValueChanged();
				});
			scrollInput.setState(MathHelper.get(modifier.offset, index));
			offsetInputs.add(scrollInput);
		}
		listeners.addAll(offsetInputs);

		countInput = new LabeledScrollInput(0, 0, 18, 18)
			.withRange(1, 100)
			.titled(Component.literal("Count"))
			.calling(value -> {
				modifier.count = value;
				onValueChanged();
			});
		countInput.setState(modifier.count);
		listeners.add(countInput);
		
		for (int i = 0; i < 3; i++) {
			offsetInputs.get(i).onChanged();
		}
		countInput.onChanged();
		
		onValueChanged();
	}

	@Override
	public void render(PoseStack ms, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
		super.render(ms, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);

		//draw offset inputs
		for (int i = 0; i < 3; i++) {
			offsetInputs.get(i).x = left + 47 + 20 * i;
			offsetInputs.get(i).y = top + 18;
			offsetInputs.get(i).render(ms, mouseX, mouseY, partialTicks);
		}
		
		//draw count input
		countInput.x = left + 47;
		countInput.y = top + 38;
		countInput.render(ms, mouseX, mouseY, partialTicks);
		
		//draw reach label
		reachLabel.x = right - 8 - getFont().width(reachLabel.text);
		reachLabel.y = top + 23;
		reachLabel.render(ms, mouseX, mouseY, partialTicks);
	}

	@Override
	public void onValueChanged() {
		super.onValueChanged();
		
		int currentReach = Math.max(-1, getArrayReach());
		int maxReach = ReachHelper.getMaxReach(Minecraft.getInstance().player);
		ChatFormatting reachColor = isCurrentReachValid(currentReach, maxReach) ? ChatFormatting.GRAY : ChatFormatting.RED;
		var reachText = "" + reachColor + currentReach + ChatFormatting.GRAY + "/" + ChatFormatting.GRAY + maxReach;
		reachLabel.text = Component.literal(reachText);
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
