package ram.talia.hexal.fabric.cc

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent
import dev.onyxstudios.cca.api.v3.component.tick.ClientTickingComponent
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import ram.talia.hexal.api.HexalAPI
import ram.talia.hexal.api.everbook.Everbook
import ram.talia.hexal.common.network.MsgRemoveEverbookS2C
import ram.talia.hexal.common.network.MsgSendEverbookC2S
import ram.talia.hexal.common.network.MsgSetEverbookS2C
import ram.talia.hexal.xplat.IClientXplatAbstractions
import ram.talia.hexal.xplat.IXplatAbstractions

class CCEverbook(private val player: Player) : AutoSyncedComponent, ClientTickingComponent {
	private var everbook: Everbook? = null
		set(value) {
			if (field == null)
				field = value
		}

	var syncedLocalToServer = false

	@Environment(EnvType.CLIENT)
	override fun clientTick() {
		if (syncedLocalToServer)
			return
		syncedLocalToServer = true

		everbook = Everbook.fromDisk(player.uuid)

		IClientXplatAbstractions.INSTANCE.sendPacketToServer(MsgSendEverbookC2S(everbook!!))

		HexalAPI.LOGGER.info("registering listener to DISCONNECT event.")
		ClientPlayConnectionEvents.DISCONNECT.register { _, client -> HexalAPI.LOGGER.info("CCEverbook saveToDisk"); saveToDisk(client.player) }
	}

	fun getIota(key: HexPattern, level: ServerLevel) = everbook?.getIota(key, level) ?: NullIota()
	fun setIota(key: HexPattern, iota: Iota) {
		if (player.level().isClientSide) throw Exception("CCEverbook.setIota can only be called on the server") // TODO

		if (everbook != null) {
			everbook!!.setIota(key, iota)
			IXplatAbstractions.INSTANCE.sendPacketToPlayer(player as ServerPlayer, MsgSetEverbookS2C(key, IotaType.serialize(iota)))
		}
	}

	fun setFullEverbook(everbook: Everbook) {
		if (player.level().isClientSide) throw Exception("CCEverbook.setFullEverbook can only be called on the server") // TODO
		this.everbook = everbook
	}

	fun removeIota(key: HexPattern) {
		if (player.level().isClientSide) throw Exception("CCEverbook.removeIota can only be called on the server") // TODO
		everbook?.removeIota(key)

		IXplatAbstractions.INSTANCE.sendPacketToPlayer(player as ServerPlayer, MsgRemoveEverbookS2C(key))
	}

	fun getMacro(key: HexPattern, level: ServerLevel) = everbook?.getMacro(key, level)

	fun toggleMacro(key: HexPattern) = everbook?.toggleMacro(key)

	fun isMacro(key: HexPattern) = everbook?.isMacro(key)

	fun getClientIota(key: HexPattern): CompoundTag? = everbook?.getClientIota(key)
	fun setClientIota(key: HexPattern, iota: CompoundTag) = everbook!!.setIota(key, iota)
	fun removeClientIota(key: HexPattern) = everbook!!.removeIota(key)

	fun getClientPattern(index: Int) = everbook?.getKey(index)

	//region read/write Nbt (unused)
	/**
	 * Data stored on the client rather than the server
	 */
	override fun readFromNbt(tag: CompoundTag) { }

	/**
	 * Data stored on the client rather than the server
	 */
	override fun writeToNbt(tag: CompoundTag) { }

	//endregion
	companion object {

		private fun saveToDisk(player: Player?) {
			if (player != null)
				HexalCardinalComponents.EVERBOOK.get(player).everbook?.saveToDisk()
		}
	}
}