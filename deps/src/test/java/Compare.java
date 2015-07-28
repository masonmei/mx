import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

/**
 * Created by mason on 7/28/15.
 */
public class Compare {
    public static void main(String[] args) throws IOException {
        String comparePath = "/Users/mason/Desktop/mx/deps/src/main/java/com/newrelic/deps";
        String toPath = "/Users/mason/Desktop/newrelic.src/com/newrelic/agent/deps";

        List<String> compareSet = loadFiles(comparePath);
        List<String> toSet = loadFiles(toPath);
        Collections.sort(compareSet);
        Collections.sort(toSet);

        System.out.println("more than +++++++++++++++++");
        for (String str : compareSet) {
            if(!toSet.contains(str)){
                System.out.println(str);
            }
        }
        System.out.println("less than +++++++++++++++++");
        for (String str : toSet) {
            if(!compareSet.contains(str)){
                System.out.println(str);
            }
        }
    }

    private static List<String> loadFiles(String path) throws IOException {
        Collection<File> files = FileUtils.listFiles(new File(path), new String[] {"java"}, true);
        Set<String> fileNames = new HashSet<String>();
        for (File file : files) {
            fileNames.add(file.getCanonicalPath().substring(path.length()));
        }
        return new ArrayList<String>(fileNames);
    }
}
