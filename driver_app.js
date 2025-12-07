// driver_app.js
// --- CONFIGURATION ---
const API_URL = "http://localhost:8080/api";
const VEHICLE_ID = "DRIVER-MOBILE-" + Math.floor(Math.random() * 10000);

// Coordinates Database (Hardcoded start, must match Java RouteOptimizer names)
const CITIES = {
    "Tunis": [36.8065, 10.1815],
    "Ben Arous": [36.7531, 10.2188],
    "Hammamet": [36.4000, 10.6167],
    "Sousse": [35.8256, 10.6084],
    "Sfax": [34.7406, 10.7603],
    "Gabes": [33.8815, 10.0982],
    "Kairouan": [35.6781, 10.0963]
};

// --- STATE ---
let map, carMarker, routePolyline;
let isDriving = false;
let simulationInterval;
let currentRoute = [];
let currentLeg = 0;
let progress = 0;

// --- INITIALIZATION ---
function initMap() {
    map = L.map('map', {zoomControl: false}).setView(CITIES["Tunis"], 10);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '© OpenStreetMap'
    }).addTo(map);

    carMarker = L.marker(CITIES["Tunis"], {
        icon: L.divIcon({
            className: 'car-icon',
            html: '<div class="w-4 h-4 bg-cyan-400 rounded-full border-2 border-white shadow-xl"></div>',
            iconSize: [20, 20]
        })
    }).addTo(map);
}

// Heartbeat Loop (Checks server status every 5s)
setInterval(checkServer, 5000);
initMap();
checkServer();

async function checkServer() {
    try {
        const response = await fetch(API_URL + "/status?t=" + Date.now());
        if (response.ok) {
            document.getElementById('connection-status').innerText = "ONLINE";
            document.getElementById('connection-status').classList.remove('bg-red-600');
            document.getElementById('connection-status').classList.add('bg-green-600', 'text-black');
        } else {
            throw new Error("Server Error");
        }
    } catch (e) {
        document.getElementById('connection-status').innerText = "OFFLINE";
        document.getElementById('connection-status').classList.remove('bg-green-600', 'text-black');
        document.getElementById('connection-status').classList.add('bg-red-600');
    }
}

// --- CORE LOGIC ---
window.startMission = async function() {
    const dest = document.getElementById('destination-select').value;
    const start = "Tunis";

    document.getElementById('btn-start').innerText = "CALCULATING ROUTE...";
    document.getElementById('btn-start').disabled = true;

    try {
        // 1. Get Route from Backend
        const response = await fetch(`${API_URL}/route?from=${start}&to=${dest}`);
        const routeNames = await response.json();

        if(routeNames.length < 2 || routeNames.includes("Error")) {
            alert("Routing failed or destination unreachable.");
            resetUI();
            return;
        }

        // 2. Map Names to Coordinates
        currentRoute = routeNames.map(name => CITIES[name]).filter(c => c !== undefined);

        // 3. Draw on Map
        if(routePolyline) map.removeLayer(routePolyline);
        if (currentRoute.length > 0) {
            routePolyline = L.polyline(currentRoute, {color: '#f39c12', weight: 5, opacity: 0.8}).addTo(map);
            try { map.fitBounds(routePolyline.getBounds(), {padding: [50, 50]}); } catch(err) { console.warn("Bounds failed."); }
        } else {
            alert("No valid coordinate path found.");
            resetUI();
            return;
        }

        // 4. Start Simulation
        isDriving = true;
        currentLeg = 0;
        progress = 0;

        toggleUI(true);
        simulationInterval = setInterval(simulateStep, 1000);
    } catch (e) {
        alert("CRITICAL ERROR: Cannot reach RMI Gateway.");
        resetUI();
    }
}

function simulateStep() {
    if (!isDriving || currentLeg >= currentRoute.length - 1) {
        stopMission();
        return;
    }

    const startPt = currentRoute[currentLeg];
    const endPt = currentRoute[currentLeg + 1];

    progress += 0.1;

    // Interpolation (current position)
    const lat = startPt[0] + (endPt[0] - startPt[0]) * progress;
    const lng = startPt[1] + (endPt[1] - startPt[1]) * progress;

    const speed = Math.floor(70 + Math.random() * 50); // Speed 70 - 120 km/h

    // Update UI and Map
    carMarker.setLatLng([lat, lng]);
    map.panTo([lat, lng]);

    document.getElementById('speed-display').innerText = speed;
    document.getElementById('speed-display').classList.toggle('text-red-500', speed > 100);
    document.getElementById('speed-display').classList.toggle('alert-pulse', speed > 100);

    document.getElementById('dist-display').innerText = routeNames[currentLeg + 1] + " (" + Math.floor((1 - progress) * 10) + "km)";

    sendTelemetry(lat, lng, speed);

    if (progress >= 1.0) {
        currentLeg++;
        progress = 0;
        if (currentLeg < currentRoute.length - 1) {
            console.log(`Arrived at ${routeNames[currentLeg]}. Proceeding to next leg.`);
        }
    }
}

async function sendTelemetry(lat, lon, speed) {
    try {
        await fetch(`${API_URL}/report`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ vehicleId: VEHICLE_ID, lat: lat, lon: lon, speed: speed })
        });
    } catch (e) { console.error("Telemetry report failed."); }
}

window.stopMission = function() {
    isDriving = false;
    clearInterval(simulationInterval);
    alert("MISSION COMPLETE. Arrival time noted.");
    resetUI();
}

function toggleUI(active) {
    document.getElementById('controls-setup').style.display = active ? 'none' : 'block';
    document.getElementById('controls-active').style.display = active ? 'block' : 'none';
}

function resetUI() {
    toggleUI(false);
    document.getElementById('btn-start').innerText = "INITIATE TRIP";
    document.getElementById('btn-start').disabled = false;
    document.getElementById('speed-display').innerText = "0";
}