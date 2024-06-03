package ram.talia.hexal.common.network

import at.petrak.hexcasting.common.msgs.IMessage
import io.netty.buffer.ByteBuf
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import ram.talia.hexal.api.HexalAPI
import ram.talia.hexal.api.everbook.Everbook
import ram.talia.hexal.xplat.IXplatAbstractions

data class MsgSendEverbookC2S(val everbook: Everbook) : IMessage {
	override fun serialize(buf: FriendlyByteBuf) {
		buf.writeNbt(everbook.serialiseToNBT())
	}

	override fun getFabricId() = ID

	fun handle(server: MinecraftServer, sender: ServerPlayer) {
		server.execute {
			IXplatAbstractions.INSTANCE.setFullEverbook(sender, everbook.filterIotasIllegalInterworld(server.overworld()))
		}
	}

	companion object {
		@JvmField
		val ID: ResourceLocation = HexalAPI.modLoc("sendever")

		@JvmStatic
		fun deserialise(buffer: ByteBuf): MsgSendEverbookC2S {
			val buf = FriendlyByteBuf(buffer)
			return MsgSendEverbookC2S(Everbook.fromNbt(buf.readNbt()!!))
		}
	}
}