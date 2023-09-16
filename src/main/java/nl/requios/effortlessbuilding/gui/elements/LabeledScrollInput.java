package nl.requios.effortlessbuilding.gui.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.Label;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.ScrollInput;
import nl.requios.effortlessbuilding.create.foundation.utility.Components;
import nl.requios.effortlessbuilding.create.foundation.utility.Lang;
import org.jetbrains.annotations.NotNull;

public class LabeledScrollInput extends ScrollInput {
    protected Label label;
    protected final Component controlScrollsSlowerText = Lang.translateDirect("gui.scrollInput.controlScrollsSlower");
    protected boolean controlScrollsSlower;
    
    public LabeledScrollInput(int xIn, int yIn, int widthIn, int heightIn) {
        super(xIn, yIn, widthIn, heightIn);
        min = Integer.MIN_VALUE;
        max = Integer.MAX_VALUE;
        
        label = new Label(0, 0, Components.immutableEmpty()).withShadow();
        writingTo(label);
    }
    
    public LabeledScrollInput showControlScrollsSlowerTooltip() {
        controlScrollsSlower = true;
        return this;
    }
    
    @Override
    public void renderButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderButton(graphics, mouseX, mouseY, partialTicks);
        
        label.setX(getX() + width / 2 - Minecraft.getInstance().font.width(label.text) / 2);
        label.setY(getY() + height / 2 - Minecraft.getInstance().font.lineHeight / 2);
        label.renderWidget(graphics, mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void updateTooltip() {
        super.updateTooltip();
        if (title == null || !controlScrollsSlower)
            return;
        toolTip.add(controlScrollsSlowerText.plainCopy()
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
    }
}
