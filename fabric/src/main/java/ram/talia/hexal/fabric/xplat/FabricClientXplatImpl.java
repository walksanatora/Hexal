package ram.talia.hexal.fabric.xplat;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.msgs.IMessage;
import at.petrak.hexcasting.fabric.client.ExtendedTexture;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import ram.talia.hexal.fabric.cc.HexalCardinalComponents;
import ram.talia.hexal.xplat.IClientXplatAbstractions;

import java.util.function.Function;

public class FabricClientXplatImpl implements IClientXplatAbstractions {
	@Override
	public void sendPacketToServer (IMessage packet) {
		ClientPlayNetworking.send(packet.getFabricId(), packet.toBuf());
	}
	
	@Override
	public void initPlatformSpecific () {
	
	}
	
	@Override
	public <T extends Entity> void registerEntityRenderer (EntityType<? extends T> type, EntityRendererProvider<T> renderer) {
		EntityRendererRegistry.register(type, renderer);
	}
	
	@Override
	public <T extends ParticleOptions> void registerParticleType (ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> factory) {
		ParticleFactoryRegistry.getInstance().register(type, factory::apply);
	}
	
	// suck it fabric trying to be "safe"
	private record UnclampedClampedItemPropFunc(ItemPropertyFunction inner) implements ClampedItemPropertyFunction {
		@Override
		public float unclampedCall (ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
			return inner.call(stack, level, entity, seed);
		}
		
		@Override
		public float call (ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
			return this.unclampedCall(stack, level, entity, seed);
		}
	}
	
	@Override
	public void registerItemProperty (Item item, ResourceLocation id, ItemPropertyFunction func) {
		ItemProperties.register(item, id, new UnclampedClampedItemPropFunc(func));
	}
	
	@Override
	public CompoundTag getClientEverbookIota (HexPattern key) {
		assert Minecraft.getInstance().player != null;
		return HexalCardinalComponents.EVERBOOK.get(Minecraft.getInstance().player).getClientIota(key);
	}
	
	@Override
	public void setClientEverbookIota (HexPattern key, CompoundTag iota) {
		assert Minecraft.getInstance().player != null;
		HexalCardinalComponents.EVERBOOK.get(Minecraft.getInstance().player).setClientIota(key, iota);
	}
	
	@Override
	public void removeClientEverbookIota (HexPattern key) {
		assert Minecraft.getInstance().player != null;
		HexalCardinalComponents.EVERBOOK.get(Minecraft.getInstance().player).removeClientIota(key);
	}
	
	@Override
	public HexPattern getClientEverbookPattern (int index) {
		assert Minecraft.getInstance().player != null;
		return HexalCardinalComponents.EVERBOOK.get(Minecraft.getInstance().player).getClientPattern(index);
	}
	
	@Override
	public void toggleClientEverbookMacro (HexPattern key) {
		assert Minecraft.getInstance().player != null;
		HexalCardinalComponents.EVERBOOK.get(Minecraft.getInstance().player).toggleMacro(key);
	}
	
	@Override
	public boolean isClientEverbookMacro (HexPattern key) {
		assert Minecraft.getInstance().player != null;
		return Boolean.TRUE.equals(HexalCardinalComponents.EVERBOOK.get(Minecraft.getInstance().player).isMacro(key));
	}
	
	@Override
	public void setFilterSave (AbstractTexture texture, boolean filter, boolean mipmap) {
		((ExtendedTexture) texture).setFilterSave(filter, mipmap);
	}
	
	@Override
	public void restoreLastFilter (AbstractTexture texture) {
		((ExtendedTexture) texture).restoreLastFilter();
	}
}
