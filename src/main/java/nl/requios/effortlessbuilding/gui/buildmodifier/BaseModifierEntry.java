package nl.requios.effortlessbuilding.gui.buildmodifier;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import nl.requios.effortlessbuilding.AllGuiTextures;
import nl.requios.effortlessbuilding.buildmodifier.BaseModifier;
import nl.requios.effortlessbuilding.create.foundation.gui.AbstractSimiScreen;
import nl.requios.effortlessbuilding.create.foundation.gui.AllIcons;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.BoxWidget;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.IconButton;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.Label;
import nl.requios.effortlessbuilding.gui.elements.GuiCollapsibleScrollEntry;
import nl.requios.effortlessbuilding.gui.elements.GuiScrollPane;

public abstract class BaseModifierEntry<T extends BaseModifier> extends ModifiersScreenList.Entry {

    public T modifier;
    protected AllGuiTextures background;
    protected IconButton enableButton;
    protected Label nameLabel;
    protected IconButton removeButton;

    public BaseModifierEntry(ModifiersScreen screen, T modifier, Component name, AllGuiTextures background) {
        super(screen);

        this.modifier = modifier;
        this.background = background;

        enableButton = new IconButton(35, 8, AllIcons.I_PLACE)
            .withCallback(() -> {
                modifier.enabled = !modifier.enabled;
                onValueChanged();
            });
        listeners.add(enableButton);
        
        nameLabel = new Label(65, 8, name);
        nameLabel.text = name;
        
        removeButton = new IconButton(0, 0, AllIcons.I_TRASH)
            .withCallback(() -> {
                screen.removeModifier(this);
            });
        listeners.add(removeButton);
    }

    @Override
    public void tick() {
        super.tick();
//        enableButton.tick();
    }

    @Override
    public void render(PoseStack ms, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {

        background.render(ms, x + 22, y, screen);
        
        enableButton.x = x + width - 60;
        enableButton.y = y + 18;
        enableButton.render(ms, mouseX, mouseY, partialTicks);
        
        nameLabel.x = x + 65;
        nameLabel.y = y + 4;
        nameLabel.render(ms, mouseX, mouseY, partialTicks);
        
        removeButton.x = x + width - 60;
        removeButton.y = y + 38;
        removeButton.render(ms, mouseX, mouseY, partialTicks);
        
        super.render(ms, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);
    }

    public void onValueChanged() {
        enableButton.setIcon(modifier.enabled ? AllIcons.I_PLACE : AllIcons.I_CLEAR);
    }
}
