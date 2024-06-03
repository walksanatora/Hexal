package ram.talia.hexal.common.casting.actions.spells.wisp

import at.petrak.hexcasting.api.casting.*
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import ram.talia.hexal.api.casting.eval.env.WispCastEnv
import ram.talia.hexal.api.config.HexalConfig
import ram.talia.hexal.api.casting.mishaps.MishapNoWisp
import ram.talia.hexal.common.entities.TickingWisp
import kotlin.math.ln
import kotlin.math.min

object OpMoveSpeedSet : SpellAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val newMax = args.getPositiveDouble(0, OpMoveTargetSet.argc)
        val newMult = newMax / TickingWisp.BASE_MAX_SPEED_PER_TICK

        if (env !is WispCastEnv || env.wisp !is TickingWisp)
            throw MishapNoWisp()

        val oldMax = env.wisp.maximumMoveMultiplier

        // cost scales quadratically with newMult, but as with Impulse a player can reduce the cost requirement with
        // many small increases to the max (limited to a minimum cost of moveSpeedSetCost if the maximum is being updated at
        // all to not incentivise very laggy stuff). If the newMult is older than the old max, there is no cost;
        // reducing speed is not penalised.
        val cost = if (newMult > oldMax) {
            (HexalConfig.server.moveSpeedSetCost * min(1.0, (newMult - oldMax) * (newMult - oldMax))).toLong()
        } else 0

        return SpellAction.Result(
                Spell(env.wisp, newMult),
                cost,
                listOf(ParticleSpray.burst(env.wisp.position(), min(1.0, ln(newMult))))
        )
    }

    private data class Spell(val wisp: TickingWisp, val newMult: Double) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            wisp.currentMoveMultiplier = newMult.toFloat()
        }
    }
}