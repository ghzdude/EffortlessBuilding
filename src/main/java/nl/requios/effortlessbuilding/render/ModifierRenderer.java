package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.buildmodifier.Mirror;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.RadialMirror;

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public class ModifierRenderer {

	protected static final Color colorX = new Color(255, 72, 52);
	protected static final Color colorY = new Color(67, 204, 51);
	protected static final Color colorZ = new Color(52, 247, 255);
	protected static final Color colorRadial = new Color(52, 247, 255);
	protected static final int lineAlpha = 200;
	protected static final int planeAlpha = 75;
	protected static final Vector3d epsilon = new Vector3d(0.001, 0.001, 0.001); //prevents z-fighting

	public static void render(MatrixStack matrixStack, IRenderTypeBuffer.Impl renderTypeBuffer, ModifierSettingsManager.ModifierSettings modifierSettings) {
		//Mirror lines and areas
		Mirror.MirrorSettings m = modifierSettings.getMirrorSettings();
		if (m != null && m.enabled && (m.mirrorX || m.mirrorY || m.mirrorZ)) {
			Vector3d pos = m.position.add(epsilon);
			int radius = m.radius;

			if (m.mirrorX) {
				Vector3d posA = new Vector3d(pos.x, pos.y - radius, pos.z - radius);
				Vector3d posB = new Vector3d(pos.x, pos.y + radius, pos.z + radius);

				drawMirrorPlane(matrixStack, renderTypeBuffer, posA, posB, colorX, m.drawLines, m.drawPlanes, true);
			}
			if (m.mirrorY) {
				Vector3d posA = new Vector3d(pos.x - radius, pos.y, pos.z - radius);
				Vector3d posB = new Vector3d(pos.x + radius, pos.y, pos.z + radius);

				drawMirrorPlaneY(matrixStack, renderTypeBuffer, posA, posB, colorY, m.drawLines, m.drawPlanes);
			}
			if (m.mirrorZ) {
				Vector3d posA = new Vector3d(pos.x - radius, pos.y - radius, pos.z);
				Vector3d posB = new Vector3d(pos.x + radius, pos.y + radius, pos.z);

				drawMirrorPlane(matrixStack, renderTypeBuffer, posA, posB, colorZ, m.drawLines, m.drawPlanes, true);
			}

			//Draw axis coordinated colors if two or more axes are enabled
			//(If only one is enabled the lines are that planes color)
			if (m.drawLines && ((m.mirrorX && m.mirrorY) || (m.mirrorX && m.mirrorZ) || (m.mirrorY && m.mirrorZ))) {
				drawMirrorLines(matrixStack, renderTypeBuffer, m);
			}
		}

		//Radial mirror lines and areas
		RadialMirror.RadialMirrorSettings r = modifierSettings.getRadialMirrorSettings();
		if (r != null && r.enabled) {
			Vector3d pos = r.position.add(epsilon);
			int radius = r.radius;

			float angle = 2f * ((float) Math.PI) / r.slices;
			Vector3d relStartVec = new Vector3d(radius, 0, 0);
			if (r.slices % 4 == 2) relStartVec = relStartVec.yRot(angle / 2f);

			for (int i = 0; i < r.slices; i++) {
				Vector3d relNewVec = relStartVec.yRot(angle * i);
				Vector3d newVec = pos.add(relNewVec);

				Vector3d posA = new Vector3d(pos.x, pos.y - radius, pos.z);
				Vector3d posB = new Vector3d(newVec.x, pos.y + radius, newVec.z);
				drawMirrorPlane(matrixStack, renderTypeBuffer, posA, posB, colorRadial, r.drawLines, r.drawPlanes, false);
			}
		}
	}


	//----Mirror----

	protected static void drawMirrorPlane(MatrixStack matrixStack, IRenderTypeBuffer.Impl renderTypeBuffer, Vector3d posA, Vector3d posB, Color c, boolean drawLines, boolean drawPlanes, boolean drawVerticalLines) {

//        GL11.glColor4d(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
		Matrix4f matrixPos = matrixStack.last().pose();

		if (drawPlanes) {
			IVertexBuilder buffer = RenderHandler.beginPlanes(renderTypeBuffer);

			buffer.vertex(matrixPos, (float) posA.x, (float) posA.y, (float) posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posA.x, (float) posB.y, (float) posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posB.x, (float) posA.y, (float) posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posB.x, (float) posB.y, (float) posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			//backface (using triangle strip)
			buffer.vertex(matrixPos, (float) posA.x, (float) posA.y, (float) posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posA.x, (float) posB.y, (float) posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();

			RenderHandler.endPlanes(renderTypeBuffer);
		}

		if (drawLines) {
			IVertexBuilder buffer = RenderHandler.beginLines(renderTypeBuffer);

			Vector3d middle = posA.add(posB).scale(0.5);
			buffer.vertex(matrixPos, (float) posA.x, (float) middle.y, (float) posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posB.x, (float) middle.y, (float) posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
			if (drawVerticalLines) {
				buffer.vertex(matrixPos, (float) middle.x, (float) posA.y, (float) middle.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
				buffer.vertex(matrixPos, (float) middle.x, (float) posB.y, (float) middle.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
			}

			RenderHandler.endLines(renderTypeBuffer);
		}
	}

	protected static void drawMirrorPlaneY(MatrixStack matrixStack, IRenderTypeBuffer.Impl renderTypeBuffer, Vector3d posA, Vector3d posB, Color c, boolean drawLines, boolean drawPlanes) {

//        GL11.glColor4d(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
		Matrix4f matrixPos = matrixStack.last().pose();

		if (drawPlanes) {
			IVertexBuilder buffer = RenderHandler.beginPlanes(renderTypeBuffer);

			buffer.vertex(matrixPos, (float) posA.x, (float) posA.y, (float) posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posA.x, (float) posA.y, (float) posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posB.x, (float) posA.y, (float) posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posB.x, (float) posA.y, (float) posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			//backface (using triangle strip)
			buffer.vertex(matrixPos, (float) posA.x, (float) posA.y, (float) posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posA.x, (float) posA.y, (float) posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha).endVertex();

			RenderHandler.endPlanes(renderTypeBuffer);
		}

		if (drawLines) {
			IVertexBuilder buffer = RenderHandler.beginLines(renderTypeBuffer);

			Vector3d middle = posA.add(posB).scale(0.5);
			buffer.vertex(matrixPos, (float) middle.x, (float) middle.y, (float) posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
			buffer.vertex(matrixPos, (float) middle.x, (float) middle.y, (float) posB.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posA.x, (float) middle.y, (float) middle.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();
			buffer.vertex(matrixPos, (float) posB.x, (float) middle.y, (float) middle.z).color(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha).endVertex();

			RenderHandler.endLines(renderTypeBuffer);
		}
	}

	protected static void drawMirrorLines(MatrixStack matrixStack, IRenderTypeBuffer.Impl renderTypeBuffer, Mirror.MirrorSettings m) {

//        GL11.glColor4d(100, 100, 100, 255);
		IVertexBuilder buffer = RenderHandler.beginLines(renderTypeBuffer);
		Matrix4f matrixPos = matrixStack.last().pose();

		Vector3d pos = m.position.add(epsilon);

		buffer.vertex(matrixPos, (float) pos.x - m.radius, (float) pos.y, (float) pos.z).color(colorX.getRed(), colorX.getGreen(), colorX.getBlue(), lineAlpha).endVertex();
		buffer.vertex(matrixPos, (float) pos.x + m.radius, (float) pos.y, (float) pos.z).color(colorX.getRed(), colorX.getGreen(), colorX.getBlue(), lineAlpha).endVertex();
		buffer.vertex(matrixPos, (float) pos.x, (float) pos.y - m.radius, (float) pos.z).color(colorY.getRed(), colorY.getGreen(), colorY.getBlue(), lineAlpha).endVertex();
		buffer.vertex(matrixPos, (float) pos.x, (float) pos.y + m.radius, (float) pos.z).color(colorY.getRed(), colorY.getGreen(), colorY.getBlue(), lineAlpha).endVertex();
		buffer.vertex(matrixPos, (float) pos.x, (float) pos.y, (float) pos.z - m.radius).color(colorZ.getRed(), colorZ.getGreen(), colorZ.getBlue(), lineAlpha).endVertex();
		buffer.vertex(matrixPos, (float) pos.x, (float) pos.y, (float) pos.z + m.radius).color(colorZ.getRed(), colorZ.getGreen(), colorZ.getBlue(), lineAlpha).endVertex();

		RenderHandler.endLines(renderTypeBuffer);
	}
}
