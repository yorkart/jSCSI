package org.jscsi.target;

import org.jscsi.target.context.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author wangyue1
 * @since 2019/9/20
 */
public class TargetApplication {

    /**
     * Starts the jSCSI target.
     *
     * @param args all command line arguments are ignored
     * @throws IOException
     */
    public static void main(String[] args) throws Exception {
        TargetServer target;

        System.out.println("This system provides more than one IP Address to advertise.\n");

        Enumeration<NetworkInterface> interfaceEnum = NetworkInterface.getNetworkInterfaces();
        NetworkInterface i;
        int addressCounter = 0;
        List<InetAddress> addresses = new ArrayList<>();
        while (interfaceEnum.hasMoreElements()) {
            i = interfaceEnum.nextElement();
            Enumeration<InetAddress> addressEnum = i.getInetAddresses();
            InetAddress address;

            while (addressEnum.hasMoreElements()) {
                address = addressEnum.nextElement();
                System.out.println("[" + addressCounter + "] " + address.getHostAddress());
                addresses.add(address);
                addressCounter++;
            }
        }

        /*
         * Getting the desired address from the command line. You can't automatically make sure to always use the
         * correct host address.
         */
        System.out.print("\nWhich one should be used?\nType in the number: ");
        Integer chosenIndex = null;

        while (chosenIndex == null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String line = br.readLine();
            try {
                chosenIndex = Integer.parseInt(line);
            } catch (NumberFormatException nfe) {
                chosenIndex = null;
            }
        }

        String targetAddress = addresses.get(chosenIndex).getHostAddress();
        System.out.println("Using ip address " + addresses.get(chosenIndex).getHostAddress());


        switch (args.length) {
            case 0:
                target = new TargetServer(Configuration.create(targetAddress));
                break;
            case 1:

                // Checking if the schema file is at the default location
                target = new TargetServer(
                        Configuration.create(Configuration.CONFIGURATION_SCHEMA_FILE.exists() ?
                                        new FileInputStream(Configuration.CONFIGURATION_SCHEMA_FILE) :
                                        TargetServer.class.getResourceAsStream("/jscsi-target.xsd"),
                                new FileInputStream(args[0]), targetAddress));
                break;
            case 2:
                target = new TargetServer(Configuration.create(new File(args[0]), new File(args[1]), targetAddress));
                break;
            default:
                throw new IllegalArgumentException("Only zero or one Parameter (Path to Configuration-File) allowed!");
        }

        target.call();
    }

}
