
package com.mi.milink.sdk.account.manager;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RSAPublicKey {
	
	public static String key2 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDp9WJz0gTAQP5oneVMs+ubmBDn"
             + "7uFbNJOetNH19Ze+4EyYxyg7zPr9rGlWuNmiQzd7KeHB6uOOBTOIYtRl0J/z4fx5"
             + "aPejleHfJEd8urjkMCEReDTLKrFa0IeBHRqy8FyGjMtVQJUw9V30Jvy7WFFB07rT"
             + "w1Lt2mJsQOpPt4CjPwIDAQAB";

	public static String key3 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQD+qNU/W2iWBi5APoJ9nOSgD1IF"
             + "CI18OQ6ksWDqjWK0GIpOU0wapEa9cVKbbhDkGwpX5I5JEuHygPsAEMWLRF6zr9h5"
             + "RqdOjISlaeAU4nwsd4dJRNHeHON5COkGo38Eu9PJSGzOed7sjCC7XxCI+E2N7hia"
             + "FRQlF2obHQch6Cnb9wIDAQAB";

	public static  String key4 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCm4DNek5oJVR0AkGNG1A3WMAcS"
             + "f2ifKI6Ythj1SYeeL0CZhxRn2FBQtGlxRZRDq0FFws0VlzZpo4xatJpEJh2CBpoo"
             + "MK2VJYQ3Erm8eyBw2vQEJ6dhOZ8UqRSMwaKT4g6IEp8TqtArjbFaOcvKLq4APanv"
             + "1OXTi/Xo5Px3T84gUQIDAQAB";
	
	public Set<PublicKeyAndId> getPublicKeySet() {
		Set<PublicKeyAndId> set = new HashSet<RSAPublicKey.PublicKeyAndId>();

		PublicKeyAndId pki2 = new PublicKeyAndId("2", key2);
		PublicKeyAndId pki3 = new PublicKeyAndId("3", key3);
		PublicKeyAndId pki4 = new PublicKeyAndId("4", key4);

		set.add(pki2);
		set.add(pki3);
		set.add(pki4);
		return set;
	}
	
    public static PublicKeyAndId getPublicKeyAndId() {
    	
    	
        Random r = new Random();
       
        String publicKeyArray[] = new String[] {
                key2, key3, key4
        };

        int t = r.nextInt(publicKeyArray.length);
        String key = publicKeyArray[t];
        String id = String.valueOf(t + 2);
        return new PublicKeyAndId(id, key);
    }

    public static class PublicKeyAndId {
        public String id;

        public String key;

        public PublicKeyAndId(String id, String key) {
            this.id = id;
            this.key = key;
        }

    }
}
