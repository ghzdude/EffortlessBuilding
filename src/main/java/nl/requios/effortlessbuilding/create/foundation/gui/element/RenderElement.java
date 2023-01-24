package nl.requios.effortlessbuilding.create.foundation.gui.element;

import com.mojang.blaze3d.vertex.PoseStack;

public abstract class RenderElement implements ScreenElement {

	public static final RenderElement EMPTY = new RenderElement() {
		@Override
		public void render(PoseStack ms) {
		}
	};

	public static RenderElement of(ScreenElement renderable) {
		return new SimpleRenderElement(renderable);
	}

	protected int width = 16, height = 16;
	protected float x = 0, y = 0, z = 0;
	protected float alpha = 1f;

	public <T extends RenderElement> T at(float x, float y) {
		this.x = x;
		this.y = y;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends RenderElement> T at(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends RenderElement> T withBounds(int width, int height) {
		this.width = width;
		this.height = height;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends RenderElement> T withAlpha(float alpha) {
		this.alpha = alpha;
		//noinspection unchecked
		return (T) this;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}

	public abstract void render(PoseStack ms);

	@Override
	public void render(PoseStack ms, int x, int y) {
		this.at(x, y).render(ms);
	}

	public static class SimpleRenderElement extends RenderElement {

		private ScreenElement renderable;

		public SimpleRenderElement(ScreenElement renderable) {
			this.renderable = renderable;
		}

		@Override
		public void render(PoseStack ms) {
			renderable.render(ms, (int) x, (int) y);
		}
	}
}
