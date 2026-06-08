package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in;

import java.io.File;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.ExceptionHandler;

public final class SavedItemReference {
    private final ProjectAddon addon;
    private final File file;
    private final String pathWithoutExtension;
    private final String source;
    private ItemStack cachedItem;
    private ItemStack cachedDisplayItem;
    private boolean loadAttempted;
    private boolean warningLogged;

    public SavedItemReference(ProjectAddon addon, File file, String pathWithoutExtension) {
        this.addon = addon;
        this.file = file;
        this.pathWithoutExtension = pathWithoutExtension;
        this.source = addon.getAddonId() + ";" + pathWithoutExtension;
    }

    public String source() {
        return source;
    }

    public @Nullable ItemStack loadItem() {
        if (loadAttempted) {
            return cachedItem == null ? null : cachedItem.clone();
        }

        loadAttempted = true;
        try {
            ItemStack item = YamlConfiguration.loadConfiguration(file).getItemStack("item");
            if (item == null) {
                return null;
            }

            item.editMeta(meta -> meta.getPersistentDataContainer()
                    .set(SaveditemsGroup.SOURCE_KEY, PersistentDataType.STRING, source()));
            cachedItem = item;
            return item.clone();
        } catch (Exception e) {
            logLoadWarning(e);
            return null;
        }
    }

    public ItemStack getDisplayItem() {
        if (cachedDisplayItem != null) {
            return cachedDisplayItem.clone();
        }

        ItemStack item = loadItem();
        if (item != null) {
            cachedDisplayItem = item;
            return item.clone();
        }

        ItemStack fallback = new ItemStack(Material.BARRIER);
        fallback.editMeta(meta -> {
            meta.setDisplayName("Cannot read saved item");
            meta.getPersistentDataContainer().set(SaveditemsGroup.SOURCE_KEY, PersistentDataType.STRING, source());
        });
        cachedDisplayItem = fallback;
        return fallback.clone();
    }

    private void logLoadWarning(Exception e) {
        if (warningLogged) {
            return;
        }

        warningLogged = true;
        ExceptionHandler.handleWarning("Cannot read saved item " + source() + ": " + e.getMessage());
    }
}
