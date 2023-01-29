package nl.requios.effortlessbuilding.gui.elements;

import net.minecraft.network.chat.Component;
import nl.requios.effortlessbuilding.create.foundation.gui.widget.ElementWidget;

public class MiniButton extends ElementWidget {
    public MiniButton(int x, int y) {
        super(x, y);
    }
    
    public MiniButton(int x, int y, int width, int height) {
        super(x, y, width, height);
    }
    
    public void setToolTip(Component text) {
        toolTip.clear();
        toolTip.add(text);
    }
}
