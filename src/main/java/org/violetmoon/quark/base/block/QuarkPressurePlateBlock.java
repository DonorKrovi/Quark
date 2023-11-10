package org.violetmoon.quark.base.block;

import java.util.function.BooleanSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.violetmoon.zeta.module.ZetaModule;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.PressurePlateBlock;

/**
 * @author WireSegal
 * Created at 9:41 PM on 10/8/19.
 */
public class QuarkPressurePlateBlock extends PressurePlateBlock implements IQuarkBlock {

	private final ZetaModule module;
	private BooleanSupplier enabledSupplier = () -> true;

	public QuarkPressurePlateBlock(Sensitivity sensitivity, String regname, ZetaModule module, CreativeModeTab creativeTab, Properties properties) {
		super(sensitivity, properties);
		this.module = module;

		module.zeta.registry.registerBlock(this, regname, true);
		module.zeta.registry.setCreativeTab(this, creativeTab);
	}

	@Override
	public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> items) {
		if(isEnabled() || group == CreativeModeTab.TAB_SEARCH)
			super.fillItemCategory(group, items);
	}

	@Override
	public QuarkPressurePlateBlock setCondition(BooleanSupplier enabledSupplier) {
		this.enabledSupplier = enabledSupplier;
		return this;
	}

	@Override
	public boolean doesConditionApply() {
		return enabledSupplier.getAsBoolean();
	}

	@Nullable
	@Override
	public ZetaModule getModule() {
		return module;
	}

}
