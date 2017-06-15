package minetweaker.mc1120.recipes;

import minetweaker.*;
import minetweaker.api.item.*;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.player.IPlayer;
import minetweaker.api.recipes.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.registry.*;
import net.minecraftforge.oredict.*;
import stanhebben.zenscript.annotations.Optional;

import java.util.*;

import static minetweaker.api.minecraft.MineTweakerMC.*;

/**
 * @author Stan
 */
public class MCRecipeManager implements IRecipeManager {
    
   private static Set<Map.Entry<ResourceLocation, IRecipe>> recipes;
    private static List<ICraftingRecipe> transformerRecipes;
    
    public MCRecipeManager() {
        //TODO change this back
        recipes = GameData.getRecipeRegistry().getEntries();
        transformerRecipes = new ArrayList<>();
    }
    
    private static boolean matches(Object input, IIngredient ingredient) {
        if((input == null) != (ingredient == null)) {
            return false;
        } else if(ingredient != null) {
            if(input instanceof ItemStack) {
                if(((ItemStack) input).isEmpty()) {
                    return false;
                }
                if(!ingredient.matches(getIItemStack((ItemStack) input))) {
                    return false;
                }
            } else if(input instanceof String) {
                if(!ingredient.contains(getOreDict((String) input))) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public boolean hasTransformerRecipes() {
        return transformerRecipes.size() > 0;
    }
    
    public void applyTransformations(ICraftingInventory inventory, IPlayer byPlayer) {
        for(ICraftingRecipe recipe : transformerRecipes) {
            if(recipe.matches(inventory)) {
                recipe.applyTransformers(inventory, byPlayer);
                return;
            }
        }
    }
    
    @Override
    public List<ICraftingRecipe> getRecipesFor(IIngredient ingredient) {
        List<ICraftingRecipe> results = new ArrayList<>();
    
        recipes.stream().filter(recipe -> ingredient.matches(MineTweakerMC.getIItemStack(recipe.getValue().getRecipeOutput()))).forEach(recipe -> {
            ICraftingRecipe converted = RecipeConverter.toCraftingRecipe(recipe.getValue());
            results.add(converted);
        });
        
        return results;
    }
    
    @Override
    public List<ICraftingRecipe> getAll() {
        List<ICraftingRecipe> results = new ArrayList<>();
        for(Map.Entry<ResourceLocation, IRecipe> recipe : recipes) {
            ICraftingRecipe converted = RecipeConverter.toCraftingRecipe(recipe.getValue());
            results.add(converted);
        }
        
        return results;
    }
    
    @Override
    public int remove(IIngredient output, @Optional boolean nbtMatch) {
        List<IRecipe> toRemove = new ArrayList<>();
        List<Integer> removeIndex = new ArrayList<>();
        for(Map.Entry<ResourceLocation, IRecipe> recipe : recipes) {
            
            // certain special recipes have no predefined output. ignore those
            // since these cannot be removed with MineTweaker scripts
            int counter = 0;
            if(!recipe.getValue().getRecipeOutput().isEmpty()) {
                if(nbtMatch ? output.matchesExact(getIItemStack(recipe.getValue().getRecipeOutput())) : output.matches(getIItemStack(recipe.getValue().getRecipeOutput()))) {
                    toRemove.add(recipe.getValue());
                    removeIndex.add(counter);
                }
            }
        }
        
        MineTweakerAPI.apply(new ActionRemoveRecipes(toRemove, removeIndex));
        return toRemove.size();
    }
    
    @Override
    public void addShaped(String name,IItemStack output, IIngredient[][] ingredients, @Optional IRecipeFunction function, @Optional IRecipeAction action) {
        addShaped(name, output, ingredients, function, action, false);
    }
    
    @Override
    public void addShapedMirrored(String name,IItemStack output, IIngredient[][] ingredients, @Optional IRecipeFunction function, @Optional IRecipeAction action) {
        addShaped(name, output, ingredients, function, action, true);
    }
    
    @Override
    public void addShapeless(String name, IItemStack output, IIngredient[] ingredients, @Optional IRecipeFunction function, @Optional IRecipeAction action) {
        ShapelessRecipe recipe = new ShapelessRecipe(name, output, ingredients, function, action);
        IRecipe irecipe = RecipeConverter.convert(recipe);
        MineTweakerAPI.apply(new ActionAddRecipe(irecipe, recipe));
    }
    
    @Override
    public int removeShaped(IIngredient output, IIngredient[][] ingredients) {
        int ingredientsWidth = 0;
        int ingredientsHeight = 0;
        
        if(ingredients != null) {
            ingredientsHeight = ingredients.length;
            
            for(int i = 0; i < ingredients.length; i++) {
                ingredientsWidth = Math.max(ingredientsWidth, ingredients[i].length);
            }
        }
        
        List<IRecipe> toRemove = new ArrayList<>();
        List<Integer> removeIndex = new ArrayList<>();
        outer:
        for(Map.Entry<ResourceLocation, IRecipe> recipe : recipes) {
            if(recipe.getValue().getRecipeOutput().isEmpty() || !output.matches(getIItemStack(recipe.getValue().getRecipeOutput()))) {
                continue;
            }
            
            if(ingredients != null) {
                if(recipe instanceof ShapedRecipes) {
                    ShapedRecipes srecipe = (ShapedRecipes) recipe;
                    if(ingredientsWidth != srecipe.recipeWidth || ingredientsHeight != srecipe.recipeHeight) {
                        continue;
                    }
                    
                    for(int j = 0; j < ingredientsHeight; j++) {
                        IIngredient[] row = ingredients[j];
                        for(int k = 0; k < ingredientsWidth; k++) {
                            IIngredient ingredient = k > row.length ? null : row[k];
                            ItemStack recipeIngredient = srecipe.getIngredients().get(j * srecipe.recipeWidth + k).getMatchingStacks()[0];
                            
                            if(!matches(recipeIngredient, ingredient)) {
                                continue outer;
                            }
                        }
                    }
                } else if(recipe instanceof ShapedOreRecipe) {
                    ShapedOreRecipe srecipe = (ShapedOreRecipe) recipe;
                    int recipeWidth = srecipe.getWidth();
                    int recipeHeight = srecipe.getHeight();
                    if(ingredientsWidth != recipeWidth || ingredientsHeight != recipeHeight) {
                        continue;
                    }
                    
                    for(int j = 0; j < ingredientsHeight; j++) {
                        IIngredient[] row = ingredients[j];
                        for(int k = 0; k < ingredientsWidth; k++) {
                            IIngredient ingredient = k > row.length ? null : row[k];
                            Object input = srecipe.getIngredients().get(j * recipeWidth + k).getMatchingStacks()[0];
                            if(!matches(input, ingredient)) {
                                continue outer;
                            }
                        }
                    }
                } else {
                    if(recipe instanceof ShapelessRecipes) {
                        continue;
                    } else if(recipe instanceof ShapelessOreRecipe) {
                        continue;
                    }
                }
            } else {
                if(recipe instanceof ShapelessRecipes) {
                    continue;
                } else if(recipe instanceof ShapelessOreRecipe) {
                    continue;
                }
            }
            
            toRemove.add(recipe.getValue());
    //TODO fix this
            //            removeIndex.add(i);
        }
        
        MineTweakerAPI.apply(new ActionRemoveRecipes(toRemove, removeIndex));
        return toRemove.size();
    }
    
    @Override
    public int removeShapeless(IIngredient output, IIngredient[] ingredients, boolean wildcard) {
        List<IRecipe> toRemove = new ArrayList<>();
        List<Integer> removeIndex = new ArrayList<>();
        outer:
        for(Map.Entry<ResourceLocation, IRecipe> recipe : recipes) {
            
            if(recipe.getValue().getRecipeOutput().isEmpty() || !output.matches(getIItemStack(recipe.getValue().getRecipeOutput()))) {
                continue;
            }
            
            if(ingredients != null) {
                if(recipe instanceof ShapelessRecipes) {
                    ShapelessRecipes srecipe = (ShapelessRecipes) recipe;
                    
                    if(ingredients.length > srecipe.getIngredients().size()) {
                        continue;
                    } else if(!wildcard && ingredients.length < srecipe.getIngredients().size()) {
                        continue;
                    }
                    
                    checkIngredient:
                    for(int j = 0; j < ingredients.length; j++) {
                        for(int k = 0; k < srecipe.getIngredients().size(); k++) {
                            if(matches(srecipe.recipeItems.get(k), ingredients[j])) {
                                continue checkIngredient;
                            }
                        }
                        
                        continue outer;
                    }
                } else if(recipe instanceof ShapelessOreRecipe) {
                    ShapelessOreRecipe srecipe = (ShapelessOreRecipe) recipe;
                    NonNullList<Ingredient> inputs = srecipe.getIngredients();
                    
                    if(inputs.size() < ingredients.length) {
                        continue;
                    }
                    if(!wildcard && inputs.size() > ingredients.length) {
                        continue;
                    }
                    
                    checkIngredient:
                    for(int j = 0; j < ingredients.length; j++) {
                        for(int k = 0; k < srecipe.getIngredients().size(); k++) {
                            if(matches(inputs.get(k), ingredients[j])) {
                                continue checkIngredient;
                            }
                        }
                        
                        continue outer;
                    }
                }
                if(recipe instanceof ShapedRecipes) {
                    continue;
                } else if(recipe instanceof ShapedOreRecipe) {
                    continue;
                }
            } else {
                if(recipe instanceof ShapedRecipes) {
                    continue;
                } else if(recipe instanceof ShapedOreRecipe) {
                    continue;
                }
            }
            toRemove.add(recipe.getValue());
            //TODO fix this
//            removeIndex.add(i);
        }
        
        MineTweakerAPI.apply(new ActionRemoveRecipes(toRemove, removeIndex));
        return toRemove.size();
    }
    
    @Override
    public IItemStack craft(IItemStack[][] contents) {
        Container container = new ContainerVirtual();
        
        int width = 0;
        int height = contents.length;
        for(IItemStack[] row : contents) {
            width = Math.max(width, row.length);
        }
        
        ItemStack[] iContents = new ItemStack[width * height];
        for(int i = 0; i < height; i++) {
            for(int j = 0; j < contents[i].length; j++) {
                if(contents[i][j] != null) {
                    iContents[i * width + j] = getItemStack(contents[i][j]);
                }
            }
        }
        
        InventoryCrafting inventory = new InventoryCrafting(container, width, height);
        for(int i = 0; i < iContents.length; i++) {
            inventory.setInventorySlotContents(i, iContents[i]);
        }
        ItemStack result = CraftingManager.findMatchingResult(inventory, null);
        if(result.isEmpty()) {
            return null;
        } else {
            return getIItemStack(result);
        }
    }
    
    private void addShaped(String name, IItemStack output, IIngredient[][] ingredients, IRecipeFunction function, IRecipeAction action, boolean mirrored) {
        ShapedRecipe recipe = new ShapedRecipe(name, output, ingredients, function, action, mirrored);
        IRecipe irecipe = RecipeConverter.convert(recipe);
        MineTweakerAPI.apply(new ActionAddRecipe(irecipe, recipe));
    }
    
    private class ContainerVirtual extends Container {
        
        @Override
        public boolean canInteractWith(EntityPlayer var1) {
            return false;
        }
    }
    
    public static class ActionRemoveRecipes implements IUndoableAction {
        
        private final List<Integer> removingIndices;
        private final List<IRecipe> removingRecipes;
        
        public ActionRemoveRecipes(List<IRecipe> recipes, List<Integer> indices) {
            this.removingIndices = indices;
            this.removingRecipes = recipes;
        }
        
        @Override
        public void apply() {
            for(int i = removingIndices.size() - 1; i >= 0; i--) {
                recipes.remove((int) removingIndices.get(i));
                if(removingRecipes.get(i) != null)
                    MineTweakerAPI.getIjeiRecipeRegistry().removeRecipe(removingRecipes.get(i), "minecraft.crafting");
            }
        }
        
        @Override
        public boolean canUndo() {
            return true;
        }
        
        @Override
        public void undo() {
            for(int i = 0; i < removingIndices.size(); i++) {
                int index = Math.min(recipes.size(), removingIndices.get(i));
                GameRegistry.register(removingRecipes.get(i), removingRecipes.get(i).getRegistryName());
                if(removingRecipes.get(i) != null)
                    MineTweakerAPI.getIjeiRecipeRegistry().addRecipe(removingRecipes.get(i), "minecraft.crafting");
            }
        }
        
        @Override
        public String describe() {
            return "Removing " + removingIndices.size() + " recipes";
        }
        
        @Override
        public String describeUndo() {
            return "Restoring " + removingIndices.size() + " recipes";
        }
        
        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
    
    private class ActionAddRecipe implements IUndoableAction {
        
        private final IRecipe recipe;
        private final ICraftingRecipe craftingRecipe;
        
        public ActionAddRecipe(IRecipe recipe, ICraftingRecipe craftingRecipe) {
            this.recipe = recipe;
            this.craftingRecipe = craftingRecipe;
        }
        
        @Override
        public void apply() {
            GameRegistry.register(recipe, new ResourceLocation("crafttweaker", craftingRecipe.getName()));
            if(craftingRecipe.hasTransformers()) {
                transformerRecipes.add(craftingRecipe);
            }
            if(recipe != null)
                MineTweakerAPI.getIjeiRecipeRegistry().addRecipe(recipe, "minecraft.crafting");
        }
        
        @Override
        public boolean canUndo() {
            return true;
        }
        
        @Override
        public void undo() {
            recipes.remove(recipe);
            if(craftingRecipe.hasTransformers()) {
                transformerRecipes.remove(craftingRecipe);
            }
            if(recipe != null)
                MineTweakerAPI.getIjeiRecipeRegistry().removeRecipe(recipe, "minecraft.crafting");
        }
        
        @Override
        public String describe() {
            return "Adding recipe for " + recipe.getRecipeOutput().getDisplayName();
        }
        
        @Override
        public String describeUndo() {
            return "Removing recipe for " + recipe.getRecipeOutput().getDisplayName();
        }
        
        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}