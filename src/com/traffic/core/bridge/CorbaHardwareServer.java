package com.traffic.core.bridge;

import TrafficLegacy.TrafficControllerPOA;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import java.io.PrintWriter;
import java.util.Properties;

// 1. The Implementation (What the hardware actually does)
class TrafficControllerImpl extends TrafficControllerPOA {
    @Override
    public String setSignalState(String junctionId, int state) {
        String color = (state == 1) ? "GREEN" : "RED";
        System.out.println(">>> [CORBA HARDWARE] Junction " + junctionId + " switched to " + color);
        return "ACK: " + junctionId + " is now " + color;
    }
}

// 2. The Server (Boots up the CORBA ORB)
public class CorbaHardwareServer {
    public static void main(String[] args) {
        try {
            System.out.println(">>> [LEGACY] Starting Hardware Emulation (CORBA/Java)...");

            // Configure ORB on Port 1050
            Properties props = new Properties();
            props.put("org.omg.CORBA.ORBInitialPort", "1050");
            props.put("org.omg.CORBA.ORBInitialHost", "localhost");

            // Initialize ORB
            ORB orb = ORB.init(args, props);
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            // Create Service
            TrafficControllerImpl service = new TrafficControllerImpl();

            // Generate IOR (The "Contact Card" for the bridge)
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(service);
            String ior = orb.object_to_string(ref);

            System.out.println(">>> [LEGACY] Server Ready.");
            System.out.println(">>> [LEGACY] IOR: " + ior);

            // Save IOR to file so the Bridge can find us
            try (PrintWriter out = new PrintWriter("hardware.ior")) {
                out.println(ior);
            }
            System.out.println(">>> [LEGACY] 'hardware.ior' file written.");

            orb.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}