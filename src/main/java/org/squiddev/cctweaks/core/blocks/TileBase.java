package org.squiddev.cctweaks.core.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import org.squiddev.cctweaks.api.IWorldPosition;

/**
 * org.squiddev.cctweaks.core.blocks (CC-Tweaks
 */
public abstract class TileBase extends TileEntity implements IWorldPosition {
	@Override
	public int getX() {
		return xCoord;
	}

	@Override
	public int getY() {
		return yCoord;
	}

	@Override
	public int getZ() {
		return zCoord;
	}

	@Override
	public IBlockAccess getWorld() {
		return worldObj;
	}
}
