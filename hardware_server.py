import sys
import omniORB
from omniORB import CORBA, PortableServer

# ==========================================
# PART 1: MANUAL COMPILATION
# ==========================================
print(">>> [PYTHON] Initializing Manual Stubs...")

TrafficLegacy = omniORB.openModule("TrafficLegacy", "TrafficLight.idl")
TrafficLegacy__POA = omniORB.openModule("TrafficLegacy__POA", "TrafficLight.idl")

repo_id = "IDL:TrafficLegacy/TrafficController:1.0"

class TrafficController(CORBA.Object):
    _NP_RepositoryId = repo_id

TrafficLegacy.TrafficController = TrafficController
TrafficLegacy._d_TrafficController = (omniORB.tcInternal.tv_objref, repo_id, "TrafficController")
omniORB.typeMapping[repo_id] = TrafficLegacy._d_TrafficController

TrafficController._d_setSignalState = (
    (omniORB.tcInternal.tv_string, omniORB.tcInternal.tv_long),
    (omniORB.tcInternal.tv_string,),
    None
)

class TrafficControllerPOA(PortableServer.Servant):
    _NP_RepositoryId = repo_id
    _NP_TypeInfo = TrafficLegacy._d_TrafficController

TrafficLegacy__POA.TrafficController = TrafficControllerPOA
print(">>> [PYTHON] Stubs Ready. Starting Server Logic...")

# ==========================================
# PART 2: SERVER IMPLEMENTATION
# ==========================================
class TrafficControllerImpl(TrafficControllerPOA):
    def setSignalState(self, junctionId, state):
        color = "GREEN" if state == 1 else "RED"
        print(f">>> [PYTHON HARDWARE] Junction {junctionId} -> Switched to {color}")
        return f"ACK_FROM_PYTHON: {junctionId} is now {color}"

# ==========================================
# PART 3: MAIN EXECUTION
# ==========================================
try:
    # --- FIX IS HERE: Correct flag name and format ---
    # We include sys.argv[0] (script name) just to be safe
    argv = [sys.argv[0], "-ORBendPoint", "giop:tcp:localhost:1050"]

    orb = CORBA.ORB_init(argv, CORBA.ORB_ID)

    poa = orb.resolve_initial_references("RootPOA")
    poaManager = poa._get_the_POAManager()
    poaManager.activate()

    service = TrafficControllerImpl()
    obj = service._this()

    ior = orb.object_to_string(obj)
    print(">>> [PYTHON] Hardware Server Running on Port 1050")
    print(f">>> [PYTHON] IOR: {ior}")

    with open("hardware.ior", "w") as f:
        f.write(ior)

    print(">>> [PYTHON] 'hardware.ior' written.")
    print(">>> [PYTHON] WAITING FOR JAVA REQUESTS...")

    orb.run()

except Exception as e:
    print(f"!!! [PYTHON ERROR] {e}")