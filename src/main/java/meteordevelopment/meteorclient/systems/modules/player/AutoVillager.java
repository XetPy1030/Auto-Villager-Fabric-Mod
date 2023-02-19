/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.VillagerEvent;
import meteordevelopment.meteorclient.events.entity.VillagerTradesEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOfferList;


public class AutoVillager extends Module {
    public AutoVillager() {
        super(Categories.Player, "auto-villager", "Automatically create trades with villagers.");
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> isRequiredMaxLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("required-max-level")
        .description("Whether the enchantment must be at max level.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxDiffMaxPrice = sgGeneral.add(new IntSetting.Builder()
        .name("max-diff-max-price")
        .description("The maximum difference between the maximum price and the current price.")
        .defaultValue(10)
        .build()
    );

    // block pos
    private final Setting<Integer> addXPos = sgGeneral.add(new IntSetting.Builder()
        .name("add-x-pos")
        .description("The X position to add to the block pos.")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> addYPos = sgGeneral.add(new IntSetting.Builder()
        .name("add-y-pos")
        .description("The Y position to add to the block pos.")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> addZPos = sgGeneral.add(new IntSetting.Builder()
        .name("add-z-pos")
        .description("The Z position to add to the block pos.")
        .defaultValue(0)
        .build()
    );

    private TradeOfferList offers;

    static class EnchantmentRegistry {

        public static EnchantmentRegistry getInstance() {
            return new EnchantmentRegistry();
        }

        public static int getEnchantmentId(Enchantment enchantment) {
            Identifier enchantId = Registries.ENCHANTMENT.getId(enchantment);
            return enchantId == null ? -1 : Registries.ENCHANTMENT.getRawId(enchantment);
        }

        Enchantment get(String name) {
            return Registries.ENCHANTMENT.get(new Identifier(name));
        }
    }

    int enchantmentNameToId(String name) {
        EnchantmentRegistry registry = EnchantmentRegistry.getInstance();
        Enchantment enchantment = registry.get(name);
        if (enchantment == null) {
            return -1; // Enchantment not found
        }
        return EnchantmentRegistry.getEnchantmentId(enchantment);
    }

    int getMinCostEnchantment(String enchantName, int enchantmentLevel) {
        int idEnchant = enchantmentNameToId(enchantName);
        if (idEnchant == -1) {
            return -1;
        }
        Enchantment enchantment = Enchantment.byRawId(idEnchant);

        int minCost = 2;
        minCost += enchantmentLevel * 3;
        if (enchantment.isTreasure()) {
            minCost *= 2;
        }
        if (minCost > 64) {
            minCost = 64;
        }

        return minCost;
    }

    int getMaxEnchantLevel(String enchantName) {
        int idEnchant = enchantmentNameToId(enchantName);
        if (idEnchant == -1) {
            return -1;
        }
        Enchantment enchantment = Enchantment.byRawId(idEnchant);
        return enchantment.getMaxLevel();
    }

    @EventHandler
    private void onUpdateVillage(VillagerEvent event) {
        System.out.println("VillagerEvent" + event.status);

        TradeOfferList offers = this.offers;
        System.out.println(
            "offers: " + offers.size() + " " + offers
        );

//        mc.interactionManager.interactEntity(mc.player, event.villager, Hand.MAIN_HAND);

        offers.forEach(offer -> {
            System.out.println(
                offer.getSellItem().getItem()
            );
            if (offer.getSellItem().getItem() == Items.ENCHANTED_BOOK) {
                System.out.println("equal");
                ItemStack book = offer.getSellItem();
                NbtList tag = book.getNbt().getList("StoredEnchantments", 10);
                System.out.println(
                    "tag: " + tag
                );
                String enchant = tag.getCompound(0).getString("id");
                int level = tag.getCompound(0).getInt("lvl");

                System.out.println(
                    "enchant: " + enchant + " level: " + level
                );
                System.out.println(
                    "max: " + getMaxEnchantLevel(enchant)
                );

                if (isRequiredMaxLevel.get()) {
                    if (getMaxEnchantLevel(enchant) != level) {
                        System.out.println("not max level");
                        return;
                    }
                }

                int currentCost = offer.getAdjustedFirstBuyItem().getCount();
                int minCost = getMinCostEnchantment(enchant, level);

                System.out.println("enchant: " + enchant + " level: " + level + " currentCost: " + currentCost + " minCost: " + minCost);

                if (
                    currentCost < minCost
                    || currentCost > 64
                ) {
                    System.out.println("not correct cost");
                    return;
                }

                if (currentCost - minCost > maxDiffMaxPrice.get()) {
                    System.out.println("too expensive");

                    BlockPos blockPos = mc.player.getBlockPos();
                    blockPos = blockPos.add(
                        addXPos.get(),
                        addYPos.get(),
                        addZPos.get()
                    );
                    BlockUtils.breakBlock(blockPos, true);

                    FindItemResult findItemResult = InvUtils.findInHotbar(Items.BOOKSHELF);
                    BlockUtils.place(blockPos, findItemResult, 100);

                    return;
                }
            }
        });
        // get enchantment by name
//        Enchantment enchantment = Enchantment.byRawId(16);
//        enchantment.isTreasure();
//
//        mc.interactionManager.interactEntity(mc.player, event.villager, Hand.MAIN_HAND);

//        BlockPos blockPos = mc.player.getBlockPos();
//        blockPos = blockPos.add(0, -1, 0);
//        BlockUtils.breakBlock(blockPos, true);
    }

    @EventHandler
    private void onTradesUpdate(VillagerTradesEvent event) {
        this.offers = event.offers;
        System.out.println("TradeOffersUpdatedEvent " + offers.size());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof VillagerEntity) {
                VillagerEntity villager = (VillagerEntity) entity;
            }
//            if (entity instanceof VillagerEntity) {
//                // если животное поблизости - взять его в руки
//                mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
//                // если в руках есть книга
//                if (mc.player.getMainHandStack().getItem() == Items.ENCHANTED_BOOK) {
//                    // если вторая рука пуста
//                    if (mc.player.getOffHandStack().isEmpty()) {
//                        // взять вторую руку
//                        mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
//                    }
//                    // если вторая рука не пуста
//                    else {
//                        // выбросить вторую руку
//                        mc.interactionManager.dropItem(mc.player.getOffHandStack(), false);
//                        // взять вторую руку
//                        mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
//                    }
//                }
//                // если в руках нет книги
//                else {
//                    // выбросить вторую руку
//                    mc.interactionManager.dropItem(mc.player.getOffHandStack(), false);
//                    // выбросить первую руку
//                    mc.interactionManager.dropItem(mc.player.getMainHandStack(), false);
//                }
//            }
        }

        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler) {
            MerchantScreenHandler villagerScreenHandler = (MerchantScreenHandler) mc.player.currentScreenHandler;
            villagerScreenHandler.getRecipes().forEach(recipe -> {
                if (
                    recipe.getSellItem().getCount() == 1 &&
                    recipe.getSecondBuyItem().getCount() == 1 &&
                        recipe.getSellItem().getName().getString().equals("Чародейская книга")
                ) {
                    ItemStack enchantedBook = recipe.getSellItem();
                    NbtList enchantments = enchantedBook.getNbt().getList("StoredEnchantments", 10);
                    enchantments.forEach(enchantment -> {
                        NbtCompound enchantmentCompound = (NbtCompound) enchantment;
                        String enchantmentName = enchantmentCompound.getString("id");
                        int enchantmentLevel = enchantmentCompound.getInt("lvl");

//                        System.out.println(enchantmentName + " " + enchantmentLevel);
                    });
                }
            });
        }
    }
}
