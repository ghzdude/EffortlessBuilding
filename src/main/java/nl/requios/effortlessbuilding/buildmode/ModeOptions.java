package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.ClientEvents;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.PerformRedoPacket;
import nl.requios.effortlessbuilding.network.PerformUndoPacket;
import nl.requios.effortlessbuilding.systems.BuildSettings;

@OnlyIn(Dist.CLIENT)
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

	public static void performAction(Player player, ActionEnum action) {
		if (action == null) return;

		switch (action) {
			case UNDO -> PacketHandler.INSTANCE.sendToServer(new PerformUndoPacket());
			case REDO -> PacketHandler.INSTANCE.sendToServer(new PerformRedoPacket());
			case OPEN_MODIFIER_SETTINGS -> ClientEvents.openModifierSettings();
			case OPEN_PLAYER_SETTINGS -> ClientEvents.openPlayerSettings();

			case REPLACE_ONLY_AIR -> EffortlessBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.ONLY_AIR);
			case REPLACE_BLOCKS_AND_AIR -> EffortlessBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.BLOCKS_AND_AIR);
			case REPLACE_ONLY_BLOCKS -> EffortlessBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.ONLY_BLOCKS);
			case REPLACE_FILTERED_BY_OFFHAND -> EffortlessBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.FILTERED_BY_OFFHAND);
			case TOGGLE_PROTECT_TILE_ENTITIES -> EffortlessBuildingClient.BUILD_SETTINGS.toggleProtectTileEntities();

			case NORMAL_SPEED -> buildSpeed = ActionEnum.NORMAL_SPEED;
			case FAST_SPEED -> buildSpeed = ActionEnum.FAST_SPEED;

			case FULL -> fill = ActionEnum.FULL;
			case HOLLOW -> fill = ActionEnum.HOLLOW;

			case CUBE_FULL -> cubeFill = ActionEnum.CUBE_FULL;
			case CUBE_HOLLOW -> cubeFill = ActionEnum.CUBE_HOLLOW;
			case CUBE_SKELETON -> cubeFill = ActionEnum.CUBE_SKELETON;

			case SHORT_EDGE -> raisedEdge = ActionEnum.SHORT_EDGE;
			case LONG_EDGE -> raisedEdge = ActionEnum.LONG_EDGE;

			case THICKNESS_1 -> lineThickness = ActionEnum.THICKNESS_1;
			case THICKNESS_3 -> lineThickness = ActionEnum.THICKNESS_3;
			case THICKNESS_5 -> lineThickness = ActionEnum.THICKNESS_5;

			case CIRCLE_START_CENTER -> circleStart = ActionEnum.CIRCLE_START_CENTER;
			case CIRCLE_START_CORNER -> circleStart = ActionEnum.CIRCLE_START_CORNER;
		}

		if (player.level.isClientSide &&
			action != ActionEnum.OPEN_MODIFIER_SETTINGS &&
			action != ActionEnum.OPEN_PLAYER_SETTINGS) {

			EffortlessBuilding.logTranslate(player, "", action.name, "", true);
		}
	}

	public enum ActionEnum {
		UNDO("effortlessbuilding.action.undo"),
		REDO("effortlessbuilding.action.redo"),
		OPEN_MODIFIER_SETTINGS("effortlessbuilding.action.open_modifier_settings"),
		OPEN_PLAYER_SETTINGS("effortlessbuilding.action.open_player_settings"),

		REPLACE_ONLY_AIR("effortlessbuilding.action.replace_only_air"),
		REPLACE_BLOCKS_AND_AIR("effortlessbuilding.action.replace_blocks_and_air"),
		REPLACE_ONLY_BLOCKS("effortlessbuilding.action.replace_only_blocks"),
		REPLACE_FILTERED_BY_OFFHAND("effortlessbuilding.action.replace_filtered_by_offhand"),
		TOGGLE_PROTECT_TILE_ENTITIES("effortlessbuilding.action.toggle_protect_tile_entities"),

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
