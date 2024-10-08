package net.joseph.vaultfilters.mixin.compat.create;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.joseph.vaultfilters.VFTests;
import net.joseph.vaultfilters.VaultFilters;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FilteringBehaviour.class, remap = false)
public abstract class MixinCreateFilteringBehaviourLegacy extends BlockEntityBehaviour {
    @Shadow
    private ItemStack filter;

    protected MixinCreateFilteringBehaviourLegacy(SmartBlockEntity be) {
        super(be);
    }

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    public void checkFilter(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!this.isActive() || this.filter.isEmpty() || VFTests.checkFilter(stack,this.filter, true, this.blockEntity.getLevel()));
    }

    @Shadow
    public abstract boolean isActive();
}
