public class Main {
    public static void main(String [] args) throws Exception {
        System.out.println("Starting application...");
        if (args.length == 0) {
            System.out.println("No args found, terminating application...");
            System.exit(1);
        }

        int counter = -1;

        String parentLocation = null, flags = "";

        for (String arg : args) {
            if (isNumeric(arg)) {
                counter = Integer.parseInt(arg);
            }

            if (arg.contains("-") && !arg.contains("/")) {
                flags = arg;
            }

            if (arg.contains("self-replicate.jar")) {
                parentLocation = arg;
            }
        }

        if (counter == -1 && !flags.contains("i")) {
            System.out.println("Please use number to specify how many iterations or use -i for infinite iterations");
            System.exit(1);
        }

        if (counter != -1 && flags.contains("i")) {
            System.out.println("Cannot determine whether to use specified number or -i flag for iteration count");
            System.exit(1);
        }

        //new OldReplicator().selfReplicate(counter - 1, parentLocation, flags);
        Replicator.getNewInstance().selfReplicate(counter - 1, flags, parentLocation);
    }

    public static boolean isNumeric(String s) {
        return s.matches("[-+]?\\d*\\.?\\d+");
    }
}
