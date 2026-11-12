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

// 💛 새로고침 시 조종사 본인이 날았던 이전 실시간 궤적을 복원하는 함수
async function loadMyActiveTrack(channelKey) {
  try {
    const res = await fetch(`/api/drone/active-tracks?teamCode=${channelKey}`);
    if (!res.ok) return;
    const activeTracks = await res.json();

    const myName = $("#myPilotName").val()?.trim();
    if (!myName) return;
    const myCoords = activeTracks[myName];

    // 내 이름으로 서버에 누적된 좌표가 있으면 지도와 메모리에 복원
    if (myCoords && myCoords.length > 0) {
      window.myFlightTrackCoords = myCoords;

      if (!window.myFlightTrackLayer && typeof map !== "undefined") {
        window.myFlightTrackLayer = L.polyline(myCoords, {
          color: "#2f80ed",
          weight: 4,
          opacity: 0.85
        }).addTo(map);
      } else if (window.myFlightTrackLayer) {
        window.myFlightTrackLayer.setLatLngs(myCoords);
      }

      // 드론의 현재 위치와 지도 중심을 마지막 비행 위치로 맞춤
      const last = myCoords[myCoords.length - 1];
      currentLat = last[0];
      currentLng = last[1];
      if (typeof map !== "undefined") {
        map.setView([currentLat, currentLng], 16);
      }

      // 마지막 비행 위치에 조종사 드론 마커 즉시 표시
      if (typeof updateMarker === "function" && myName) {
        updateMarker(myName, currentLat, currentLng);
      }

      console.log(`🔄 [조종사 궤적 복원 성공]: 이전 점 ${myCoords.length}개 복원됨`);
    }
  } catch (err) {
    console.error("조종사 비행 궤적 복원 실패:", err);
  }
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

    $("#status .status-text").text("방제단 관제 채널 연결됨");
    loadMyActiveTrack(channelKey);
    

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
  $("#status .status-text").text("연결 끊김 (3초 후 재연결...)");

   $("#status").css("color", "#f2b705");
    setTimeout(connect, 3000);
  });
}

// 2. 지도 위 조종사 마커
function updateMarker(pilotName, lat, lng) {
  console.log(
    `[위치 수신] ${pilotName}`,
    lat,
    lng,
    new Date().toLocaleTimeString()
  );

  if (markers[pilotName]) {
    markers[pilotName].setLatLng([lat, lng]);
  } else {
    markers[pilotName] = L.marker([lat, lng]).addTo(map)
      .bindPopup(`<b>🚁 ${pilotName}</b>`);
  }
   // 로그인한 본인의 위치만 지도 중심에 반영
  const myPilotName = document.getElementById("myPilotName")?.value;

  if (pilotName === myPilotName) {
    console.log(`[내 위치 지도 이동] ${pilotName}`);
    map.panTo([lat, lng]);
  }
    
}

