package dev.zoid.border;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HexFormat;
import java.util.UUID;

// 1.21.4 -> {resource-pack=https://cdn.minepack.fr/pack/94ac6653-c59a-4153-b64a-2dbfcc168f5b
//resource-pack-sha1=b6ffdf48ac5cb5ebd0543b736556f8e850266e07
//resource-pack-id=94ac6653-c59a-4153-b64a-2dbfcc168f5b}

//1.21.6 -> {resource-pack=https://cdn.minepack.fr/pack/25366384-85be-4f3c-bea1-d9663d296c6d
//resource-pack-sha1=93ec2eea09266775b7dc348daf8590191348640d
//resource-pack-id=25366384-85be-4f3c-bea1-d9663d296c6d}

// 26.1 -> {resource-pack=https://cdn.minepack.fr/pack/ce0191c6-f31e-40bc-9354-b561e06cd7de
//resource-pack-sha1=82a25912dff1e6528f0a90c2f1d1be47c132240a
//resource-pack-id=ce0191c6-f31e-40bc-9354-b561e06cd7de}

// 26.2 -> {resource-pack=https://cdn.minepack.fr/pack/db3a24a8-2ef1-4aec-8ebe-13d2ed9b2715
//resource-pack-sha1=c4b4ff78d8566d3fb43735b870f0c9db730d2708
//resource-pack-id=db3a24a8-2ef1-4aec-8ebe-13d2ed9b2715}

// 26.3 -> {resource-pack=https://cdn.minepack.fr/pack/648b981b-1929-4209-b0e2-9ac7e47eb446
//resource-pack-sha1=7f5fcb1623824a4bac6c8dbb9a75a3cbe0c1656d
//resource-pack-id=648b981b-1929-4209-b0e2-9ac7e47eb446}

public class PackSetter {
    private record Pack(UUID id, String url, byte[] hash) {
        private static Pack of(String id, String hash) {
            return new Pack(UUID.fromString(id), "https://cdn.minepack.fr/pack/" + id, HexFormat.of().parseHex(hash));
        }
    }

    private static final Pack PACK_1_21_4 = Pack.of("94ac6653-c59a-4153-b64a-2dbfcc168f5b", "b6ffdf48ac5cb5ebd0543b736556f8e850266e07");
    private static final Pack PACK_1_21_6 = Pack.of("25366384-85be-4f3c-bea1-d9663d296c6d", "93ec2eea09266775b7dc348daf8590191348640d");
    private static final Pack PACK_26_1 = Pack.of("ce0191c6-f31e-40bc-9354-b561e06cd7de", "82a25912dff1e6528f0a90c2f1d1be47c132240a");
    private static final Pack PACK_26_2 = Pack.of("db3a24a8-2ef1-4aec-8ebe-13d2ed9b2715", "c4b4ff78d8566d3fb43735b870f0c9db730d2708");
    private static final Pack PACK_26_3 = Pack.of("648b981b-1929-4209-b0e2-9ac7e47eb446", "7f5fcb1623824a4bac6c8dbb9a75a3cbe0c1656d");

    public static void set(Player player) {
        ClientVersion version = PacketEvents.getAPI().getPlayerManager().getClientVersion(player);

        Pack pack = switch (version) {
            case V_1_21_4, V_1_21_5 -> PACK_1_21_4;
            case V_1_21_6, V_1_21_7, V_1_21_9, V_1_21_11 -> PACK_1_21_6;
            case V_26_1 -> PACK_26_1;
            case V_26_2 -> PACK_26_2;
            case V_26_3 -> PACK_26_3;
            default -> version.isNewerThanOrEquals(ClientVersion.V_26_3) ? PACK_26_3 : null;
        };

        if (pack != null) {
            player.setResourcePack(pack.id, pack.url, pack.hash, (Component) null, false);
        }
    }
}