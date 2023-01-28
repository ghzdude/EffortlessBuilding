package nl.requios.effortlessbuilding.gui.buildmodifier;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import nl.requios.effortlessbuilding.create.foundation.gui.TickableGuiEventListener;
import nl.requios.effortlessbuilding.create.foundation.gui.UIRenderHelper;
import nl.requios.effortlessbuilding.create.foundation.utility.Color;
import nl.requios.effortlessbuilding.create.foundation.utility.Components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Based on Create's ConfigScreenList
public class ModifiersScreenList extends ObjectSelectionList<ModifiersScreenList.Entry> implements TickableGuiEventListener {

    public ModifiersScreenList(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
        super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(false);
        headerHeight = 3;
    }

    @Override
    public void render(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
        Color c = new Color(0x60_000000);
        UIRenderHelper.angledGradient(ms, 90, x0 + width / 2, y0, width, 5, c, Color.TRANSPARENT_BLACK);
        UIRenderHelper.angledGradient(ms, -90, x0 + width / 2, y1, width, 5, c, Color.TRANSPARENT_BLACK);
        UIRenderHelper.angledGradient(ms, 0, x0, y0 + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);
        UIRenderHelper.angledGradient(ms, 180, x1, y0 + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);

        super.render(ms, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderList(PoseStack p_239228_, int p_239229_, int p_239230_, float p_239231_) {
        Window window = minecraft.getWindow();
        double d0 = window.getGuiScale();
        RenderSystem.enableScissor((int) (this.x0 * d0), (int) (window.getHeight() - (this.y1 * d0)), (int) (this.width * d0), (int) (this.height * d0));
        super.renderList(p_239228_, p_239229_, p_239230_, p_239231_);
        RenderSystem.disableScissor();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        children().stream().forEach(e -> e.mouseClicked(x, y, button));

        return super.mouseClicked(x, y, button);
    }

    @Override
    public int getRowWidth() {
        return width - 16;
    }

    @Override
    protected int getScrollbarPosition() {
        return x0 + this.width - 6;
    }

    @Override
    public void tick() {
        children().forEach(Entry::tick);
    }

    public static abstract class Entry extends ObjectSelectionList.Entry<Entry> implements TickableGuiEventListener {
        protected List<GuiEventListener> listeners;

        protected Entry() {
            listeners = new ArrayList<>();
        }

        @Override
        public boolean mouseClicked(double x, double y, int button) {
            return getGuiListeners().stream().anyMatch(l -> l.mouseClicked(x, y, button));
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return getGuiListeners().stream().anyMatch(l -> l.keyPressed(keyCode, scanCode, modifiers));
        }

        @Override
        public boolean charTyped(char ch, int modifiers) {
            return getGuiListeners().stream().anyMatch(l -> l.charTyped(ch, modifiers));
        }

        @Override
        public void render(PoseStack ms, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {}

        @Override
        public void tick() {}

        public List<GuiEventListener> getGuiListeners() {
            return listeners;
        }

        @Override
        public Component getNarration() {
            return Components.immutableEmpty();
        }
    }
}
