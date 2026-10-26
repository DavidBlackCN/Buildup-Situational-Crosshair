package dev.buildup.situationalcrosshair.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import dev.buildup.situationalcrosshair.CrosshairOptions;
import dev.buildup.situationalcrosshair.crosshair.AdaptiveContrast;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

/** One bounded asynchronous readback, before any HUD pixels are rendered. */
public final class CrosshairBackground {
    private static final long INTERVAL = 50_000_000L, MAX_AGE = 500_000_000L;
    private static final AdaptiveContrast SELECTOR = new AdaptiveContrast();
    private static GpuBuffer buffer;
    private static boolean inFlight, failed, closed, requested;
    private static long generation, nextSample, consumed = -1;
    private static Sample latest;
    private static int color;
    private static Object level;
    private static int width, height, scale;
    private record Sample(long generation, long time, double luminance, Object surface) { }
    private record BlockSurface(net.minecraft.core.BlockPos position, net.minecraft.core.Direction face,
                                net.minecraft.world.level.block.state.BlockState state) { }

    private CrosshairBackground() { }

    /** Called only for the actually visible, supported central crosshair. */
    public static void request() { requested = true; }

    public static void reset() {
        generation++;
        requested = false;
        latest = null;
        consumed = -1;
        color = 0;
        nextSample = 0;
        SELECTOR.reset();
    }

    /** Zero means use pure inverse rendering, including stale/unsupported samples. */
    public static int color() {
        if (latest == null || latest.generation != generation || System.nanoTime() - latest.time > MAX_AGE) {
            SELECTOR.reset();
            return 0;
        }
        if (consumed != latest.time) {
            color = SELECTOR.accept(latest.luminance, latest.time, latest.surface);
            consumed = latest.time;
        }
        return color;
    }

    public static void beforeGui() {
        boolean wanted = requested;
        requested = false;
        var client = Minecraft.getInstance();
        if (!wanted || !CrosshairOptions.enabled() || !CrosshairOptions.adaptiveContrast()
                || client.level == null || client.gui.screen() != null || client.gui.overlay() != null) {
            if (latest != null || inFlight) reset();
            level = null; // Do not retain the departed ClientLevel while at the title screen.
            return;
        }
        if (closed || failed) return;
        var target = client.gameRenderer.mainRenderTarget();
        int guiScale = client.getWindow().getGuiScale();
        if (level != client.level || width != target.width || height != target.height || scale != guiScale) {
            reset();
            level = client.level; width = target.width; height = target.height; scale = guiScale;
        }
        var texture = target.getColorTexture();
        if (texture == null || texture.isClosed() || texture.getFormat() != GpuFormat.RGBA8_UNORM
                || (texture.usage() & com.mojang.renderpearl.api.textures.GpuTexture.USAGE_COPY_SRC) == 0) {
            reset();
            return;
        }
        long now = System.nanoTime();
        if (inFlight || now < nextSample) return;
        // Include neighboring texture details, not just the bright/dark texel under the center.
        int size = Math.min(Math.min(width, height), Math.min(127, 31 * Math.max(1, guiScale)));
        if (size <= 0) return;
        long bytes = (long) size * size * 4;
        long ticket = generation;
        Object surface = null;
        if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK)
            surface = new BlockSurface(hit.getBlockPos(), hit.getDirection(), client.level.getBlockState(hit.getBlockPos()));
        else if (client.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit) surface = hit.getEntity();
        Object capturedSurface = surface;
        try {
            if (buffer == null || buffer.size() != bytes) {
                if (buffer != null) buffer.close();
                buffer = RenderSystem.getDevice().createBuffer(() -> "Crosshair background sample",
                        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bytes);
            }
            var readback = buffer;
            inFlight = true;
            nextSample = now + INTERVAL;
            RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(texture, readback, 0, () -> {
                // Minecraft invokes this fenced callback on the render thread after the copy completes.
                try (var mapped = readback.map(true, false)) {
                    if (!closed && ticket == generation)
                        latest = new Sample(ticket, now, AdaptiveContrast.luminance(mapped.data()), capturedSurface);
                } catch (RuntimeException ex) {
                    fail(ex);
                } finally {
                    inFlight = false;
                    if (closed) { readback.close(); buffer = null; }
                }
            }, 0, (width - size) / 2, (height - size) / 2, size, size);
        } catch (RuntimeException ex) {
            inFlight = false;
            fail(ex);
        }
    }

    private static void fail(RuntimeException ex) {
        failed = true;
        reset();
        LoggerFactory.getLogger("buildup_situational_crosshair").warn(
                "Adaptive crosshair sampling unavailable; using inverse rendering for this session", ex);
    }

    public static void close() {
        closed = true;
        reset();
        if (!inFlight && buffer != null) { buffer.close(); buffer = null; }
    }
}
