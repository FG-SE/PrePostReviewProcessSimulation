package de.unihannover.se.processSimulation.interactive;

public class ServerMain {

//    public static void main(final String[] args) throws IOException {
//        final Properties settings = loadSettings(args[0]);
//
//        final List<Contestant> contestants = Contestant.loadFromCsv(settings);
//        final Writer redo = new FileWriter("redo." + System.currentTimeMillis() + ".log");
//        final Tournament t = new Tournament(contestants, redo);
//
//        final HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
//        server.createContext("/doubleOut", new Handler(t, Integer.parseInt(settings.getProperty("breakLimit"))));
//        server.setExecutor(null);
//        server.start();
//        System.out.println("Server started...");
//    }
//
//    private static Properties loadSettings(final String filename) throws IOException {
//        try (FileInputStream is = new FileInputStream(filename)) {
//            final Properties settings = new Properties();
//            settings.load(is);
//            return settings;
//        }
//    }

}
