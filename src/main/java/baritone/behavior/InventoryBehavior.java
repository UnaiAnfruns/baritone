/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.behavior;

import baritone.Baritone;
import baritone.api.event.events.TickEvent;
import baritone.utils.ToolSet;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.NonNullList;

public class InventoryBehavior extends Behavior {
    public InventoryBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void onTick(TickEvent event) {
        if (!Baritone.settings().allowInventory.get()) {
            return;
        }
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        if (ctx.player().openContainer != ctx.player().inventoryContainer) {
            // we have a crafting table or a chest or something open
            return;
        }
        if (firstValidThrowaway() >= 9) { // aka there are none on the hotbar, but there are some in main inventory
            swapWithHotBar(firstValidThrowaway(), 8);
        }
        int pick = bestToolAgainst(Blocks.STONE, ItemPickaxe.class);
        if (pick >= 9) {
            swapWithHotBar(pick, 0);
        }
    }

    private void swapWithHotBar(int inInventory, int inHotbar) {
        ctx.playerController().windowClick(ctx.player().inventoryContainer.windowId, inInventory < 9 ? inInventory + 36 : inInventory, inHotbar, ClickType.SWAP, ctx.player());
    }

    private int firstValidThrowaway() { // TODO offhand idk
        NonNullList<ItemStack> invy = ctx.player().inventory.mainInventory;
        for (int i = 0; i < invy.size(); i++) {
            if (Baritone.settings().acceptableThrowawayItems.get().contains(invy.get(i).getItem())) {
                return i;
            }
        }
        return -1;
    }

    private int bestToolAgainst(Block against, Class<? extends ItemTool> klass) {
        NonNullList<ItemStack> invy = ctx.player().inventory.mainInventory;
        int bestInd = -1;
        double bestSpeed = -1;
        for (int i = 0; i < invy.size(); i++) {
            ItemStack stack = invy.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (klass.isInstance(stack.getItem())) {
                double speed = ToolSet.calculateStrVsBlock(stack, against.getDefaultState()); // takes into account enchants
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestInd = i;
                }
            }
        }
        return bestInd;
    }

    public boolean hasGenericThrowaway() {
        for (Item item : Baritone.settings().acceptableThrowawayItems.get()) {
            if (throwaway(false, item)) {
                return true;
            }
        }
        return false;
    }

    public boolean selectThrowawayForLocation(int x, int y, int z) {
        Item maybe = baritone.getBuilderProcess().placeAt(x, y, z);
        if (maybe != null && throwaway(true, maybe)) {
            return true; // gotem
        }
        for (Item item : Baritone.settings().acceptableThrowawayItems.get()) {
            if (throwaway(true, item)) {
                return true;
            }
        }
        return false;
    }

    private boolean throwaway(boolean select, Item desired) {
        EntityPlayerSP p = ctx.player();
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            // this usage of settings() is okay because it's only called once during pathing
            // (while creating the CalculationContext at the very beginning)
            // and then it's called during execution
            // since this function is never called during cost calculation, we don't need to migrate
            // acceptableThrowawayItems to the CalculationContext
            if (desired.equals(item.getItem())) {
                if (select) {
                    p.inventory.currentItem = i;
                }
                return true;
            }
        }
        if (desired.equals(p.inventory.offHandInventory.get(0).getItem())) {
            // main hand takes precedence over off hand
            // that means that if we have block A selected in main hand and block B in off hand, right clicking places block B
            // we've already checked above ^ and the main hand can't possible have an acceptablethrowawayitem
            // so we need to select in the main hand something that doesn't right click
            // so not a shovel, not a hoe, not a block, etc
            for (byte i = 0; i < 9; i++) {
                ItemStack item = inv.get(i);
                if (item.isEmpty() || item.getItem() instanceof ItemPickaxe) {
                    if (select) {
                        p.inventory.currentItem = i;
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
