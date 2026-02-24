package com.hareidus.taboo.farm.modules.l1.plot

import org.bukkit.World
import org.bukkit.generator.ChunkGenerator
import java.util.Random

/**
 * 农场 Void 世界生成器
 *
 * 生成完全空白的区块（无地形），所有地块地面由 PlotManager 按需生成。
 */
class FarmWorldGenerator : ChunkGenerator() {

    override fun generateChunkData(
        world: World,
        random: Random,
        x: Int,
        z: Int,
        biome: BiomeGrid
    ): ChunkData {
        return createChunkData(world)
    }

    override fun canSpawn(world: World, x: Int, z: Int): Boolean {
        return true
    }
}
