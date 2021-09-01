package nl.requios.effortlessbuilding.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.Array;
import nl.requios.effortlessbuilding.buildmodifier.Mirror;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.RadialMirror;

import java.util.function.Supplier;

import static nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager.ModifierSettings;

import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager.ModifierSettings;

/**
 * Shares modifier settings (see ModifierSettingsManager) between server and client
 */
public class ModifierSettingsMessage {

	private ModifierSettings modifierSettings;

	public ModifierSettingsMessage() {
	}

	public ModifierSettingsMessage(ModifierSettings modifierSettings) {
		this.modifierSettings = modifierSettings;
	}

	public static void encode(ModifierSettingsMessage message, FriendlyByteBuf buf) {
		//MIRROR
		Mirror.MirrorSettings m = message.modifierSettings.getMirrorSettings();
		buf.writeBoolean(m != null);
		if (m != null) {
			buf.writeBoolean(m.enabled);
			buf.writeDouble(m.position.x);
			buf.writeDouble(m.position.y);
			buf.writeDouble(m.position.z);
			buf.writeBoolean(m.mirrorX);
			buf.writeBoolean(m.mirrorY);
			buf.writeBoolean(m.mirrorZ);
			buf.writeInt(m.radius);
			buf.writeBoolean(m.drawLines);
			buf.writeBoolean(m.drawPlanes);
		}

		//ARRAY
		Array.ArraySettings a = message.modifierSettings.getArraySettings();
		buf.writeBoolean(a != null);
		if (a != null) {
			buf.writeBoolean(a.enabled);
			buf.writeInt(a.offset.getX());
			buf.writeInt(a.offset.getY());
			buf.writeInt(a.offset.getZ());
			buf.writeInt(a.count);
		}

		buf.writeBoolean(message.modifierSettings.doQuickReplace());

		buf.writeInt(message.modifierSettings.getReachUpgrade());

		//RADIAL MIRROR
		RadialMirror.RadialMirrorSettings r = message.modifierSettings.getRadialMirrorSettings();
		buf.writeBoolean(r != null);
		if (r != null) {
			buf.writeBoolean(r.enabled);
			buf.writeDouble(r.position.x);
			buf.writeDouble(r.position.y);
			buf.writeDouble(r.position.z);
			buf.writeInt(r.slices);
			buf.writeBoolean(r.alternate);
			buf.writeInt(r.radius);
			buf.writeBoolean(r.drawLines);
			buf.writeBoolean(r.drawPlanes);
		}
	}

	public static ModifierSettingsMessage decode(FriendlyByteBuf buf) {
		//MIRROR
		Mirror.MirrorSettings m = new Mirror.MirrorSettings();
		if (buf.readBoolean()) {
			boolean mirrorEnabled = buf.readBoolean();
			Vec3 mirrorPosition = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
			boolean mirrorX = buf.readBoolean();
			boolean mirrorY = buf.readBoolean();
			boolean mirrorZ = buf.readBoolean();
			int mirrorRadius = buf.readInt();
			boolean mirrorDrawLines = buf.readBoolean();
			boolean mirrorDrawPlanes = buf.readBoolean();
			m = new Mirror.MirrorSettings(mirrorEnabled, mirrorPosition, mirrorX, mirrorY, mirrorZ, mirrorRadius,
				mirrorDrawLines, mirrorDrawPlanes);
		}

		//ARRAY
		Array.ArraySettings a = new Array.ArraySettings();
		if (buf.readBoolean()) {
			boolean arrayEnabled = buf.readBoolean();
			BlockPos arrayOffset = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
			int arrayCount = buf.readInt();
			a = new Array.ArraySettings(arrayEnabled, arrayOffset, arrayCount);
		}

		boolean quickReplace = buf.readBoolean();

		int reachUpgrade = buf.readInt();

		//RADIAL MIRROR
		RadialMirror.RadialMirrorSettings r = new RadialMirror.RadialMirrorSettings();
		if (buf.readBoolean()) {
			boolean radialMirrorEnabled = buf.readBoolean();
			Vec3 radialMirrorPosition = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
			int radialMirrorSlices = buf.readInt();
			boolean radialMirrorAlternate = buf.readBoolean();
			int radialMirrorRadius = buf.readInt();
			boolean radialMirrorDrawLines = buf.readBoolean();
			boolean radialMirrorDrawPlanes = buf.readBoolean();
			r = new RadialMirror.RadialMirrorSettings(radialMirrorEnabled, radialMirrorPosition, radialMirrorSlices,
				radialMirrorAlternate, radialMirrorRadius, radialMirrorDrawLines, radialMirrorDrawPlanes);
		}

		ModifierSettings modifierSettings = new ModifierSettings(m, a, r, quickReplace, reachUpgrade);
		return new ModifierSettingsMessage(modifierSettings);
	}

	public static class Handler {
		public static void handle(ModifierSettingsMessage message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);

				// Sanitize
				ModifierSettingsManager.sanitize(message.modifierSettings, player);

				ModifierSettingsManager.setModifierSettings(player, message.modifierSettings);
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
