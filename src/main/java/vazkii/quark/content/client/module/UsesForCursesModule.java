package vazkii.quark.content.client.module;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import vazkii.zeta.module.ZetaLoadModule;
import vazkii.zeta.module.ZetaModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.base.module.hint.Hint;
import vazkii.quark.content.tweaks.client.layer.ArmorStandFakePlayerLayer;
import vazkii.zeta.event.ZConfigChanged;
import vazkii.zeta.event.bus.LoadEvent;
import vazkii.zeta.client.event.ZAddModelLayers;

@ZetaLoadModule(category = "client")
public class UsesForCursesModule extends ZetaModule {

	private static final ResourceLocation PUMPKIN_OVERLAY = new ResourceLocation("textures/misc/pumpkinblur.png");

	public static boolean staticEnabled;

	@Config(flag = "use_for_vanishing")
	public static boolean vanishPumpkinOverlay = true;

	@Config(flag = "use_for_binding")
	public static boolean bindArmorStandsWithPlayerHeads = true;
	
	@Hint(key = "use_for_vanishing", value = "use_for_vanishing")
	Item pumpkin = Items.CARVED_PUMPKIN;
	
	@Hint(key = "use_for_binding", value = "use_for_binding")
	List<Item> bindingItems = Arrays.asList(Items.ARMOR_STAND, Items.PLAYER_HEAD);

	@LoadEvent
	public final void configChanged(ZConfigChanged event) {
		staticEnabled = enabled;
	}

	public static boolean shouldHideArmorStandModel(ItemStack stack) {
		if(!staticEnabled || !bindArmorStandsWithPlayerHeads || !stack.is(Items.PLAYER_HEAD))
			return false;
		return EnchantmentHelper.hasBindingCurse(stack);
	}

	public static boolean shouldHidePumpkinOverlay(ResourceLocation location, Player player) {
		if(!staticEnabled || !vanishPumpkinOverlay || !location.equals(PUMPKIN_OVERLAY))
			return false;
		ItemStack stack = player.getInventory().getArmor(3);
		return stack.is(Blocks.CARVED_PUMPKIN.asItem()) &&
			EnchantmentHelper.hasVanishingCurse(stack);
	}

	@ZetaLoadModule(clientReplacement = true)
	public static class Client extends UsesForCursesModule {

		@LoadEvent
		public void modelLayers(ZAddModelLayers event) {
			ArmorStandRenderer render = event.getRenderer(EntityType.ARMOR_STAND);
			render.addLayer(new ArmorStandFakePlayerLayer<>(render, event.getEntityModels()));
		}

	}

}
