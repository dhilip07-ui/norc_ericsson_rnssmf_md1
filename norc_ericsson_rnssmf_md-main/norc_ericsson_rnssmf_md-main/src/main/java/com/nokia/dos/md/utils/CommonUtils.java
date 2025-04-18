package com.nokia.dos.md.utils;

import java.util.Set;

import com.nokia.fo.nero.CertificateStore;
import com.nokia.fo.nero.Connection;

/**
 * Common utilities used by mechanism driver
 **/
public class CommonUtils {
    
    /**
     * toInterface store truststore
     * fromInterface store keystore
     **/
    public static Set<CertificateStore> getCertificateStore(Connection connection){
        Set<CertificateStore> cs = null;
        
        if (connection.getToNeInterface() != null && connection.getToNeInterface().getCertificateStores() != null) {
            cs = connection.getToNeInterface().getCertificateStores();
        }
        if (connection.getFromNeInterface() != null && connection.getFromNeInterface().getCertificateStores() != null) {
            if(cs == null)
                cs = connection.getFromNeInterface().getCertificateStores();
            else
                cs.addAll(connection.getFromNeInterface().getCertificateStores());
        }
        return cs;
    }

    public static boolean checkNullOrEmpty(String inputString) {
        return (inputString == null) || (("").equals(inputString));
    }
}
