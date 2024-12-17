package buildcraft.compat;

import buildcraft.compat.module.forge.BlockPowerConvertor;
import buildcraft.compat.module.forge.TilePowerConvertor;
import buildcraft.lib.block.BlockPropertiesCreator;
import buildcraft.lib.registry.RegistrationHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.RegistryObject;

public class BCCompatBlocks {
    public static final RegistrationHelper HELPER = new RegistrationHelper(BCCompat.MODID);
    public static final RegistryObject<BlockPowerConvertor> powerConvertor;
    public static final RegistryObject<BlockEntityType<TilePowerConvertor>> powerConvertorTile;

    static {
        powerConvertor = HELPER.addBlockAndItem("block.power_convertor", BlockPropertiesCreator.createDefaultProperties(Material.METAL), BlockPowerConvertor::new);
        powerConvertorTile = HELPER.registerTile("tile.power_convertor", TilePowerConvertor::new, powerConvertor);
    }

    public static void fmlPreInit() {
    }
}
