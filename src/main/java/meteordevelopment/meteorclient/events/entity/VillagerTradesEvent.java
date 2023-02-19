/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity;

import net.minecraft.village.TradeOfferList;

public class VillagerTradesEvent {
    private static final VillagerTradesEvent INSTANCE = new VillagerTradesEvent();

    public TradeOfferList offers;

    public static VillagerTradesEvent get(TradeOfferList offers) {
        INSTANCE.offers = offers;
        return INSTANCE;
    }
}
