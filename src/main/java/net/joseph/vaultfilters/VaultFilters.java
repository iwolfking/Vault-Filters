package net.joseph.vaultfilters;

import com.mojang.logging.LogUtils;
import net.joseph.vaultfilters.attributes.abstracts.Objects.Modifier;
import net.joseph.vaultfilters.attributes.affix.*;
import net.joseph.vaultfilters.attributes.card.*;
import net.joseph.vaultfilters.attributes.catalysts.CatalystHasModifierAttribute;
import net.joseph.vaultfilters.attributes.catalysts.CatalystModifierCategoryAttribute;
import net.joseph.vaultfilters.attributes.catalysts.CatalystSizeAttribute;
import net.joseph.vaultfilters.attributes.charm.*;
import net.joseph.vaultfilters.attributes.inscription.*;
import net.joseph.vaultfilters.attributes.jewel.*;
import net.joseph.vaultfilters.attributes.gear.*;
import net.joseph.vaultfilters.attributes.old.InscriptionCompletionAttribute;
import net.joseph.vaultfilters.attributes.old.InscriptionInstabilityAttribute;
import net.joseph.vaultfilters.attributes.old.InscriptionTimeAttribute;
import net.joseph.vaultfilters.attributes.other.*;
import net.joseph.vaultfilters.attributes.soul.*;
import net.joseph.vaultfilters.attributes.tool.ToolMaterialAttribute;
import net.joseph.vaultfilters.attributes.trinket.*;
import net.joseph.vaultfilters.configs.VFServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

//import com.simibubi.create.content.logistics.filter.FilterItemStack;


