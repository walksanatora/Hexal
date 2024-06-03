package ram.talia.hexal.common.blocks.entity

import at.petrak.hexcasting.api.block.HexBlockEntity
import at.petrak.hexcasting.api.utils.getList
import at.petrak.hexcasting.api.utils.putList
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import ram.talia.hexal.api.config.HexalConfig
import ram.talia.hexal.api.mediafieditems.ItemRecord
import ram.talia.hexal.api.mediafieditems.MediafiedItemManager
import ram.talia.hexal.common.lib.HexalBlockEntities
import java.util.*
import kotlin.math.min

class BlockEntityMediafiedStorage(pos: BlockPos, val state: BlockState) : HexBlockEntity(HexalBlockEntities.MEDIAFIED_STORAGE, pos, state) {
    val pos: BlockPos = pos.immutable()

    var uuid: UUID = UUID.randomUUID()
        private set

    //begins fully closed
    var currentAnimation: AnimationState = AnimationState.Closing(ANIMATION_LENGTH)

    private var currentItemIndex = 0

    private var hasRegisteredToMediafiedItemManager: Boolean = false

    private val _storedItems: MutableMap<Int, ItemRecord> = mutableMapOf()
    val storedItems: Map<Int, ItemRecord>
        get() = _storedItems

    fun removeStoredItem(index: Int) {
        _storedItems.remove(index)

        if (_storedItems.isEmpty()) {
            sync()
        }
    }

    fun contains(index: Int) = storedItems.contains(index)

    fun isFull(): Boolean = storedItems.size >= HexalConfig.server.maxRecordsInMediafiedStorage

    fun assignItem(itemRecord: ItemRecord): MediafiedItemManager.Index {
        val index = currentItemIndex
        val wasEmpty = _storedItems.isEmpty()
        _storedItems[index] = itemRecord
        currentItemIndex += 1

        if (wasEmpty) {
            sync()
        }

        return MediafiedItemManager.Index(uuid, index)
    }

    fun getAllContainedItemTypes(): Set<Item> {
        return storedItems.values.map { it.item }.toHashSet()
    }

    fun getItemRecordsMatching(item: Item): Map<Int, ItemRecord> {
        return storedItems.filter { (_, record) -> record.item == item }
    }

    fun getItemRecordsMatching(itemRecord: ItemRecord): Map<Int, ItemRecord> {
        return storedItems.filter { (_, record) -> record.item == itemRecord.item && record.tag == itemRecord.tag }
    }

    fun serverTick() {
        if (!hasRegisteredToMediafiedItemManager) {
            MediafiedItemManager.addStorage(uuid, this)

            hasRegisteredToMediafiedItemManager = true
        }

        this.setChanged() // tracking when records get changed sounds horrible! we're just gonna always request to be saved!
    }

    fun clientTick() {
        currentAnimation.progress = min(currentAnimation.progress + 1, ANIMATION_LENGTH)
    }

    /**
     * When the BlockEntityMediafiedStorage is broken it should drop all its contents into the world.
     */
    fun dropAllContents(level: ServerLevel, pos: BlockPos) {
        for ((_, record) in storedItems) {
            val toDrop = mutableListOf<ItemStack>()

            record.addDrops(Long.MAX_VALUE, toDrop)

            for (stack in toDrop) {
                level.addFreshEntity(ItemEntity(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack))
            }
        }
    }

    /**
     * When sending a sync packet to the client, only include this boolean rather than all the save data.
     */
    override fun getUpdateTag() = CompoundTag().also { it.putBoolean(TAG_HAS_ITEMS, _storedItems.isNotEmpty()) }

    override fun saveModData(tag: CompoundTag) {
        tag.putUUID(TAG_UUID, uuid)
        tag.putInt(TAG_INDEX, currentItemIndex)

        val stored = ListTag()

        for ((id, record) in storedItems) {
            val c = CompoundTag()
            c.putInt(TAG_ID, id)
            record.writeToTag(c)
            stored.add(c)
        }

        tag.putList(TAG_STORED, stored)

        tag.putBoolean(TAG_HAS_ITEMS, _storedItems.isNotEmpty())
    }

    override fun loadModData(tag: CompoundTag) {
        if (TAG_UUID in tag)
            uuid = tag.getUUID(TAG_UUID)

        if (TAG_INDEX in tag)
            currentItemIndex = tag.getInt(TAG_INDEX)

        _storedItems.clear()
        if (TAG_STORED in tag) {
            val stored = tag.getList(TAG_STORED, Tag.TAG_COMPOUND)

            for (entry in stored) {
                val cEntry = entry as CompoundTag
                val record = ItemRecord.readFromTag(cEntry)
                if (record != null)
                    _storedItems[cEntry.getInt(TAG_ID)] = record
            }
        }

        if (TAG_HAS_ITEMS in tag) {
            if (tag.getBoolean(TAG_HAS_ITEMS)) {
                if (currentAnimation is AnimationState.Closing)
                    currentAnimation = AnimationState.Opening(0)
            } else {
                if (currentAnimation is AnimationState.Opening)
                    currentAnimation = AnimationState.Closing(0)
            }
        }
    }

    companion object {
        const val TAG_UUID = "hexal:uuid"
        const val TAG_INDEX = "hexal:index"
        const val TAG_STORED = "hexal:stored"

        const val TAG_ID = "hexal:storage_id"

        const val TAG_HAS_ITEMS = "hexal:has_items"

        const val ANIMATION_LENGTH = 20
    }

    sealed class AnimationState(var progress: Int) {
        class Closing(progress: Int) : AnimationState(progress)
        class Opening(progress: Int) : AnimationState(progress)
    }
}