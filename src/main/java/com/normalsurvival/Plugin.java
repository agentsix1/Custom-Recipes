package com.normalsurvival;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin {

    private final Set<NamespacedKey> registeredRecipes = new HashSet<>();
    private final Set<NamespacedKey> registeredSmeltingRecipes = new HashSet<>();
    private final Set<NamespacedKey> registeredBlastingRecipes = new HashSet<>();

    @Override
    public void onEnable() {
        loadRecipes();
        loadSmeltingRecipes();
        loadBlastingRecipes();
    }

    @Override
    public void onDisable() {
        removeRecipes();
        removeSmeltingRecipes();
        removeBlastingRecipes();
    }

    private void loadRecipes() {
        File configFile = new File(getDataFolder(), "crafting.yml");

        // Save default config if not present
        if (!configFile.exists()) {
            saveResource("crafting.yml", false);
        }

        // Load the configuration
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Iterate through the recipe definitions
        Set<String> keys = config.getKeys(false);
        for (String key : keys) {
            ConfigurationSection recipeSection = config.getConfigurationSection(key);
            if (recipeSection != null) {
                addRecipeFromConfig(recipeSection, key);
            }
        }
    }

    private void addRecipeFromConfig(ConfigurationSection section, String key) {
        String outputMaterial = section.getString("result", "Minecraft:STONE").toUpperCase();
        String recipeType = section.getString("type", "SHAPED").toUpperCase();
        Integer amount = section.getInt( "amount" );

        Material output = Material.getMaterial(outputMaterial.split(":")[1]);
        if (output == null) {
            getLogger().warning("Invalid output material: " + outputMaterial);
            return;
        }

        ItemStack result = new ItemStack(output, amount);
        NamespacedKey recipeKey = new NamespacedKey(this, key);

        switch (recipeType) {
            case "SHAPED":
                createShapedRecipe(section, recipeKey, result);
                break;

            case "SHAPELESS":
                createShapelessRecipe(section, recipeKey, result);
                break;

            default:
                getLogger().warning("Unsupported recipe type: " + recipeType);
        }

        // Keep track of the registered recipe
        registeredRecipes.add(recipeKey);
    }

    private void createShapedRecipe(ConfigurationSection section, NamespacedKey key, ItemStack result) {
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        // Set shape
        List<String> shape = section.getStringList("shape");
        recipe.shape(shape.toArray(new String[0]));

        // Set ingredients
        ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
        if (ingredients != null) {
            for (String symbol : ingredients.getKeys(false)) {
                List<String> materials = ingredients.getStringList(symbol);
                RecipeChoice.MaterialChoice choice = new RecipeChoice.MaterialChoice(
                        materials.stream()
                                .map(mat -> Material.getMaterial(mat.split(":")[1].toUpperCase()))
                                .filter(Objects::nonNull)
                                .toList()
                );

                recipe.setIngredient(symbol.charAt(0), choice);
            }
        }

        // Add the recipe
        Bukkit.addRecipe(recipe);
        getLogger().info("Registered shaped recipe: " + key.getKey());
    }

    private void createShapelessRecipe(ConfigurationSection section, NamespacedKey key, ItemStack result) {
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        // Set ingredients
        ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
        if (ingredients != null) {
            for (String ingredientKey : ingredients.getKeys(false)) {
                List<String> materials = ingredients.getStringList(ingredientKey);
                RecipeChoice.MaterialChoice choice = new RecipeChoice.MaterialChoice(
                        materials.stream()
                                .map(mat -> Material.getMaterial(mat.split(":")[1].toUpperCase()))
                                .filter(Objects::nonNull)
                                .toList()
                );

                recipe.addIngredient(choice);
            }
        }

        // Add the recipe
        Bukkit.addRecipe(recipe);
        getLogger().info("Registered shapeless recipe: " + key.getKey());
    }

    private void removeRecipes() {
        for (NamespacedKey key : registeredRecipes) {
            if (Bukkit.removeRecipe(key)) {
                getLogger().info("Removed recipe: " + key.getKey());
            } else {
                getLogger().warning("Failed to remove recipe: " + key.getKey());
            }
        }

        registeredRecipes.clear();
    }

    private void loadSmeltingRecipes() {
      File configFile = new File(getDataFolder(), "smelting.yml");

      // Save default config if not present
      if (!configFile.exists()) {
          saveResource("smelting.yml", false);
      }

      // Load the configuration
      FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

      // Iterate through the recipe definitions
      Set<String> keys = config.getKeys(false);
      for (String key : keys) {
          ConfigurationSection recipeSection = config.getConfigurationSection(key);
          if (recipeSection != null) {
              addSmeltingRecipeFromConfig(recipeSection, key);
          }
      }
  }

  private void addSmeltingRecipeFromConfig(ConfigurationSection section, String key) {
      String recipeType = section.getString("type");
      if ("smelting".equalsIgnoreCase(recipeType)) {
          String ingredientMaterial = section.getString("ingredient");
          String resultMaterial = section.getString("result");
          float experience = section.getInt("experience");
          int cookingTime = section.getInt("cookingtime");
          int amount = section.getInt("amount");

          // Convert materials
          Material ingredient = Material.getMaterial(ingredientMaterial.split(":")[1].toUpperCase());
          Material result = Material.getMaterial(resultMaterial.split(":")[1].toUpperCase());

          if (ingredient == null || result == null) {
              getLogger().warning("Invalid ingredient or result material for smelting recipe: " + key);
              return;
          }

          // Create and register the smelting recipe
          ItemStack resultItem = new ItemStack(result, amount);
          FurnaceRecipe recipe =  new FurnaceRecipe(new NamespacedKey(this, key+1000), resultItem, ingredient, experience, cookingTime );

          // Add the recipe
          getServer().addRecipe(recipe);
          registeredSmeltingRecipes.add(new NamespacedKey(this, key+1000));
          getLogger().info("Registered smelting recipe: " + key+1000);
      }
  }

  private void removeSmeltingRecipes() {
      for (NamespacedKey key : registeredSmeltingRecipes) {
          if (getServer().removeRecipe(key)) {
              getLogger().info("Removed smelting recipe: " + key.getKey());
          } else {
              getLogger().warning("Failed to remove smelting recipe: " + key.getKey());
          }
      }

      // Clear the set of registered recipes after removal
      registeredSmeltingRecipes.clear();
  }
  private void loadBlastingRecipes() {
    File configFile = new File(getDataFolder(), "blasting.yml");

    // Save default config if not present
    if (!configFile.exists()) {
        saveResource("blasting.yml", false);
    }

    // Load the configuration
    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

    // Iterate through the recipe definitions
    Set<String> keys = config.getKeys(false);
    for (String key : keys) {
        ConfigurationSection recipeSection = config.getConfigurationSection(key);
        if (recipeSection != null) {
            addBlastingRecipeFromConfig(recipeSection, key);
        }
    }
}

private void addBlastingRecipeFromConfig(ConfigurationSection section, String key) {
    String recipeType = section.getString("type");
    if ("smelting".equalsIgnoreCase(recipeType)) {
        String ingredientMaterial = section.getString("ingredient");
        String resultMaterial = section.getString("result");
        float experience = section.getInt("experience");
        int cookingTime = section.getInt("cookingtime");
        int amount = section.getInt("amount");

        // Convert materials
        Material ingredient = Material.getMaterial(ingredientMaterial.split(":")[1].toUpperCase());
        Material result = Material.getMaterial(resultMaterial.split(":")[1].toUpperCase());

        if (ingredient == null || result == null) {
            getLogger().warning("Invalid ingredient or result material for blasting recipe: " + key);
            return;
        }

        // Create and register the smelting recipe
        ItemStack resultItem = new ItemStack(result, amount);
        BlastingRecipe recipe =  new BlastingRecipe(new NamespacedKey(this, key+2000), resultItem, ingredient, experience, cookingTime );

        // Add the recipe
        getServer().addRecipe(recipe);
        registeredBlastingRecipes.add(new NamespacedKey(this, key+2000));
        getLogger().info("Registered blasting recipe: " + key+2000);
    }
}

private void removeBlastingRecipes() {
    for (NamespacedKey key : registeredBlastingRecipes) {
        if (getServer().removeRecipe(key)) {
            getLogger().info("Removed blasting recipe: " + key.getKey());
        } else {
            getLogger().warning("Failed to remove blasting recipe: " + key.getKey());
        }
    }

    // Clear the set of registered recipes after removal
    registeredBlastingRecipes.clear();
}
}
