import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class UnixBasedReplicator extends Replicator {
    @Override
    public void selfReplicate(int counter, String flags, String parentLocation) throws Exception {
        System.out.println("Self Replicating...");

        File dir = new File(newDir);
        dir.mkdir();

        parseFlags(flags);
        fetchDecompiler();
        unjarSelf();
        if (onlyDecompileFlag) {
            System.out.println("Halting program...");
            System.exit(0);
        }
        compileNewSource();
        if (onlyCompileFlag) {
            if (cleanupFlag) cleanup(null);
            System.out.println("Halting program...");
            System.exit(0);
        }
        if (cleanupFlag) cleanup(parentLocation);
        if (counter > 0)
            startReplicate(counter, flags);
    }

    @Override
    protected void fetchDecompiler() throws Exception {
        System.out.println("Fetching decompiler...");

        URL jadZip = new URL(jadUrl());
        ReadableByteChannel rbc = Channels.newChannel(jadZip.openStream());
        FileOutputStream fos = new FileOutputStream("jadZip.zip");
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

        String [] cmds =
                {"unzip jadZip.zip -d jadFiles", "mv jadFiles/jad jad", "rm -rf jadFiles", "rm jadZip.zip"};
        for (String cmd : cmds) {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
        }

        Process p = new ProcessBuilder("mv", "jad", newDir + "/jad").start();
        p.waitFor();
    }

    @Override
    protected void unjarSelf() throws Exception {
        System.out.println("Unjarring self...");
        String classesFolder = newDir + "/classes";

        Process p = new ProcessBuilder("mkdir", classesFolder).start();
        p.waitFor();

        ProcessBuilder pb = new ProcessBuilder("jar", "-xf", "../../self-replicate.jar");
        pb = pb.directory(new File(classesFolder));
        p = pb.start();
        p.waitFor();

        decompileClasses(classesFolder);
    }

    @Override
    protected void decompileClasses(String classesFolder) throws Exception {
        System.out.println("Decompiling classes...");

        ProcessBuilder pb = new ProcessBuilder("mkdir", classesFolder + "/src");
        Process p = pb.start();
        p.waitFor();

        pb = new ProcessBuilder("../jad", "-o", "-dsrc", "-sjava", "*.class");
        pb = pb.directory(new File(classesFolder));
        p = pb.start();
        p.waitFor();

        pb = new ProcessBuilder("mv", "src", "../src");
        pb = pb.directory(new File(classesFolder));
        p = pb.start();
        p.waitFor();
    }

    @Override
    protected void compileNewSource() throws Exception {
        System.out.println("Compile java...");
        String srcFolder = newDir + "/src";
        ProcessBuilder pb = new ProcessBuilder("mkdir", "classes");
        pb = pb.directory(new File(srcFolder));
        Process p = pb.start();
        p.waitFor();

//        pb = new ProcessBuilder("javac", "-d", "classes", "Main.java", "Replicator.java",
//                "UnixBasedReplicator.java", "WindowsBasedReplicator.java");
//        pb = pb.directory(new File(srcFolder));
//        p = pb.start();
//        p.waitFor();

        pb = new ProcessBuilder("javac", "-d", "classes", "Main.java", "Replicator.java",
                "UnixBasedReplicator.java", "WindowsBasedReplicator.java");
        pb = pb.directory(new File(srcFolder));
        p = pb.start();
        p.waitFor();

        pb = new ProcessBuilder("java", "-cp", "./classes:.", "classes/*.class");
        pb = pb.directory(new File(srcFolder));
        p = pb.start();
        p.waitFor();

        createManifest(srcFolder);

        pb = new ProcessBuilder("jar", "-cfm", "../../self-replicate.jar", "manifest.txt", "Main.class",
                "Replicator.class", "UnixBasedReplicator.class", "WindowsBasedReplicator.class");
        pb = pb.directory(new File(srcFolder + "/classes"));
        p = pb.start();
        p.waitFor();
    }

    @Override
    protected void cleanup(String parentLocation) throws Exception {
        System.out.println("Cleaning up...");

        ProcessBuilder pb = new ProcessBuilder("rm", "-rf", "classes");
        pb = pb.directory(new File(newDir));
        Process p = pb.start();
        p.waitFor();

        pb = new ProcessBuilder("rm", "-rf", "src");
        pb = pb.directory(new File(newDir));
        p = pb.start();
        p.waitFor();

        pb = new ProcessBuilder("rm", "jad");
        pb = pb.directory(new File(newDir));
        p = pb.start();
        p.waitFor();

        if (parentLocation != null) {
            System.out.println("Deleting parent...");
            pb = new ProcessBuilder("rm", parentLocation);
            p = pb.start();
            p.waitFor();
        }
    }

    @Override
    protected void startReplicate(int counter, String flags) throws Exception {
        System.out.println("Starting replicate and terminating self...");

        String s = null;
        ProcessBuilder pb = null;
        if (counter != -1) {
            if (flags != null) {
                if (deleteParentFlag) {
                    s = "java -jar self-replicate.jar " + Integer.toString(counter) + " " +
                            flags + " " + currentDir + "/self-replicate.jar";
                    pb = new ProcessBuilder("java", "-jar", "self-replicate.jar",
                            Integer.toString(counter), flags, currentDir + "/self-replicate.jar");
                }
                else {
                    s = "java -jar self-replicate.jar " + Integer.toString(counter) + " " + flags;
                    pb = new ProcessBuilder("java", "-jar", "self-replicate.jar",
                            Integer.toString(counter), flags);
                }
            }
            else {
                s = "java -jar self-replicate.jar " + Integer.toString(counter);
                pb = new ProcessBuilder("java", "-jar", "self-replicate.jar", Integer.toString(counter));
            }
        }
        else {
            if (flags != null) {
                if (deleteParentFlag) {
                    s = "java -jar self-replicate.jar " + " " + flags + " " + currentDir + "/self-replicate.jar";
                    pb = new ProcessBuilder("java", "-jar", "self-replicate.jar",
                            flags, currentDir + "/self-replicate.jar");
                }
                else {
                    s = "java -jar self-replicate.jar " + " " + flags;
                    pb = new ProcessBuilder("java", "-jar", "self-replicate.jar", flags);
                }
            }
            else {
                s = "java -jar self-replicate.jar";
                pb = new ProcessBuilder("java", "-jar", "self-replicate.jar");
            }
        }
        //System.out.println("Starting process: " + s);
        pb = pb.directory(new File(newDir));
        Process p = pb.start();
        if (showChildOutputFlag) {
            s = null;
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        }
        System.exit(0);
    }
}
