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

    /**
     * FIX: Called AFTER the player takes the result item from the crafting slot.
     *
     * The original bug was likely trying to fill items BEFORE or DURING the take,
     * which caused wheat to pop out instead of the cake.
     *
     * This inject fires AFTER onTakeItem completes, so the grid is already cleared
     * and we can safely place new ingredients.
     */
    @Inject(method = "onTakeItem", at = @At("TAIL"))
    private void afterTakeCake(PlayerEntity player, ItemStack takenStack, CallbackInfo ci) {
        // Only do anything if the player took a Cake
        if (!takenStack.isOf(Items.CAKE)) return;

        // Only if auto-mode is enabled for this player
        if (player.getWorld().isClient()) return; // server side only
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) return;
        if (!CakeAutoState.isAutoEnabled(serverPlayer)) return;

        // Make sure the open screen handler is actually a crafting one (3x3)
        if (!(player.currentScreenHandler instanceof CraftingScreenHandler handler)) return;

        // Recipe layout for cake (3x3 grid, slots 1-9 in CraftingScreenHandler):
        // [Milk ][Milk ][Milk ]
        // [Sugar][Egg  ][Sugar]
        // [Wheat][Wheat][Wheat]
        //
        // In CraftingScreenHandler the crafting slots are indices 1..9
        // index 0 is the result slot.
        int[] milkSlots  = {1, 2, 3};
        int[] sugarSlots = {4, 6};
        int   eggSlot    = 5;
        int[] wheatSlots = {7, 8, 9};

        var inv = player.getInventory();

        // Helper: find item in player inventory, return slot index or -1
        java.util.function.Function<net.minecraft.item.Item, Integer> findItem = item -> {
            for (int i = 0; i < inv.size(); i++) {
                if (inv.getStack(i).isOf(item)) return i;
            }
            return -1;
        };

        // ── Place ingredients into crafting grid ──────────────────────────────
        // We iterate over each required slot and try to place ONE item.
        // We collect what we need first (3 milk, 2 sugar, 1 egg, 3 wheat).

        // Count available items in inventory (excluding hotbar/armour — player.getInventory covers all)
        int milkCount  = countItem(inv, Items.MILK_BUCKET);
        int sugarCount = countItem(inv, Items.SUGAR);
        int eggCount   = countItem(inv, Items.EGG);
        int wheatCount = countItem(inv, Items.WHEAT);

        if (milkCount < 3 || sugarCount < 2 || eggCount < 1 || wheatCount < 3) {
            // Not enough ingredients — show action bar message
            serverPlayer.sendMessage(
                    net.minecraft.text.Text.literal(
                            "§eНедостаточно ингредиентов для торта! " +
                            "(молоко:" + milkCount + "/3, сахар:" + sugarCount +
                            "/2, яйцо:" + eggCount + "/1, пшеница:" + wheatCount + "/3)"),
                    true);
            return;
        }

        // Place items. We use CraftingScreenHandler slots directly.
        // handler.slots.get(i) gives the slot; we simulate inserting from inventory.
        fillSlots(handler, inv, milkSlots, Items.MILK_BUCKET);
        fillSlots(handler, inv, sugarSlots, Items.SUGAR);
        fillSlot (handler, inv, eggSlot,    Items.EGG);
        fillSlots(handler, inv, wheatSlots, Items.WHEAT);

        // Sync the crafting result to the client
        handler.sendContentUpdates();
    }

    /** Count how many of a given item the player has in their inventory. */
    private int countItem(net.minecraft.entity.player.PlayerInventory inv, net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(item)) count += inv.getStack(i).getCount();
        }
        return count;
    }

    /** Move one item from player inventory into a set of crafting grid slots. */
    private void fillSlots(CraftingScreenHandler handler,
                           net.minecraft.entity.player.PlayerInventory inv,
                           int[] craftSlotIndices,
                           net.minecraft.item.Item item) {
        for (int craftIdx : craftSlotIndices) {
            fillSlot(handler, inv, craftIdx, item);
        }
    }

    /** Move one item from player inventory into a single crafting grid slot. */
    private void fillSlot(CraftingScreenHandler handler,
                          net.minecraft.entity.player.PlayerInventory inv,
                          int craftIdx,
                          net.minecraft.item.Item item) {
        Slot slot = handler.slots.get(craftIdx);
        // Only fill if the slot is empty
        if (!slot.getStack().isEmpty()) return;

        // Find a stack in the player's inventory
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) {
                // Take exactly 1 item
                ItemStack single = stack.copyWithCount(1);
                stack.decrement(1);
                if (stack.isEmpty()) inv.setStack(i, ItemStack.EMPTY);
                slot.setStack(single);
                return;
            }
        }
    }
}
