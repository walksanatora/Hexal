package ram.talia.hexal.common.casting;

import at.petrak.hexcasting.api.PatternRegistry;
import at.petrak.hexcasting.api.spell.math.HexDir;
import at.petrak.hexcasting.api.spell.math.HexPattern;
import ram.talia.hexal.common.casting.actions.OpCompareBlocks;
import ram.talia.hexal.common.casting.actions.OpCompareEntities;
import ram.talia.hexal.common.casting.actions.OpCompareTypes;

import static ram.talia.hexal.api.HexalAPI.modLoc;

public class RegisterPatterns {
	public static void registerPatterns () {
		try {
			
			PatternRegistry.mapPattern(HexPattern.fromAngles("qaqqaea", HexDir.EAST),
									   modLoc("compare_blocks"),
									   OpCompareBlocks.INSTANCE);
			PatternRegistry.mapPattern(HexPattern.fromAngles("qawde", HexDir.SOUTH_WEST),
									   modLoc("compare_entities"),
									   OpCompareEntities.INSTANCE);
			PatternRegistry.mapPattern(HexPattern.fromAngles("awd", HexDir.SOUTH_WEST),
									   modLoc("compare_types"),
									   OpCompareTypes.INSTANCE);
		}
		catch (PatternRegistry.RegisterPatternException e) {
			e.printStackTrace();
		}
	}
}
