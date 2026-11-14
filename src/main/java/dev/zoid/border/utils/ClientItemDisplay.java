package dev.zoid.border.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class ClientItemDisplay {

    public final int id = ThreadLocalRandom.current().nextInt(100000, Integer.MAX_VALUE);
    public final UUID uuid = UUID.randomUUID();

    public Vector3d pos = Vector3d.zero();
    public float pitch;
    public float yaw;

    public int interpolationDuration;
    public int startInterpolation;
    public int transformDuration;

    public Vector3f translation = new Vector3f(0.0f, 0.0f, 0.0f);
    public Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
    public Quaternion4f rightRotation = new Quaternion4f(0.0f, 0.0f, 0.0f, 1.0f);
    public Quaternion4f leftRotation = new Quaternion4f(0.0f, 0.0f, 0.0f, 1.0f);

    public byte billboard = 0;
    public int brightness = -1;
    public float viewRange = 1.0f;
    public float shadowRadius = 0.0f;
    public float shadowStrength = 1.0f;
    public float width = 0.0f;
    public float height = 0.0f;
    public int glowColor = -1;

    public ItemStack item = ItemStack.EMPTY;
    public byte context = 0;

    public int vehicleId = -1;
    public int[] passengers = new int[0];

    public ClientItemDisplay pos(Vector3d pos) {
        this.pos = pos;
        return this;
    }

    public ClientItemDisplay rot(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        return this;
    }

    public ClientItemDisplay scale(Vector3f scale) {
        this.scale = scale;
        return this;
    }

    public ClientItemDisplay translation(Vector3f translation) {
        this.translation = translation;
        return this;
    }

    public ClientItemDisplay leftRotation(Quaternion4f rotation) {
        this.leftRotation = rotation;
        return this;
    }

    public ClientItemDisplay rightRotation(Quaternion4f rotation) {
        this.rightRotation = rotation;
        return this;
    }

    public ClientItemDisplay billboard(byte billboard) {
        this.billboard = billboard;
        return this;
    }

    public ClientItemDisplay interpolation(int duration, int startDelta) {
        this.transformDuration = duration;
        this.startInterpolation = startDelta;
        return this;
    }

    public ClientItemDisplay item(ItemStack item, byte context) {
        this.item = item;
        this.context = context;
        return this;
    }

    public ClientItemDisplay vehicle(int vehicleId) {
        this.vehicleId = vehicleId;
        return this;
    }

    public ClientItemDisplay passengers(int... passengers) {
        this.passengers = passengers;
        return this;
    }

    public List<EntityData<?>> meta() {
        List<EntityData<?>> data = new ArrayList<>(16);
        data.add(new EntityData<>(10, EntityDataTypes.INT, interpolationDuration));
        data.add(new EntityData<>(8, EntityDataTypes.INT, startInterpolation));
        data.add(new EntityData<>(9, EntityDataTypes.INT, transformDuration));
        data.add(new EntityData<>(11, EntityDataTypes.VECTOR3F, translation));
        data.add(new EntityData<>(12, EntityDataTypes.VECTOR3F, scale));
        data.add(new EntityData<>(13, EntityDataTypes.QUATERNION, leftRotation));
        data.set(5, new EntityData<>(13, EntityDataTypes.QUATERNION, leftRotation));
        data.add(new EntityData<>(14, EntityDataTypes.QUATERNION, rightRotation));
        data.add(new EntityData<>(15, EntityDataTypes.BYTE, billboard));
        data.add(new EntityData<>(16, EntityDataTypes.INT, brightness));
        data.add(new EntityData<>(17, EntityDataTypes.FLOAT, viewRange));
        data.add(new EntityData<>(18, EntityDataTypes.FLOAT, shadowRadius));
        data.add(new EntityData<>(19, EntityDataTypes.FLOAT, shadowStrength));
        data.add(new EntityData<>(20, EntityDataTypes.FLOAT, width));
        data.add(new EntityData<>(21, EntityDataTypes.FLOAT, height));
        data.add(new EntityData<>(22, EntityDataTypes.INT, glowColor));
        data.add(new EntityData<>(23, EntityDataTypes.ITEMSTACK, item));
        data.add(new EntityData<>(24, EntityDataTypes.BYTE, context));
        return data;
    }

    private Quaternion4f rotation(Quaternion4f q) {
        return q;
    }

    public WrapperPlayServerSpawnEntity spawnPacket() {
        return new WrapperPlayServerSpawnEntity(id, Optional.of(uuid), EntityTypes.ITEM_DISPLAY, pos, pitch, yaw, yaw, 0, Optional.empty());
    }

    public WrapperPlayServerEntityMetadata metaPacket() {
        return new WrapperPlayServerEntityMetadata(id, meta());
    }

    public WrapperPlayServerSetPassengers passengerPacket() {
        return vehicleId != -1
                ? new WrapperPlayServerSetPassengers(vehicleId, append(passengers, id))
                : new WrapperPlayServerSetPassengers(id, passengers);
    }

    public WrapperPlayServerDestroyEntities destroyPacket() {
        return new WrapperPlayServerDestroyEntities(id);
    }

    public void spawn(Object channel) {
        sendBundle(channel, w -> {
            w.accept(spawnPacket());
            w.accept(metaPacket());
            if (vehicleId != -1 || passengers.length > 0) {
                w.accept(passengerPacket());
            }
        });
    }

    public void update(Object channel) {
        send(channel, metaPacket());
    }

    public void destroy(Object channel) {
        send(channel, destroyPacket());
    }

    public void bundle(Object channel, Consumer<Consumer<PacketWrapper<?>>> emitter) {
        sendBundle(channel, emitter);
    }

    private void sendBundle(Object channel, Consumer<Consumer<PacketWrapper<?>>> emitter) {
        send(channel, new WrapperPlayServerBundle());
        emitter.accept(packet -> send(channel, packet));
        send(channel, new WrapperPlayServerBundle());
    }

    private void send(Object channel, PacketWrapper<?> wrapper) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(channel, wrapper);
    }

    private int[] append(int[] src, int val) {
        for (int cur : src) {
            if (cur == val) return src;
        }
        int[] out = new int[src.length + 1];
        System.arraycopy(src, 0, out, 0, src.length);
        out[src.length] = val;
        return out;
    }
}