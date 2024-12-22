package buildcraft.compat.module.forge;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.*;
import buildcraft.compat.BCCompatBlocks;
import buildcraft.lib.tile.TileBC_Neptune;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

// Calen 1.20.1
public class TilePowerConvertor extends TileBC_Neptune implements IEnergyStorage, IMjConnector, IMjReceiver, IMjRedstoneReceiver, IMjReadable, IMjPassiveProvider {
    public TilePowerConvertor(BlockPos pos, BlockState blockState) {
        super(BCCompatBlocks.powerConvertorTile.get(), pos, blockState);
        caps.addCapabilityInstance(ForgeCapabilities.ENERGY, this, EnumPipePart.VALUES);
        caps.addCapabilityInstance(MjAPI.CAP_CONNECTOR, this, EnumPipePart.VALUES);
        caps.addCapabilityInstance(MjAPI.CAP_REDSTONE_RECEIVER, this, EnumPipePart.VALUES);
        caps.addCapabilityInstance(MjAPI.CAP_READABLE, this, EnumPipePart.VALUES);
        caps.addCapabilityInstance(MjAPI.CAP_RECEIVER, this, EnumPipePart.VALUES);
        caps.addCapabilityInstance(MjAPI.CAP_PASSIVE_PROVIDER, this, EnumPipePart.VALUES);
    }

    // IMjConnector
    @Override
    public boolean canConnect(@Nonnull IMjConnector other) {
        return true;
    }

    // IMjPassiveProvider
    @Override
    public long extractPower(long min, long max, final boolean simulate) {
        int min_FE = convert_mMJ_2_FE(min);
        int max_FE = convert_mMJ_2_FE(max);
        List<Pair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(ForgeCapabilities.ENERGY);
        // try getting power from each IEnergyStorage in toQuery
        List<Pair<Direction, Pair<IEnergyStorage, Integer>>> queried = toQuery.stream().map(p -> {
                    IEnergyStorage energyStorage = p.getSecond();
                    int extractedFE = energyStorage.extractEnergy(max_FE, true);
                    return new Pair<>(p.getFirst(), new Pair<>(p.getSecond(), extractedFE));
                })
                .filter(p -> p.getSecond().getSecond() > 0)
                .toList();
        // get max power can get
        int neighbourTotalCanProvide_FE = 0;
        for (Pair<Direction, Pair<IEnergyStorage, Integer>> p : queried) {
            neighbourTotalCanProvide_FE += p.getSecond().getSecond();
        }
        if (neighbourTotalCanProvide_FE < min_FE) {
            // if cannot support [min], fail
            return 0;
        } else {
            // if can support [min]
            final int final_neighbourTotalCanProvide_FE = neighbourTotalCanProvide_FE;
            // if can provide more than [max], each provides weighted average value
            if (neighbourTotalCanProvide_FE > max_FE) {
                queried = queried.stream()
                        .map(p -> new Pair<>(p.getFirst(), new Pair<>(p.getSecond().getFirst(), p.getSecond().getSecond() * max_FE / final_neighbourTotalCanProvide_FE)))
                        .toList();
            }
            // extract!
            int trueExtracted_FE = 0;
            for (Pair<Direction, Pair<IEnergyStorage, Integer>> p : queried) {
                trueExtracted_FE += p.getSecond().getFirst().extractEnergy(p.getSecond().getSecond(), simulate);
            }
            return convert_FE_2_mMJ(trueExtracted_FE);
        }
    }

    // IMjReadable
    @Override
    public long getStored() {
        List<Pair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(ForgeCapabilities.ENERGY);
        int stored_FE = 0;
        for (Pair<Direction, IEnergyStorage> p : toQuery) {
            stored_FE += p.getSecond().getEnergyStored();
        }
        return convert_FE_2_mMJ(stored_FE);
    }

    // IMjReadable
    @Override
    public long getCapacity() {
        List<Pair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(ForgeCapabilities.ENERGY);
        int stored_FE = 0;
        for (Pair<Direction, IEnergyStorage> p : toQuery) {
            stored_FE += p.getSecond().getMaxEnergyStored();
        }
        return convert_FE_2_mMJ(stored_FE);
    }

    // IMjReceiver
    @Override
    public long getPowerRequested() {
        List<Pair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(ForgeCapabilities.ENERGY);
        int stored_FE = 0;
        for (Pair<Direction, IEnergyStorage> p : toQuery) {
            IEnergyStorage capFE = p.getSecond();
            stored_FE += capFE.canReceive() ? Math.max(capFE.receiveEnergy(Integer.MAX_VALUE, true), 0) : 0;
        }
        return convert_FE_2_mMJ(stored_FE);
    }

