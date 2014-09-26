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
 * @author Fredrik Thulin (fredrik@yubico.com)
 *
 */
public class HttpOathOtpPrincipal implements Principal {
	/** The name of the principal. */
	private String name;
	/** The realm of this principal. Something like a domain name. */
	private String realm = null;
	
	/**
     * Constructor.
     * 
     * @param name  principal's name.
     */
    public HttpOathOtpPrincipal(String name) {
        this.name = name;
    }

	/**
     * Constructor.
     * 
     * @param id  principal's name.
     * @param realm  Realm of id.
     */
    public HttpOathOtpPrincipal(String id, String realm) {
        this.name = id;
        this.realm = realm;
    }

    /** {@inheritDoc} */
    public String getName() {
    	if (realm != null) {
    		return this.name + this.realm;    		
    	}
        return name;
    }
    
    /** {@inheritDoc} */
    public String toString() {
    	return "<HttpOathOtpPrincipal>" + getName();
    }
}
