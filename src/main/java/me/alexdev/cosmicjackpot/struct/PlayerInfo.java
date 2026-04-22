package me.alexdev.cosmicjackpot.struct;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;

public class PlayerInfo {
    private UUID uuid;
    private String username;

    public PlayerInfo(Player player) {
        this.username = player.getName();
        this.uuid = player.getUniqueId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlayerInfo)) {
            return false;
        }
        PlayerInfo other = (PlayerInfo) obj;
        return Objects.equals(this.uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uuid);
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getUsername() {
        return this.username;
    }
}
