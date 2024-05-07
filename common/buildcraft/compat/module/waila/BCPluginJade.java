package buildcraft.compat.module.waila;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.tile.TileBC_Neptune;
import mcp.mobius.waila.api.*;

@WailaPlugin
public class BCPluginJade implements IWailaPlugin {
    // Calen: in 1.18.2 we should use Block or TE class instead of interface here, it's too difficult to get all matched if allowing some BC modules absent,
    // so just use TileBC_Neptune and BlockBCTile_Neptune
    @Override
    public void register(IWailaCommonRegistration registrar) {
        IServerDataProvider autoCraftNbtProvider = new AutoCraftDataProvider.NBTProvider();
        IServerDataProvider laserTargetNbtProvider = new LaserTargetDataProvider.NBTProvider();
        IServerDataProvider assemblyCraftNbtProvider = new AssemblyCraftDataProvider.NBTProvider();

        registrar.registerBlockDataProvider(autoCraftNbtProvider, TileBC_Neptune.class);
        registrar.registerBlockDataProvider(laserTargetNbtProvider, TileBC_Neptune.class);
        registrar.registerBlockDataProvider(assemblyCraftNbtProvider, TileBC_Neptune.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registrar) {
        IComponentProvider autoCraftBodyProvider = new AutoCraftDataProvider.BodyProvider();
        IComponentProvider laserTargetBodyProvider = new LaserTargetDataProvider.BodyProvider();
        IComponentProvider assemblyCraftBodyProvider = new AssemblyCraftDataProvider.BodyProvider();

        registrar.registerComponentProvider(autoCraftBodyProvider, TooltipPosition.BODY, BlockBCTile_Neptune.class);
        registrar.registerComponentProvider(laserTargetBodyProvider, TooltipPosition.BODY, BlockBCTile_Neptune.class);
        registrar.registerComponentProvider(assemblyCraftBodyProvider, TooltipPosition.BODY, BlockBCTile_Neptune.class);
    }
}
