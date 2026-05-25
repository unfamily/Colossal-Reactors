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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders footprint preview markers per builder origin. Client-only.
 */
public class PreviewMarkRenderer {

    /** Markers not owned by a builder (validation hints). */
    public static final BlockPos EPHEMERAL_OWNER = BlockPos.ZERO;

    private static final PreviewMarkRenderer INSTANCE = new PreviewMarkRenderer();
    private final Map<BlockPos, Map<BlockPos, MarkData>> markersByBuilder = new ConcurrentHashMap<>();

    public static PreviewMarkRenderer getInstance() {
        return INSTANCE;
    }

    /** Clears markers for every builder (disconnect / world unload). */
    public void clearMarkers() {
        markersByBuilder.clear();
    }

    public void clearMarkersForBuilder(BlockPos builderOrigin) {
        if (builderOrigin != null) {
            markersByBuilder.remove(builderOrigin.immutable());
        }
    }

    public void addMarker(BlockPos builderOrigin, BlockPos worldPos, int color, int durationTicks) {
        if (worldPos == null) {
            return;
        }
        BlockPos owner = builderOrigin == null ? EPHEMERAL_OWNER : builderOrigin;
        Minecraft mc = Minecraft.getInstance();
        Runnable add = () -> {
            if (mc.level == null) {
                return;
            }
            long expire = durationTicks > 0 ? mc.level.getGameTime() + durationTicks : Long.MAX_VALUE;
            markersByBuilder
                    .computeIfAbsent(owner.immutable(), k -> new ConcurrentHashMap<>())
                    .put(worldPos.immutable(), new MarkData(color, expire));
        };
        if (mc.level == null) {
            mc.execute(add);
        } else {
            add.run();
        }
    }

    public void render(PoseStack poseStack, float partialTick) {
        if (markersByBuilder.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        long currentTime = mc.level.getGameTime();
        pruneExpired(currentTime);
        if (markersByBuilder.isEmpty()) {
            return;
        }

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Map<BlockPos, MarkData> worldMarkers : markersByBuilder.values()) {
            for (Map.Entry<BlockPos, MarkData> entry : worldMarkers.entrySet()) {
                drawSmallCube(buffer, entry.getKey(), cameraPos, entry.getValue().color);
            }
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void pruneExpired(long currentTime) {
        Iterator<Map.Entry<BlockPos, Map<BlockPos, MarkData>>> builderIt = markersByBuilder.entrySet().iterator();
        while (builderIt.hasNext()) {
            Map<BlockPos, MarkData> worldMarkers = builderIt.next().getValue();
            worldMarkers.entrySet().removeIf(e -> e.getValue().expirationTime <= currentTime);
            if (worldMarkers.isEmpty()) {
                builderIt.remove();
            }
        }
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

        buffer.addVertex(x, y, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z).setColor(r, g, b, a);
        buffer.addVertex(x, y, z).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z).setColor(r, g, b, a);
        buffer.addVertex(x, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z).setColor(r, g, b, a);
        buffer.addVertex(x + size, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x + size, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y, z).setColor(r, g, b, a);
        buffer.addVertex(x, y, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z + size).setColor(r, g, b, a);
        buffer.addVertex(x, y + size, z).setColor(r, g, b, a);
    }

    private record MarkData(int color, long expirationTime) {}
}