// 3. 2초 간격 가상 비행
function startFakeAuto() {
  stopAuto(false);

  const selectedTask = document.querySelector(".pilot-task.selected");
  if (!selectedTask) return alert("먼저 비행할 농지를 선택해주세요!");

  const taskNo = selectedTask.dataset.no;
  const lat = Number(selectedTask.dataset.lat);
  const lng = Number(selectedTask.dataset.lng);

  if (!Number.isFinite(lat) || !Number.isFinite(lng) || (lat === 0 && lng === 0)) {
  return alert("선택한 농지의 위치 정보가 올바르지 않습니다.");
}

  // 주의 사항, 이전 궤적이 있고, 같은 농지라면 이어 비행 여부 확인
  const hasHistory = window.myFlightTrackCoords && window.myFlightTrackCoords.length > 0;
  let resume = false;
  
  if (hasHistory) {
    resume = confirm("이전 비행 기록이 있습니다. \n마지막 중단 위치에서 이어서 비행하시겠습니까?")
  }
  
  if (resume) {
    // 1) 이이서 비행 : 마지막 좌표를 현재 출발지로 설정 (선은 지우지 않음!)
    const last = window.myFlightTrackCoords[window.myFlightTrackCoords.length - 1];
    currentLat = last[0];
    currentLng = last[1];
  } else {
    // 2) 새로 시작: 기존 선 지우고 농지 원점으로 리셋
    resetMyFlightTrack();
    currentLat = lat;
    currentLng = lng;
  }

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

// 실제 GPS (거리 검증 가드 추가)
function startRealGpsAuto() {
  if (!stompClient || !stompClient.connected) return alert("먼저 채널 연결을 확인해주세요!");
  if (!navigator.geolocation) return alert("GPS를 지원하지 않는 환경입니다.");
  stopAuto(false);

  const selectedTask = document.querySelector(".pilot-task.selected");
  if (!selectedTask) return alert ("먼저 비행할 농지를 선택해주세요!");     

  $("#autoStatus").text("📱 실제 GPS 신호 확인 중...").css("color", "#238451");

  gpsWatchId = navigator.geolocation.watchPosition(
    (position) => {
      const curLat = position.coords.latitude;
      const curLng = position.coords.longitude;
      const acc = Math.round(position.coords.accuracy);

      if (acc > 20) return console.warn(`[무시됨] 위치 신호 불안정 (오차: ${acc}m)`);

      // [💛] 이전 중단점이 있다면 실제 거리 (10m) 검증!
      if (window.myFlightTrackCoords && window.myFlightTrackCoords.length > 0) {
        const last = window.myFlightTrackCoords[window.myFlightTrackCoords.length - 1];
        const lastPoint = L.latLng(last[0], last[1]);
        const curPoint = L.latLng(curLat, curLng);
        const distance = map.distance(lastPoint, curPoint);

        // 10m 이상 떨어져 있으면 비행을 차단하고 경고
        if (distance > 10) {
          stopAuto(false);
          return alert(`⚠ 현재 위치가 이전 작업 중단점과 약 ${Math.round(distance)}m 떨어져 있습니다.`)
        } 
      } else { 
        // 첫 비행이라면 초기화
        resetMyFlightTrack();
      }

      $("#autoStatus").text(`📱 실제 GPS 전송 중! (오차범위: 약 ${acc}m)`).css("color", "#238451");
      sendLocation(curLat, curLng);
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


    // 비행 중단 시 서버 DB에 비행 궤적 자동 저장 함수 
    function stopAuto(shouldSave = true) {
      if (fakeInterval) {
        clearInterval(fakeInterval);
        fakeInterval = null;
      }

      if (gpsWatchId !== null) {
        navigator.geolocation.clearWatch(gpsWatchId);
        gpsWatchId = null;
      }

      $("#autoStatus").text("⏹️ 위치 전송이 중지되었습니다.").css("color", "#6c757d");

      // 사용자가 중지 버튼을 눌렀을 때만 저장
      if (shouldSave) {
        saveFlightPathToServer();
      }
    }

    // 작업 일시정지 
    async function pauseWork() {
      const ok = await showAskPopup("작업을 일시중단하시겠습니까?\n지금까지의 비행 궤적이 저장되며 위치 전송이 멈춥니다. ", {
        yesText: "일시정지",
        noText: "취소",
      });

      if (!ok) return;

      // 1) 비행 타이머 중지 및 지금까지의 비행 궤적 DB 저장 (true)
      // 지도 위 비행선은 지우지 않고 그대로
      stopAuto(true);

      $("#autoStatus").text("⏸️ 작업이 일시정지되었습니다. ( 궤적 저장 완료) ").css("color", "#f2994a");

      // 2) 선택된 농지의 작업 상태를 PAUSED로 서버에 업데이트
      const selected = document.querySelector(".pilot-task.selected");
      if (!selected) return;

      try {
        const res = await fetch("/api/pilot/assignments");
        if (!res.ok) return;

        const list = await res.json();

        const target = list.find(
          (a) => String(a.landNo) === selected.dataset.no && a.status === "IN_PROGRESS"
        );

        if (!target) return;

        await $ajax({
          url: `/api/pilot/assignments/${target.assignmentNo}/status`,
          method: "PATCH",
          data: { status: "INTERRUPTED" },
          success: () => showPopup(
            "작업이 일시정지되었습니다.\n중단 위치와 시각이 저장되어 다음 조종사에게 인계하거나 이어서 진행할 수 있습니다.", "ok"),
          error: (r) => showPopup( (r && r.message) || "일시정지에 실패했습니다.", "warn"),
        });
      } catch (e) {
        console.error("일시정지 처리 오류:", e);
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

      // 선택한 농지가 없거나, 비행 좌표가 없으면 전송 중단
      if (!selectedTask || coords.length === 0) return;

      const taskNo = selectedTask.dataset.no;
      const pathJson = JSON.stringify(coords);

      const formData = new FormData();
      formData.append("flightPath", pathJson);

      // 비행 궤적 저장 API
      fetch(`/pilot/tasks/${taskNo}/save-path`, {
        method: "POST",
        body: formData,
      })
        .then((res) => {
          if (res.ok) {
            console.log(
              `[${taskNo}번 농지] 비행 궤적 (${coords.length}개 점) DB 저장 성공!`
            );

            selectedTask.dataset.flightPath = pathJson;
          }
        })
        .catch((err) => console.error("비행 궤적 저장 실패:", err));
    }
