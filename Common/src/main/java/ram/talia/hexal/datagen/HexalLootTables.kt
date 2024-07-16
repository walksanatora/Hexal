package ram.talia.hexal.datagen

import at.petrak.paucal.api.datagen.PaucalLootTableSubProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import ram.talia.hexal.api.HexalAPI
import ram.talia.hexal.common.lib.HexalBlocks
import java.util.HashMap
import java.util.function.BiConsumer

class HexalLootTables : PaucalLootTableSubProvider(HexalAPI.MOD_ID) {
    override fun makeLootTables(blockTables: MutableMap<Block, LootTable.Builder>, lootTables: MutableMap<ResourceLocation, LootTable.Builder>) {
        dropSelf(blockTables, HexalBlocks.MEDIAFIED_STORAGE)
    }

    override fun generate(register: BiConsumer<ResourceLocation, LootTable.Builder>) {
        val blockTables = HashMap<Block, LootTable.Builder>()
        val lootTables = HashMap<ResourceLocation, LootTable.Builder>()
        this.makeLootTables(blockTables, lootTables)

        for ((key, value) in blockTables) {
            register.accept(key.lootTable, value.setParamSet(LootContextParamSets.BLOCK))
        }
        // isn't that slick
        lootTables.forEach(register)
    }
}