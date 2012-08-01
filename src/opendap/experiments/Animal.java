package opendap.experiments;


/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/29/11
 * Time: 5:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class Animal {
    public static void staticMethod() {
        System.out.println("The staticMethod in Animal.");
    }
    public void testInstanceMethod() {
        System.out.println("The instance method in Animal.");
    }



    public static void main(String[] args) {

        String s = "///////";
            // Condition applicationID.
            if (s != null)
            {
                while (s.startsWith("/")) { // Strip leading slashes
                    s = s.substring(1, s.length());
                    System.out.println("s="+s);

                }
                if (s.equals("")){
                    System.out.println("s is empty string so we are making it null.");
                    s = null;
                }
            }

            if(s == null){
                System.out.println("s is now null.");
                return;
            }


    }



}

