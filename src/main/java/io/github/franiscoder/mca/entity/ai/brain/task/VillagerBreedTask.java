package io.github.franiscoder.mca.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import io.github.franiscoder.mca.entity.MCAVillagerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Optional;

public class VillagerBreedTask extends Task<MCAVillagerEntity> {
	private long breedEndTime;
	
	public VillagerBreedTask() {
		super(ImmutableMap.of(MemoryModuleType.BREED_TARGET, MemoryModuleState.VALUE_PRESENT, MemoryModuleType.VISIBLE_MOBS, MemoryModuleState.VALUE_PRESENT), 350, 350);
	}
	
	protected boolean shouldRun(ServerWorld serverWorld, MCAVillagerEntity villagerEntity) {
		return this.isReadyToBreed(villagerEntity);
	}
	
	protected boolean shouldKeepRunning(ServerWorld serverWorld, MCAVillagerEntity villagerEntity, long l) {
		return l <= this.breedEndTime && this.isReadyToBreed(villagerEntity);
	}
	
	protected void run(ServerWorld serverWorld, MCAVillagerEntity villagerEntity, long l) {
		PassiveEntity passiveEntity = villagerEntity.getBrain().getOptionalMemory(MemoryModuleType.BREED_TARGET).get();
		LookTargetUtil.lookAtAndWalkTowardsEachOther(villagerEntity, passiveEntity, 0.5F);
		serverWorld.sendEntityStatus(passiveEntity, (byte) 18);
		serverWorld.sendEntityStatus(villagerEntity, (byte) 18);
		int i = 275 + villagerEntity.getRandom().nextInt(50);
		this.breedEndTime = l + (long) i;
	}
	
	protected void keepRunning(ServerWorld serverWorld, MCAVillagerEntity villagerEntity, long l) {
		MCAVillagerEntity villagerEntity2 = (MCAVillagerEntity) villagerEntity.getBrain().getOptionalMemory(MemoryModuleType.BREED_TARGET).get();
		if (villagerEntity.squaredDistanceTo(villagerEntity2) <= 5.0D) {
			LookTargetUtil.lookAtAndWalkTowardsEachOther(villagerEntity, villagerEntity2, 0.5F);
			if (l >= this.breedEndTime) {
				villagerEntity.eatForBreeding();
				villagerEntity2.eatForBreeding();
				this.goHome(serverWorld, villagerEntity, villagerEntity2);
			} else if (villagerEntity.getRandom().nextInt(35) == 0) {
				serverWorld.sendEntityStatus(villagerEntity2, (byte) 12);
				serverWorld.sendEntityStatus(villagerEntity, (byte) 12);
			}
			
		}
	}
	
	private void goHome(ServerWorld world, MCAVillagerEntity first, MCAVillagerEntity second) {
		Optional<BlockPos> optional = this.getReachableHome(world, first);
		if (!optional.isPresent()) {
			world.sendEntityStatus(second, (byte) 13);
			world.sendEntityStatus(first, (byte) 13);
		} else {
			Optional<MCAVillagerEntity> optional2 = this.createChild(world, first, second);
			if (optional2.isPresent()) {
				this.setChildHome(world, optional2.get(), optional.get());
			} else {
				world.getPointOfInterestStorage().releaseTicket(optional.get());
				DebugInfoSender.sendPointOfInterest(world, optional.get());
			}
		}
		
	}
	
	protected void finishRunning(ServerWorld serverWorld, MCAVillagerEntity villagerEntity, long l) {
		villagerEntity.getBrain().forget(MemoryModuleType.BREED_TARGET);
	}
	
	private boolean isReadyToBreed(MCAVillagerEntity villager) {
		Brain<MCAVillagerEntity> brain = villager.getBrain();
		Optional<PassiveEntity> optional = brain.getOptionalMemory(MemoryModuleType.BREED_TARGET).filter((passiveEntity) -> passiveEntity.getType() == EntityType.VILLAGER);
		return optional.filter(passiveEntity -> LookTargetUtil.canSee(brain, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER) && villager.isReadyToBreed() && passiveEntity.isReadyToBreed()).isPresent();
	}
	
	private Optional<BlockPos> getReachableHome(ServerWorld world, MCAVillagerEntity villager) {
		return world.getPointOfInterestStorage().getPosition(PointOfInterestType.HOME.getCompletionCondition(), (blockPos) -> this.canReachHome(villager, blockPos), villager.getBlockPos(), 48);
	}
	
	private boolean canReachHome(MCAVillagerEntity villager, BlockPos pos) {
		Path path = villager.getNavigation().findPathTo(pos, PointOfInterestType.HOME.getSearchDistance());
		return path != null && path.reachesTarget();
	}
	
	private Optional<MCAVillagerEntity> createChild(ServerWorld serverWorld, MCAVillagerEntity villagerEntity, MCAVillagerEntity villagerEntity2) {
		MCAVillagerEntity villagerEntity3 = villagerEntity.createChild(serverWorld, villagerEntity2);
		if (villagerEntity3 == null) {
			return Optional.empty();
		} else {
			villagerEntity.setBreedingAge(6000);
			villagerEntity2.setBreedingAge(6000);
			villagerEntity3.setBreedingAge(-24000);
			villagerEntity3.refreshPositionAndAngles(villagerEntity.getX(), villagerEntity.getY(), villagerEntity.getZ(), 0.0F, 0.0F);
			serverWorld.spawnEntityAndPassengers(villagerEntity3);
			serverWorld.sendEntityStatus(villagerEntity3, (byte) 12);
			return Optional.of(villagerEntity3);
		}
	}
	
	private void setChildHome(ServerWorld world, MCAVillagerEntity child, BlockPos pos) {
		GlobalPos globalPos = GlobalPos.create(world.getRegistryKey(), pos);
		child.getBrain().remember(MemoryModuleType.HOME, globalPos);
	}
}