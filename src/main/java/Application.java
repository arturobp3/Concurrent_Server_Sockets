import org.apache.commons.lang3.math.NumberUtils;

public class Application {

    public static void main(String[] args) {
        //TODO: Comentar todas las funciones del codigo

        Server server;
        if (args.length == NumberUtils.INTEGER_ZERO) {
            server = new Server();
            server.run();
        } else if (args.length == NumberUtils.INTEGER_TWO) {
            if (NumberUtils.isDigits(args[0]) && NumberUtils.isDigits(args[1])) {
                int port = Integer.parseInt(args[0]);
                int maxClients = Integer.parseInt(args[1]);
                server = new Server(port, maxClients);
                server.run();
            } else {
                throw new RuntimeException("One or both arguments are not a number");
            }
        } else {
            throw new RuntimeException("Only zero and two arguments are allowed");
        }
    }
}
