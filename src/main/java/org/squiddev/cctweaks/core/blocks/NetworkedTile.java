package org.squiddev.cctweaks.core.blocks;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.util.ForgeDirection;
import org.squiddev.cctweaks.api.IWorldPosition;
import org.squiddev.cctweaks.api.network.INetworkNode;
import org.squiddev.cctweaks.api.network.Packet;

import java.util.Map;

/**
 * Abstract TE to prevent having to override every item
 */
public abstract class NetworkedTile extends TileBase implements INetworkNode {
	private Object lock = new Object();

	@Override
	public boolean canBeVisited(ForgeDirection from) {
		return true;
	}

	@Override
	public boolean canVisitTo(ForgeDirection to) {
		return true;
	}

	@Override
	public Map<String, IPeripheral> getConnectedPeripherals() {
		return null;
	}

	@Override
	public void receivePacket(Packet packet, int distanceTravelled) {
	}

	@Override
	public void networkInvalidated() {
	}

	@Override
	public Iterable<IWorldPosition> getExtraNodes() {
		return null;
	}

	@Override
	public Object lock() {
		return lock;
	}
}
