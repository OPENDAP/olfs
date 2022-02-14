package opendap.auth;

public class AuthorizationHeader {

    public static final String BASIC_SCHEME = "Basic";
    public static final String BEARER_SCHEME = "Bearer";
    public static final String DIGEST_SCHEME = "Digest";
    public static final String HOBA_SCHEME = "HOBA";
    public static final String MUTUAL_SCHEME = "Mutual";
    public static final String NEGOTIATE_NTLM_SCHEME = "Negotiate/NTLM";
    public static final String VAPID_SCHEME = "VAPID";
    public static final String SCRAM_SCHEME = "SCRAM";
    public static final String AWS4_HMAC_SHA256_SCHEME = "AWS4-HMAC-SHA256";

    // public enum scheme { Basic, Bearer, Digest, HOBA, Mutual, Negotiate_NTLM, VAPID, SCRAM, AWS4_HMAC_SHA256 };



    public static boolean checkAuthorizationHeader(String scheme, String auth_header_value){
        boolean retVal = false;
        if(auth_header_value!=null) {
            String[] tokens = auth_header_value.split(" ");
            if(tokens.length == 2){
                retVal = tokens[0].equalsIgnoreCase(scheme);
            }
        }
        return retVal;
    }

    public static boolean isBasic(String auth_header){
        return checkAuthorizationHeader(BASIC_SCHEME,auth_header);
    }
    public static boolean isBearer(String auth_header){
        return checkAuthorizationHeader(BEARER_SCHEME,auth_header);
    }
    public static boolean isDigest(String auth_header){
        return checkAuthorizationHeader(DIGEST_SCHEME,auth_header);
    }
    public static boolean isHOBA(String auth_header){
        return checkAuthorizationHeader(HOBA_SCHEME,auth_header);
    }
    public static boolean isMutual(String auth_header){
        return checkAuthorizationHeader(MUTUAL_SCHEME,auth_header);
    }
    public static boolean isNegotiateNTLM(String auth_header){
        return checkAuthorizationHeader(NEGOTIATE_NTLM_SCHEME,auth_header);
    }
    public static boolean isVAPID(String auth_header){
        return checkAuthorizationHeader(VAPID_SCHEME,auth_header);
    }
    public static boolean isSCRAM(String auth_header){
        return checkAuthorizationHeader(SCRAM_SCHEME,auth_header);
    }

    public static boolean isAWS4_HMAC_SHA256(String auth_header){
        return checkAuthorizationHeader(AWS4_HMAC_SHA256_SCHEME,auth_header);
    }


}
