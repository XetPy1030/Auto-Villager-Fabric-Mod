/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.VillagerEvent;
import meteordevelopment.meteorclient.events.entity.VillagerTradesEvent;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VillagerEntity.class)
public abstract class VillagerMixin  {
    @Inject(method = "handleStatus", at = @At("HEAD"), cancellable = true)
    private void onHandleStatus(byte status, CallbackInfo info) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        System.out.println(
            "VillagerMixin.onHandleStatus: " + villager.getOffers().size()
        );
        System.out.println(
            "VillagerMixin.onHandleStatus0: " + status
        );
        if (status == (byte) 14) {
            System.out.println(
                "VillagerMixin.onHandleStatus1: " + villager.getOffers().size()
            );
            TradeOfferList offers = villager.getOffers();
            System.out.println(
                "VillagerMixin.onHandleStatus2: " + offers.size()
            );
        }
        MeteorClient.EVENT_BUS.post(VillagerEvent.get(status, villager));
    }

    @Inject(method = "setOffers", at = @At("TAIL"), cancellable = true)
    private void onSetOffers(TradeOfferList offers, CallbackInfo info) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
//        MeteorClient.EVENT_BUS.post(VillagerEvent.get(offers));
    }

    @Inject(method = "setVillagerData", at = @At("TAIL"), cancellable = true)
    private void onSetVillagerData(CallbackInfo info) {
        System.out.println(
            "VillagerMixin.onSetVillagerData: " + ((VillagerEntity) (Object) this).getOffers().size());
        MeteorClient.EVENT_BUS.post(VillagerTradesEvent.get(((VillagerEntity) (Object) this).getOffers()));
    }

    @Inject(method = "setCustomer", at = @At("TAIL"), cancellable = true)
    private void onSetCustomer(CallbackInfo info) {
        System.out.println(
            "VillagerMixin.onSetCustomer: " + ((VillagerEntity) (Object) this).getOffers().size());
    }

}
