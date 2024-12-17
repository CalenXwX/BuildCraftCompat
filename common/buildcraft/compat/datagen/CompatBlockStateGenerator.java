package buildcraft.compat.datagen;

import buildcraft.compat.BCCompat;
import buildcraft.compat.BCCompatBlocks;
import buildcraft.datagen.base.BCBaseBlockStateGenerator;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class CompatBlockStateGenerator extends BCBaseBlockStateGenerator {
    public CompatBlockStateGenerator(DataGenerator gen, ExistingFileHelper exFileHelper) {
        super(gen, BCCompat.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // power_convertor
        getVariantBuilder(BCCompatBlocks.powerConvertor.get()).forAllStates(s ->
                ConfiguredModel.builder().modelFile(
                                models().withExistingParent("buildcraftcompat:block/power_convertor", CUBE_ALL)
                                        .texture("all", "buildcraftcompat:block/power_convertor")
                        )
                        .build()
        );
    }
}
