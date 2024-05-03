/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.VillagerEvent;
import meteordevelopment.meteorclient.events.entity.VillagerTradesEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.village.TradeOfferList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


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

    private final Setting<String> needEnchant = sgGeneral.add(new StringSetting.Builder()
        .name("enchant")
        .description("The need description for find")
        .defaultValue("")
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> waitBuildTable = sgGeneral.add(new IntSetting.Builder()
        .name("wait-build")
        .description("Wait in ms before the place lectern.")
        .defaultValue(100)
        .build()
    );

    private TradeOfferList offers;

    private final List<MyBlock> blocks = new ArrayList<>();

    private boolean isPlaceLectern = false;

    private BlockPos lecternPos;

    @Override
    public void onActivate() {
        lecternPos = getBlockPos();
    }

    @Override
    public void onDeactivate() {
        lecternPos = null;
        isPlaceLectern = false;
        blocks.clear();
        offers.clear();
    }

    int getMaxEnchantLevel(String enchantName) {
        int idEnchant = enchantmentNameToId(enchantName);
        if (idEnchant == -1) {
            return -1;
        }
        Enchantment enchantment = Enchantment.byRawId(idEnchant);
        assert enchantment != null;
        return enchantment.getMaxLevel();
    }

    @EventHandler
    private void onUpdateVillage(VillagerEvent event) {
        System.out.println("VillagerEvent" + event.status);

        TradeOfferList offers = this.offers;
        System.out.println(
            "offers: " + offers.size() + " " + offers
        );

        AtomicBoolean isFind = new AtomicBoolean(false);

        offers.forEach(offer -> {
            System.out.println(
                offer.getSellItem().getItem()
            );
            if (offer.getSellItem().getItem() != Items.ENCHANTED_BOOK) {
                System.out.println("Not equal item");
                return;
            }

            System.out.println("equal");
            ItemStack book = offer.getSellItem();
            if (book.getNbt() == null) {
                return;
            }
            NbtList tag = book.getNbt().getList("StoredEnchantments", 10);
            System.out.println(
                "tag: " + tag
            );
            String enchant = tag.getCompound(0).getString("id");
            int level = tag.getCompound(0).getInt("lvl");

            System.out.println(
                "enchant: " + enchant + " level: " + level
            );
            if (!needEnchant.get().equals("") && !enchant.equals(needEnchant.get())) {
                System.out.println("not equal enchant");
                return;
            }
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

            if (currentCost < minCost || currentCost > 64) {
                System.out.println("not correct cost");
                return;
            }

            if (currentCost - minCost > maxDiffMaxPrice.get()) {
                System.out.println("too expensive");
                return;
            }

            isFind.set(true);
        });

        if (!isFind.get()) {
            System.out.println("not find");

            updateVillager();
        }
    }

    FindItemResult findBestAxeInHotBar() {
        assert mc.player != null;

        FindItemResult bestAxe = null;
        ItemStack bestAxeStack = null;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                if (bestAxe == null) {
                    bestAxe = new FindItemResult(i, stack.getCount());
                    bestAxeStack = stack;
                } else {
                    if (stack.getDamage() < bestAxeStack.getDamage()) {
                        bestAxe = new FindItemResult(i, stack.getCount());
                    }
                }
            }
        }

        if (bestAxe == null) {
            bestAxe = new FindItemResult(0, mc.player.getInventory().getStack(0).getCount());
        }

        System.out.println("Best axe: " + bestAxe.slot());

        return bestAxe;
    }

    private void updateVillager() {
        mc.player.getInventory().selectedSlot = findBestAxeInHotBar().slot();
        MyBlock block = new MyBlock();
        block.set(lecternPos, Direction.DOWN);

        if (blocks.size() != 0) {
            System.out.println("Blocks is not length 0");
            return;
        }

        blocks.add(block);
    }

    private void onLecternMined() {
        isPlaceLectern = true;
    }

    private void attemptPlaceLectern() {
        if (!isPlaceLectern) {
            return;
        }

        FindItemResult findItemResult = InvUtils.findInHotbar(Items.LECTERN);
        System.out.println(
            "findItemResult: " + findItemResult.slot()
        );
        if (findItemResult.slot() == -1) {
            return;
        }

        isPlaceLectern = false;

        mc.player.getInventory().selectedSlot = findItemResult.slot();

        new Thread(() -> {
            try {
                Thread.sleep(waitBuildTable.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BlockUtils.place(lecternPos, findItemResult, 100);
        }).start();
    }

    private BlockPos getBlockPos() {
        BlockPos playerPos = mc.player.getBlockPos();
        playerPos = playerPos.add(
            addXPos.get(),
            addYPos.get(),
            addZPos.get()
        );
        System.out.println("blockPos: " + playerPos);

        return playerPos;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        int oldBlocksLength = blocks.size();
        blocks.removeIf(MyBlock::shouldRemove);
        int newBlocksLength = blocks.size();

        if (oldBlocksLength != newBlocksLength) {
            onLecternMined();
        }

        if (!blocks.isEmpty()) {
            blocks.get(0).mine();
        }

        attemptPlaceLectern();
    }

    private class MyBlock {
        public BlockPos blockPos;
        public Direction direction;
        public Block originalBlock;
        public boolean mining;

        public void set(BlockPos pos, Direction dir) {
            this.blockPos = pos;
            this.direction = dir;
            this.originalBlock = mc.world.getBlockState(pos).getBlock();
            System.out.println("original " + this.originalBlock.getName());
            this.mining = false;
        }

        public boolean shouldRemove() {
            return mc.world.getBlockState(blockPos).getBlock() != originalBlock || Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, blockPos.getX() + direction.getOffsetX(), blockPos.getY() + direction.getOffsetY(), blockPos.getZ() + direction.getOffsetZ()) > mc.interactionManager.getReachDistance();
        }

        public void mine() {
            if (!mining) {
                mc.player.swingHand(Hand.MAIN_HAND);
                mining = true;
            }
            else updateBlockBreakingProgress();
        }

        private void updateBlockBreakingProgress() {
            BlockUtils.breakBlock(blockPos, swingHand.get());
        }
    }

    @EventHandler
    private void onTradesUpdate(VillagerTradesEvent event) {
        this.offers = event.offers;
        System.out.println("TradeOffersUpdatedEvent " + offers.size());
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
        assert enchantment != null;

        if (enchantment.isTreasure()) {
            minCost *= 2;
        }
        if (minCost > 64) {
            minCost = 64;
        }

        return minCost;
    }
}

class EnchantmentRegistry {
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
