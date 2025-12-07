package com.traffic.core.bridge;

import TrafficLegacy.TrafficController;
import TrafficLegacy.TrafficControllerHelper;
import org.omg.CORBA.ORB;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

/**
 * RESTORED CORBA BRIDGE
 * Reads 'hardware.ior' and uses Java IDL to talk to the Hardware Server.
 */
public class LegacyTrafficLightSystem {

    private TrafficController corbaHardware;

    public LegacyTrafficLightSystem() {
        try {
            // Initialize local ORB (Client side)
            Properties props = new Properties();
            props.put("org.omg.CORBA.ORBInitialPort", "1050");
            props.put("org.omg.CORBA.ORBInitialHost", "localhost");
            ORB orb = ORB.init(new String[]{}, props);

            // Read the IOR (Object Reference) from file
            BufferedReader br = new BufferedReader(new FileReader("hardware.ior"));
            String ior = br.readLine();
            br.close();

            if (ior != null) {
                org.omg.CORBA.Object objRef = orb.string_to_object(ior);
                corbaHardware = TrafficControllerHelper.narrow(objRef);
                System.out.println("[BRIDGE] Connected to Legacy Hardware via CORBA.");
            }

        } catch (Exception e) {
            System.err.println("[BRIDGE ERROR] Could not connect to CORBA Hardware.");
            System.err.println("   -> Is 'CorbaHardwareServer' running?");
        }
    }

    public void sendSignal(String junctionId, String signalCode) {
        if (corbaHardware != null) {
            try {
                // Convert to CORBA int (1=Green, 0=Red)
                int state = signalCode.equals("0x01") ? 1 : 0;
                String response = corbaHardware.setSignalState(junctionId, state);
                System.out.println("[BRIDGE] Hardware Response: " + response);
            } catch (Exception e) {
                System.err.println("[BRIDGE] Error sending signal: " + e.getMessage());
            }
        } else {
            System.out.println("[BRIDGE] Hardware Offline. Signal ignored.");
        }
    }
}