package nl.requios.effortlessbuilding.gui.elements;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formattable;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiNumberField extends AbstractGui {

	public int x, y, width, height;
	public int buttonWidth = 10;

	protected TextFieldWidget textField;
	protected Button minusButton, plusButton;

	List<ITextComponent> tooltip = new ArrayList<>();

	public GuiNumberField(FontRenderer font, List<Widget> buttonList, int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;

		textField = new TextFieldWidget(font, x + buttonWidth + 1, y + 1, width - 2 * buttonWidth - 2, height - 2, StringTextComponent.EMPTY);
		minusButton = new Button(x, y - 1, buttonWidth, height + 2, new StringTextComponent("-"), button -> {
			float valueChanged = 1f;
			if (Screen.hasControlDown()) valueChanged = 5f;
			if (Screen.hasShiftDown()) valueChanged = 10f;

			setNumber(getNumber() - valueChanged);
		});
		plusButton = new Button(x + width - buttonWidth, y - 1, buttonWidth, height + 2, new StringTextComponent("+"), button -> {
			float valueChanged = 1f;
			if (Screen.hasControlDown()) valueChanged = 5f;
			if (Screen.hasShiftDown()) valueChanged = 10f;

			setNumber(getNumber() + valueChanged);
		});

		buttonList.add(minusButton);
		buttonList.add(plusButton);
	}

	public double getNumber() {
		if (textField.getText().isEmpty()) return 0;
		try {
			return DecimalFormat.getInstance().parse(textField.getText()).doubleValue();
		} catch (ParseException e) {
			return 0;
		}
	}

	public void setNumber(double number) {
		textField.setText(DecimalFormat.getInstance().format(number));
	}

	public void setTooltip(ITextComponent tooltip) {
		setTooltip(Arrays.asList(tooltip));
	}

	public void setTooltip(List<ITextComponent> tooltip) {
		this.tooltip = tooltip;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		boolean result = textField.mouseClicked(mouseX, mouseY, mouseButton);

		//Check if clicked inside textfield
		boolean flag = mouseX >= x + buttonWidth && mouseX < x + width - buttonWidth && mouseY >= y && mouseY < y + height;

		//Rightclicked inside textfield
		if (flag && mouseButton == 1) {
			textField.setText("");
			textField.setFocused2(true);
			result = true;
		}

		return result;
	}

	public void drawNumberField(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
		textField.y = y + 1;
		minusButton.y = y - 1;
		plusButton.y = y - 1;

		textField.render(ms, mouseX, mouseY, partialTicks);
		minusButton.render(ms, mouseX, mouseY, partialTicks);
		plusButton.render(ms, mouseX, mouseY, partialTicks);
	}

	public void drawTooltip(MatrixStack ms, Screen screen, int mouseX, int mouseY) {
		boolean insideTextField = mouseX >= x + buttonWidth && mouseX < x + width - buttonWidth && mouseY >= y && mouseY < y + height;
		boolean insideMinusButton = mouseX >= x && mouseX < x + buttonWidth && mouseY >= y && mouseY < y + height;
		boolean insidePlusButton = mouseX >= x + width - buttonWidth && mouseX < x + width && mouseY >= y && mouseY < y + height;

		// List<String> textLines = new ArrayList<>();

		StringTextComponent textLines = new StringTextComponent("");


		if (insideTextField) {
			tooltip.forEach(s -> textLines.append(s).appendString("\n"));
//            textLines.add(TextFormatting.GRAY + "Tip: try scrolling.");
		}

		if (insideMinusButton) {
			textLines.appendString("Hold ").append(new StringTextComponent("shift ").mergeStyle(TextFormatting.AQUA)).appendString("for ")
				.append(new StringTextComponent("10").mergeStyle(TextFormatting.RED)).appendString("\n");
			textLines.appendString("Hold ").append(new StringTextComponent("ctrl ").mergeStyle(TextFormatting.AQUA)).appendString("for ")
				.append(new StringTextComponent("5").mergeStyle(TextFormatting.RED)).appendString("\n");
		}

		if (insidePlusButton) {
			textLines.appendString("Hold ").append(new StringTextComponent("shift ").mergeStyle(TextFormatting.DARK_GREEN)).appendString("for ")
				.append(new StringTextComponent("10").mergeStyle(TextFormatting.RED)).appendString("\n");
			textLines.appendString("Hold ").append(new StringTextComponent("ctrl ").mergeStyle(TextFormatting.DARK_GREEN)).appendString("for ")
				.append(new StringTextComponent("5").mergeStyle(TextFormatting.RED));
		}

		screen.renderTooltip(ms, textLines, mouseX - 10, mouseY + 25);

	}

	public void update() {
		textField.tick();
	}

	public boolean charTyped(char typedChar, int keyCode) {
		if (!textField.isFocused()) return false;
//        if (Character.isDigit(typedChar) || typedChar == '.' || typedChar == '-' || keyCode == Keyboard.KEY_BACK
//            || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT
//            || keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
		return textField.charTyped(typedChar, keyCode);
//        }
	}

	//Scroll inside textfield to change number
	//Disabled because entire screen can be scrolled
//    public void handleMouseInput(int mouseX, int mouseY) {
//        boolean insideTextField = mouseX >= x + buttonWidth && mouseX < x + width - buttonWidth && mouseY >= y && mouseY < y + height;
//
//        if (insideTextField)
//        {
//            int valueChanged = 0;
//            if (Mouse.getEventDWheel() > 0)
//                valueChanged = 1;
//            if (Mouse.getEventDWheel() < 0)
//                valueChanged = -1;
//
//            if (valueChanged != 0)
//                setNumber(getNumber() + valueChanged);
//        }
//    }
}
