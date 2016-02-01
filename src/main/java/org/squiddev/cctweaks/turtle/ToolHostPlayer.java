package org.squiddev.cctweaks.turtle;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.Pair;
import org.squiddev.cctweaks.api.IWorldPosition;
import org.squiddev.cctweaks.core.Config;
import org.squiddev.cctweaks.core.McEvents;
import org.squiddev.cctweaks.core.utils.FakeNetHandler;
import org.squiddev.cctweaks.core.utils.WorldPosition;

/**
 * Handles various turtle actions.
 */
public class ToolHostPlayer extends TurtlePlayer {
	private final ITurtleAccess turtle;

	private BlockPos digPosition;
	private Block digBlock;

	private int currentDamage = -1;
	private int currentDamageState = -1;

	/**
	 * A copy of the active stack for applying/removing attributes
	 */
	private ItemStack activeStack;

	public ItemStack itemInUse;

	public final McEvents.IDropConsumer consumer = new McEvents.IDropConsumer() {
		@Override
		public void consumeDrop(ItemStack drop) {
			ItemStack remainder = InventoryUtil.storeItems(drop, turtle.getInventory(), 0, turtle.getInventory().getSizeInventory(), turtle.getSelectedSlot());
			if (remainder != null) {
				BlockPos position = getPlayerCoordinates();
				WorldUtil.dropItemStack(remainder, worldObj, position, turtle.getDirection().getOpposite());
			}
		}
	};

	public ToolHostPlayer(ITurtleAccess turtle) {
		super((WorldServer) turtle.getWorld());
		this.turtle = turtle;

		playerNetServerHandler = new FakeNetHandler(this);
	}

	public TurtleCommandResult attack(EnumFacing direction) {
		updateInformation(direction);

		Vec3 rayDir = getLook(1.0f);
		Vec3 rayStart = new Vec3(posX, posY, posZ);

		Pair<Entity, Vec3> hit = WorldUtil.rayTraceEntities(turtle.getWorld(), rayStart, rayDir, 1.5);

		if (hit != null) {
			Entity hitEntity = hit.getLeft();
			loadInventory(getItem());

			McEvents.addEntityConsumer(hitEntity, consumer);
			attackTargetEntityWithCurrentItem(hitEntity);
			McEvents.removeEntityConsumer(hitEntity);

			unloadInventory(turtle);
			return TurtleCommandResult.success();
		}

		return TurtleCommandResult.failure("Nothing to attack here");
	}

	private void setState(Block block, BlockPos pos) {
		theItemInWorldManager.cancelDestroyingBlock();
		theItemInWorldManager.durabilityRemainingOnBlock = -1;

		digPosition = pos;
		digBlock = block;
		currentDamage = -1;
		currentDamageState = -1;
	}

	public TurtleCommandResult dig(EnumFacing direction) {
		updateInformation(direction);

		BlockPos pos = getPlayerCoordinates().offset(direction);
		World world = turtle.getWorld();
		Block block = world.getBlockState(pos).getBlock();

		if (block != digBlock || !pos.equals(digPosition)) setState(block, pos);

		if (!world.isAirBlock(pos) && !block.getMaterial().isLiquid()) {
			if (block == Blocks.bedrock || block.getBlockHardness(world, pos) <= -1) {
				return TurtleCommandResult.failure("Unbreakable block detected");
			}

			loadInventory(getItem());

			ItemInWorldManager manager = theItemInWorldManager;
			for (int i = 0; i < Config.Turtle.ToolHost.digFactor; i++) {
				if (currentDamageState == -1) {
					// TODO: Migrate checks to here
					manager.onBlockClicked(pos, direction.getOpposite());
					currentDamageState = manager.durabilityRemainingOnBlock;
				} else {
					currentDamage++;
					float hardness = block.getPlayerRelativeBlockHardness(this, world, pos) * (currentDamage + 1);
					int hardnessState = (int) (hardness * 10);

					if (hardnessState != currentDamageState) {
						world.sendBlockBreakProgress(getEntityId(), pos, hardnessState);
						currentDamageState = hardnessState;
					}

					if (hardness >= 1) {
						IWorldPosition position = new WorldPosition(world, pos);
						McEvents.addBlockConsumer(position, consumer);
						manager.tryHarvestBlock(pos);
						McEvents.removeBlockConsumer(position);

						setState(null, null);
						break;
					}
				}
			}

			return TurtleCommandResult.success();
		}

		return TurtleCommandResult.failure("Nothing to dig here");
	}

	public BlockPos getPlayerCoordinates() {
		return turtle.getPosition();
	}

	@Override
	public Vec3 getPositionVector() {
		return turtle.getVisualPosition(0);
	}

	@Override
	public void loadInventory(ItemStack currentStack) {
		// Copy properties over
		if (currentStack != null) {
			getAttributeMap().applyAttributeModifiers(currentStack.getAttributeModifiers());
			activeStack = currentStack.copy();
		}

		super.loadInventory(currentStack);
	}

	@Override
	public ItemStack unloadInventory(ITurtleAccess turtle) {
		// Revert to old properties
		if (activeStack != null) {
			getAttributeMap().removeAttributeModifiers(activeStack.getAttributeModifiers());
			activeStack = null;
		}

		return super.unloadInventory(turtle);
	}

	public void loadInventory() {
		loadInventory(getItem());
	}

	public void unloadInventory() {
		unloadInventory(turtle);
	}

	/**
	 * Basically just {@link #getHeldItem()}
	 */
	public ItemStack getItem() {
		return turtle.getInventory().getStackInSlot(turtle.getSelectedSlot());
	}

	/**
	 * Update the player information
	 */
	public void updateInformation(EnumFacing direction) {
		BlockPos position = turtle.getPosition();

		setPositionAndRotation(
			position.getX() + 0.5 + 0.48 * direction.getFrontOffsetX(),
			position.getY() + 0.5 + 0.48 * direction.getFrontOffsetY(),
			position.getZ() + 0.5 + 0.48 * direction.getFrontOffsetZ(),
			direction.getAxis() != EnumFacing.Axis.Y ? DirectionUtil.toYawAngle(direction) : DirectionUtil.toYawAngle(turtle.getDirection()),
			direction.getAxis() != EnumFacing.Axis.Y ? 0 : DirectionUtil.toPitchAngle(direction)
		);
	}

	public void loadWholeInventory() {
		IInventory turtleInventory = turtle.getInventory();
		int size = turtleInventory.getSizeInventory();
		int largerSize = inventory.getSizeInventory();

		for (int i = 0; i < size; i++) {
			inventory.setInventorySlotContents(i, turtleInventory.getStackInSlot(i));
		}
		for (int i = size; i < largerSize; i++) {
			inventory.setInventorySlotContents(i, null);
		}
	}

	public void unloadWholeInventory() {
		IInventory turtleInventory = turtle.getInventory();
		int size = turtleInventory.getSizeInventory();
		int largerSize = inventory.getSizeInventory();

		for (int i = 0; i < size; i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			turtleInventory.setInventorySlotContents(i, stack == null || stack.stackSize <= 0 ? null : stack);
		}
		for (int i = size; i < largerSize; i++) {
			consumer.consumeDrop(inventory.getStackInSlot(i));
		}
	}

	@Override
	public void setItemInUse(ItemStack stack, int duration) {
		super.setItemInUse(stack, duration);
		itemInUse = stack;
	}

	@Override
	public void clearItemInUse() {
		super.clearItemInUse();
		itemInUse = null;
	}
}
