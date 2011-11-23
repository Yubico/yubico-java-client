/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.yubico.jaas;

import com.sun.appserv.security.AppservPasswordLoginModule;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.YubicoResponse;
import com.yubico.client.v2.YubicoResponseStatus;
import com.yubico.jaas.impl.YubikeyToUserMapImpl;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;

/**
 *
 * @author duncan
 */
public class YubikeyGFLoginModule extends AppservPasswordLoginModule {

    private static final String ROLES_FILE = "rolesfile";
    
    private YubikeyToUserMapImpl ykmap;
    private YubicoClient yc;
    private String publicId;
        
    @Override
    public void authenticateUser() throws LoginException {
        if (_passwd.length < 32) {
            Logger.getLogger(YubikeyGFLoginModule.class.getName()).log(Level.INFO, "Skipping token, not a valid YubiKey OTP (too short, {0} < 32)", _passwd.length);
            throw new LoginException("YubiKey OTP authentication failed - no OTPs supplied");
        }

        ykmap = new YubikeyToUserMapImpl();
        ykmap.setOptions(_options);
                
        if (validate_otps(_passwd, _username)) {
            String[] grpList = getGroupList(_username, publicId);
            commitUserAuthentication(grpList);
            return ;
        }

        throw new LoginException("YubiKey OTP authentication failed");        
    }

    private boolean validate_otps(char[] otp, String _username) {
        boolean validated = false;

        String password = String.valueOf(otp);
        Logger.getLogger(YubikeyGFLoginModule.class.getName()).log(Level.FINE, "Checking OTP {0}", password);

        yc = YubicoClient.getClient(4711);
        YubicoResponse ykr = this.yc.verify(password);
        
        if (ykr != null) {
            Logger.getLogger(YubikeyGFLoginModule.class.getName()).log(Level.FINE, "OTP {0} verify result : {1}", new Object[]{password, ykr.getStatus().toString()});
            if (ykr.getStatus() == YubicoResponseStatus.OK) {
                publicId = YubicoClient.getPublicId(password);
                Logger.getLogger(YubikeyGFLoginModule.class.getName()).log(Level.INFO, "OTP verified successfully (YubiKey client {0})", publicId);
                validated = true;
            } else {
                Logger.getLogger(YubikeyGFLoginModule.class.getName()).log(Level.FINER, "OTP validation returned {0}", ykr.getStatus().toString());
            }
        } else {
            Logger.getLogger(YubikeyGFLoginModule.class.getName()).log(Level.WARNING, "null YubicoResponse");
        }

        return validated;        
    }

    private String[] getGroupList(String _username, String publicId) throws LoginException {
        Logger.getLogger(YubikeyGFLoginModule.class.getName()).log(Level.FINE, "Check if YubiKey {0} belongs to user {1}", new Object[]{publicId, _username});
        FileRealm fileRealm;
        try {
            fileRealm = new FileRealm(_options.get(ROLES_FILE).toString());
            String[] roles = fileRealm.authenticate(_username, publicId.toCharArray());
            if (roles == null){
                throw new LoginException("YubiKey " + publicId + " NOT registered to user "+ _username);
            }
            return roles;
        } catch (BadRealmException ex) {
            Logger.getLogger(YubikeyGFLoginModule.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchRealmException ex) {
            Logger.getLogger(YubikeyGFLoginModule.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new String[]{};
    }
    
}
