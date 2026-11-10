// 조종사 대시보드: 실시간 지도 + 웹소켓 위치 전송

let stompClient = null;
let currentLat = 36.3504; // 초기 위치: 대전 시청
let currentLng = 127.3845;
let fakeInterval = null;
let gpsWatchId = null;

// 지도
const map = L.map("pilot-map").setView([currentLat, currentLng], 16);
L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 19,
  attribution: "&copy; OpenStreetMap contributors",
}).addTo(map);

const markers = {};



// 로그인 정보로 조종사명 채우기
function applyUser(user) {
  if (user?.userName) {
    $("#pilotName").val(user.userName);
  }
}

if (window.currentUser) {
  applyUser(window.currentUser);
} else {
  document.addEventListener("user-ready", (e) => {
    applyUser(e.detail);
  }, { once: true });
}

// 페이지 진입 시 방제단 고유 채널 자동 연결
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", connect, { once: true });
} else {
  connect();
}



// 1. 웹소켓 자동 연결 및 팀별 채널 구독
function connect() {

  const channelKey = document.getElementById("teamChannelKey")?.value?.trim();

  if (!channelKey) {
    $("#status .status-text")
     .text("⚠️ 소속 방제단 정보가 없어 관제 채널을 열 수 없습니다.");

     $("#status").css("color", "#dc3545");
    return;
  }

  if (typeof SockJS === "undefined" || typeof Stomp === "undefined") {
    $("#status .status-text")
    .text("⚠️ 웹소켓 라이브러리를 불러오지 못했습니다.");

     $("#status").css("color", "#dc3545");
    return;
  }

  if (stompClient && stompClient.connected) {
    stompClient.disconnect();
  }

  const socket = new SockJS("/ws-drone");
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  stompClient.connect({}, () => {

    $("#status .status-text")
    .text("방제단 관제 채널 연결됨");

     $("#status").css("color", "#238451");

    // 로그인한 조종사의 소속 방제단 전용 채널 구독
    stompClient.subscribe("/sub/team/" + channelKey, (message) => {
      const data = JSON.parse(message.body);

      updateMarker(
        data.pilotName,
        data.latitude,
        data.longitude
      );
    });

  }, () => {

    $("#status .status-text")
  .text("연결 끊김 (3초 후 재연결...)");

   $("#status").css("color", "#f2b705");
    setTimeout(connect, 3000);
  });
}

// 2. 지도 위 조종사 마커
function updateMarker(pilotName, lat, lng) {
  if (markers[pilotName]) {
    markers[pilotName].setLatLng([lat, lng]);
  } else {
    markers[pilotName] = L.marker([lat, lng]).addTo(map)
      .bindPopup(`<b>🚁 ${pilotName}</b>`).openPopup();
  }
  map.panTo([lat, lng]);
}

// 3. 2초 간격 가상 비행
function startFakeAuto() {
  if (!stompClient || !stompClient.connected) return alert("먼저 채널 연결을 확인해주세요!");
  stopAuto();

  const selectedTask = document.querySelector(".pilot-task.selected");

  if (!selectedTask) return alert("먼저 비행할 농지를 선택해주세요!");

  const lat = Number(selectedTask.dataset.lat);
  const lng = Number(selectedTask.dataset.lng);

  if (!Number.isFinite(lat) || !Number.isFinite(lng) ||
    (lat === 0 && lng === 0)) {
  return alert("선택한 농지의 위치 정보가 올바르지 않습니다.");
}

  currentLat = lat;
  currentLng = lng;

  $("#autoStatus").text("✈️ 가짜 위치 자동 비행 중... (2초마다 전송)").css("color", "#2f80ed");

  fakeInterval = setInterval(() => {
    currentLat += 0.0002;
    currentLng += 0.0002;
    sendLocation(currentLat, currentLng);
  }, 2000);
}

// 4. 실제 GPS (오차 30m 초과 신호 무시)
function startRealGpsAuto() {
  if (!stompClient || !stompClient.connected) return alert("먼저 채널 연결을 확인해주세요!");
  if (!navigator.geolocation) return alert("GPS를 지원하지 않는 환경입니다.");
  stopAuto();
  $("#autoStatus").text("📱 실제 GPS 실시간 추적 시작...").css("color", "#238451");

  gpsWatchId = navigator.geolocation.watchPosition(
    (position) => {
      const acc = Math.round(position.coords.accuracy);
      if (acc > 30) return console.warn(`[무시됨] 위치 신호 불안정 (오차: ${acc}m)`);
      $("#autoStatus").text(`📱 실제 GPS 전송 중! (오차범위: 약 ${acc}m)`).css("color", "#238451");
      sendLocation(position.coords.latitude, position.coords.longitude);
    },
    () => alert("GPS 신호를 가져오지 못했습니다. 위치 권한을 확인해주세요."),
    { enableHighAccuracy: true }
  );
}

function sendLocation(latitude, longitude) {

  if (!stompClient || !stompClient.connected) {
    console.warn("GPS 전송 중단: 관제 채널이 연결되지 않았습니다.");
    return;
  }

  stompClient.send("/pub/location", {}, JSON.stringify({
    pilotName: $("#pilotName").val(),
    latitude,
    longitude,
  }));
}

// 5. 중지
function stopAuto() {
  if (fakeInterval) { clearInterval(fakeInterval); fakeInterval = null; }
  if (gpsWatchId !== null) { navigator.geolocation.clearWatch(gpsWatchId); gpsWatchId = null; }
  $("#autoStatus").text("⏹️ 위치 전송이 중지되었습니다.").css("color", "#6c757d");
}