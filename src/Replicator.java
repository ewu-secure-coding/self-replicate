import java.io.PrintWriter;
import java.util.UUID;

public abstract class Replicator {

    protected String currentDir = System.getProperty("user.dir");
    protected String newDir = currentDir + "/self-replicate-" + UUID.randomUUID();
    protected String os = System.getProperty("os.name");
    protected boolean infiniteFlag, onlyDecompileFlag, onlyCompileFlag,
            deleteParentFlag, cleanupFlag, showChildOutputFlag;

    public static Replicator getNewInstance() {
        if (System.getProperty("os.name").contains("Windows")) {
            System.out.println("Using Windows based replicator...");
            return new WindowsBasedReplicator();
        }
        System.out.println("Using Unix based replicator...");
        return new UnixBasedReplicator();
    }

    protected void parseFlags(String flags) {
        System.out.println("Parsing flags...");

        infiniteFlag = onlyDecompileFlag = onlyCompileFlag =
                deleteParentFlag = cleanupFlag = showChildOutputFlag = false;

        if (flags.contains("i")) infiniteFlag = true;
        if (flags.contains("d") && !flags.contains("c")) onlyDecompileFlag = true;
        if (flags.contains("c") && !flags.contains("d")) onlyCompileFlag = true;
        if (flags.contains("p")) deleteParentFlag = true;
        if (flags.contains("x")) cleanupFlag = true;
        if (flags.contains("s")) showChildOutputFlag = true;
    }

    protected String jadUrl() {
        if (os.contains("Windows")) return "http://varaneckas.com/jad/jad158g.win.zip";
        else if (os.contains("Mac")) return "http://varaneckas.com/jad/jad158g.mac.intel.zip";
        else if (os.contains("Linux")) return "http://varaneckas.com/jad/jad158e.linux.static.zip";
        else return null;
    }

    protected void createManifest(String srcFolder) throws Exception {
        PrintWriter writer = new PrintWriter(srcFolder + "/classes/manifest.txt");
        writer.println("Manifest-Version: 1.0");
        writer.println("Main-Class: Main");
        writer.println("");
        writer.flush();
        writer.close();
    }

    public abstract void selfReplicate(int counter, String flags, String parentLocation) throws Exception;

    protected abstract void fetchDecompiler() throws Exception;

    protected abstract void unjarSelf() throws Exception;

    protected abstract void decompileClasses(String classesFolder) throws Exception;

    protected abstract void compileNewSource() throws Exception;

    protected abstract void cleanup(String parentLocation) throws Exception;

    protected abstract void startReplicate(int counter, String flags) throws Exception;

}