    // IMjReceiver
    @Override
    public long receivePower(long max_mMJ, boolean simulate) {
        int max_FE = convert_mMJ_2_FE(max_mMJ);
        List<Pair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(ForgeCapabilities.ENERGY);
        // try getting power from each IEnergyStorage in toQuery
        List<Pair<Direction, Pair<IEnergyStorage, Integer>>> queried = toQuery.stream().map(p -> {
                    IEnergyStorage energyStorage = p.getSecond();
                    int receivedFE = energyStorage.receiveEnergy(max_FE, true);
                    return new Pair<>(p.getFirst(), new Pair<>(p.getSecond(), receivedFE));
                })
                .filter(p -> p.getSecond().getSecond() > 0)
                .toList();
        // get max power can receive
        int neighbourTotalCanReceive_FE = 0;
        for (Pair<Direction, Pair<IEnergyStorage, Integer>> p : queried) {
            neighbourTotalCanReceive_FE += p.getSecond().getSecond();
        }
        // if can provide more than [max], each provides weighted average value
        final int final_neighbourTotalCanReceive_FE = neighbourTotalCanReceive_FE;
        if (neighbourTotalCanReceive_FE > max_FE) {
            queried = queried.stream()
                    .map(p -> new Pair<>(p.getFirst(), new Pair<>(p.getSecond().getFirst(), p.getSecond().getSecond() * max_FE / final_neighbourTotalCanReceive_FE)))
                    .toList();
        }
        // receive!
        int trueReceived_FE = 0;
        for (Pair<Direction, Pair<IEnergyStorage, Integer>> p : queried) {
            trueReceived_FE += p.getSecond().getFirst().receiveEnergy(p.getSecond().getSecond(), simulate);
        }
        int trueExcess_FE = max_FE - trueReceived_FE;
        return convert_FE_2_mMJ(trueExcess_FE);
    }

    // IEnergyStorage
    // IMjReceiver
    @Override
    public boolean canReceive() {
        return true;
    }

    // IEnergyStorage
    @Override
    public int receiveEnergy(int max_FE, boolean simulate) {
        long max_mMJ = convert_FE_2_mMJ(max_FE);
        List<Pair<Direction, IMjReceiver>> toQuery = getAllHasCapAround(MjAPI.CAP_RECEIVER);
        // try getting power from each IMjReceiver in toQuery
        List<Pair<Direction, Pair<IMjReceiver, Long>>> queried = toQuery.stream().map(p -> {
                    IMjReceiver mjReceiver = p.getSecond();
                    long excess_FE = mjReceiver.receivePower(max_mMJ, true);
                    long received_FE = max_mMJ - excess_FE;
                    return new Pair<>(p.getFirst(), new Pair<>(p.getSecond(), received_FE));
                })
                .filter(p -> p.getSecond().getSecond() > 0)
                .toList();
        // get max power can receive
        long neighbourTotalCanReceive_FE = 0;
        for (Pair<Direction, Pair<IMjReceiver, Long>> p : queried) {
            neighbourTotalCanReceive_FE += p.getSecond().getSecond();
        }
        // if can provide more than [max], each provides weighted average value
        final long final_neighbourTotalCanReceive_FE = neighbourTotalCanReceive_FE;
        if (neighbourTotalCanReceive_FE > max_mMJ) {
            queried = queried.stream()
                    .map(p -> new Pair<>(p.getFirst(), new Pair<>(p.getSecond().getFirst(), p.getSecond().getSecond() * max_mMJ / final_neighbourTotalCanReceive_FE)))
                    .toList();
        }
        // receive!
        long trueExcess_FE = 0;
        for (Pair<Direction, Pair<IMjReceiver, Long>> p : queried) {
            trueExcess_FE += p.getSecond().getFirst().receivePower(p.getSecond().getSecond(), simulate);
        }
        long trueReceived_FE = max_FE - trueExcess_FE;
        return convert_mMJ_2_FE(trueReceived_FE);
    }

