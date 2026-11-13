let stompClient = null;
let currentLat = 36.3504; // 초기 위치: 대전 시청
let currentLng = 127.3845;
let fakeInterval = null;
let gpsWatchId = null;
// 조종사 본인의 실시간 비행선 레이어 및 좌표 누적 배열
window.myFlightTrackLayer = null; 
window.myFlightTrackCoords = [];
window.myFlightTrackLandNo = null;   // 현재 궤적이 어느 농지의 것인지

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

    // 선택된 작업에 저장된 궤적이 있을 때만 복원 (완료된 이전 농지 궤적이 따라오는 것 방지)
    const selected = document.querySelector(".pilot-task.selected")
                  || document.querySelector('.pilot-task[data-status="INTERRUPTED"]');
    const savedPath = selected?.dataset.flightPath;
    if (!selected || !savedPath || savedPath === "null" || savedPath.trim() === "") return;

    // 내 이름으로 서버에 누적된 좌표가 있으면 지도와 메모리에 복원
    if (myCoords && myCoords.length > 0) {
      window.myFlightTrackCoords = myCoords;
      window.myFlightTrackLandNo = selected.dataset.no;
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
  // stompClient.debug = null;

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

// 실시간 이동선 그리고 + 웹소켓 전송
function sendLocation(latitude, longitude) {
  if (!stompClient || !stompClient.connected) {
    console.warn("GPS 전송 중단: 관제 채널이 연결되지 않았습니다.");
    return;
  }

  // 1. 내 화면 지도에 실시간 비행선(polyline) 쓱쓱 그리기!
  if (!window.myFlightTrackLayer && typeof map !== "undefined") {
    window.myFlightTrackLayer = L.polyline([], {
      color: "#2f80ed",
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
    pilotName: $("#myPilotName").val(),
    latitude,
    longitude,
  }));
}
// [공통 함수] 비행 시작 전 선택된 농지 및 작업 상태(IN_PROGRESS / 진행 중) 검증
function getValidSelectedTask() {
  const selectedTask = document.querySelector(".pilot-task.selected");
  if (!selectedTask) {
    showPopup("먼저 비행할 농지를 선택해주세요!", "warn");
    return null;
  }

  const taskStatus = selectedTask.dataset.status;
  if (taskStatus && taskStatus !== "진행 중" && taskStatus !== "IN_PROGRESS") {
    const statusKor = {
      ASSIGNED: "대기",
      INTERRUPTED: "일시정지",
      STOPPED: "인계됨",
      COMPLETED: "완료",
    }[taskStatus] || taskStatus;

    showPopup(
      `현재 [${statusKor}] 상태인 작업입니다.\n먼저 [작업 시작] 또는 [작업 재개]를 진행한 후 비행을 시작해주세요.`,
      "warn"
    );
    return null;
  }
  return selectedTask;
}

function isPointInRing(lat, lng, ringLatLngs) {
  let inside = false;
  for (let i = 0, j = ringLatLngs.length - 1; i < ringLatLngs.length; j = i++) {
    const xi = ringLatLngs[i][0], yi = ringLatLngs[i][1];
    const xj = ringLatLngs[j][0], yj = ringLatLngs[j][1];
    const intersect = ((yi > lng) !== (yj > lng))
      && (lat < ((xj - xi) * (lng - yi) / (yj - yi + 1e-12) + xi))
    if (intersect) inside = !inside;
  }
  return inside;
}

// [시연용 핵심] 주황색 담당 구역(assignmentGeometry) 내부를 훑는 ㄹ자 비행 경로 자동 생성
function buildSprayPathInPolygon(selectedTask, fallbackLat, fallbackLng) {
  const assignedRaw = selectedTask.dataset.assignedGeometry;
  const polyRaw = selectedTask.dataset.polygon;

  let targetLayer = null;
  const rings = [];

  const scanArea = (layer, areaRings) => {
    let s = fallbackLat - 0.0003, n = fallbackLat + 0.0003;
    let w = fallbackLng - 0.0004, e = fallbackLng + 0.0004;

    if (layer) {
      const b = layer.getBounds();
      const latSpan = b.getNorth() - b.getSouth();
      const lngSpan = b.getEast() - b.getWest();
      s = b.getSouth() + latSpan * 0.1;
      n = b.getNorth() - latSpan * 0.1;
      w = b.getWest() + lngSpan * 0.1;
      e = b.getEast() - lngSpan * 0.1;
    }
    const rows = 4, cols = 4, subPoints = [];
    for (let r = 0; r < rows; r++) {
      const lat = n - ((n - s) * r) / (rows - 1);
      for (let c = 0; c < cols; c++) {
        const colRatio = (r % 2 == 0) ? c / (cols - 1) : (cols - 1 - c) / (cols - 1);
        const lng = w + (e - w) * colRatio;
        
        if (areaRings.length === 0 || areaRings.some((ring) => isPointInRing(lat, lng, ring))) {
          subPoints.push([lat, lng]);          
        }
      }     
    }
    return subPoints.length >= 2 ? subPoints : [[n, w], [n, e], [s, e], [s, w]];
  };

  // 1순위: assignedGeometry 각 조각(feature)마다 따로 ㄹ자 생성 후 순서대로 이어붙임!
  if (assignedRaw && assignedRaw !== "null" && assignedRaw.trim() !== "") {
    try {
      const geometries = JSON.parse(assignedRaw);
      if (Array.isArray(geometries) && geometries.length > 0) {
        const allPoints = [];

        geometries.forEach((g) => {
          const features = {
            type: "Feature",
            properties: {},
            geometry: g.type === "Feature" ? g.geometry : g
        };
        const singleLayer = L.geoJSON(features);
        const singleRings = [];
        const geom = features.geometry;

        if (geom?.type === "Polygon" && geom.coordinates[0]) {
          singleRings.push(geom.coordinates[0].map((c) => [c[1], c[0]])); 
        } else if (geom?.type === "MultiPolygon") {
          geom.coordinates.forEach((poly) => {
            if (poly[0]) singleRings.push(poly[0].map((c) => [c[1], c[0]]));
          });
        }

        allPoints.push(...scanArea(singleLayer, singleRings));
        });

        if (allPoints.length >= 2) return allPoints;
      }
    } catch (e) {
      console.warn("담당 구역 경로 파싱 실패:", e);
    }
  }

  // 2순위: 분할 구역이 없을 때 기존처럼 전체 농지(polyRaw) 1개 스캔
  if (polyRaw && polyRaw !== "null" && polyRaw.trim() !== "") {
    try {
      const polyCoords = JSON.parse(polyRaw);
      if (Array.isArray(polyCoords) && polyCoords.length >= 3) {
        return scanArea(L.polygon(polyCoords), [polyCoords]);
      }
    } catch (e) {
      console.warn("전체 농지 경로 파싱 실패:", e);
    } 
  }

  return scanArea(null, []);
}

// 3. 2초 간격 가상 비행 (농지 영역 내부 ㄹ자 시연 모드 + 항상 마지막 좌표에서 이어서 비행)
async function startFakeAuto() {
  stopAuto(false);

  let resume = false;
  const selectedTask = getValidSelectedTask();

  if (!selectedTask) return;

  const lat = Number(selectedTask.dataset.lat);
  const lng = Number(selectedTask.dataset.lng);

  if (!Number.isFinite(lat) || !Number.isFinite(lng) || (lat === 0 && lng === 0)) {
    return alert("선택한 농지의 위치 정보가 올바르지 않습니다.");
  }

  // 해당 농지 영역에 딱 맞는 ㄹ자 비행 경로 생성
  const sprayPath = buildSprayPathInPolygon(selectedTask, lat, lng);
  let stepIndex = 0;

  // 1. 메모리에 없으면 HTML(DB)에 저장된 기존 궤적(dataset.flightPath)을 먼저 복원!
  if ((!window.myFlightTrackCoords || window.myFlightTrackCoords.length === 0)
       && selectedTask.dataset.flightPath
       && selectedTask.dataset.flightPath !== "null"
       && selectedTask.dataset.flightPath.trim() !== "") {
    try {
      const saved = JSON.parse(selectedTask.dataset.flightPath);
      if (Array.isArray(saved) && saved.length > 0) {
        window.myFlightTrackCoords = saved;
      }
    } catch (e) { /* 무시 */}
  }

  const hasHistory = window.myFlightTrackCoords && window.myFlightTrackCoords.length > 0;

  if (hasHistory) {
    resume = await showAskPopup("이전 비행 기록이 있습니다.\n마지막 중단 위치에서 이어서 비행하시겠습니까?", {
      yesText: "이어서 비행",
      noText: "새로 시작",
      persistent: true,
    });
  }
  
  // 이어서 비행: 기존 비행 기록이 있으면 무조건 마지막 좌표(last)에서 그대로 이어서
  if (resume) {
    const last = window.myFlightTrackCoords[window.myFlightTrackCoords.length - 1];
    currentLat = last[0];
    currentLng = last[1];

    // 다음 2초 뒤에 이동할 목표 인덱스만 가장 가까운 sprayPath 점으로 맞춤
    let minDist = Infinity;
    sprayPath.forEach((pt, idx) => {
      const dist = Math.hypot(pt[0] - last[0], pt[1] - last[1]);
      if (dist < minDist) {
        minDist =dist;
        stepIndex = idx;
      }
    });
  } else {
    // 새로 시작하거나 첫 비행일 때: 화면 선 비우고 + 기존 기록이 있었으면 DB궤적도 비움
    resetMyFlightTrack();
    if (hasHistory && typeof clearFlightPathInDB === "function") {
      await clearFlightPathInDB(selectedTask.dataset.no);
    }
    currentLat = sprayPath[0][0];
    currentLng = sprayPath[0][1];
  }

  window.myFlightTrackLandNo = selectedTask.dataset.no;

  if (typeof map !== "undefined") {
    map.setView([currentLat, currentLng], 17);
  }

  $("#autoStatus").text("✈️ 가상 위치 자동 비행 중... (영역 내 방제 패턴)").css("color", "#2f80ed");

  // 첫 위치 즉시 전송
  sendLocation(currentLat, currentLng);

  fakeInterval = setInterval(() => {
    stepIndex++;
    if (stepIndex >= sprayPath.length) {
      stopAuto(true);
      showPopup("농지 방제 비행 경로를 모두 완료했습니다!", "ok");
      return;
    }

    currentLat = sprayPath[stepIndex][0];
    currentLng = sprayPath[stepIndex][1];

    if (typeof map !== "undefined") {
      map.panTo([currentLat, currentLng]);
    }
    sendLocation(currentLat, currentLng);
  }, 2000);
}

// 새로 시작 시 DB에 남아있던 이전 flightPath를 비워주는 헬퍼
async function clearFlightPathInDB(landNo) {
  if (!landNo || landNo === "undefined") return;
  try {
    const res = await fetch("/api/pilot/assignment");
    if (!res.ok) return;
    const list = await res.json();
    const target = list.find((a) => String(a.landNo) === String(landNo) && a.status === "IN_PROGRESS");
    if(!target) return;

    await $ajax({
      url: `/pilot/tasks/${target.assignmentNo}/save-path`,
      method: "POST",
      data: { flightPath: "[]"},
    });
  } catch (e) {
    console.warn("DB 궤적 초기화 실패:", e);
  } 
}

// 실제 GPS (거리 검증 가드 추가)
function startRealGpsAuto() {
  const selectedTask = getValidSelectedTask();
  if (!selectedTask) return;

  if (!stompClient || !stompClient.connected) return alert("먼저 채널 연결을 확인해주세요!");
  if (!navigator.geolocation) return alert("GPS를 지원하지 않는 환경입니다.");
  stopAuto(false);

  $("#autoStatus").text("📱 실제 GPS 신호 확인 중...").css("color", "#238451");

  gpsWatchId = navigator.geolocation.watchPosition(
    (position) => {
      const curLat = position.coords.latitude;
      const curLng = position.coords.longitude;
      const acc = Math.round(position.coords.accuracy);

      if (acc > 20) return console.warn(`[무시됨] 위치 신호 불안정 (오차: ${acc}m)`);

      // 이전 중단점이 있다면 실제 거리 (10m) 검증
      if (window.myFlightTrackCoords && window.myFlightTrackCoords.length > 0) {
        const last = window.myFlightTrackCoords[window.myFlightTrackCoords.length - 1];
        const lastPoint = L.latLng(last[0], last[1]);
        const curPoint = L.latLng(curLat, curLng);
        const distance = map.distance(lastPoint, curPoint);

        if (distance > 10) {
          stopAuto(false);
          return alert(`⚠ 현재 위치가 이전 작업 중단점과 약 ${Math.round(distance)}m 떨어져 있습니다.`);
        }
      } else {
        resetMyFlightTrack();
      }

      $("#autoStatus").text(`📱 실제 GPS 전송 중! (오차범위: 약 ${acc}m)`).css("color", "#238451");
      sendLocation(curLat, curLng);
    },
    () => alert("GPS 신호를 가져오지 못했습니다. 위치 권한을 확인해주세요."),
    { enableHighAccuracy: true }
  );
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
    if (!getValidSelectedTask()) return;

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
          "작업이 일시정지되었습니다.\n중단 위치와 시각이 저장되어 다음 조종사에게 인계하거나 이어서 진행할 수 있습니다.", "ok",
          () => location.reload()),
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
    async function saveFlightPathToServer() {
      const selectedTask = document.querySelector(".pilot-task.selected");
      const coords = window.myFlightTrackCoords || [];

      // 선택한 농지가 없거나, 비행 좌표가 없으면 전송 중단
      if (!selectedTask || coords.length === 0) return;

      const landNo = selectedTask.dataset.no;
      if (!landNo || landNo === "undefined") return;

      try {
        // 1. 선택된 농지(landNo)에 해당하는 내 작업 (assignmentNo) 조회
        const res = await fetch("/api/pilot/assignments")
        if (!res.ok) return;
        const list = await res.json();

        const target = list.find(
          (a) => String(a.landNo) === String(landNo) && a.status === "IN_PROGRESS"
        );
        if (! target) return;

        const pathJson = JSON.stringify(coords);

        // 2. CSRF 토큰이 자동 포함되는 $ajax로 저장 요청!
        await $ajax({
          url: `/pilot/tasks/${target.assignmentNo}/save-path`,
          method: "POST",
          data: { flightPath: pathJson},
          success: () => {
            console.log(`[작업 ${target.assignmentNo}번] 비행 궤적 (${coords.length}개 점)DB 저장 성공!`)
          },
          error: (err) => console.error("비행 궤적 저장 실패:", err),
        });
      } catch (err) {
        console.error("비행 궤적 저장 중 오류:", err);
      }
    }
