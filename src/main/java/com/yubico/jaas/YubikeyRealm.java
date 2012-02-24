/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.yubico.jaas;

import com.sun.appserv.security.AppservRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author duncan
 */
public class YubikeyRealm extends AppservRealm{

    private String jaasCtxName;
    private String startWith;
    
    @Override
    protected void init(Properties props) throws BadRealmException, NoSuchRealmException {
        jaasCtxName = props.getProperty("jaas-context", "yubikeyRealm");
        startWith = props.getProperty("startWith", "z");
    }

    @Override
    public String getJAASContext() { 
        return jaasCtxName;
    }
    
    @Override
    public String getAuthType() {
        return "Yubikey Realm"; 
    }

    @Override
    public Enumeration getGroupNames(String string) throws InvalidOperationException, NoSuchUserException {
        List groupNames = new LinkedList(); 
        return (Enumeration) groupNames;
    }
    
    public String getStartWith() { 
        return startWith; 
    }
    
}