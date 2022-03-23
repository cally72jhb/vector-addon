package cally72jhb.addon.mixin;

import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.recipe.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(RecipeResultCollection.class)
public interface RecipeResultCollectionAccessor {
    @Accessor("recipes")
    List<Recipe<?>> getRecipes();
}
