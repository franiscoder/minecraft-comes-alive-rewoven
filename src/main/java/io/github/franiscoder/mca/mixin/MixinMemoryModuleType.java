package io.github.franiscoder.mca.mixin;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MemoryModuleType.class)
public interface MixinMemoryModuleType {
	@Invoker
	static <U> MemoryModuleType<U> invokeRegister(String id) {
		throw new IllegalStateException();
	}
    /*
    @Invoker(value = "register(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lnet/minecraft/entity/ai/brain/MemoryModuleType;")
    static <U> MemoryModuleType<U> invokeRegister(String id, Codec<U> codec) {
        throw new IllegalStateException();
    }

     */
}
