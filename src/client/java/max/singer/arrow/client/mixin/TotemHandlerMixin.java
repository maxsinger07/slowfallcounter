package max.singer.arrow.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import max.singer.arrow.client.ArrowClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class TotemHandlerMixin {
    @Shadow private ClientWorld world;

    @Inject(method = "onEntityStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V"))
    public void onTotemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        Entity entity = packet.getEntity(world);
        MinecraftClient client = MinecraftClient.getInstance();

        if (entity instanceof OtherClientPlayerEntity && entity != client.player && client.world != null && client.player != null) {
            long nearbyPlayerCount = client.world.getPlayers()
                    .stream()
                    .filter(p -> p != client.player)
                    .count();

            if (nearbyPlayerCount == 1) {
                ArrowClient.stopTimer(true); // true indicates it's a totem pop
            }
        }
    }
}