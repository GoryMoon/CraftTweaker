package minetweaker.mc1102.recipes;

import minetweaker.api.item.IItemStack;
import minetweaker.api.player.IPlayer;
import minetweaker.api.recipes.ICraftingInventory;
import minetweaker.mc1102.player.MCPlayer;
import minetweaker.mc1102.util.MineTweakerHacks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;

import java.util.*;

import static minetweaker.api.minecraft.MineTweakerMC.*;

/**
 * @author Stan
 */
public class MCCraftingInventory implements ICraftingInventory {

    private final IInventory inventory;
    private final IPlayer player;
    private final EntityPlayer playerOrig;
    private int width;
    private int height;
    private IItemStack[] stacks;
    private ItemStack[] original;
    private int stackCount;

    private MCCraftingInventory(InventoryCrafting inventory) {
        this.inventory = inventory;
        width = height = (int) Math.sqrt(inventory.getSizeInventory());
        stacks = new IItemStack[width * height];
        original = new ItemStack[stacks.length];
        stackCount = 0;
        update();

        Container container = MineTweakerHacks.getCraftingContainer(inventory);
        if(container != null) {
            List<Slot> slots = container.inventorySlots;
            if(!slots.isEmpty() && slots.get(0) instanceof SlotCrafting) {
                SlotCrafting slotCrafting = (SlotCrafting) slots.get(0);
                playerOrig = MineTweakerHacks.getCraftingSlotPlayer(slotCrafting);
                player = getIPlayer(playerOrig);
            } else {
                playerOrig = null;
                player = null;
            }
        } else {
            playerOrig = null;
            player = null;
        }
    }

    public MCCraftingInventory(IInventory inventory, EntityPlayer player) {
        this.inventory = inventory;
        width = height = (int) Math.sqrt(inventory.getSizeInventory());
        stacks = new IItemStack[width * height];
        original = new ItemStack[stacks.length];
        stackCount = 0;
        update();

        playerOrig = player;
        this.player = player == null ? null : new MCPlayer(player);
    }

    public static MCCraftingInventory get(InventoryCrafting inventory) {
            MCCraftingInventory result = new MCCraftingInventory(inventory);
            return result;
    }

    public static MCCraftingInventory get(IInventory inventory, EntityPlayer player) {
            MCCraftingInventory result = new MCCraftingInventory(inventory, player);
            return result;
    }

    private void update() {
        if(inventory.getSizeInventory() != original.length) {
            width = height = (int) Math.sqrt(inventory.getSizeInventory());
            stacks = new IItemStack[inventory.getSizeInventory()];
            original = new ItemStack[stacks.length];
            stackCount = 0;
        }

        for(int i = 0; i < inventory.getSizeInventory(); i++) {
            if(changed(i)) {
                // System.out.println("Slot " + i + " changed");
                original[i] = inventory.getStackInSlot(i);
                if(inventory.getStackInSlot(i) != null) {
                    if(stacks[i] == null)
                        stackCount++;
                    stacks[i] = getIItemStack(original[i]);
                } else {
                    if(stacks[i] != null)
                        stackCount--;
                    stacks[i] = null;
                }
            }
        }
        // System.out.println("Num stack count: " + stackCount);
    }

    @Override
    public IPlayer getPlayer() {
        return player;
    }

    @Override
    public int getSize() {
        return width * height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getStackCount() {
        return stackCount;
    }

    @Override
    public IItemStack getStack(int i) {
        return stacks[i];
    }

    @Override
    public IItemStack getStack(int x, int y) {
        return stacks[y * width + x];
    }

    @Override
    public void setStack(int x, int y, IItemStack stack) {
        // System.out.println("SetStack(" + x + ", " + y + ") " + stack);

        int ix = y * width + x;
        if(stack != stacks[ix]) {
            if(stack == null) {
                stackCount--;
                inventory.setInventorySlotContents(ix, null);
            } else {
                inventory.setInventorySlotContents(ix, getItemStack(stack));

                if(stacks[ix] == null) {
                    stackCount++;
                }
            }
            stacks[ix] = stack;
        }
    }

    @Override
    public void setStack(int i, IItemStack stack) {
        // System.out.println("SetStack(" + i + ") " + stack);

        if(stack != stacks[i]) {
            if(stack == null) {
                stackCount--;
                inventory.setInventorySlotContents(i, null);
            } else {
                inventory.setInventorySlotContents(i, getItemStack(stack));

                if(stacks[i] == null) {
                    stackCount++;
                }
            }
            stacks[i] = stack;
        }
    }

    private boolean changed(int i) {
        return original[i] != inventory.getStackInSlot(i) || original[i] != null && stacks[i].getAmount() != original[i].stackSize;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MCCraftingInventory{");
        sb.append("inventory=").append(inventory);
        sb.append(", player=").append(player);
        sb.append(", playerOrig=").append(playerOrig);
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", stacks=").append(Arrays.toString(stacks));
        sb.append(", original=").append(Arrays.toString(original));
        sb.append(", stackCount=").append(stackCount);
        sb.append('}');
        return sb.toString();
    }
}
