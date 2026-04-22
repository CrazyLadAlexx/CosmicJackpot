package me.alexdev.cosmicjackpot.struct;

import java.util.UUID;
import java.util.Objects;
import org.bukkit.entity.Player;

public class PlayerInfo {
    private UUID uuid;
    private String username;

    public PlayerInfo(Player player) {
        this.username = player.getName();
        this.uuid = player.getUniqueId();
    }

    public boolean equals(Object obj) {
        PlayerInfo other;
        if (obj instanceof PlayerInfo && (other = (PlayerInfo)obj).getUuid().equals(this.uuid)) {
            return true;
        }
        return super.equals(obj);
    }

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
