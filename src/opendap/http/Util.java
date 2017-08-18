package opendap.http;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.io.File;
import java.io.IOException;


public class Util {

    static public CredentialsProvider  getCredentials() throws IOException{
        String default_file=".netrc";
        String home = System.getProperty("user.home");

        if(home!=null)
            default_file = home + "/" +default_file;

        return getCredentials(default_file, true);

    }

    static public CredentialsProvider  getCredentials(String filename, boolean secure_transport) throws IOException {


        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        NetRC netRC = new NetRC(filename);

        for( NetRC.NetRCEntry entry: netRC.getEntries()){
            String userId = entry.login;
            String pword = String.valueOf(entry.password);

            credsProvider.setCredentials(
                    new AuthScope(entry.machine, secure_transport?443:80),
                    new UsernamePasswordCredentials(userId, pword));

        }
        return credsProvider;
    }




}
