package nl.requios.effortlessbuilding.buildmode;

import nl.requios.effortlessbuilding.buildmode.buildmodes.*;

public enum BuildModeEnum {
    DISABLED("normal", new Disabled(), BuildModeCategoryEnum.BASIC),
    SINGLE("normal_plus", new Single(), BuildModeCategoryEnum.BASIC, ModeOptions.OptionEnum.BUILD_SPEED),
    LINE("line", new Line(), BuildModeCategoryEnum.BASIC /*, OptionEnum.THICKNESS*/),
    WALL("wall", new Wall(), BuildModeCategoryEnum.BASIC, ModeOptions.OptionEnum.FILL),
    FLOOR("floor", new Floor(), BuildModeCategoryEnum.BASIC, ModeOptions.OptionEnum.FILL),
    CUBE("cube", new Cube(), BuildModeCategoryEnum.BASIC, ModeOptions.OptionEnum.CUBE_FILL),
    DIAGONAL_LINE("diagonal_line", new DiagonalLine(), BuildModeCategoryEnum.DIAGONAL /*, OptionEnum.THICKNESS*/),
    DIAGONAL_WALL("diagonal_wall", new DiagonalWall(), BuildModeCategoryEnum.DIAGONAL /*, OptionEnum.FILL*/),
    SLOPE_FLOOR("slope_floor", new SlopeFloor(), BuildModeCategoryEnum.DIAGONAL, ModeOptions.OptionEnum.RAISED_EDGE),
    CIRCLE("circle", new Circle(), BuildModeCategoryEnum.CIRCULAR, ModeOptions.OptionEnum.CIRCLE_START, ModeOptions.OptionEnum.FILL),
    CYLINDER("cylinder", new Cylinder(), BuildModeCategoryEnum.CIRCULAR, ModeOptions.OptionEnum.CIRCLE_START, ModeOptions.OptionEnum.FILL),
    SPHERE("sphere", new Sphere(), BuildModeCategoryEnum.CIRCULAR, ModeOptions.OptionEnum.CIRCLE_START, ModeOptions.OptionEnum.FILL);
//		PYRAMID("pyramid", new Pyramid(), BuildModeCategoryEnum.ROOF),
//		CONE("cone", new Cone(), BuildModeCategoryEnum.ROOF),
//		DOME("dome", new Dome(), BuildModeCategoryEnum.ROOF);

    private final String name;
    public final IBuildMode instance;
    public final BuildModeCategoryEnum category;
    public final ModeOptions.OptionEnum[] options;

    BuildModeEnum(String name, IBuildMode instance, BuildModeCategoryEnum category, ModeOptions.OptionEnum... options) {
        this.name = name;
        this.instance = instance;
        this.category = category;
        this.options = options;
    }

    public String getNameKey() {
        return "effortlessbuilding.mode." + name;
    }

    public String getDescriptionKey() {
        return "effortlessbuilding.modedescription." + name;
    }

}
