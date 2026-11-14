package dev.zoid.border.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.color.Color;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemCustomModelData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import dev.zoid.border.utils.ClientItemDisplay;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Border {

    private static final double BASE_Y = 65536.0;
    private static final double REANCHOR_SQUARED = 32.0 * 32.0;
    private static final int SETTLE_TICKS = 2;

    public double x;
    public double z;
    public double size;
    public int color = 0xFFFFFF;
    public boolean enabled = true;

    private final Map<Object, View> views = new ConcurrentHashMap<>();
    private final Set<Object> hidden = ConcurrentHashMap.newKeySet();
    public static boolean square = true;
    private static final double SQUARE_Y = 65536.0;
    private long time;
    private long version;
    private double fromX;
    private double fromZ;
    private double fromSize;
    private double toX;
    private double toZ;
    private double toSize;
    private long moveStart;
    private int moveTicks;
    private int fromColor = 0xFFFFFF;
    private int toColor = 0xFFFFFF;
    private long colorStart;
    private int colorTicks;

    public void advance() {
        time++;
        if (x != toX || z != toZ || size != toSize) {
            set(x, z, size, 0);
        }
        if ((color & 0xFFFFFF) != toColor) {
            color(color, 0);
        }
    }

    public void set(double x, double z, double size, int ticks) {
        fromX = currentX();
        fromZ = currentZ();
        fromSize = currentSize();
        this.x = toX = x;
        this.z = toZ = z;
        this.size = toSize = size;
        moveStart = time;
        moveTicks = Math.max(0, ticks);
        version++;
    }

    public void size(double size, int ticks) {
        set(x, z, size, ticks);
    }

    public void center(double x, double z, int ticks) {
        set(x, z, size, ticks);
    }

    public void color(int rgb, int ticks) {
        fromColor = currentColor();
        color = toColor = rgb & 0xFFFFFF;
        colorStart = time;
        colorTicks = Math.max(0, ticks);
    }

    public double currentX() {
        return fromX + (toX - fromX) * moveProgress();
    }

    public double currentZ() {
        return fromZ + (toZ - fromZ) * moveProgress();
    }

    public double currentSize() {
        return fromSize + (toSize - fromSize) * moveProgress();
    }

    public int currentColor() {
        double progress = colorTicks <= 0 ? 1.0 : Math.min(1.0, (time - colorStart) / (double) colorTicks);
        return (channel(16, progress) << 16) | (channel(8, progress) << 8) | channel(0, progress);
    }

    public int remaining() {
        return (int) Math.max(0L, moveStart + moveTicks - time);
    }

    public void tick(Object channel, double viewerX, double viewerY, double viewerZ) {
        if (!enabled || hidden.contains(channel)) {
            remove(channel);
            return;
        }
        View view = views.get(channel);
        if (view == null || view.far(viewerX, viewerY, viewerZ)) {
            spawn(channel, view, viewerX, viewerY, viewerZ);
            return;
        }
        sync(channel, view);
    }

    public void hide(Object channel) {
        hidden.add(channel);
        remove(channel);
    }

    public void show(Object channel) {
        hidden.remove(channel);
    }

    public void remove(Object channel) {
        View view = views.remove(channel);
        if (view != null) {
            view.display.destroy(channel);
        }
    }

    public void forget(Object channel) {
        views.remove(channel);
    }

    public void clear() {
        views.forEach((channel, view) -> view.display.destroy(channel));
        views.clear();
    }

    private double moveProgress() {
        return moveTicks <= 0 ? 1.0 : Math.min(1.0, (time - moveStart) / (double) moveTicks);
    }

    private int channel(int shift, double progress) {
        int from = (fromColor >> shift) & 0xFF;
        int to = (toColor >> shift) & 0xFF;
        return (int) Math.round(from + (to - from) * progress);
    }

    private void spawn(Object channel, View previous, double viewerX, double viewerY, double viewerZ) {
        View next = new View(viewerX, viewerY, viewerZ);
        int rgb = currentColor();
        place(next, currentX(), currentZ(), currentSize(), 0,channel);
        next.display.item(stack(rgb), (byte) 0);
        next.sentColor = rgb;
        next.sentVersion = version;
        next.dirty = moveProgress() < 1.0;
        next.settle = next.dirty ? SETTLE_TICKS : 0;
        next.display.bundle(channel, emit -> {
            emit.accept(next.display.spawnPacket());
            emit.accept(next.display.metaPacket());
            if (previous != null) {
                emit.accept(previous.display.destroyPacket());
            }
        });
        views.put(channel, next);
    }

    private void sync(Object channel, View view) {
        int rgb = currentColor();
        boolean colored = rgb != view.sentColor;
        boolean flying = moveProgress() < 1.0;
        if (view.sentVersion != version) {
            view.sentVersion = version;
            view.dirty = true;
        }
        view.settle = Math.max(0, view.settle - 1);
        boolean ready = view.settle == 0;
        boolean retarget = ready && (view.dirty || colored && flying);
        if (!retarget && !colored) {
            return;
        }
        if (retarget) {
            place(view, toX, toZ, toSize, remaining(),channel);
            view.dirty = false;
        }
        if (colored) {
            view.display.item(stack(rgb), (byte) 0);
            view.sentColor = rgb;
        }
        view.display.update(channel);
    }

    private void place(View view, double centerX, double centerZ, double diameter, int ticks, Object channel) {
        int protocol = PacketEvents.getAPI().getPlayerManager()
                .getClientVersion(channel)
                .getProtocolVersion();

        if (protocol >= com.github.retrooper.packetevents.protocol.player.ClientVersion.V_26_1.getProtocolVersion()) {
            view.display.translation(new Vector3f(
                    (float) (centerX - view.anchorX),
                    (float) (BASE_Y + diameter / 2.0 - view.anchorY),
                    (float) (centerZ - view.anchorZ)
            ));
        } else {
            double top = square ? SQUARE_Y : BASE_Y + diameter / 2.0;
            view.display.translation(new Vector3f(
                    (float) (centerX - view.anchorX),
                    (float) (top - view.anchorY),
                    (float) (centerZ - view.anchorZ)
            ));
            view.display.scale(square
                    ? new Vector3f((float) diameter, 1.0f, (float) diameter)
                    : new Vector3f(1.0f, 1.0f, 1.0f));
        }

        view.display.interpolation(ticks, 0);
    }

    private ItemStack stack(int rgb) {
        return ItemStack.builder()
                .type(ItemTypes.LEATHER_CHESTPLATE)
                .component(ComponentTypes.CUSTOM_MODEL_DATA_LISTS, new ItemCustomModelData(
                        List.of(),
                        List.of(),
                        List.of("glacier_border"),
                        List.of(new Color(rgb & ~1), new Color(rgb | 1))
                ))
                .build();
    }

    private static class View {

        final ClientItemDisplay display = new ClientItemDisplay();
        final double anchorX;
        final double anchorY;
        final double anchorZ;
        int sentColor;
        long sentVersion;
        boolean dirty;
        int settle;

        View(double x, double y, double z) {
            anchorX = x;
            anchorY = y;
            anchorZ = z;
            display.pos(new Vector3d(x, y, z));
        }

        boolean far(double x, double y, double z) {
            double dx = anchorX - x;
            double dy = anchorY - y;
            double dz = anchorZ - z;
            return dx * dx + dy * dy + dz * dz >= REANCHOR_SQUARED;
        }
    }
}