    @Override
    public int extractEnergy(int max_FE, boolean simulate) {
        long max_mMJ = convert_FE_2_mMJ(max_FE);
        List<Pair<Direction, IMjPassiveProvider>> toQuery = getAllHasCapAround(MjAPI.CAP_PASSIVE_PROVIDER);
        // try getting power from each IEnergyStorage in toQuery
        List<Pair<Direction, Pair<IMjPassiveProvider, Long>>> queried = toQuery.stream().map(p -> {
                    IMjPassiveProvider energyStorage = p.getSecond();
                    long extractedFE = energyStorage.extractPower(0, max_mMJ, true);
                    return new Pair<>(p.getFirst(), new Pair<>(p.getSecond(), extractedFE));
                })
                .filter(p -> p.getSecond().getSecond() > 0)
                .toList();
        // get max power can get
        long neighbourTotalCanProvide_mMJ = 0;
        for (Pair<Direction, Pair<IMjPassiveProvider, Long>> p : queried) {
            neighbourTotalCanProvide_mMJ += p.getSecond().getSecond();
        }
        // if can provide more than [max], each provides weighted average value
        final long final_neighbourTotalCanProvide_FE = neighbourTotalCanProvide_mMJ;
        if (neighbourTotalCanProvide_mMJ > max_mMJ) {
            queried = queried.stream()
                    .map(p -> new Pair<>(p.getFirst(), new Pair<>(p.getSecond().getFirst(), p.getSecond().getSecond() * max_mMJ / final_neighbourTotalCanProvide_FE)))
                    .toList();
        }
        // extract!
        long trueExtracted_mMJ = 0;
        for (Pair<Direction, Pair<IMjPassiveProvider, Long>> p : queried) {
            trueExtracted_mMJ += p.getSecond().getFirst().extractPower(0, p.getSecond().getSecond(), simulate);
        }
        return convert_mMJ_2_FE(trueExtracted_mMJ);
    }

    @Override
    public int getEnergyStored() {
        List<Pair<Direction, IMjReadable>> toQuery = getAllHasCapAround(MjAPI.CAP_READABLE);
        long neighbourTotalEnergyStored_mMJ = 0;
        for (Pair<Direction, IMjReadable> p : toQuery) {
            neighbourTotalEnergyStored_mMJ += p.getSecond().getStored();
        }
        return convert_mMJ_2_FE(neighbourTotalEnergyStored_mMJ);
    }

    @Override
    public int getMaxEnergyStored() {
        List<Pair<Direction, IMjReadable>> toQuery = getAllHasCapAround(MjAPI.CAP_READABLE);
        long neighbourTotalMaxEnergyStored_mMJ = 0;
        for (Pair<Direction, IMjReadable> p : toQuery) {
            neighbourTotalMaxEnergyStored_mMJ += p.getSecond().getCapacity();
        }
        return convert_mMJ_2_FE(neighbourTotalMaxEnergyStored_mMJ);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    /**
     * To get all capability instances of capability provider TileEntities all around this.
     * {@link TilePowerConvertor} and any TileEntity both has {@link ForgeCapabilities#ENERGY} and {@link MjAPI#CAP_CONNECTOR} will be excluded.
     */
    private <T> List<Pair<Direction, T>> getAllHasCapAround(Capability<T> capability) {
        return Arrays.stream(Direction.VALUES)
                .map(d -> new Pair<>(d, this.worldPosition.relative(d)))
                .filter(p -> this.level.getBlockState(p.getSecond()).getBlock() != BCCompatBlocks.powerConvertor.get())
                .map(p -> new Pair<>(p.getFirst(), this.level.getBlockEntity(p.getSecond())))
                .filter(p -> p.getSecond() != null)
                // should not have both
                .filter(p -> {
                    Direction dir = p.getFirst();
                    BlockEntity te = p.getSecond();
                    boolean has_FORGE_ENERGY = te.getCapability(ForgeCapabilities.ENERGY, dir).isPresent();
                    boolean has_CAP_CONNECTOR = te.getCapability(MjAPI.CAP_CONNECTOR, dir).isPresent();
                    return !(has_FORGE_ENERGY && has_CAP_CONNECTOR);
                })
                .map(p -> new Pair<>(p.getFirst(), p.getSecond().getCapability(capability, p.getFirst().getOpposite())))
                .filter(p -> p.getSecond().isPresent())
                .map(p -> new Pair<>(p.getFirst(), p.getSecond().orElse(null)))
                .toList();
    }

    public static int convert_mMJ_2_FE(long mMJ) {
        return (int) Math.min(Integer.MAX_VALUE, mMJ * 16L / MjAPI.MJ);
    }

    public static long convert_FE_2_mMJ(int fe) {
        return fe * MjAPI.MJ / 16L;
    }
}
