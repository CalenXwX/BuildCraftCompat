package buildcraft.compat.module.forge;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPowerConvertor extends BlockBCTile_Neptune<TilePowerConvertor> {
    public BlockPowerConvertor(String idBC, Properties props) {
        super(idBC, props);
    }

    @Override
    public TileBC_Neptune newBlockEntity(BlockPos pos, BlockState state) {
        return new TilePowerConvertor(pos, state);
    }
}
