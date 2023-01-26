package nl.requios.effortlessbuilding.gui.buildmodifier;

import nl.requios.effortlessbuilding.buildmodifier.BaseModifier;
import nl.requios.effortlessbuilding.gui.elements.GuiCollapsibleScrollEntry;
import nl.requios.effortlessbuilding.gui.elements.GuiScrollPane;

public abstract class BaseModifierPanel extends GuiCollapsibleScrollEntry {

    public BaseModifierPanel(GuiScrollPane scrollPane) {
        super(scrollPane);
    }

    public abstract void setModifier(BaseModifier modifier);
}
