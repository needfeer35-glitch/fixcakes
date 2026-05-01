package com.cakeauto.mixin;

import com.cakeauto.CakeAutoState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotMixin {

    @Shadow private PlayerEntity player;

    @Inject(method = "onTakeItem", at = @At("TAIL"))
    private void afterTakeCake(PlayerEntity player, ItemStack takenStack, CallbackInfo ci) {
        if (!takenStack.isOf(Items.CAKE)) return;
        if (player.getWorld().isClient()) return;
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) return;
        if (!CakeAutoState.isAutoEnabled(serverPlayer)) return;
        if (!(player.currentScreenHandler instanceof CraftingScreenHandler handler)) return;

        var inv = player.getInventory();

        int milkCount  = countItem(inv, Items.MILK_BUCKET);
        int sugarCount = countItem(inv, Items.SUGAR);
        int eggCount   = countItem(inv, Items.EGG);
        int wheatCount = countItem(inv, Items.WHEAT);

        if (milkCount < 3 || sugarCount < 2 || eggCount < 1 || wheatCount < 3) {
            serverPlayer.sendMessage(
                    net.minecraft.text.Text.literal("§eНедостаточно ингредиентов!"), true);
            return;
        }

        fillSlots(handler, inv, new int[]{1, 2, 3}, Items.MILK_BUCKET);
        fillSlots(handler, inv, new int[]{4, 6},    Items.SUGAR);
        fillSlot (handler, inv, 5,                  Items.EGG);
        fillSlots(handler, inv, new int[]{7, 8, 9}, Items.WHEAT);

        handler.sendContentUpdates();
    }

    private int countItem(net.minecraft.entity.player.PlayerInventory inv, net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < inv.size(); i++)
            if (inv.getStack(i).isOf(item)) count += inv.getStack(i).getCount();
        return count;
    }

    private void fillSlots(CraftingScreenHandler handler,
                           net.minecraft.entity.player.PlayerInventory inv,
                           int[] indices, net.minecraft.item.Item item) {
        for (int i : indices) fillSlot(handler, inv, i, item);
    }

    private void fillSlot(CraftingScreenHandler handler,
                          net.minecraft.entity.player.PlayerInventory inv,
                          int craftIdx, net.minecraft.item.Item item) {
        Slot slot = handler.slots.get(craftIdx);
        if (!slot.getStack().isEmpty()) return;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) {
                ItemStack single = stack.copyWithCount(1);
                stack.decrement(1);
                if (stack.isEmpty()) inv.setStack(i, ItemStack.EMPTY);
                slot.setStack(single);
                return;
            }
        }
    }
}
