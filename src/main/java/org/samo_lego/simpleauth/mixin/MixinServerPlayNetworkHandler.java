package org.samo_lego.simpleauth.mixin;

import net.minecraft.client.network.packet.EntityS2CPacket;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.ChatMessageC2SPacket;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import org.samo_lego.simpleauth.event.entity.player.ChatCallback;
import org.samo_lego.simpleauth.event.entity.player.PlayerMoveCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(
            method = "onChatMessage(Lnet/minecraft/server/network/packet/ChatMessageC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    // Thanks to Liach for helping me out!
                    target = "net/minecraft/network/NetworkThreadUtils.forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    // todo: redirect packets to off-thread packet manager?
    private void onChatMessage(ChatMessageC2SPacket chatMessageC2SPacket_1, CallbackInfo ci) {
        ActionResult result = ChatCallback.EVENT.invoker().onPlayerChat(player, chatMessageC2SPacket_1);
        if (result == ActionResult.FAIL) {
            ci.cancel();
        }
    }

    @Inject(
            method="onPlayerMove(Lnet/minecraft/server/network/packet/PlayerMoveC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    // Thanks to Liach for helping me out!
                    target = "net/minecraft/network/NetworkThreadUtils.forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerMove(PlayerMoveC2SPacket playerMoveC2SPacket_1, CallbackInfo ci) {
        ActionResult result = PlayerMoveCallback.EVENT.invoker().onPlayerMove(player);
        if (result == ActionResult.FAIL) {
            // A bit ugly, I know. (we need to update player position)
            player.teleport(player.getX(), player.getY(), player.getZ());
            ci.cancel();
        }
    }
}