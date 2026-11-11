// 조종사 대시보드: 실시간 지도 + 웹소켓 위치 전송

let stompClient = null;
let currentLat = 36.3504; // 초기 위치: 대전 시청
let currentLng = 127.3845;
let fakeInterval = null;
let gpsWatchId = null;
// 조종사 본인의 실시간 비행선 레이어 및 좌표 누적 배열
window.myFlightTrackLayer = null; 
window.myFlightTrackCoords = [];

// 지도
const map = L.map("pilot-map").setView([currentLat, currentLng], 16);
L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 19,
  attribution: "&copy; OpenStreetMap contributors",
}).addTo(map);

const markers = {};

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
  stopAuto(false);

  const selectedTask = document.querySelector(".pilot-task.selected");
  if (!selectedTask) return alert("먼저 비행할 농지를 선택해주세요!");

  const lat = Number(selectedTask.dataset.lat);
  const lng = Number(selectedTask.dataset.lng);

  if (!Number.isFinite(lat) || !Number.isFinite(lng) || (lat === 0 && lng === 0)) {
  return alert("선택한 농지의 위치 정보가 올바르지 않습니다.");
}

  // 비행 시작 시 이전 실시간 비행선 초기화
  resetMyFlightTrack();
  
  currentLat = lat;
  currentLng = lng;

  // 지도를 해당 농지로 즉시 줌인 이동! (이게 있어야 선이 눈에 보임)
  if (typeof map !== "undefined") {
    map.setView([currentLat, currentLng], 16);
  }

  $("#autoStatus").text("✈️ 가상 위치 자동 비행 중... (2초마다 전송)").css("color", "#2f80ed");

  // 첫 위치 즉시 전송 및 첫 점 등록
  sendLocation(currentLat, currentLng);

  fakeInterval = setInterval(() => {
    currentLat += 0.0002;
    currentLng += 0.0002;

    // 비행하면서 지도 카메라도 드론을 따라가도록 이동
    if (typeof map !== "undefined") {
      map.panTo([currentLat, currentLng]);
      
    }
    sendLocation(currentLat, currentLng);
  }, 2000);
}

// 실제 GPS (오차 30m 초과 신호 무시)
function startRealGpsAuto() {
  if (!stompClient || !stompClient.connected) return alert("먼저 채널 연결을 확인해주세요!");
  if (!navigator.geolocation) return alert("GPS를 지원하지 않는 환경입니다.");
  stopAuto();

  const selectedTask = document.querySelector(".pilot-task.selected");
  if (!selectedTask) return alert ("먼저 비행할 농지를 선택해주세요!");     

  // 비행 시작 시 이전 실시간 비행선 초기화 
  resetMyFlightTrack();

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

  // 실시간 이동선 그리고 + 웹소켓 전송
  function sendLocation(latitude, longitude) {
    if (!stompClient || !stompClient.connected) {
      console.warn("GPS 전송 중단: 관제 채널이 연결되지 않았습니다.");
      return;
    }

    // 1. 내 화면 지도에 실시간 비행선(polyline) 쓱쓱 그리기!
    if ( !window.myFlightTrackLayer && typeof map !== "undefined") {
      window.myFlightTrackLayer = L.polyline([], {
        color: "#2f80ed", // 선명하고 시원한 비행 블루 색상
        weight: 4,
        opacity: 0.85
      }).addTo(map);
    }
    if (window.myFlightTrackLayer) {
      window.myFlightTrackLayer.addLatLng([latitude, longitude]);
    }
    
    // 2. 비행 궤적 좌표를 메모리 배열에 실시간 누적
    window.myFlightTrackCoords.push([latitude, longitude]);

    // 3. 단장 대시보드로 실시간 웹소켓 발행
    stompClient.send("/pub/location", {}, JSON.stringify({
      pilotName: $("#pilotName").val(),
      latitude,
      longitude,
    }));
  }

// 중지 (비행 중단 시 서버 DB에 비행 궤적 자동 저장!)
function stopAuto(shouldSave = true) {
  if (fakeInterval) { clearInterval(fakeInterval); fakeInterval = null; }
  if (gpsWatchId !== null) { navigator.geolocation.clearWatch(gpsWatchId); gpsWatchId = null; }
  $("#autoStatus").text("⏹️ 위치 전송이 중지되었습니다.").css("color", "#6c757d");

  // 사용자가 중지 버튼을 눌렀을 때(shouldSave === ture)만 저장 함수 실행
  if (shouldSave) {
    saveFlightPathToServer();
  }
}

// 궤적 초기화 헬퍼
function resetMyFlightTrack() {
  if (window.myFlightTrackLayer && typeof map !== "undefined") {
    map.removeLayer(window.myFlightTrackLayer);
    window.myFlightTrackLayer = null;
  }
  window.myFlightTrackCoords = [];
}

// [백엔드 API 호출] 지금까지의 비행 궤적을 DB에 저장하는 함수
function saveFlightPathToServer() {
  const selectedTask = document.querySelector(".pilot-task.selected");
  const coords = window.myFlightTrackCoords || [];

  // 선택한 농지가 없거나, 날아간 좌표가 없으면 전송 중단
  if (!selectedTask || coords.length === 0) return; 

  const taskNo = selectedTask.dataset.no;
  const pathJson = JSON.stringify(coords);

  const formData = new FormData();
  formData.append("flightPath", pathJson);

  // 우리가 만들어둔 POST /pilot/tasks/{no}/save-path 호출!
  fetch(`/pilot/tasks/${taskNo}/save-path`, {
    method: "POST",
    body: formData,
  }). then(res => {
    if (res.ok) {
    console.log(`[${taskNo}번 농지] 비행 궤적 (${coords.length}개 점) DB 저장 성공!`);
    // 태그의 data-flight-path 속성도 실시간 업데이트
    selectedTask.dataset.flightPath = pathJson;
    }
  })
  .catch(err => console.error("비행 궤적 저장 실패:", err));
}