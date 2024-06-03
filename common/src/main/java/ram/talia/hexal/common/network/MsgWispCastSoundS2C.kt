package ram.talia.hexal.common.network

import at.petrak.hexcasting.common.msgs.IMessage
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import ram.talia.hexal.api.HexalAPI.modLoc
import ram.talia.hexal.common.entities.BaseCastingWisp

data class MsgWispCastSoundS2C private constructor(val wispId: Int) : IMessage {
	constructor(wisp: BaseCastingWisp) : this(wisp.id)

	override fun serialize(buf: FriendlyByteBuf) {
		buf.writeInt(wispId)
	}

	override fun getFabricId() = ID

	companion object {
		@JvmField
		val ID: ResourceLocation = modLoc("wcstsnd")

		@JvmStatic
		fun deserialise(buffer: ByteBuf) = MsgWispCastSoundS2C(buffer.readInt())

		@JvmStatic
		fun handle(self: MsgWispCastSoundS2C) {
			Minecraft.getInstance().execute {
				val mc = Minecraft.getInstance()
				val level = mc.level ?: return@execute

				(level.getEntity(self.wispId) as? BaseCastingWisp)?.playCastSoundClient()
			}
		}
	}
}
