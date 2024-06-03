package ram.talia.hexal.common.blocks.entity

import at.petrak.hexcasting.api.block.HexBlockEntity
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.common.particles.ConjureParticleOptions
import net.minecraft.Util
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import ram.talia.hexal.api.nextColour
import ram.talia.hexal.api.nextGaussian
import ram.talia.hexal.common.entities.WanderingWisp
import ram.talia.hexal.common.lib.HexalBlockEntities
import java.util.*

class BlockEntitySlipway(pos: BlockPos, state: BlockState) : HexBlockEntity(HexalBlockEntities.SLIPWAY, pos, state) {
	val pos: BlockPos = pos.immutable()

	private val random = RandomSource.create()

	private var isActive = false

	private var nextSpawnTick: Long = 0

	fun tick(level: Level, blockPos: BlockPos) {
		if (level.isClientSide)
			clientTick(level, blockPos)
		else
			serverTick(level as ServerLevel, blockPos)
	}

	private fun clientTick(level: Level, blockPos: BlockPos) {
		val vec = Vec3.atCenterOf(blockPos)

		for (colouriser in HexItems.DYE_PIGMENTS.values) {
			val colour: Int = colours[random.nextInt(colours.size)]

			level.addParticle(
					ConjureParticleOptions(colour),
					(vec.x + RENDER_RADIUS * random.nextGaussian()),
					(vec.y + RENDER_RADIUS * random.nextGaussian()),
					(vec.z + RENDER_RADIUS * random.nextGaussian()),
					0.0125 * (random.nextDouble() - 0.5),
					0.0125 * (random.nextDouble() - 0.5),
					0.0125 * (random.nextDouble() - 0.5)
			)
		}
	}

	private fun serverTick(level: ServerLevel, blockPos: BlockPos) {
		if (!isActive) {
			if (level.hasNearbyAlivePlayer(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(), ACTIVE_RANGE)) {
				isActive = true
				sync()
			}

			return
		}

		val tick = level.gameTime

		val aabb = AABB.ofSize(Vec3.atCenterOf(blockPos), 64.0, 64.0, 64.0)

		if (tick >= nextSpawnTick && level.getEntitiesOfClass(WanderingWisp::class.java, aabb).size < 20) {
			nextSpawnTick = tick + random.nextGaussian(SPAWN_INTERVAL_MU.toDouble(), SPAWN_INTERVAL_SIG.toDouble()).toLong()

			val colouriser = getRandomPigment()

			val wisp = WanderingWisp(level, Vec3.atCenterOf(blockPos))
			wisp.setPigment(colouriser)
			level.addFreshEntity(wisp)

			sync()
		}
	}

	override fun saveModData(tag: CompoundTag) {
		tag.putBoolean(TAG_IS_ACTIVE, isActive)
		tag.putLong(TAG_NEXT_SPAWN_TICK, nextSpawnTick)
	}

	override fun loadModData(tag: CompoundTag) {
		isActive = tag.getBoolean(TAG_IS_ACTIVE)
		nextSpawnTick = tag.getLong(TAG_NEXT_SPAWN_TICK)
	}

	companion object {
		const val TAG_IS_ACTIVE = "is_active"
		const val TAG_NEXT_SPAWN_TICK = "last_spawned_tick"

		const val ACTIVE_RANGE = 5.0
		const val SPAWN_INTERVAL_MU = 80
		const val SPAWN_INTERVAL_SIG = 10

		const val RENDER_RADIUS = 0.5

		private val RANDOM = Random()

		fun getRandomPigment(): FrozenPigment {
			return FrozenPigment(ItemStack(HexItems.DYE_PIGMENTS.values.elementAt(RANDOM.nextInt(HexItems.DYE_PIGMENTS.size))), Util.NIL_UUID)
		}

		private val colours = makeColours()

		private fun makeColours(): Array<Int> {
			val random = RandomSource.create()
			val coloursList = mutableListOf<Int>()

			for (i in 0..32) {
				for (colouriser in HexItems.DYE_PIGMENTS.values) {
					val frozenColouriser = FrozenPigment(ItemStack(colouriser), Util.NIL_UUID)
					coloursList.add(frozenColouriser.nextColour(random))
				}
			}

			return coloursList.toTypedArray()
		}
	}
}