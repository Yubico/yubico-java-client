/*
 * Copyright 2011, Yubico AB. All rights reserved.
 * This file is derivative work from YubikeyToUserMapImpl.java in the
 * Yubico Java client. The following copyright applies to the
 * derivative work:
 * Copyright 2011, Université de Lausanne. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.yubico.jaas.impl;

import com.yubico.jaas.YubikeyToUserMap;
import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.LdapConfig;
import edu.vt.middleware.ldap.SearchFilter;
import java.util.Iterator;
import java.util.Map;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to verify that a user is the rightful owner of a YubiKey.
 *
 * This implementation uses a LDAP directory to look up the Yubikey's
 * publicId and fetch the associated username.
 *
 * @author Etienne Dysli <etienne.dysli@unil.ch>
 */
public class YubikeyToUserLDAPMap implements YubikeyToUserMap {
    /* Supported JAAS configuration options */
    /** Enable verification of Yubikey owner (default: true) */
    public static final String OPTION_YUBICO_VERIFY_YK_OWNER = "verify_yubikey_owner";
    /** Name of the LDAP attribute containing the Yubikey publicId (default: empty) */
    public static final String OPTION_YUBICO_LDAP_PUBLICID_ATTRIBUTE = "ldap_publicid_attribute";
    /** Name of the LDAP attribute containing the username (default: "uid") */
    public static final String OPTION_YUBICO_LDAP_USERNAME_ATTRIBUTE = "ldap_username_attribute";
    /** URL of the LDAP directory (default: empty) */
    public static final String OPTION_LDAP_URL = "ldap_url";
    /** Base DN for LDAP searches (default: empty) */
    public static final String OPTION_LDAP_BASE_DN = "ldap_base_dn";
    /** DN used to log into the LDAP directory (default: empty) */
    public static final String OPTION_LDAP_BIND_DN = "ldap_bind_dn";
    /** Password for the bind DN (default: empty) */
    public static final String OPTION_LDAP_BIND_CREDENTIAL = "ldap_bind_credential";
    private boolean verify_yubikey_owner = true;
    private String publicid_attribute = "";
    private String username_attribute = "uid";
    private String ldap_url = "";
    private String ldap_base_dn = "";
    private String ldap_bind_dn = "";
    private String ldap_bind_credential = "";
    private Ldap ldap;
    private final Logger log = LoggerFactory.getLogger(YubikeyToUserLDAPMap.class);

    /** {@inheritDoc} */
    public boolean is_right_user(String username, String publicId) {
        log.trace("In is_right_user()");
        if (!this.verify_yubikey_owner) {
            log.debug("YubiKey owner verification disabled, returning 'true'");
            return true;
        }
        String ykuser = null;
        try {
            SearchFilter filter = new SearchFilter("({0}={1})", new String[]{this.publicid_attribute, publicId});
            log.debug("Searching for YubiKey publicId with filter: {}", filter.toString());
            Iterator<SearchResult> results = ldap.search(filter, new String[]{this.username_attribute});
            if (results.hasNext()) {
                Attributes results_attributes = results.next().getAttributes();
                log.debug("Found attributes: {}", results_attributes.toString());
                ykuser = results_attributes.get(this.username_attribute).get().toString();
            } else {
                log.debug("No search results");
            }
        } catch (NamingException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
        if (ykuser != null) {
            if (!ykuser.equals(username)) {
                log.info("YubiKey " + publicId + " registered to user {}, NOT {}", ykuser, username);
                return false;
            } else {
                log.info("YubiKey " + publicId + " registered to user {}", ykuser);
                return true;
            }
        } else {
            log.info("No record of YubiKey {} found. Returning 'false'.", publicId);
            return false;
        }
    }

    /** {@inheritDoc} */
    public final void setOptions(Map<String, ?> options) {
        /* Is verification of YubiKey owners enabled? */
        this.verify_yubikey_owner = true;
        if (options.get(OPTION_YUBICO_VERIFY_YK_OWNER) != null) {
            if ("false".equals(options.get(OPTION_YUBICO_VERIFY_YK_OWNER).toString())) {
                this.verify_yubikey_owner = false;
            }
        }
        if (options.get(OPTION_YUBICO_LDAP_PUBLICID_ATTRIBUTE) != null) {
            this.publicid_attribute = options.get(OPTION_YUBICO_LDAP_PUBLICID_ATTRIBUTE).toString();
        }
        if (options.get(OPTION_YUBICO_LDAP_USERNAME_ATTRIBUTE) != null) {
            this.username_attribute = options.get(OPTION_YUBICO_LDAP_USERNAME_ATTRIBUTE).toString();
        }
        if (options.get(OPTION_LDAP_URL) != null) {
            this.ldap_url = options.get(OPTION_LDAP_URL).toString();
        }
        if (options.get(OPTION_LDAP_BASE_DN) != null) {
            this.ldap_base_dn = options.get(OPTION_LDAP_BASE_DN).toString();
        }
        if (options.get(OPTION_LDAP_BIND_DN) != null) {
            this.ldap_bind_dn = options.get(OPTION_LDAP_BIND_DN).toString();
        }
        if (options.get(OPTION_LDAP_BIND_CREDENTIAL) != null) {
            this.ldap_bind_credential = options.get(OPTION_LDAP_BIND_CREDENTIAL).toString();
        }
        LdapConfig config = new LdapConfig(this.ldap_url, this.ldap_base_dn);
        config.setBindDn(this.ldap_bind_dn);
        config.setBindCredential(this.ldap_bind_credential);
        this.ldap = new Ldap(config);
    }
}
