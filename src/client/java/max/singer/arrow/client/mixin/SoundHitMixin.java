package max.singer.arrow.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import max.singer.arrow.client.ArrowClient;

@Mixin(SoundSystem.class)
public class SoundHitMixin {
    @Inject(method = "play", at = @At("HEAD"))
    private void onSoundPlay(SoundInstance sound, CallbackInfo ci) {
        if (sound.getId().equals(SoundEvents.ENTITY_ARROW_HIT_PLAYER.getId())) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                for (int i = 0; i < client.player.getInventory().size(); i++) {
                    ItemStack stack = client.player.getInventory().getStack(i);
                    String itemName = stack.getName().toString();
                    if (itemName.contains("slow_falling")) {
                        ArrowClient.startTimer();
                        break;
                    }
                }
            }
        }
    }
}