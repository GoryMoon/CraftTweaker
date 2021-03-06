package minetweaker.mc1102.recipes;

import minetweaker.api.recipes.*;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import static minetweaker.api.minecraft.MineTweakerMC.getItemStack;

/**
 * @author Stan
 */
public class ShapelessRecipeOre extends ShapelessOreRecipe implements IMTRecipe {

    private final ShapelessRecipe recipe;

    public ShapelessRecipeOre(Object[] ingredients, ShapelessRecipe recipe) {
        super(getItemStack(recipe.getOutput()), ingredients);

        this.recipe = recipe;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return recipe.matches(MCCraftingInventory.get(inventory));
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        return getItemStack(recipe.getCraftingResult(MCCraftingInventory.get(inventory))).copy();
    }
    
    @Override
    public ICraftingRecipe getRecipe() {
        return recipe;
    }
}
