package net.unfamily.colossal_reactors.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders preview markers (small cubes) for reactor footprint. Client-only.
 */
public class PreviewMarkRenderer {

    private static final PreviewMarkRenderer INSTANCE = new PreviewMarkRenderer();
    private final Map<BlockPos, MarkData> markers = new ConcurrentHashMap<>();

    public static PreviewMarkRenderer getInstance() {
        return INSTANCE;
    }

    public void addMarker(BlockPos pos, int color, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            mc.execute(() -> {
                if (mc.level != null) {
                    long expire = mc.level.getGameTime() + durationTicks;
                    markers.put(pos.immutable(), new MarkData(color, expire));
                }
            });
        } else {
            long expire = mc.level.getGameTime() + durationTicks;
            markers.put(pos.immutable(), new MarkData(color, expire));
        }
    }

    public void render(PoseStack poseStack, float partialTick) {
        if (markers.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long currentTime = mc.level.getGameTime();
        markers.entrySet().removeIf(e -> e.getValue().expirationTime <= currentTime);
        if (markers.isEmpty()) return;

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Map.Entry<BlockPos, MarkData> entry : markers.entrySet()) {
            drawSmallCube(buffer, entry.getKey(), cameraPos, entry.getValue().color);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawSmallCube(BufferBuilder buffer, BlockPos pos, Vec3 cameraPos, int color) {
        float size = 12.0f / 16.0f;
        float half = size / 2.0f;
        float x = pos.getX() + 0.5f - half - (float) cameraPos.x;
        float y = pos.getY() + 0.5f - half - (float) cameraPos.y;
        float z = pos.getZ() + 0.5f - half - (float) cameraPos.z;

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // bottom
        buffer.addVertex(x, y, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y, z + size).setColor(r, g, b, a);
        // top
        buffer.addVertex(x, y + size, z).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z).setColor(r, g, b, a);
        // north
        buffer.addVertex(x, y, z).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z).setColor(r, g, b, a);
        // south
        buffer.addVertex(x, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z + size).setColor(r, g, b, a);
        // west
        buffer.addVertex(x, y, z).setColor(r, g, b, a);
        buffer.addVertex(x, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z).setColor(r, g, b, a);
        // east
        buffer.addVertex(x + size, y, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z + size).setColor(r, g, b, a);
    }

    private record MarkData(int color, long expirationTime) {}
}
