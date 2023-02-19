/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.TradeOfferList;

public class VillagerEvent {
    private static final VillagerEvent INSTANCE = new VillagerEvent();

    public byte status;
    public VillagerEntity villager;
    public TradeOfferList offers;

    public static VillagerEvent get(byte status, VillagerEntity villager, TradeOfferList offers) {
        return INSTANCE;
    }

    public static VillagerEvent get(byte status, VillagerEntity villager) {
        return INSTANCE;
    }
}
