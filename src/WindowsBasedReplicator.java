import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;

public class WindowsBasedReplicator extends Replicator {
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

        Files.createDirectory(new File(currentDir + "\\jadFiles").toPath());

        ProcessBuilder pb = new ProcessBuilder("jar", "-xf", "..\\jadZip.zip");
        pb = pb.directory(new File(currentDir + "\\jadFiles"));
        Process p = pb.start();
        p.waitFor();

        Files.copy(new File(currentDir + "\\jadFiles\\jad.exe").toPath(), new File(newDir + "\\jad.exe").toPath(), StandardCopyOption.REPLACE_EXISTING);

        File toDelete = new File(currentDir + "\\jadZip.zip");
        toDelete.delete();

        removeTree(currentDir + "\\jadFiles");
    }

    @Override
    protected void unjarSelf() throws Exception {
        System.out.println("Unjarring self...");
        String classesFolder = newDir + "\\classes";

        File classes = new File(classesFolder);
        classes.mkdir();

        ProcessBuilder pb = new ProcessBuilder("jar", "-xf", "..\\..\\self-replicate.jar");
        pb = pb.directory(new File(classesFolder));
        Process p = pb.start();
        p.waitFor();

        decompileClasses(classesFolder);
    }

    @Override
    protected void decompileClasses(String classesFolder) throws Exception {
        System.out.println("Decompiling classes...");
        File dir = new File(newDir + "..\\src");
        dir.mkdir();

        ProcessBuilder pb = new ProcessBuilder("jad.exe", "-o", "-dsrc", "-sjava", "classes\\*.class");
        pb = pb.directory(new File(newDir));
        Process p = pb.start();
        p.waitFor();
    }

    @Override
    protected void compileNewSource() throws Exception {
        System.out.println("Compile java...");
        String srcFolder = newDir + "\\src";
        File classesFolder = new File(srcFolder + "\\classes");
        classesFolder.mkdir();

        ProcessBuilder pb = new ProcessBuilder("javac", "-d", "classes", "Main.java", "Replicator.java",
                "UnixBasedReplicator.java", "WindowsBasedReplicator.java");
        pb = pb.directory(new File(srcFolder));
        Process p = pb.start();
        p.waitFor();

        pb = new ProcessBuilder("java", "-cp", "./classes;.", "classes\\*.class");
        pb = pb.directory(new File(srcFolder));
        p = pb.start();
        p.waitFor();

        createManifest(srcFolder);

        pb = new ProcessBuilder("jar", "-cfm", "..\\..\\self-replicate.jar", "manifest.txt", "Main.class",
                "Replicator.class", "UnixBasedReplicator.class", "WindowsBasedReplicator.class");
        pb = pb.directory(new File(srcFolder + "\\classes"));
        p = pb.start();
        p.waitFor();
    }

    @Override
    protected void cleanup(String parentLocation) throws Exception {
        removeTree(newDir + "\\classes");
        removeTree(newDir + "\\src");
        Files.delete(new File(newDir + "\\src").toPath());
        Files.delete(new File(newDir + "\\jad.exe").toPath());
        if (parentLocation != null) {
            System.out.println("Deleting parent...");
            Files.delete(new File(parentLocation).toPath());
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

    private void removeTree(String dir) {
        File root = new File(dir);
        String [] children = root.list();
        for (String child : children) {
            File childNode = new File(child);
            if (childNode.isDirectory()) {
                removeTree(child);
            }
            childNode.delete();
        }
        root.delete();
    }
}
