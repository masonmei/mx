import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

/**
 * Created by mason on 7/23/15.
 */
public class Test {
    public static void main(String[] args) {
        Tomcat tomcat = new Tomcat();
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
    }
}
