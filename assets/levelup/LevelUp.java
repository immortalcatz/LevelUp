package assets.levelup;

import java.util.*;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = "levelup", name = "LevelUp!", useMetadata = true, guiFactory = "assets.levelup.ConfigLevelUp")
public class LevelUp {
	@Instance(value = "levelup")
	public static LevelUp instance;
	@SidedProxy(clientSide = "assets.levelup.SkillClientProxy", serverSide = "assets.levelup.SkillProxy")
	public static SkillProxy proxy;
	private static Item xpTalisman;
	private static Map<Item, Integer> towItems;
    private static List<Item>[] tiers;
    private static Configuration config;
	public static boolean allowHUD, renderTopLeft, renderExpBar;
    private static boolean bonusMiningXP = true, bonusCraftingXP = true, bonusFightingXP = true, oreMiningXP = true;
    public static FMLEventChannel initChannel, skillChannel, classChannel;

	@EventHandler
	public void load(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new BowEventHandler());
		MinecraftForge.EVENT_BUS.register(new FightEventHandler());
		NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
        SkillPacketHandler sk = new SkillPacketHandler();
        initChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(SkillPacketHandler.CHAN[0]);
        initChannel.register(sk);
        classChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(SkillPacketHandler.CHAN[1]);
        classChannel.register(sk);
        skillChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(SkillPacketHandler.CHAN[2]);
        skillChannel.register(sk);
		proxy.registerGui();
	}

	@EventHandler
	public void load(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		allowHUD = config.get("HUD", "allow HUD", true, "If anything should be rendered on screen at all").setRequiresMcRestart(true).getBoolean();
		renderTopLeft = config.get("HUD", "render HUD on Top Left", true).getBoolean();
		renderExpBar = config.get("HUD", "render HUD on Exp Bar", true).getBoolean();
        boolean talismanEnabled = config.get("Items", "Enable Talisman", true, "Enable item and related recipes").getBoolean();
        boolean bookEnabled = config.get("Items", "Enable Unlearning Book", true, "Enable item and related recipe").getBoolean();
        if(bookEnabled)
            ItemRespecBook.resClass = config.get("Cheats", "unlearning Book Reset Class", false).getBoolean();
        boolean legacyRecipes = config.get("Items", "Enable Recipes", true, "Enable legacy pumpkin and flint recipes").getBoolean();
        int option = config.get("Cheats", "Max points per skill", 50, "Minimum is 1").getInt(50);
        ClassBonus.maxSkillPoints = option > 0 ? option : 50;
        option = config.get("Cheats", "Xp gain per level", 3, "Minimum is 0").getInt(3);
		PlayerEventHandler.xpPerLevel = option >= 0 ? option : 3;
        PlayerEventHandler.oldSpeedDigging = config.get("Cheats", "Use old speed for dirt and gravel digging", true).getBoolean();
        PlayerEventHandler.oldSpeedRedstone = config.get("Cheats", "Use old speed for redstone breaking", true, "Makes the redstone mining efficient").getBoolean();
        PlayerEventHandler.resetSkillOnDeath = config.get("Cheats", "Reset player skill points on death", false, "Do the player death remove the skill points ?").getBoolean();
        PlayerEventHandler.resetClassOnDeath = config.get("Cheats", "Reset player class on death", false, "Do the player lose the class he choose on death ?").getBoolean();
        bonusCraftingXP = config.get("Cheats", "Add Bonus XP on Craft", bonusCraftingXP, "This is a bonus related to a few classes").getBoolean();
        bonusMiningXP = config.get("Cheats", "Add Bonus XP on Mining", bonusMiningXP, "This is a bonus related to a few classes").getBoolean();
        boolean tierCraftingXP = config.get("Cheats", "Add XP on Crafting some items", true, "This is a global bonus, limited to a few craftable items").getBoolean();
        oreMiningXP = config.get("Cheats", "Add XP on Mining some ore", oreMiningXP, "This is a global bonus, limited to a few ores").getBoolean();
        bonusFightingXP = config.get("Cheats", "Add Bonus XP on Fighting", bonusFightingXP, "This is a bonus related to a few classes").getBoolean();
		List<String> blackList = Arrays.asList(config.getStringList("Crops for farming", "BlackList", new String[]{""}, "That won't be affect by farming growth skill, uses internal block name"));
        PlayerEventHandler.addCropsToBlackList(blackList);
        if (config.hasChanged())
			config.save();
        if(tierCraftingXP) {
            List<Item> ingrTier1, ingrTier2, ingrTier3, ingrTier4;
            ingrTier1 = Arrays.asList(Items.stick, Items.leather, Item.getItemFromBlock(Blocks.stone));
            ingrTier2 = Arrays.asList(Items.iron_ingot, Items.gold_ingot, Items.paper, Items.slime_ball);
            ingrTier3 = Arrays.asList(Items.redstone, Items.glowstone_dust, Items.ender_pearl);
            ingrTier4 = Arrays.asList(Items.diamond);
            tiers = new List[]{ingrTier1, ingrTier2, ingrTier3, ingrTier4};
        }
        if(talismanEnabled) {
            towItems = new HashMap<Item, Integer>();
            towItems.put(Item.getItemFromBlock(Blocks.log), 2);
            towItems.put(Items.coal, 2);
            towItems.put(Items.brick, 4);
            towItems.put(Items.book, 4);
            towItems.put(Item.getItemFromBlock(Blocks.iron_ore), 8);
            towItems.put(Items.dye, 8);
            towItems.put(Items.redstone, 8);
            towItems.put(Items.bread, 10);
            towItems.put(Items.melon, 10);
            towItems.put(Item.getItemFromBlock(Blocks.pumpkin), 10);
            towItems.put(Items.cooked_porkchop, 12);
            towItems.put(Items.cooked_beef, 12);
            towItems.put(Items.cooked_chicken, 12);
            towItems.put(Items.cooked_fished, 12);
            towItems.put(Items.iron_ingot, 16);
            towItems.put(Item.getItemFromBlock(Blocks.gold_ore), 20);
            towItems.put(Items.gold_ingot, 24);
            towItems.put(Items.diamond, 40);
            xpTalisman = new Item().setUnlocalizedName("xpTalisman").setTextureName("levelup:XPTalisman").setCreativeTab(CreativeTabs.tabTools);
            GameRegistry.registerItem(xpTalisman, "Talisman of Wonder");
            GameRegistry.addRecipe(new ShapedOreRecipe(xpTalisman, "GG ", " R ", " GG", 'G', "ingotGold", 'R', "dustRedstone"));
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Items.coal);
            GameRegistry.addRecipe(new ShapelessOreRecipe(xpTalisman, xpTalisman, "oreGold"));
            GameRegistry.addRecipe(new ShapelessOreRecipe(xpTalisman, xpTalisman, "oreIron"));
            GameRegistry.addRecipe(new ShapelessOreRecipe(xpTalisman, xpTalisman, "gemDiamond"));
            GameRegistry.addRecipe(new ShapelessOreRecipe(xpTalisman, xpTalisman, "logWood"));
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Items.brick);
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Items.book);
            GameRegistry.addRecipe(new ShapelessOreRecipe(xpTalisman, xpTalisman, "gemLapis"));
            GameRegistry.addRecipe(new ShapelessOreRecipe(xpTalisman, xpTalisman, "dustRedstone"));
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Items.bread);
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Items.melon);
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Items.cooked_porkchop);
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Items.cooked_beef);
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Items.cooked_chicken);
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Items.cooked_fished);
            GameRegistry.addRecipe(new ShapelessOreRecipe(xpTalisman, xpTalisman, "ingotIron"));
            GameRegistry.addRecipe(new ShapelessOreRecipe(xpTalisman, xpTalisman, "ingotGold"));
            GameRegistry.addShapelessRecipe(new ItemStack(xpTalisman), xpTalisman, Blocks.pumpkin);
        }
        if(bookEnabled) {
            Item respecBook = new ItemRespecBook().setUnlocalizedName("respecBook").setTextureName("levelup:RespecBook").setCreativeTab(CreativeTabs.tabTools);
            GameRegistry.registerItem(respecBook, "Book of Unlearning");
            GameRegistry.addRecipe(new ItemStack(respecBook, 1), "OEO", "DBD", "ODO", 'O', Blocks.obsidian, 'D', new ItemStack(Items.dye, 1, 0),
                    'E', Items.ender_pearl, 'B', Items.book);
        }
        if(legacyRecipes) {
            GameRegistry.addShapelessRecipe(new ItemStack(Items.pumpkin_seeds, 4), Blocks.pumpkin);
            GameRegistry.addRecipe(new ItemStack(Blocks.gravel, 4), "##", "##", '#', Items.flint);
        }

        if(event.getSourceFile().getName().endsWith(".jar") && event.getSide().isClient()){
            try {
                Class.forName("mods.mud.ModUpdateDetector").getDeclaredMethod("registerMod", ModContainer.class, String.class, String.class).invoke(null,
                        FMLCommonHandler.instance().findContainerFor(this),
                        "https://raw.github.com/GotoLink/LevelUp/master/update.xml",
                        "https://raw.github.com/GotoLink/LevelUp/master/changelog.md"
                );
            } catch (Throwable ignored) {
            }
        }

        PlayerEventHandler handler = new PlayerEventHandler();
        FMLCommonHandler.instance().bus().register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
	}

    public static void refreshValues(boolean[] values){
        LevelUp.allowHUD = values[0];
        LevelUp.renderTopLeft = values[1];
        LevelUp.renderExpBar = values[2];
        config.get("HUD", "allow HUD", true).set(values[0]);
        config.get("HUD", "render HUD on Top Left", true).set(values[1]);
        config.get("HUD", "render HUD on Exp Bar", true).set(values[2]);
        config.save();
    }

    public static void giveBonusFightingXP(EntityPlayer player) {
        if(bonusFightingXP) {
            byte pClass = PlayerExtendedProperties.getPlayerClass(player);
            if (pClass == 2 || pClass == 5 || pClass == 8 || pClass == 11) {
                player.addExperience(2);
            }
        }
    }

	public static void giveBonusCraftingXP(EntityPlayer player) {
        if(bonusCraftingXP) {
            byte pClass = PlayerExtendedProperties.getPlayerClass(player);
            if (pClass == 3 || pClass == 6 || pClass == 9 || pClass == 12) {
                runBonusCounting(player, 1);
            }
        }
	}

	public static void giveBonusMiningXP(EntityPlayer player) {
        if(bonusMiningXP) {
            byte pClass = PlayerExtendedProperties.getPlayerClass(player);
            if (pClass == 1 || pClass == 4 || pClass == 7 || pClass == 10) {
                runBonusCounting(player, 0);
            }
        }
	}

    private static void runBonusCounting(EntityPlayer player, int type){
        Map<String, int[]> counters = PlayerExtendedProperties.getCounterMap(player);
        int[] bonus = counters.get(PlayerExtendedProperties.counters[2]);
        if (bonus == null || bonus.length == 0) {
            bonus = new int[]{0, 0, 0};
        }
        if (bonus[type] < 4) {
            bonus[type]++;
        } else {
            bonus[type] = 0;
            player.addExperience(2);
        }
        counters.put(PlayerExtendedProperties.counters[2], bonus);
    }

	public static void giveCraftingXP(EntityPlayer player, ItemStack itemstack) {
        if(tiers!=null)
            for (int i = 0; i < tiers.length; i++) {
                if (tiers[i].contains(itemstack.getItem())) {
                    incrementCraftCounter(player, i);
                }
            }
	}

	private static void incrementCraftCounter(EntityPlayer player, int i) {
		Map<String, int[]> counters = PlayerExtendedProperties.getCounterMap(player);
		int[] craft = counters.get(PlayerExtendedProperties.counters[1]);
		if (craft.length <= i) {
			int[] craftnew = new int[i + 1];
			System.arraycopy(craft, 0, craftnew, 0, craft.length);
			counters.put(PlayerExtendedProperties.counters[1], craftnew);
			craft = craftnew;
		}
		craft[i]++;
		float f = (float) Math.pow(2D, 3 - i);
		boolean flag;
		for (flag = false; f <= craft[i]; f += 0.5F) {
			player.addExperience(1);
			flag = true;
		}
		if (flag) {
			craft[i] = 0;
		}
		counters.put(PlayerExtendedProperties.counters[1], craft);
	}

	public static void incrementOreCounter(EntityPlayer player, int i) {
        if(oreMiningXP) {
            Map<String, int[]> counters = PlayerExtendedProperties.getCounterMap(player);
            int[] ore = counters.get(PlayerExtendedProperties.counters[0]);
            if (ore.length <= i) {
                int[] orenew = new int[i + 1];
                System.arraycopy(ore, 0, orenew, 0, ore.length);
                counters.put(PlayerExtendedProperties.counters[0], orenew);
                ore = orenew;
            }
            ore[i]++;
            float f = (float) Math.pow(2D, 3 - i) / 2.0F;
            boolean flag;
            for (flag = false; f <= ore[i]; f += 0.5F) {
                player.addExperience(1);
                flag = true;
            }
            if (flag) {
                ore[i] = 0;
            }
            counters.put(PlayerExtendedProperties.counters[0], ore);
        }
        giveBonusMiningXP(player);
	}

	public static boolean isTalismanRecipe(IInventory iinventory) {
        if(xpTalisman!=null)
            for (int i = 0; i < iinventory.getSizeInventory(); i++) {
                if (iinventory.getStackInSlot(i) != null && iinventory.getStackInSlot(i).getItem() == xpTalisman) {
                    return true;
                }
            }
		return false;
	}

	public static void takenFromCrafting(EntityPlayer player, ItemStack itemstack, IInventory iinventory) {
		if (isTalismanRecipe(iinventory)) {
			for (int i = 0; i < iinventory.getSizeInventory(); i++) {
				ItemStack itemstack1 = iinventory.getStackInSlot(i);
				if (itemstack1 != null) {
					if (towItems.containsKey(itemstack1.getItem())) {
						player.addExperience((int) Math.floor(itemstack1.stackSize * towItems.get(itemstack1.getItem()) / 4D));
						iinventory.getStackInSlot(i).stackSize = 0;
					}
				}
			}
		} else {
			for (int j = 0; j < iinventory.getSizeInventory(); j++) {
				ItemStack itemstack2 = iinventory.getStackInSlot(j);
				if (itemstack2 != null && !isUncraftable(itemstack.getItem())) {
					giveCraftingXP(player, itemstack2);
					giveBonusCraftingXP(player);
				}
			}
		}
	}

    public static boolean isUncraftable(Item item){
        return item == Item.getItemFromBlock(Blocks.hay_block) || item == Item.getItemFromBlock(Blocks.gold_block) || item == Item.getItemFromBlock(Blocks.iron_block) || item == Item.getItemFromBlock(Blocks.diamond_block);
    }
}
