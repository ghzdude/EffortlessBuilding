package nl.requios.effortlessbuilding.gui.buildmodifier;

import com.mojang.blaze3d.vertex.PoseStack;
import nl.requios.effortlessbuilding.buildmodifier.BaseModifier;
import nl.requios.effortlessbuilding.create.foundation.gui.AllIcons;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.BoxWidget;
import nl.requios.effortlessbuilding.gui.elements.GuiCollapsibleScrollEntry;
import nl.requios.effortlessbuilding.gui.elements.GuiScrollPane;

public abstract class BaseModifierEntry<T extends BaseModifier> extends ModifiersScreenList.Entry {

    public T modifier;
    BoxWidget enableButton;

    public BaseModifierEntry(T modifier) {
        super();

        this.modifier = modifier;

        enableButton = new BoxWidget()
            .showingElement(AllIcons.I_CONFIRM.asStencil())
            .withCallback(() -> {
                modifier.enabled = !modifier.enabled;
                onValueChanged();
            });
    }

    @Override
    public void tick() {
        super.tick();
        enableButton.tick();
    }

    @Override
    public void render(PoseStack ms, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
        super.render(ms, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);

        enableButton.x = x + width - 80;
        enableButton.y = y + 10;
        enableButton.setWidth(35);
        enableButton.setHeight(height - 20);
        enableButton.render(ms, mouseX, mouseY, partialTicks);
    }

    public void onValueChanged() {
        enableButton.showingElement(modifier.enabled ? AllIcons.I_CONFIRM.asStencil() : AllIcons.I_DISABLE.asStencil());
    }
}
