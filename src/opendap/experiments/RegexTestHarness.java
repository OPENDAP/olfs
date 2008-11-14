


package opendap.experiments;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class RegexTestHarness {


    public static void main(String[] args) throws Exception {
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));

        String pString = null;
        String input = null;
        String k;

        while (true) {

            System.out.print("\nEnter your regex ["+pString+"]: ");
            k = kybrd.readLine();
            if(!k.equals(""))
                pString = k;
            Pattern pattern =
            Pattern.compile(pString);

            System.out.print("Enter input string to search["+input+"]: ");
            k = kybrd.readLine();
            if(!k.equals(""))
                input = k;
            Matcher matcher =
            pattern.matcher(input);

            boolean found = false;
            while (matcher.find()) {
                System.out.println("matcher.find() found the text \""+matcher.group()+"\" starting at " +
                   "index "+matcher.start()+" and ending at index "+matcher.end());
                found = true;
            }

            System.out.println("pattern.matcher("+input+").matches(): "+pattern.matcher(input).matches());



            if(!found){
                System.out.println("No match found.");
            }
        }
    }
}
