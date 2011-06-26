/**
 * This file is part of the Nilestore project.
 * 
 * Copyright (C) (2011) Nile University (NU)
 *
 * Nilestore is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package eg.nileu.cis.nilestore.introducer.port;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.p2p.overlay.OverlayAddress;
import eg.nileu.cis.nilestore.introducer.Service;

// TODO: Auto-generated Javadoc
/**
 * The Class Publish.
 * 
 * @author Mahmoud Ismail <mahmoudahmedismail@gmail.com>
 */
public class Publish extends Message {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4029068015676628889L;

	/** The service. */
	private final Service service;

	/** The overlay address. */
	private final OverlayAddress overlayAddress;

	/**
	 * Instantiates a new publish.
	 * 
	 * @param source
	 *            the source
	 * @param destination
	 *            the destination
	 * @param service
	 *            the service
	 * @param overlayAddress
	 *            the overlay address
	 */
	public Publish(Address source, Address destination, Service service,
			OverlayAddress overlayAddress) {
		super(source, destination);
		this.service = service;
		this.overlayAddress = overlayAddress;
	}

	/**
	 * Gets the overlay address.
	 * 
	 * @return the overlay address
	 */
	public OverlayAddress getOverlayAddress() {
		return overlayAddress;
	}

	/**
	 * Gets the service.
	 * 
	 * @return the service
	 */
	public Service getService() {
		return service;
	}

}
