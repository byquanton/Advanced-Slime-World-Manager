package com.grinderwolf.swm.nms.v1182;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NMSSlimeChunk implements SlimeChunk {

    private LevelChunk chunk;

    public NMSSlimeChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public String getWorldName() {
        return chunk.getLevel().getMinecraftWorld().serverLevelData.getLevelName();
    }

    @Override
    public int getX() {
        return chunk.getPos().x;
    }

    @Override
    public int getZ() {
        return chunk.getPos().z;
    }

    @Override
    public SlimeChunkSection[] getSections() {
        SlimeChunkSection[] sections = new SlimeChunkSection[this.chunk.getMaxSection() - this.chunk.getMinSection() + 1];
        LevelLightEngine lightEngine = chunk.getLevel().getChunkSource().getLightEngine();

        Registry<Biome> biomeRegistry = chunk.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);

        // Ignore deprecation, spigot only method
        Codec<PalettedContainer<Holder<Biome>>> codec = PalettedContainer.codec(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.getHolderOrThrow(Biomes.PLAINS));

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            LevelChunkSection section = chunk.getSections()[sectionId];
            // Sections CANNOT be null in 1.18

            // Block Light Nibble Array
            NibbleArray blockLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Sky light Nibble Array
            NibbleArray skyLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Tile/Entity Data

            // Block Data
            Tag blockStateData = ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow(false, System.err::println); // todo error handling
            Tag biomeData = codec.encodeStart(NbtOps.INSTANCE, section.getBiomes()).getOrThrow(false, System.err::println); // todo error handling

            CompoundTag blockStateTag = (CompoundTag) Converter.convertTag("", blockStateData);
            CompoundTag biomeTag = (CompoundTag) Converter.convertTag("", biomeData);

            sections[sectionId] = new CraftSlimeChunkSection(null, null, blockStateTag, biomeTag, blockLightArray, skyLightArray);
        }

        return sections;
    }

    @Override
    public int getMinSection() {
        return this.chunk.getMinSection();
    }

    @Override
    public int getMaxSection() {
        return this.chunk.getMaxSection();
    }

    @Override
    public CompoundTag getHeightMaps() {
        // HeightMap
        CompoundMap heightMaps = new CompoundMap();

        for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.heightmaps.entrySet()) {
            if (!entry.getKey().keepAfterWorldgen()) {
                continue;
            }

            Heightmap.Types type = entry.getKey();
            Heightmap map = entry.getValue();

            heightMaps.put(type.name(), new LongArrayTag(type.name(), map.getRawData()));
        }

        return new CompoundTag("", heightMaps);
    }

    @Override
    public int[] getBiomes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        List<CompoundTag> tileEntities = new ArrayList<>();

        for (BlockEntity entity : chunk.blockEntities.values()) {
            final net.minecraft.nbt.CompoundTag entityNbt = entity.saveWithFullMetadata();
            tileEntities.add((CompoundTag) Converter.convertTag(entityNbt.getString("id"), entityNbt));
        }

        return tileEntities;
    }

    @Override
    public List<CompoundTag> getEntities() {
        List<CompoundTag> entities = new ArrayList<>();

        PersistentEntitySectionManager<Entity> entityManager = chunk.level.entityManager;

        for (Entity entity : entityManager.getEntityGetter().getAll()) {
            ChunkPos chunkPos = chunk.getPos();
            ChunkPos entityPos = entity.chunkPosition();

            if (chunkPos.x == entityPos.x && chunkPos.z == entityPos.z) {
                net.minecraft.nbt.CompoundTag entityNbt = new net.minecraft.nbt.CompoundTag();
                if (entity.save(entityNbt)) {
                    entities.add((CompoundTag) Converter.convertTag("", entityNbt));
                }
            }
        }
        return entities;
    }

    public LevelChunk getChunk() {
        return chunk;
    }

    public void setChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }
}
