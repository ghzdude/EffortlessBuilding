package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.world.entity.player.Player;
import net.minecraft.ChatFormatting;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedo;
import nl.requios.effortlessbuilding.proxy.ClientProxy;

public class ModeOptions {

	private static ActionEnum buildSpeed = ActionEnum.NORMAL_SPEED;
	private static ActionEnum fill = ActionEnum.FULL;
	private static ActionEnum cubeFill = ActionEnum.CUBE_FULL;
	private static ActionEnum raisedEdge = ActionEnum.SHORT_EDGE;
	private static ActionEnum lineThickness = ActionEnum.THICKNESS_1;
	private static ActionEnum circleStart = ActionEnum.CIRCLE_START_CORNER;

	public static ActionEnum getOptionSetting(OptionEnum option) {
		switch (option) {
			case BUILD_SPEED:
				return getBuildSpeed();
			case FILL:
				return getFill();
			case CUBE_FILL:
				return getCubeFill();
			case RAISED_EDGE:
				return getRaisedEdge();
			case LINE_THICKNESS:
				return getLineThickness();
			case CIRCLE_START:
				return getCircleStart();
			default:
				return null;
		}
	}

	public static ActionEnum getBuildSpeed() {
		return buildSpeed;
	}

	public static ActionEnum getFill() {
		return fill;
	}

	public static ActionEnum getCubeFill() {
		return cubeFill;
	}

	public static ActionEnum getRaisedEdge() {
		return raisedEdge;
	}

	public static ActionEnum getLineThickness() {
		return lineThickness;
	}

	public static ActionEnum getCircleStart() {
		return circleStart;
	}

	//Called on both client and server
	public static void performAction(Player player, ActionEnum action) {
		if (action == null) return;

		switch (action) {
			case UNDO:
				UndoRedo.undo(player);
				break;
			case REDO:
				UndoRedo.redo(player);
				break;
			case REPLACE:
				ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
				modifierSettings.setQuickReplace(!modifierSettings.doQuickReplace());
				EffortlessBuilding.log(player, "Set " + ChatFormatting.GOLD + "Quick Replace " + ChatFormatting.RESET + (
					modifierSettings.doQuickReplace() ? "on" : "off"), true);
				break;
			case OPEN_MODIFIER_SETTINGS:
				if (player.level.isClientSide)
					ClientProxy.openModifierSettings();
				break;
			case OPEN_PLAYER_SETTINGS:
				if (player.level.isClientSide)
					ClientProxy.openPlayerSettings();
				break;

			case NORMAL_SPEED:
				buildSpeed = ActionEnum.NORMAL_SPEED;
				break;
			case FAST_SPEED:
				buildSpeed = ActionEnum.FAST_SPEED;
				break;
			case FULL:
				fill = ActionEnum.FULL;
				break;
			case HOLLOW:
				fill = ActionEnum.HOLLOW;
				break;
			case CUBE_FULL:
				cubeFill = ActionEnum.CUBE_FULL;
				break;
			case CUBE_HOLLOW:
				cubeFill = ActionEnum.CUBE_HOLLOW;
				break;
			case CUBE_SKELETON:
				cubeFill = ActionEnum.CUBE_SKELETON;
				break;
			case SHORT_EDGE:
				raisedEdge = ActionEnum.SHORT_EDGE;
				break;
			case LONG_EDGE:
				raisedEdge = ActionEnum.LONG_EDGE;
				break;
			case THICKNESS_1:
				lineThickness = ActionEnum.THICKNESS_1;
				break;
			case THICKNESS_3:
				lineThickness = ActionEnum.THICKNESS_3;
				break;
			case THICKNESS_5:
				lineThickness = ActionEnum.THICKNESS_5;
				break;
			case CIRCLE_START_CENTER:
				circleStart = ActionEnum.CIRCLE_START_CENTER;
				break;
			case CIRCLE_START_CORNER:
				circleStart = ActionEnum.CIRCLE_START_CORNER;
				break;
		}

		if (player.level.isClientSide &&
			action != ActionEnum.REPLACE &&
			action != ActionEnum.OPEN_MODIFIER_SETTINGS &&
			action != ActionEnum.OPEN_PLAYER_SETTINGS) {

			EffortlessBuilding.logTranslate(player, "", action.name, "", true);
		}
	}

	public enum ActionEnum {
		UNDO("effortlessbuilding.action.undo"),
		REDO("effortlessbuilding.action.redo"),
		REPLACE("effortlessbuilding.action.replace"),
		OPEN_MODIFIER_SETTINGS("effortlessbuilding.action.open_modifier_settings"),
		OPEN_PLAYER_SETTINGS("effortlessbuilding.action.open_player_settings"),

		NORMAL_SPEED("effortlessbuilding.action.normal_speed"),
		FAST_SPEED("effortlessbuilding.action.fast_speed"),

		FULL("effortlessbuilding.action.full"),
		HOLLOW("effortlessbuilding.action.hollow"),

		CUBE_FULL("effortlessbuilding.action.full"),
		CUBE_HOLLOW("effortlessbuilding.action.hollow"),
		CUBE_SKELETON("effortlessbuilding.action.skeleton"),

		SHORT_EDGE("effortlessbuilding.action.short_edge"),
		LONG_EDGE("effortlessbuilding.action.long_edge"),

		THICKNESS_1("effortlessbuilding.action.thickness_1"),
		THICKNESS_3("effortlessbuilding.action.thickness_3"),
		THICKNESS_5("effortlessbuilding.action.thickness_5"),

		CIRCLE_START_CORNER("effortlessbuilding.action.start_corner"),
		CIRCLE_START_CENTER("effortlessbuilding.action.start_center");

		public String name;

		ActionEnum(String name) {
			this.name = name;
		}
	}

	public enum OptionEnum {
		BUILD_SPEED("effortlessbuilding.action.build_speed", ActionEnum.NORMAL_SPEED, ActionEnum.FAST_SPEED),
		FILL("effortlessbuilding.action.filling", ActionEnum.FULL, ActionEnum.HOLLOW),
		CUBE_FILL("effortlessbuilding.action.filling", ActionEnum.CUBE_FULL, ActionEnum.CUBE_HOLLOW, ActionEnum.CUBE_SKELETON),
		RAISED_EDGE("effortlessbuilding.action.raised_edge", ActionEnum.SHORT_EDGE, ActionEnum.LONG_EDGE),
		LINE_THICKNESS("effortlessbuilding.action.thickness", ActionEnum.THICKNESS_1, ActionEnum.THICKNESS_3, ActionEnum.THICKNESS_5),
		CIRCLE_START("effortlessbuilding.action.circle_start", ActionEnum.CIRCLE_START_CORNER, ActionEnum.CIRCLE_START_CENTER);

		public String name;
		public ActionEnum[] actions;

		OptionEnum(String name, ActionEnum... actions) {
			this.name = name;
			this.actions = actions;
		}
	}
}
