package ram.talia.hexal.common.casting.arithmetics.operator.mote

import at.petrak.hexcasting.api.casting.arithmetic.operator.Operator
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate.ofType
import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import ram.talia.hexal.common.casting.arithmetics.operator.nextMote
import ram.talia.hexal.common.lib.hex.HexalIotaTypes.MOTE

object OperatorMoteAdd : Operator(2, IotaMultiPredicate.all(ofType(MOTE))) {

    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation
    ): OperationResult {
        val it = image.stack.reversed().iterator().withIndex()
        var ares: Iota? = null
        val absorber = it.nextMote(arity)
        val absorbee = it.nextMote(arity)

        if (absorber == null || absorbee == null) {
            // ensure always 1 iota returned to the stack.
            val toReturn = listOfNotNull(absorber?.copy(), absorbee?.copy())
            ares = toReturn.ifEmpty { null.asActionResult }.first()
        }
        if (ares == null) {
            if (absorber!!.itemIndex == absorbee!!.itemIndex)
                ares = absorber.copy()
            if (ares == null) {
                if (!absorber.typeMatches(absorbee))
                    throw MishapInvalidIota.of(absorbee, 0, "cant_combine_motes")

                absorber.absorb(absorbee)
                ares = absorber.copy()
            }
        }

        
        val output = mutableListOf<Iota>()
        it.asSequence().toMutableList().reversed().forEach {output.add(it.value)}
        output.add(ares!!)
        return OperationResult(
            image.copy(output),
            listOf(),
            continuation,
            HexEvalSounds.NORMAL_EXECUTE
        )
    }
}