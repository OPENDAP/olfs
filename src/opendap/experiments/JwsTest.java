package opendap.experiments;

import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 9, 2010
 * Time: 4:46:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class JwsTest {


    private Logger log;


    JwsTest(){

        log = org.slf4j.LoggerFactory.getLogger(getClass());

    }

    public static void main(String[] args) {

        JwsTest jt = new JwsTest();

        jt.go(args);
        

    }


    private void go(String[] args){


        String msg = "";
        int i = 0;
        for(String s : args){
            log.debug("argument["+i++ +"]: "+s);
            msg += "arg["+i+"]: '"+s +"'  ";
        }

        JFrame frame = new JFrame("JwsTest");


        ClassLoader cl = this.getClass().getClassLoader();


        ImageIcon logoImage  = new ImageIcon(cl.getResource("images/logo.gif"));

        ImagePanel panel = new ImagePanel(logoImage.getImage());

        frame.getContentPane().add(panel);

        Dimension size = new Dimension(panel.getPreferredSize());

        size.setSize(size.getWidth()*4,size.getHeight()*4);
        frame.setPreferredSize(size);


        
        final JLabel label = new JLabel(msg);


        frame.getContentPane().add(label);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);





    }


    class ImagePanel extends JPanel {

      private Image img;

      public ImagePanel(String img) {
        this(new ImageIcon(img).getImage());
      }

      public ImagePanel(Image img) {
        this.img = img;
        Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setSize(size);
        setLayout(null);
      }

      public void paintComponent(Graphics g) {
        g.drawImage(img, 0, 0, null);
      }

    }


    

}
