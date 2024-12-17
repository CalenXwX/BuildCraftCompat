package buildcraft.compat.module.forge;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.*;
import buildcraft.compat.BCCompatBlocks;
import buildcraft.lib.tile.TileBC_Neptune;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

// Calen
public class TilePowerConvertor extends TileBC_Neptune implements IEnergyStorage, IMjConnector, IMjReceiver, IMjRedstoneReceiver, IMjReadable, IMjPassiveProvider {
    public TilePowerConvertor(BlockPos pos, BlockState blockState) {
        super(BCCompatBlocks.powerConvertorTile.get(), pos, blockState);
        caps.addCapabilityInstance(CapabilityEnergy.ENERGY, this, EnumPipePart.VALUES);
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
        List<ObjectObjectImmutablePair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(CapabilityEnergy.ENERGY);
        // try getting power from each IEnergyStorage in toQuery
        List<ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IEnergyStorage, Integer>>> queried = toQuery.stream().map(p -> {
                    IEnergyStorage energyStorage = p.right();
                    int extractedFE = energyStorage.extractEnergy(max_FE, true);
                    return new ObjectObjectImmutablePair<>(p.left(), new ObjectObjectImmutablePair<>(p.right(), extractedFE));
                })
                .filter(p -> p.right().right() > 0)
                .toList();
        // get max power can get
        int neighbourTotalCanProvide_FE = 0;
        for (ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IEnergyStorage, Integer>> p : queried) {
            neighbourTotalCanProvide_FE += p.right().right();
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
                        .map(p -> new ObjectObjectImmutablePair<>(p.left(), new ObjectObjectImmutablePair<>(p.right().left(), p.right().right() * max_FE / final_neighbourTotalCanProvide_FE)))
                        .toList();
            }
            // extract!
            int trueExtracted_FE = 0;
            for (ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IEnergyStorage, Integer>> p : queried) {
                trueExtracted_FE += p.right().left().extractEnergy(p.right().right(), simulate);
            }
            return convert_FE_2_mMJ(trueExtracted_FE);
        }
    }

    // IMjReadable
    @Override
    public long getStored() {
        List<ObjectObjectImmutablePair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(CapabilityEnergy.ENERGY);
        int stored_FE = 0;
        for (ObjectObjectImmutablePair<Direction, IEnergyStorage> p : toQuery) {
            stored_FE += p.right().getEnergyStored();
        }
        return convert_FE_2_mMJ(stored_FE);
    }

    // IMjReadable
    @Override
    public long getCapacity() {
        List<ObjectObjectImmutablePair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(CapabilityEnergy.ENERGY);
        int stored_FE = 0;
        for (ObjectObjectImmutablePair<Direction, IEnergyStorage> p : toQuery) {
            stored_FE += p.right().getMaxEnergyStored();
        }
        return convert_FE_2_mMJ(stored_FE);
    }

    // IMjReceiver
    @Override
    public long getPowerRequested() {
        List<ObjectObjectImmutablePair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(CapabilityEnergy.ENERGY);
        int stored_FE = 0;
        for (ObjectObjectImmutablePair<Direction, IEnergyStorage> p : toQuery) {
            IEnergyStorage capFE = p.right();
            stored_FE += capFE.canReceive() ? Math.max(capFE.receiveEnergy(Integer.MAX_VALUE, true), 0) : 0;
        }
        return convert_FE_2_mMJ(stored_FE);
    }

    // IMjReceiver
    @Override
    public long receivePower(long max_mMJ, boolean simulate) {
        int max_FE = convert_mMJ_2_FE(max_mMJ);
        List<ObjectObjectImmutablePair<Direction, IEnergyStorage>> toQuery = getAllHasCapAround(CapabilityEnergy.ENERGY);
        // try getting power from each IEnergyStorage in toQuery
        List<ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IEnergyStorage, Integer>>> queried = toQuery.stream().map(p -> {
                    IEnergyStorage energyStorage = p.right();
                    int receivedFE = energyStorage.receiveEnergy(max_FE, true);
                    return new ObjectObjectImmutablePair<>(p.left(), new ObjectObjectImmutablePair<>(p.right(), receivedFE));
                })
                .filter(p -> p.right().right() > 0)
                .toList();
        // get max power can receive
        int neighbourTotalCanReceive_FE = 0;
        for (ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IEnergyStorage, Integer>> p : queried) {
            neighbourTotalCanReceive_FE += p.right().right();
        }
        // if can provide more than [max], each provides weighted average value
        final int final_neighbourTotalCanReceive_FE = neighbourTotalCanReceive_FE;
        if (neighbourTotalCanReceive_FE > max_FE) {
            queried = queried.stream()
                    .map(p -> new ObjectObjectImmutablePair<>(p.left(), new ObjectObjectImmutablePair<>(p.right().left(), p.right().right() * max_FE / final_neighbourTotalCanReceive_FE)))
                    .toList();
        }
        // receive!
        int trueReceived_FE = 0;
        for (ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IEnergyStorage, Integer>> p : queried) {
            trueReceived_FE += p.right().left().receiveEnergy(p.right().right(), simulate);
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
        List<ObjectObjectImmutablePair<Direction, IMjReceiver>> toQuery = getAllHasCapAround(MjAPI.CAP_RECEIVER);
        // try getting power from each IMjReceiver in toQuery
        List<ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IMjReceiver, Long>>> queried = toQuery.stream().map(p -> {
                    IMjReceiver mjReceiver = p.right();
                    long excess_FE = mjReceiver.receivePower(max_mMJ, true);
                    long received_FE = max_mMJ - excess_FE;
                    return new ObjectObjectImmutablePair<>(p.left(), new ObjectObjectImmutablePair<>(p.right(), received_FE));
                })
                .filter(p -> p.right().right() > 0)
                .toList();
        // get max power can receive
        long neighbourTotalCanReceive_FE = 0;
        for (ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IMjReceiver, Long>> p : queried) {
            neighbourTotalCanReceive_FE += p.right().right();
        }
        // if can provide more than [max], each provides weighted average value
        final long final_neighbourTotalCanReceive_FE = neighbourTotalCanReceive_FE;
        if (neighbourTotalCanReceive_FE > max_mMJ) {
            queried = queried.stream()
                    .map(p -> new ObjectObjectImmutablePair<>(p.left(), new ObjectObjectImmutablePair<>(p.right().left(), p.right().right() * max_mMJ / final_neighbourTotalCanReceive_FE)))
                    .toList();
        }
        // receive!
        long trueExcess_FE = 0;
        for (ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IMjReceiver, Long>> p : queried) {
            trueExcess_FE += p.right().left().receivePower(p.right().right(), simulate);
        }
        long trueReceived_FE = max_FE - trueExcess_FE;
        return convert_mMJ_2_FE(trueReceived_FE);
    }

    @Override
    public int extractEnergy(int max_FE, boolean simulate) {
        long max_mMJ = convert_FE_2_mMJ(max_FE);
        List<ObjectObjectImmutablePair<Direction, IMjPassiveProvider>> toQuery = getAllHasCapAround(MjAPI.CAP_PASSIVE_PROVIDER);
        // try getting power from each IEnergyStorage in toQuery
        List<ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IMjPassiveProvider, Long>>> queried = toQuery.stream().map(p -> {
                    IMjPassiveProvider energyStorage = p.right();
                    long extractedFE = energyStorage.extractPower(0, max_mMJ, true);
                    return new ObjectObjectImmutablePair<>(p.left(), new ObjectObjectImmutablePair<>(p.right(), extractedFE));
                })
                .filter(p -> p.right().right() > 0)
                .toList();
        // get max power can get
        long neighbourTotalCanProvide_mMJ = 0;
        for (ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IMjPassiveProvider, Long>> p : queried) {
            neighbourTotalCanProvide_mMJ += p.right().right();
        }
        // if can provide more than [max], each provides weighted average value
        final long final_neighbourTotalCanProvide_FE = neighbourTotalCanProvide_mMJ;
        if (neighbourTotalCanProvide_mMJ > max_mMJ) {
            queried = queried.stream()
                    .map(p -> new ObjectObjectImmutablePair<>(p.left(), new ObjectObjectImmutablePair<>(p.right().left(), p.right().right() * max_mMJ / final_neighbourTotalCanProvide_FE)))
                    .toList();
        }
        // extract!
        long trueExtracted_mMJ = 0;
        for (ObjectObjectImmutablePair<Direction, ObjectObjectImmutablePair<IMjPassiveProvider, Long>> p : queried) {
            trueExtracted_mMJ += p.right().left().extractPower(0, p.right().right(), simulate);
        }
        return convert_mMJ_2_FE(trueExtracted_mMJ);
    }

    @Override
    public int getEnergyStored() {
        List<ObjectObjectImmutablePair<Direction, IMjReadable>> toQuery = getAllHasCapAround(MjAPI.CAP_READABLE);
        long neighbourTotalEnergyStored_mMJ = 0;
        for (ObjectObjectImmutablePair<Direction, IMjReadable> p : toQuery) {
            neighbourTotalEnergyStored_mMJ += p.right().getStored();
        }
        return convert_mMJ_2_FE(neighbourTotalEnergyStored_mMJ);
    }

    @Override
    public int getMaxEnergyStored() {
        List<ObjectObjectImmutablePair<Direction, IMjReadable>> toQuery = getAllHasCapAround(MjAPI.CAP_READABLE);
        long neighbourTotalMaxEnergyStored_mMJ = 0;
        for (ObjectObjectImmutablePair<Direction, IMjReadable> p : toQuery) {
            neighbourTotalMaxEnergyStored_mMJ += p.right().getCapacity();
        }
        return convert_mMJ_2_FE(neighbourTotalMaxEnergyStored_mMJ);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    /**
     * To get all capability instances of capability provider TileEntities all around this.
     * {@link TilePowerConvertor} and any TileEntity both has {@link CapabilityEnergy#ENERGY} and {@link MjAPI#CAP_CONNECTOR} will be excluded.
     */
    private <T> List<ObjectObjectImmutablePair<Direction, T>> getAllHasCapAround(Capability<T> capability) {
        return Arrays.stream(Direction.values())
                .map(d -> new ObjectObjectImmutablePair<>(d, this.worldPosition.relative(d)))
                .filter(p -> this.level.getBlockState(p.right()).getBlock() != BCCompatBlocks.powerConvertor.get())
                .map(p -> new ObjectObjectImmutablePair<>(p.left(), this.level.getBlockEntity(p.right())))
                .filter(p -> p.right() != null)
                // should not have both
                .filter(p -> {
                    Direction dir = p.left();
                    BlockEntity te = p.right();
                    boolean has_FORGE_ENERGY = te.getCapability(CapabilityEnergy.ENERGY, dir).isPresent();
                    boolean has_CAP_CONNECTOR = te.getCapability(MjAPI.CAP_CONNECTOR, dir).isPresent();
                    return !(has_FORGE_ENERGY && has_CAP_CONNECTOR);
                })
                .map(p -> new ObjectObjectImmutablePair<>(p.left(), p.right().getCapability(capability, p.left().getOpposite())))
                .filter(p -> p.right().isPresent())
                .map(p -> new ObjectObjectImmutablePair<>(p.left(), p.right().orElse(null)))
                .toList();
    }

    public static int convert_mMJ_2_FE(long mMJ) {
        return (int) Math.min(Integer.MAX_VALUE, mMJ / MjAPI.MJ * 16L);
    }

    public static long convert_FE_2_mMJ(int fe) {
        return fe * MjAPI.MJ / 16L;
    }
}
