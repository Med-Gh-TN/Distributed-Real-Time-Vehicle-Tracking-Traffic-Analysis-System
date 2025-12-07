// dashboard.js
// --- CONFIGURATION ---
const API_URL = 'http://localhost:8080/api';

// --- TRAFFIC LIGHTS (Interactive Junctions) ---
const junctions = [
    { id: "TUNIS-CENTER-01", lat: 36.8000, lon: 10.1800, name: "Tunis Center Jct" },
    { id: "HAMMAMET-JUNC-01", lat: 36.4000, lon: 10.6167, name: "Hammamet Entry" },
    { id: "SFAX-PORT-01", lat: 34.7406, lon: 10.7603, name: "Sfax Port" }
];

// --- STATE & INITIALIZATION ---
const map = L.map('map', {zoomControl: false}).setView([36.4, 10.4], 9);
L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap'
}).addTo(map);

const markers = {};
let trafficChart;

// Custom Icons
const carIcon = L.divIcon({ className: 'car-icon', html: "<div class='w-3 h-3 bg-cyan-400 rounded-full border-2 border-white'></div>", iconSize: [15, 15] });
const lightIcon = L.divIcon({ className: 'light-icon', html: "<div class='text-2xl'>🚦</div>", iconSize: [24, 24] });


// Initialize Map Elements
(function initDashboard() {
    // Add Junction Markers and Controls
    junctions.forEach(j => {
        const marker = L.marker([j.lat, j.lon], {icon: lightIcon}).addTo(map);
        marker.bindPopup(`
            <div style="text-align:center; color:black; font-family:sans-serif; min-width: 150px;">
                <b class="text-lg">${j.name}</b><br><small class="text-xs">ID: ${j.id}</small><br>
                <button class="ctrl-btn btn-green" onclick="controlLight('${j.id}', 'GREEN')">FORCE GREEN</button>
                <button class="ctrl-btn btn-red" onclick="controlLight('${j.id}', 'RED')">FORCE RED</button>
            </div>
        `);
    });

    // Initialize Chart
    const ctx = document.getElementById('speedChart').getContext('2d');
    trafficChart = new Chart(ctx, {
        type: 'bar',
        data: { labels: [], datasets: [{ label: 'Speed (km/h)', data: [], backgroundColor: '#3498db' }] },
        options: {
            responsive: true, maintainAspectRatio: false, animation: { duration: 0 }, // Disable animation for real-time feel
            scales: { y: { beginAtZero: true, max: 160, grid: { color: '#333' }, ticks: { color: '#aaa' } }, x: { display: false, grid: { color: '#333' } } },
            plugins: { legend: { display: false } }
        }
    });

    // Start Polling
    setInterval(updateTrafficData, 1000);
    log("System Initialized and Monitoring...");
})();

// --- DATA POLLING ---
async function updateTrafficData() {
    try {
        const res = await fetch(API_URL + '/dashboard');
        const vehicles = await res.json();

        let maxSpeed = 0;
        const labels = [];
        const dataPoints = [];
        const backgroundColors = [];

        vehicles.forEach(v => {
            // Metrics and Map Logic
            maxSpeed = Math.max(maxSpeed, v.speed);
            if(markers[v.vehicleId]) { markers[v.vehicleId].setLatLng([v.latitude, v.longitude]); }
            else { markers[v.vehicleId] = L.marker([v.latitude, v.longitude], {icon: carIcon}).addTo(map); }

            // Chart Logic
            labels.push(v.vehicleId);
            dataPoints.push(v.speed);
            backgroundColors.push(v.speed > 100 ? '#e74c3c' : '#3498db'); // Red if > 100 km/h

            // Alert Logic (Visual Check)
            if(v.speed > 100 && Math.random() > 0.95) {
                log(`⚠️ ALERT: ${v.vehicleId} speeding (${Math.floor(v.speed)} km/h)`, true);
            }
        });

        // Update Dashboard Stats
        document.getElementById('active-count').innerText = vehicles.length;
        document.getElementById('max-speed').innerText = Math.floor(maxSpeed);

        // Update Chart Data
        trafficChart.data.labels = labels;
        trafficChart.data.datasets[0].data = dataPoints;
        trafficChart.data.datasets[0].backgroundColor = backgroundColors;
        trafficChart.update('none');

    } catch(e) { /* Server might be down - silently ignore poll failure */ }
}

// --- CONTROL LOGIC (CORBA Bridge) ---
window.controlLight = async function(id, cmd) {
    log(`📡 Sending [${cmd}] signal to ${id}...`, false, true);
    try {
        const res = await fetch(API_URL + '/control', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ junctionId: id, command: cmd })
        });
        const d = await res.json();

        if(d.status === 'executed') {
            log(`✅ HW ACK: ${d.hardware_response}`, false, true);
        } else {
            log(`❌ ERROR: Failed to execute command.`, true);
        }
    } catch(e) {
        log(`❌ ERROR: Control API failed.`, true);
    }
}

// --- LOGGING UTILITY ---
function log(msg, isAlert=false, isHw=false) {
    const p = document.getElementById('log-panel');
    const d = document.createElement('div');
    d.className = 'log-entry console-font ' + (isAlert ? 'log-alert' : '') + (isHw ? 'log-hw' : '');
    d.innerText = `[${new Date().toLocaleTimeString()}] ${msg}`;
    p.prepend(d);
}