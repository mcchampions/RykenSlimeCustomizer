/*
 * RykenSlimefunCustomizer
 * Copyright (C) 2026 lijinhong11(mmmjjjkx) and balugaq
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSRealm;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.script.ScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.BlockMenuUtil;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.ExceptionHandler;

public class JavaScriptEval extends ScriptEval {
    private final Context jsEngine = Context.newBuilder("js")
            .hostClassLoader(RykenSlimefunCustomizer.class.getClassLoader())
            .allowAllAccess(true)
            .allowHostAccess(UNIVERSAL_HOST_ACCESS)
            .allowNativeAccess(false)
            .allowExperimentalOptions(true)
            .allowPolyglotAccess(PolyglotAccess.ALL)
            .allowCreateProcess(true)
            .allowValueSharing(true)
            .allowIO(IOAccess.ALL)
            .allowHostClassLookup(s -> !s.startsWith("net.luckperms")
                    && !s.startsWith("me.lucko")
                    && !s.startsWith("org.anjocaido.groupmanager"))
            .allowHostClassLoading(true)
            .engine(Engine.newBuilder("js").allowExperimentalOptions(true).build())
            .currentWorkingDirectory(getAddon().getScriptsFolder().toPath().toAbsolutePath())
            .build();

    private JavaScriptEval(@NotNull File js, ProjectAddon addon) {
        super(js, addon);

        advancedSetup();

        setup();

        contextInit();

        addon.getScriptEvals().add(this);
    }

    public static JavaScriptEval create(@NotNull File js, ProjectAddon addon) {
        try {
            return new JavaScriptEval(js, addon);
        } catch (Throwable e) {
            ExceptionHandler.handleError("无法加载脚本 " + js.getAbsolutePath(), e);
            return null;
        }
    }

    private void advancedSetup() {
        JSRealm realm = JavaScriptLanguage.getJSRealm(jsEngine);
        TruffleLanguage.Env env = realm.getEnv();
        addThing("SlimefunItems", env.asHostSymbol(SlimefunItems.class));
        addThing("SlimefunItem", env.asHostSymbol(SlimefunItem.class));
        addThing("StorageCacheUtils", env.asHostSymbol(StorageCacheUtils.class));
        addThing("SlimefunUtils", env.asHostSymbol(SlimefunUtils.class));
        addThing("BlockMenu", env.asHostSymbol(BlockMenu.class));
        addThing("BlockMenuUtil", env.asHostSymbol(BlockMenuUtil.class));
        addThing("PlayerProfile", env.asHostSymbol(PlayerProfile.class));
        addThing("Slimefun", env.asHostSymbol(Slimefun.class));

        // Pre-register commonly used Java classes as host symbols.
        // This avoids the expensive JavaPackage.lookupClass() → ClassLoader.loadClass()
        // chain on every instanceof check and property access in user scripts.
        registerHostClasses(env,
            // === Java standard library types (must be accessible for type conversions) ===
            java.lang.Float.class,
            java.lang.Double.class,
            java.lang.Integer.class,
            java.lang.Long.class,
            java.lang.Short.class,
            java.lang.Byte.class,
            java.lang.Boolean.class,
            java.lang.Character.class,
            java.lang.String.class,
            java.lang.Number.class,
            java.lang.Math.class,
            java.lang.Object.class,

            // === Core Bukkit types (frequently accessed from event getters) ===
            org.bukkit.Bukkit.class,
            org.bukkit.Server.class,
            org.bukkit.Location.class,
            org.bukkit.Material.class,
            org.bukkit.World.class,
            org.bukkit.OfflinePlayer.class,
            org.bukkit.NamespacedKey.class,
            org.bukkit.util.Vector.class,
            org.bukkit.ChatColor.class,
            org.bukkit.Sound.class,
            org.bukkit.GameMode.class,
            org.bukkit.Difficulty.class,
            org.bukkit.Color.class,
            org.bukkit.Particle.class,
            org.bukkit.Effect.class,
            org.bukkit.WorldType.class,
            org.bukkit.WeatherType.class,
            org.bukkit.Art.class,
            org.bukkit.TreeType.class,
            org.bukkit.Axis.class,
            org.bukkit.Statistic.class,
            org.bukkit.configuration.file.YamlConfiguration.class,

            // === Entity types ===
            org.bukkit.entity.Player.class,
            org.bukkit.entity.LivingEntity.class,
            org.bukkit.entity.Entity.class,
            org.bukkit.entity.EntityType.class,
            org.bukkit.entity.Item.class,
            org.bukkit.entity.Projectile.class,
            org.bukkit.entity.Damageable.class,
            org.bukkit.entity.HumanEntity.class,
            org.bukkit.entity.Animals.class,
            org.bukkit.entity.Monster.class,
            org.bukkit.entity.Creature.class,
            org.bukkit.entity.Arrow.class,
            org.bukkit.entity.Fireball.class,
            org.bukkit.entity.LightningStrike.class,
            org.bukkit.entity.Firework.class,
            org.bukkit.entity.ExperienceOrb.class,
            org.bukkit.entity.TNTPrimed.class,
            org.bukkit.entity.FallingBlock.class,
            org.bukkit.entity.Minecart.class,
            org.bukkit.entity.Boat.class,
            org.bukkit.entity.ItemFrame.class,
            org.bukkit.entity.ArmorStand.class,
            org.bukkit.entity.ThrownPotion.class,
            org.bukkit.entity.Egg.class,
            org.bukkit.entity.Snowball.class,
            org.bukkit.entity.EnderPearl.class,
            org.bukkit.entity.FishHook.class,
            org.bukkit.entity.Hanging.class,
            org.bukkit.entity.Painting.class,

            // === Inventory / Item types ===
            org.bukkit.inventory.ItemStack.class,
            org.bukkit.inventory.Inventory.class,
            org.bukkit.inventory.InventoryView.class,
            org.bukkit.inventory.PlayerInventory.class,
            org.bukkit.inventory.EquipmentSlot.class,
            org.bukkit.inventory.ItemFlag.class,
            org.bukkit.inventory.meta.ItemMeta.class,
            org.bukkit.inventory.meta.Damageable.class,
            org.bukkit.inventory.Recipe.class,
            org.bukkit.inventory.FurnaceRecipe.class,
            org.bukkit.inventory.ShapedRecipe.class,
            org.bukkit.inventory.ShapelessRecipe.class,
            org.bukkit.enchantments.Enchantment.class,
            org.bukkit.potion.PotionEffect.class,
            org.bukkit.potion.PotionEffectType.class,
            org.bukkit.attribute.Attribute.class,
            org.bukkit.attribute.AttributeModifier.class,

            // === Block types ===
            org.bukkit.block.Block.class,
            org.bukkit.block.BlockFace.class,
            org.bukkit.block.BlockState.class,
            org.bukkit.block.Chest.class,
            org.bukkit.block.Furnace.class,
            org.bukkit.block.BrewingStand.class,
            org.bukkit.block.Dispenser.class,
            org.bukkit.block.Hopper.class,
            org.bukkit.block.Dropper.class,
            org.bukkit.block.Jukebox.class,
            org.bukkit.block.Beacon.class,
            org.bukkit.block.Sign.class,
            org.bukkit.block.CreatureSpawner.class,
            org.bukkit.block.data.BlockData.class,

            // === Persistence types ===
            org.bukkit.persistence.PersistentDataContainer.class,
            org.bukkit.persistence.PersistentDataType.class,

            // === Block events ===
            org.bukkit.event.block.BlockBreakEvent.class,
            org.bukkit.event.block.BlockBurnEvent.class,
            org.bukkit.event.block.BlockCanBuildEvent.class,
            org.bukkit.event.block.BlockCookEvent.class,
            org.bukkit.event.block.BlockDamageEvent.class,
            org.bukkit.event.block.BlockDispenseArmorEvent.class,
            org.bukkit.event.block.BlockDispenseEvent.class,
            org.bukkit.event.block.BlockDropItemEvent.class,
            org.bukkit.event.block.BlockExpEvent.class,
            org.bukkit.event.block.BlockExplodeEvent.class,
            org.bukkit.event.block.BlockFadeEvent.class,
            org.bukkit.event.block.BlockFertilizeEvent.class,
            org.bukkit.event.block.BlockFormEvent.class,
            org.bukkit.event.block.BlockFromToEvent.class,
            org.bukkit.event.block.BlockGrowEvent.class,
            org.bukkit.event.block.BlockIgniteEvent.class,
            org.bukkit.event.block.BlockMultiPlaceEvent.class,
            org.bukkit.event.block.BlockPhysicsEvent.class,
            org.bukkit.event.block.BlockPistonExtendEvent.class,
            org.bukkit.event.block.BlockPistonRetractEvent.class,
            org.bukkit.event.block.BlockPlaceEvent.class,
            org.bukkit.event.block.BlockReceiveGameEvent.class,
            org.bukkit.event.block.BlockRedstoneEvent.class,
            org.bukkit.event.block.BlockShearEntityEvent.class,
            org.bukkit.event.block.BlockSpreadEvent.class,
            org.bukkit.event.block.CauldronLevelChangeEvent.class,
            org.bukkit.event.block.EntityBlockFormEvent.class,
            org.bukkit.event.block.FluidLevelChangeEvent.class,
            org.bukkit.event.block.LeavesDecayEvent.class,
            org.bukkit.event.block.MoistureChangeEvent.class,
            org.bukkit.event.block.NotePlayEvent.class,
            org.bukkit.event.block.SignChangeEvent.class,
            org.bukkit.event.block.SpongeAbsorbEvent.class,

            // === Command events ===
            org.bukkit.event.command.UnknownCommandEvent.class,

            // === Enchantment events ===
            org.bukkit.event.enchantment.EnchantItemEvent.class,
            org.bukkit.event.enchantment.PrepareItemEnchantEvent.class,

            // === Entity events ===
            org.bukkit.event.entity.AreaEffectCloudApplyEvent.class,
            org.bukkit.event.entity.ArrowBodyCountChangeEvent.class,
            org.bukkit.event.entity.BatToggleSleepEvent.class,
            org.bukkit.event.entity.CreatureSpawnEvent.class,
            org.bukkit.event.entity.CreeperPowerEvent.class,
            org.bukkit.event.entity.EnderDragonChangePhaseEvent.class,
            org.bukkit.event.entity.EntityAirChangeEvent.class,
            org.bukkit.event.entity.EntityBreakDoorEvent.class,
            org.bukkit.event.entity.EntityBreedEvent.class,
            org.bukkit.event.entity.EntityChangeBlockEvent.class,
            org.bukkit.event.entity.EntityCombustByBlockEvent.class,
            org.bukkit.event.entity.EntityCombustByEntityEvent.class,
            org.bukkit.event.entity.EntityCombustEvent.class,
            org.bukkit.event.entity.EntityDamageByBlockEvent.class,
            org.bukkit.event.entity.EntityDamageByEntityEvent.class,
            org.bukkit.event.entity.EntityDamageEvent.class,
            org.bukkit.event.entity.EntityDeathEvent.class,
            org.bukkit.event.entity.EntityDropItemEvent.class,
            org.bukkit.event.entity.EntityEnterBlockEvent.class,
            org.bukkit.event.entity.EntityEnterLoveModeEvent.class,
            org.bukkit.event.entity.EntityExhaustionEvent.class,
            org.bukkit.event.entity.EntityExplodeEvent.class,
            org.bukkit.event.entity.EntityInteractEvent.class,
            org.bukkit.event.entity.EntityPickupItemEvent.class,
            org.bukkit.event.entity.EntityPlaceEvent.class,
            org.bukkit.event.entity.EntityPortalEnterEvent.class,
            org.bukkit.event.entity.EntityPortalExitEvent.class,
            org.bukkit.event.entity.EntityPoseChangeEvent.class,
            org.bukkit.event.entity.EntityPotionEffectEvent.class,
            org.bukkit.event.entity.EntityRegainHealthEvent.class,
            org.bukkit.event.entity.EntityResurrectEvent.class,
            org.bukkit.event.entity.EntityShootBowEvent.class,
            org.bukkit.event.entity.EntitySpawnEvent.class,
            org.bukkit.event.entity.EntitySpellCastEvent.class,
            org.bukkit.event.entity.EntityTameEvent.class,
            org.bukkit.event.entity.EntityTargetEvent.class,
            org.bukkit.event.entity.EntityTargetLivingEntityEvent.class,
            org.bukkit.event.entity.EntityTeleportEvent.class,
            org.bukkit.event.entity.EntityToggleGlideEvent.class,
            org.bukkit.event.entity.EntityToggleSwimEvent.class,
            org.bukkit.event.entity.EntityTransformEvent.class,
            org.bukkit.event.entity.EntityUnleashEvent.class,
            org.bukkit.event.entity.ExpBottleEvent.class,
            org.bukkit.event.entity.ExplosionPrimeEvent.class,
            org.bukkit.event.entity.FireworkExplodeEvent.class,
            org.bukkit.event.entity.FoodLevelChangeEvent.class,
            org.bukkit.event.entity.HorseJumpEvent.class,
            org.bukkit.event.entity.ItemDespawnEvent.class,
            org.bukkit.event.entity.ItemMergeEvent.class,
            org.bukkit.event.entity.ItemSpawnEvent.class,
            org.bukkit.event.entity.LingeringPotionSplashEvent.class,
            org.bukkit.event.entity.PiglinBarterEvent.class,
            org.bukkit.event.entity.PigZapEvent.class,
            org.bukkit.event.entity.PigZombieAngerEvent.class,
            org.bukkit.event.entity.PlayerDeathEvent.class,
            org.bukkit.event.entity.PlayerLeashEntityEvent.class,
            org.bukkit.event.entity.PotionSplashEvent.class,
            org.bukkit.event.entity.ProjectileHitEvent.class,
            org.bukkit.event.entity.ProjectileLaunchEvent.class,
            org.bukkit.event.entity.SheepDyeWoolEvent.class,
            org.bukkit.event.entity.SheepRegrowWoolEvent.class,
            org.bukkit.event.entity.SlimeSplitEvent.class,
            org.bukkit.event.entity.SpawnerSpawnEvent.class,
            org.bukkit.event.entity.StriderTemperatureChangeEvent.class,
            org.bukkit.event.entity.VillagerAcquireTradeEvent.class,
            org.bukkit.event.entity.VillagerCareerChangeEvent.class,
            org.bukkit.event.entity.VillagerReplenishTradeEvent.class,

            // === Hanging events ===
            org.bukkit.event.hanging.HangingBreakByEntityEvent.class,
            org.bukkit.event.hanging.HangingBreakEvent.class,
            org.bukkit.event.hanging.HangingPlaceEvent.class,

            // === Inventory events ===
            org.bukkit.event.inventory.BrewEvent.class,
            org.bukkit.event.inventory.BrewingStandFuelEvent.class,
            org.bukkit.event.inventory.CraftItemEvent.class,
            org.bukkit.event.inventory.FurnaceBurnEvent.class,
            org.bukkit.event.inventory.FurnaceExtractEvent.class,
            org.bukkit.event.inventory.FurnaceSmeltEvent.class,
            org.bukkit.event.inventory.InventoryClickEvent.class,
            org.bukkit.event.inventory.InventoryCloseEvent.class,
            org.bukkit.event.inventory.InventoryCreativeEvent.class,
            org.bukkit.event.inventory.InventoryDragEvent.class,
            org.bukkit.event.inventory.InventoryMoveItemEvent.class,
            org.bukkit.event.inventory.InventoryOpenEvent.class,
            org.bukkit.event.inventory.InventoryPickupItemEvent.class,
            org.bukkit.event.inventory.PrepareAnvilEvent.class,
            org.bukkit.event.inventory.PrepareItemCraftEvent.class,
            org.bukkit.event.inventory.PrepareSmithingEvent.class,
            org.bukkit.event.inventory.SmithItemEvent.class,
            org.bukkit.event.inventory.TradeSelectEvent.class,

            // === Player events ===
            org.bukkit.event.player.AsyncPlayerChatEvent.class,
            org.bukkit.event.player.AsyncPlayerPreLoginEvent.class,
            org.bukkit.event.player.PlayerAdvancementDoneEvent.class,
            org.bukkit.event.player.PlayerAnimationEvent.class,
            org.bukkit.event.player.PlayerArmorStandManipulateEvent.class,
            org.bukkit.event.player.PlayerAttemptPickupItemEvent.class,
            org.bukkit.event.player.PlayerBedEnterEvent.class,
            org.bukkit.event.player.PlayerBedLeaveEvent.class,
            org.bukkit.event.player.PlayerBucketEmptyEvent.class,
            org.bukkit.event.player.PlayerBucketEntityEvent.class,
            org.bukkit.event.player.PlayerBucketFillEvent.class,
            org.bukkit.event.player.PlayerChangedMainHandEvent.class,
            org.bukkit.event.player.PlayerChangedWorldEvent.class,
            org.bukkit.event.player.PlayerCommandPreprocessEvent.class,
            org.bukkit.event.player.PlayerCommandSendEvent.class,
            org.bukkit.event.player.PlayerDropItemEvent.class,
            org.bukkit.event.player.PlayerEditBookEvent.class,
            org.bukkit.event.player.PlayerEggThrowEvent.class,
            org.bukkit.event.player.PlayerExpChangeEvent.class,
            org.bukkit.event.player.PlayerFishEvent.class,
            org.bukkit.event.player.PlayerGameModeChangeEvent.class,
            org.bukkit.event.player.PlayerHarvestBlockEvent.class,
            org.bukkit.event.player.PlayerHideEntityEvent.class,
            org.bukkit.event.player.PlayerInteractAtEntityEvent.class,
            org.bukkit.event.player.PlayerInteractEntityEvent.class,
            org.bukkit.event.player.PlayerInteractEvent.class,
            org.bukkit.event.player.PlayerItemBreakEvent.class,
            org.bukkit.event.player.PlayerItemConsumeEvent.class,
            org.bukkit.event.player.PlayerItemDamageEvent.class,
            org.bukkit.event.player.PlayerItemHeldEvent.class,
            org.bukkit.event.player.PlayerItemMendEvent.class,
            org.bukkit.event.player.PlayerJoinEvent.class,
            org.bukkit.event.player.PlayerKickEvent.class,
            org.bukkit.event.player.PlayerLevelChangeEvent.class,
            org.bukkit.event.player.PlayerLocaleChangeEvent.class,
            org.bukkit.event.player.PlayerLoginEvent.class,
            org.bukkit.event.player.PlayerMoveEvent.class,
            org.bukkit.event.player.PlayerPickupArrowEvent.class,
            org.bukkit.event.player.PlayerPickupItemEvent.class,
            org.bukkit.event.player.PlayerPortalEvent.class,
            org.bukkit.event.player.PlayerQuitEvent.class,
            org.bukkit.event.player.PlayerRecipeDiscoverEvent.class,
            org.bukkit.event.player.PlayerRegisterChannelEvent.class,
            org.bukkit.event.player.PlayerResourcePackStatusEvent.class,
            org.bukkit.event.player.PlayerRespawnEvent.class,
            org.bukkit.event.player.PlayerRiptideEvent.class,
            org.bukkit.event.player.PlayerShearEntityEvent.class,
            org.bukkit.event.player.PlayerShowEntityEvent.class,
            org.bukkit.event.player.PlayerStatisticIncrementEvent.class,
            org.bukkit.event.player.PlayerSwapHandItemsEvent.class,
            org.bukkit.event.player.PlayerTakeLecternBookEvent.class,
            org.bukkit.event.player.PlayerTeleportEvent.class,
            org.bukkit.event.player.PlayerToggleFlightEvent.class,
            org.bukkit.event.player.PlayerToggleSneakEvent.class,
            org.bukkit.event.player.PlayerToggleSprintEvent.class,
            org.bukkit.event.player.PlayerUnleashEntityEvent.class,
            org.bukkit.event.player.PlayerUnregisterChannelEvent.class,
            org.bukkit.event.player.PlayerVelocityEvent.class,

            // === Raid events ===
            org.bukkit.event.raid.RaidFinishEvent.class,
            org.bukkit.event.raid.RaidSpawnWaveEvent.class,
            org.bukkit.event.raid.RaidStopEvent.class,
            org.bukkit.event.raid.RaidTriggerEvent.class,

            // === Server events ===
            org.bukkit.event.server.BroadcastMessageEvent.class,
            org.bukkit.event.server.MapInitializeEvent.class,
            org.bukkit.event.server.PluginDisableEvent.class,
            org.bukkit.event.server.PluginEnableEvent.class,
            org.bukkit.event.server.RemoteServerCommandEvent.class,
            org.bukkit.event.server.ServerCommandEvent.class,
            org.bukkit.event.server.ServerListPingEvent.class,
            org.bukkit.event.server.ServerLoadEvent.class,
            org.bukkit.event.server.ServiceRegisterEvent.class,
            org.bukkit.event.server.ServiceUnregisterEvent.class,
            org.bukkit.event.server.TabCompleteEvent.class,

            // === Vehicle events ===
            org.bukkit.event.vehicle.VehicleBlockCollisionEvent.class,
            org.bukkit.event.vehicle.VehicleCreateEvent.class,
            org.bukkit.event.vehicle.VehicleDamageEvent.class,
            org.bukkit.event.vehicle.VehicleDestroyEvent.class,
            org.bukkit.event.vehicle.VehicleEnterEvent.class,
            org.bukkit.event.vehicle.VehicleEntityCollisionEvent.class,
            org.bukkit.event.vehicle.VehicleExitEvent.class,
            org.bukkit.event.vehicle.VehicleMoveEvent.class,
            org.bukkit.event.vehicle.VehicleUpdateEvent.class,

            // === Weather events ===
            org.bukkit.event.weather.LightningStrikeEvent.class,
            org.bukkit.event.weather.ThunderChangeEvent.class,
            org.bukkit.event.weather.WeatherChangeEvent.class,

            // === World events ===
            org.bukkit.event.world.ChunkLoadEvent.class,
            org.bukkit.event.world.ChunkPopulateEvent.class,
            org.bukkit.event.world.ChunkUnloadEvent.class,
            org.bukkit.event.world.EntitiesLoadEvent.class,
            org.bukkit.event.world.EntitiesUnloadEvent.class,
            org.bukkit.event.world.GenericGameEvent.class,
            org.bukkit.event.world.LootGenerateEvent.class,
            org.bukkit.event.world.PortalCreateEvent.class,
            org.bukkit.event.world.SpawnChangeEvent.class,
            org.bukkit.event.world.StructureGrowEvent.class,
            org.bukkit.event.world.TimeSkipEvent.class,
            org.bukkit.event.world.WorldInitEvent.class,
            org.bukkit.event.world.WorldLoadEvent.class,
            org.bukkit.event.world.WorldSaveEvent.class,
            org.bukkit.event.world.WorldUnloadEvent.class,

            // === Slimefun events ===
            io.github.thebusybiscuit.slimefun4.api.events.AncientAltarCraftEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.AndroidFarmEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.AndroidMineEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.AsyncAutoEnchanterProcessEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.AsyncMachineOperationFinishEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.AsyncProfileLoadEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.AutoDisenchantEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.AutoEnchantEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.BlockPlacerPlaceEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.ClimbingPickLaunchEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.CoolerFeedPlayerEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.GEOResourceGenerationEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.MultiBlockCraftEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.MultiBlockInteractEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.PlayerLanguageChangeEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.PlayerPreResearchEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.ReactorExplodeEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.ResearchUnlockEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockBreakEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockPlaceEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.SlimefunGuideOpenEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemSpawnEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.TalismanActivateEvent.class,
            io.github.thebusybiscuit.slimefun4.api.events.WaypointCreateEvent.class
        );
    }

    /**
     * Registers each class as a host symbol in the JS engine by its simple name.
     * This eliminates JavaPackage.lookupClass() → ClassLoader.loadClass() overhead
     * when scripts use instanceof checks or property access against these types.
     */
    private void registerHostClasses(TruffleLanguage.Env env, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            addThing(clazz.getSimpleName(), env.asHostSymbol(clazz));
        }
    }

    @Override
    public void addThing(String name, Object value) {
        jsEngine.getBindings("js").putMember(name, value);
    }

    @Override
    public String key() {
        return "js";
    }

    private final Map<String, Value> functionCache = new ConcurrentHashMap<>();
    private final Set<String> failedFunctions = ConcurrentHashMap.newKeySet();
    private final Set<String> executableFunctions = ConcurrentHashMap.newKeySet();

    public boolean hasFunction(String funName) {
        return executableFunctions.contains(funName) && !failedFunctions.contains(funName);
    }

    @Nullable @CanIgnoreReturnValue
    @Override
    public Value evalFunction(String funName, Object... args) {
        if (failedFunctions.contains(funName)) {
            return null;
        }

        Value function = functionCache.get(funName);

        if (function == null) {
            Value bindings = jsEngine.getBindings("js");

            if (!bindings.hasMember(funName)) {
                ExceptionHandler.debugLog(() -> "在附属" + addon.getAddonId() + "中加载脚本" + getFile().getName() + "时遇到了问题: " + "不存在函数 " + funName);
                failedFunctions.add(funName);
                return null;
            }

            Value member = bindings.getMember(funName);
            if (!member.canExecute()) {
                ExceptionHandler.debugLog(() -> "在附属" + addon.getAddonId() + "中加载脚本" + getFile().getName() + "时遇到了问题: " + "函数 " + funName + " 不可执行");
                failedFunctions.add(funName);
                return null;
            }

            function = member;
            functionCache.put(funName, function);
        }

        try {
            Value result = function.execute(args);
            if ("init".equals(funName)) {
                cacheExecutableFunctions();
            }
            ExceptionHandler.debugLog(
                    "运行了 " + getAddon().getAddonName() + "的脚本" + getFile().getName() + "中的函数 " + funName);
            return result;
        } catch (IllegalStateException e) {
            if (!e.getMessage().contains("Multi threaded access")) {
                handleExecutionError(e, funName);
            }
        } catch (Throwable e) {
            handleExecutionError(e, funName);
        }
        return null;
    }

    @Override
    public void close() {
        // don't close jsEngine, since we just reload the plugin, not the js engine.
    }

    private void handleExecutionError(Throwable e, String funName) {
        functionCache.remove(funName);

        ExceptionHandler.debugLog("由于开启了 debug 模式，此次脚本运行不会被记录为失败");
        if (!RykenSlimefunCustomizer.INSTANCE.getConfig().getBoolean("debug")) {
            failedFunctions.add(funName);
        }

        ExceptionHandler.handleError(
                "在运行" + getAddon().getAddonName() + "的脚本" + getFile().getName() + "时发生错误", e);
    }

    protected final void contextInit() {
        super.contextInit();
        if (jsEngine != null) {
            try {
                clearScriptCache();

                jsEngine.eval(
                        Source.newBuilder("js", getFileContext(), "JavaScript").build());
                cacheExecutableFunctions();
            } catch (IOException e) {
                ExceptionHandler.handleError(
                        "在加载" + getAddon().getAddonName() + "的脚本" + getFile().getName() + "时发生错误", e);
            }
        }
    }

    private void cacheExecutableFunctions() {
        functionCache.clear();
        executableFunctions.clear();
        Value bindings = jsEngine.getBindings("js");
        for (String memberName : bindings.getMemberKeys()) {
            Value member = bindings.getMember(memberName);
            if (member != null && member.canExecute()) {
                executableFunctions.add(memberName);
                failedFunctions.remove(memberName);
            }
        }
    }
    public void clearScriptCache() {
        failedFunctions.clear();
        functionCache.clear();
            executableFunctions.clear();
    }
}
