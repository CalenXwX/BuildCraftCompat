package buildcraft.compat.module.waila;

import buildcraft.api.mj.ILaserTarget;
import buildcraft.api.mj.MjAPI;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.List;

class LaserTargetDataProvider {
    static class BodyProvider extends BaseWailaDataProvider.BodyProvider {
        @Override
        public void getWailaBody(List<ITextComponent> currentTip, IDataAccessor accessor, IPluginConfig iPluginConfig) {
            TileEntity tile = accessor.getTileEntity();
            if (tile instanceof ILaserTarget) {
                CompoundNBT nbt = accessor.getServerData();
                if (nbt.contains("required_power", Constants.NBT.TAG_LONG)) {
                    long power = nbt.getLong("required_power");
                    if (power > 0L) {
                        currentTip.add(new TranslationTextComponent("buildcraft.waila.waiting_for_laser", MjAPI.formatMj(power)));
                    }
                }
            }
//            else {
//                currentTip.add(new TextComponent(ChatFormatting.RED + "{wrong tile entity}"));
//            }
        }
    }

    static class NBTProvider extends BaseWailaDataProvider.NBTProvider {
        @Override
        public void getNBTData(CompoundNBT nbt, ServerPlayerEntity player, World world, TileEntity tile) {
            if (tile instanceof ILaserTarget) {
                ILaserTarget target = (ILaserTarget) tile;
                nbt.putLong("required_power", target.getRequiredLaserPower());
            }
        }
    }
}
