/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.VillagerEvent;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MerchantScreenHandler.class, priority = 1001)
public class MerchantScreenHandlerMixin {
    @Inject(method = "setOffers", at = @At("TAIL"), cancellable = true)
    private void onSetOffers(TradeOfferList offers, CallbackInfo info) {
        System.out.println("MerchantScreenHandlerMixin.onSetOffers");
//        MeteorClient.EVENT_BUS.post(VillagerEvent.get(offers));
    }

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/village/Merchant;)V", at = @At("TAIL"), cancellable = true)
    private void onInit(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, net.minecraft.village.Merchant merchant, CallbackInfo info) {
        System.out.println("MerchantScreenHandlerMixin.onInit");
        System.out.println("MerchantScreenHandlerMixin.onInit: " + (MerchantAccessor) merchant.getOffers());
    }
}
