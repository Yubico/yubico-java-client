/**
 * Copyright (c) 2011, Yubico AB.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 *  CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 *  TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 *  THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 */

package com.yubico.jaas;

import java.security.Principal;

/**
 * @author Fredrik Thulin <fredrik@yubico.com>
 *
 */
public class YubicoPrincipal implements Principal {
	/** The public ID of a Yubikey */
	private String publicId;
	/** The realm of this id. Something like a domain name. */
	private String realm = null;
	
	/**
     * Constructor.
     * 
     * @param id  principal's name - YubiKey public id.
     */
    public YubicoPrincipal(String id) {
        this.publicId = id;
    }

	/**
     * Constructor.
     * 
     * @param id  principal's name - YubiKey public id.
     * @param realm  Realm of id.
     */
    public YubicoPrincipal(String id, String realm) {
        this.publicId = id;
        this.realm = realm;
    }

    /** {@inheritDoc} */
    public String getName() {
    	if (realm != null) {
    		return this.publicId + this.realm;    		
    	}
        return publicId;
    }
    
    /** {@inheritDoc} */
    public String toString() {
    	return "<YubicoPrincipal>" + getName();
    }
    
}