@Mod(VaultFilters.MOD_ID)
public class VaultFilters {
    public static final String MOD_ID = "vaultfilters";
    public static final int CHECK_FILTER_FLAG = 456;
    public static final int NO_CACHE_FLAG = 457;
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Set<UUID> PLAYERS_WITH_VAULT_FILTERS = new HashSet<>();
    public static boolean serverHasVaultFilters = false;
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public VaultFilters() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::setup);
        //eventBus.addListener(this::onWorldLoad);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static Level LEVEL_REF;


    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        MinecraftServer server = event.getWorld().getServer();
        if (server != null) {
            LEVEL_REF = server.getLevel(Level.OVERWORLD);
        }
    }

    private static int ticks = 0;
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ticks++;
            if (ticks >= 60*20) { //60 seconds * 20 tps
                ticks = 0;
                //VaultFilters.LOGGER.info("cleared");
                VFCache.cacheMap.forEach((key, value) -> {
                    if (value.TTK == 0) {
                        VFCache.cacheMap.remove(key);
                    }
                    value.TTK--;
                });
            }
        }
    }

    private void setup(FMLCommonSetupEvent event) {
        // Mod Detection System
        CHANNEL.messageBuilder(ModPresenceMessage.class, 0)
                .encoder(ModPresenceMessage::encoder)
                .decoder(ModPresenceMessage::decoder)
                .consumer(ModPresenceMessage::consumer)
                .add();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, VFServerConfig.SPEC, "vaultfilters-server.toml");
        // This has a specific order as this controls the order displayed in the filters
        new ItemNameAttribute(("Vault Helmet")).register(ItemNameAttribute::new);
        // Soul Attributes
        new HasSoulValueAttribute(true).register(HasSoulValueAttribute::new);
        new ExactSoulAttribute(0).register(ExactSoulAttribute::new);
        new AtleastSoulAttribute(0).register(AtleastSoulAttribute::new);
        // Common
        new ItemTypeAttribute("Jewel").register(ItemTypeAttribute::new);
        new IsUnidentifiedAttribute(true).register(IsUnidentifiedAttribute::new);
        // Rarities
        new GearRarityAttribute("Omega").register(GearRarityAttribute::new);
        new JewelRarityAttribute("Chipped").register(JewelRarityAttribute::new);
        new CharmRarityAttribute("Regal").register(CharmRarityAttribute::new);
        // Inscription Attributes
        new InscriptionSizeAttribute(10).register(InscriptionSizeAttribute::new);
        new InscriptionRoomTypeAttribute("Challenge").register(InscriptionRoomTypeAttribute::new);
        new InscriptionRoomAttribute("Wild West").register(InscriptionRoomAttribute::new);

        // Old (register anyway for data fixing purposes)
        new InscriptionTimeAttribute(0).register(InscriptionTimeAttribute::new);
        new InscriptionCompletionAttribute(0).register(InscriptionCompletionAttribute::new);
        new InscriptionInstabilityAttribute(0D).register(InscriptionInstabilityAttribute::new);
        // Gear
        new GearLevelAttribute(0).register(GearLevelAttribute::new);
        new GearTransmogAttribute("Sword_4").register(GearTransmogAttribute::new);
        new GearRepairSlotAttribute(0).register(GearRepairSlotAttribute::new);
        // Jewels
        new JewelSizeAttribute(0).register(JewelSizeAttribute::new);
        new JewelCutsAttribute(0).register(JewelCutsAttribute::new);
        //Tools
        new ToolMaterialAttribute("Chromatic Steel").register(ToolMaterialAttribute::new);
        // Affixes
        new HasLegendaryAttribute(true).register(HasLegendaryAttribute::new);
        new LegendaryPrefixAttribute("Attack Damage").register(LegendaryPrefixAttribute::new);
        new LegendarySuffixAttribute("Trap Disarm").register(LegendarySuffixAttribute::new);

        new ImplicitAttribute("Armor").register(ImplicitAttribute::new);
        new PrefixAttribute("Durability").register(PrefixAttribute::new);
        new SuffixAttribute("Poison Cloud").register(SuffixAttribute::new);

        new NumberImplicitAttribute("", "", 0).register(NumberImplicitAttribute::new);
        new NumberPrefixAttribute("", "", 0).register(NumberPrefixAttribute::new);
        new NumberSuffixAttribute("", "", 0).register(NumberSuffixAttribute::new);

        new ModifierGroupAttribute("ModAbility").register(ModifierGroupAttribute::new);

        // Cards
        new CardPackOpenedAttribute(true).register(CardPackOpenedAttribute::new);
        new CardPackTypeAttribute("Stat").register(CardPackTypeAttribute::new);
        new CardAtleastTierAttribute(2).register(CardAtleastTierAttribute::new);
        new CardColorAttribute("Red").register(CardColorAttribute::new);
        new CardTypeAttribute("Foil").register(CardTypeAttribute::new);
        new CardUpgradableAttribute(true).register(CardUpgradableAttribute::new);
        new CardModifierAttribute("Lucky Hit").register(CardModifierAttribute::new);
        new CardIsScalingAttribute(true).register(CardIsScalingAttribute::new);
        new CardHasConditionAttribute(true).register(CardHasConditionAttribute::new);
        new CardScaleTypesAttribute("Diagonal").register(CardScaleTypesAttribute::new);
        new CardConditionCompAttribute("At Least").register(CardConditionCompAttribute::new);
        new CardConditionGroupsAttribute("Blue").register(CardConditionGroupsAttribute::new);
        new CardConditionNumAttribute(5).register(CardConditionNumAttribute::new);
        new CardTaskAttribute("Wooden Chests").register(CardTaskAttribute::new);
        new CardTaskNumberAttribute(5).register(CardTaskNumberAttribute::new);
        new CardModifierNumberAttribute(new Modifier("+1 Attack Damage", "Attack Damage",1)).register(CardModifierNumberAttribute::new);
        // Charms
        new CharmUsesAttribute(0).register(CharmUsesAttribute::new);
        new CharmAffinityAttribute(0).register(CharmAffinityAttribute::new);
        new CharmGodAttribute("").register(CharmGodAttribute::new);
        // Trinkets
        new TrinketUsesAttribute(0).register(TrinketUsesAttribute::new);
        new TrinketNameAttribute("").register(TrinketNameAttribute::new);
        new TrinketColorAttribute("").register(TrinketColorAttribute::new);

        // Catalysts
        new CatalystSizeAttribute(10).register(CatalystSizeAttribute::new);
        new CatalystHasModifierAttribute("Ornate").register(CatalystHasModifierAttribute::new);
        new CatalystModifierCategoryAttribute("Bonus Chests").register(CatalystModifierCategoryAttribute::new);


    }

    @OnlyIn(Dist.CLIENT) @SubscribeEvent
    public static void onClientConnect(ClientPlayerNetworkEvent.LoggedInEvent event) {
        CHANNEL.sendToServer(new ModPresenceMessage());
    }

    @OnlyIn(Dist.DEDICATED_SERVER) @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ModPresenceMessage());
        }
    }
}
