package ir.soulgraves.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class ItemSerialization {

    private ItemSerialization() {}

    public static byte[] toBytes(List<ItemStack> items) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BukkitObjectOutputStream bo = new BukkitObjectOutputStream(out)) {
            bo.writeInt(items.size());
            for (ItemStack it : items) bo.writeObject(it);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize items", e);
        }
    }

    public static List<ItemStack> fromBytes(byte[] data) {
        List<ItemStack> list = new ArrayList<>();
        if (data == null || data.length == 0) return list;
        try (ByteArrayInputStream in = new ByteArrayInputStream(data);
             BukkitObjectInputStream bi = new BukkitObjectInputStream(in)) {
            int size = bi.readInt();
            for (int i = 0; i < size; i++) list.add((ItemStack) bi.readObject());
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize items", e);
        }
    }
}
