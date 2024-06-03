package ram.talia.hexal.common.casting.actions.spells.link

import at.petrak.hexcasting.api.casting.*
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import ram.talia.hexal.api.config.HexalConfig
import ram.talia.hexal.api.linkable.ILinkable
import ram.talia.hexal.api.linkable.LinkableRegistry
import ram.talia.hexal.api.casting.mishaps.MishapLinkToSelf

object OpLink : SpellAction {
	override val argc = 1

	override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
		val linkThis = LinkableRegistry.linkableFromCastingEnvironment(env)

		val linkOther = LinkableRegistry.linkableFromIota(args[0], env.world)
				?: throw MishapInvalidIota.ofType(args[0], 0, "linkable")

		if (linkThis == linkOther)
			throw MishapLinkToSelf(linkThis)

		env.assertVecInRange(linkOther.getPosition())

		if (!linkThis.isInRange(linkOther))
			throw MishapBadLocation(linkOther.getPosition())

		return SpellAction.Result(
			Spell(linkThis, linkOther),
			HexalConfig.server.linkCost,
			listOf(ParticleSpray.burst(linkOther.getPosition(), 1.5))
		)
	}

	private data class Spell(val linkThis: ILinkable, val linkOther: ILinkable) : RenderedSpell {
		override fun cast(env: CastingEnvironment) = linkThis.link(linkOther)
	}
}