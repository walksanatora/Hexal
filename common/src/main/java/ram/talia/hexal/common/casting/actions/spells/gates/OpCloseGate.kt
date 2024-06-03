package ram.talia.hexal.common.casting.actions.spells.gates

import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import at.petrak.hexcasting.common.msgs.MsgBlinkS2C
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.phys.Vec3
import ram.talia.hexal.api.config.HexalConfig
import ram.talia.hexal.api.getGate
import ram.talia.hexal.api.minus
import ram.talia.hexal.api.casting.castables.VarargSpellAction
import ram.talia.hexal.api.casting.iota.GateIota

object OpCloseGate : VarargSpellAction {
    override fun argc(stack: List<Iota>): Int {
        if (stack.isEmpty())
            return 1
        val top = stack[0]
        if (top is GateIota && !top.isDrifting)
            return 1
        return 2
    }

    override fun execute(args: List<Iota>, argc: Int, env: CastingEnvironment): SpellAction.Result {
        val gate = args.getGate(0, argc)
        val targetPos = if (gate.isDrifting) args.getVec3(1, argc) else gate.getTargetPos(env.world)
            ?: throw IllegalStateException("non-drifting gates should always have a target pos.")
        
        
        // only check if in ambit when the gate is drifting.
        if (gate.isDrifting)
            env.assertVecInRange(targetPos)

        if (!env.isVecInWorld(targetPos.subtract(0.0, 1.0, 0.0)))
            throw MishapBadLocation(targetPos, "too_close_to_out")

        val gatees = gate.getMarked(env.world)
        gate.clearMarked()

        var cost = HexalConfig.server.closeGateCost
        if (gate.isDrifting) {
            cost = gatees.fold(cost) { cumCost, gatee -> cumCost + (HexalConfig.server.closeGateDistanceCostFactor * gatee.position().distanceTo(targetPos)).toLong() }
        }

        // make particle effects at every teleported entity.
        var meanEyeHeight = 0.0
        val burst = gatees.map { meanEyeHeight += it.eyeHeight; ParticleSpray.cloud(it.position().add(0.0, it.eyeHeight / 2.0, 0.0), 2.0) } as MutableList
        meanEyeHeight /= burst.size
        burst.add(ParticleSpray.burst(targetPos.add(0.0, meanEyeHeight / 2.0, 0.0), 2.0))

        return SpellAction.Result(
                Spell(gatees, targetPos, gate.isDrifting),
                cost,
                burst
        )
    }

    private data class Spell(val gatees: Set<Entity>, val targetPos: Vec3, val dropItems: Boolean) : RenderedSpell {
        // stole all this from the default teleport; sadge that it isn't accessible.

        override fun cast(env: CastingEnvironment) {
            for (gatee in gatees) {
                teleport(gatee, gatees, targetPos - gatee.position(), env)
            }
        }

        fun teleport(teleportee: Entity, allTeleportees: Set<Entity>, delta: Vec3, env: CastingEnvironment) {
            val distance = delta.length()

            // TODO make this not a magic number (config?)
            if (distance < 32768.0) {
                teleportRespectSticky(teleportee, allTeleportees, delta)
            }

            if (teleportee is ServerPlayer && teleportee == env.caster && distance < PlayerBasedCastEnv.AMBIT_RADIUS && dropItems) {
                // Drop items conditionally, based on distance teleported.
                // MOST IMPORTANT: Never drop main hand item, since if it's a trinket, it will get duplicated later.

                val baseDropChance = distance / 10000.0

                // Armor and hotbar items have a further reduced chance to be dropped since it's particularly annoying
                // having to rearrange those. Also it makes sense for LORE REASONS probably, since the caster is more
                // aware of items they use often.
                for (armorItem in teleportee.inventory.armor) {
                    if (EnchantmentHelper.hasBindingCurse(armorItem))
                        continue

                    if (Math.random() < baseDropChance * 0.25) {
                        teleportee.drop(armorItem.copy(), true, false)
                        armorItem.shrink(armorItem.count)
                    }
                }

                for ((pos, invItem) in teleportee.inventory.items.withIndex()) {
                    if (invItem == teleportee.mainHandItem) continue
                    val dropChance = if (pos < 9) baseDropChance * 0.5 else baseDropChance // hotbar
                    if (Math.random() < dropChance) {
                        teleportee.drop(invItem.copy(), true, false)
                        invItem.shrink(invItem.count)
                    }
                }

                // we also don't drop the offhand just to be nice
            }
        }
    }

    fun teleportRespectSticky(teleportee: Entity, allTeleportees: Set<Entity>, delta: Vec3) {
        val base = teleportee.rootVehicle

        val playersToUpdate = mutableListOf<ServerPlayer>()
        val indirect = base.indirectPassengers

        val allGated = indirect.all { allTeleportees.contains(it) }
        val sticky = indirect.any { it.type.`is`(HexTags.Entities.STICKY_TELEPORTERS) }
        val cannotSticky = indirect.none { it.type.`is`(HexTags.Entities.CANNOT_TELEPORT) }
        if (sticky && cannotSticky)
            return

        if (cannotSticky || !allGated) {
            // Break it into two stacks
            teleportee.stopRiding()
            teleportee.passengers.forEach(Entity::stopRiding)
            teleportee.setPos(teleportee.position().add(delta))
            if (teleportee is ServerPlayer) {
                playersToUpdate.add(teleportee)
            }
        } else {
            // this handles teleporting the passengers
            val target = base.position().add(delta)
            base.teleportTo(target.x, target.y, target.z)
            indirect
                    .filterIsInstance<ServerPlayer>()
                    .forEach(playersToUpdate::add)
        }

        for (player in playersToUpdate) {
            player.connection.resetPosition()
            IXplatAbstractions.INSTANCE.sendPacketToPlayer(player, MsgBlinkS2C(delta))
        }
    }
}