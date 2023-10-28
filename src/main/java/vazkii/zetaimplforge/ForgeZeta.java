package vazkii.zetaimplforge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import vazkii.zeta.event.ZCommonSetup;
import vazkii.zeta.event.ZLivingDeath;
import vazkii.zeta.event.ZLivingTick;
import vazkii.zeta.event.ZLoadComplete;
import vazkii.zeta.event.ZLootTableLoad;
import vazkii.zeta.event.ZPlayNoteBlock;
import vazkii.zeta.event.ZRightClickBlock;
import vazkii.zeta.registry.BrewingRegistry;
import vazkii.zeta.Zeta;
import vazkii.zeta.config.IZetaConfigInternals;
import vazkii.zeta.config.SectionDefinition;
import vazkii.zeta.event.ZRegister;
import vazkii.zeta.event.bus.ZResult;
import vazkii.zeta.network.ZetaNetworkHandler;
import vazkii.zeta.registry.CraftingExtensionsRegistry;
import vazkii.zeta.registry.ZetaRegistry;
import vazkii.zeta.util.ZetaSide;
import vazkii.zetaimplforge.config.ForgeBackedConfig;
import vazkii.zetaimplforge.config.TerribleForgeConfigHackery;
import vazkii.zetaimplforge.event.ForgeZCommonSetup;
import vazkii.zetaimplforge.event.ForgeZLivingDeath;
import vazkii.zetaimplforge.event.ForgeZLivingTick;
import vazkii.zetaimplforge.event.ForgeZLoadComplete;
import vazkii.zetaimplforge.event.ForgeZLootTableLoad;
import vazkii.zetaimplforge.event.ForgeZPlayNoteBlock;
import vazkii.zetaimplforge.event.ForgeZRightClickBlock;
import vazkii.zetaimplforge.network.ForgeZetaNetworkHandler;
import vazkii.zetaimplforge.registry.ForgeBrewingRegistry;
import vazkii.zetaimplforge.registry.ForgeCraftingExtensionsRegistry;
import vazkii.zetaimplforge.registry.ForgeZetaRegistry;

/**
 * ideally do not touch quark from this package, it will later be split off
 */
public class ForgeZeta extends Zeta {
	public ForgeZeta(String modid, Logger log) {
		super(modid, log, ZetaSide.fromClient(FMLEnvironment.dist.isClient()));
	}

	@Override
	public boolean isModLoaded(String modid) {
		return ModList.get().isLoaded(modid);
	}

	@Override
	public @Nullable String getModDisplayName(String modid) {
		return ModList.get().getModContainerById(modid)
			.map(c -> c.getModInfo().getDisplayName())
			.orElse(null);
	}

	@Override
	public IZetaConfigInternals makeConfigInternals(SectionDefinition rootSection) {
		ForgeConfigSpec.Builder bob = new ForgeConfigSpec.Builder();
		ForgeBackedConfig forge = new ForgeBackedConfig(rootSection, bob);
		ForgeConfigSpec spec = bob.build();

		TerribleForgeConfigHackery.registerAndLoadConfigEarlierThanUsual(spec);

		return forge;
	}

	@Override
	public ZetaRegistry createRegistry() {
		return new ForgeZetaRegistry(this);
	}

	@Override
	public CraftingExtensionsRegistry createCraftingExtensionsRegistry() {
		return new ForgeCraftingExtensionsRegistry(this);
	}

	@Override
	public BrewingRegistry createBrewingRegistry() {
		return new ForgeBrewingRegistry(this);
	}

	@Override
	public ZetaNetworkHandler createNetworkHandler(String modid, int protocolVersion) {
		return new ForgeZetaNetworkHandler(modid, protocolVersion);
	}

	@Override
	public boolean fireRightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult bhr) {
		return MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, hand, pos, bhr));
	}

	@Override
	public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
		return stack.canElytraFly(entity);
	}

	@Override
	public void start() {
		IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
		modbus.addListener(EventPriority.HIGHEST, this::registerHighest);
		modbus.addListener(this::commonSetup);
		modbus.addListener(this::loadComplete);

		MinecraftForge.EVENT_BUS.addListener(this::rightClickBlock);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, this::rightClickBlockLow);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::livingDeathLowest);
		MinecraftForge.EVENT_BUS.addListener(this::livingTick);
		MinecraftForge.EVENT_BUS.addListener(this::playNoteBlock);
		MinecraftForge.EVENT_BUS.addListener(this::lootTableLoad);
	}

	boolean registerDone = false;
	public void registerHighest(RegisterEvent e) {
		if(registerDone)
			return;

		loadBus.fire(new ZRegister(this));
		loadBus.fire(new ZRegister.Post());

		registerDone = true;
	}

	public void commonSetup(FMLCommonSetupEvent e) {
		loadBus.fire(new ForgeZCommonSetup(e), ZCommonSetup.class);
	}

	public void loadComplete(FMLLoadCompleteEvent e) {
		loadBus.fire(new ForgeZLoadComplete(e), ZLoadComplete.class);
	}

	public void rightClickBlock(PlayerInteractEvent.RightClickBlock e) {
		playBus.fire(new ForgeZRightClickBlock(e), ZRightClickBlock.class);
	}

	public void rightClickBlockLow(PlayerInteractEvent.RightClickBlock e) {
		playBus.fire(new ForgeZRightClickBlock.Low(e), ZRightClickBlock.Low.class);
	}

	public void livingDeathLowest(LivingDeathEvent e) {
		playBus.fire(new ForgeZLivingDeath.Lowest(e), ZLivingDeath.Lowest.class);
	}

	public void livingTick(LivingEvent.LivingTickEvent e) {
		playBus.fire(new ForgeZLivingTick(e), ZLivingTick.class);
	}

	public void playNoteBlock(NoteBlockEvent.Play e) {
		playBus.fire(new ForgeZPlayNoteBlock(e), ZPlayNoteBlock.class);
	}

	public void lootTableLoad(LootTableLoadEvent e) {
		playBus.fire(new ForgeZLootTableLoad(e), ZLootTableLoad.class);
	}

	public static ZResult from(Event.Result r) {
		return switch(r) {
			case DENY -> ZResult.DENY;
			case DEFAULT -> ZResult.DEFAULT;
			case ALLOW -> ZResult.ALLOW;
		};
	}

	public static Event.Result to(ZResult r) {
		return switch(r) {
			case DENY -> Event.Result.DENY;
			case DEFAULT -> Event.Result.DEFAULT;
			case ALLOW -> Event.Result.ALLOW;
		};
	}
}
