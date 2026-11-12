// 단장 관제 대시보드
// - 요약 / 미배정 농지 / 조종사 목록 : 서버(th:each)가 그림
// - 지도 데이터 : FARMS (th:inline)
// - JS : 지도, 농지 선택 → 배정 칸 채우기, 조종사 위치(웹소켓)
// - 배정 / 취소 : 폼 POST (서버)
document.addEventListener("DOMContentLoaded", () => {
  const mapEl = document.getElementById("leader-map");
  if (!mapEl || typeof L === "undefined") return;

  const $ = (sel) => document.querySelector(sel);
  const farms = typeof FARMS !== "undefined" ? FARMS : [];
  const findFarm = (no) => farms.find((f) => f.no === Number(no));

  // 중단됨(INTERRUPTED) 조종사 작업이 하나라도 있는 농지인지 → 빨간 점
  const isInterrupted = (f) =>
    Array.isArray(f.pilotAssignments) &&
    f.pilotAssignments.some((a) => a.status === "INTERRUPTED");
  const farmColor = (f) => (isInterrupted(f) ? "#e11d48" : f.color);
  const farmLabel = (f) => (isInterrupted(f) ? "중단됨" : f.statusLabel);
  const setStatus = (text, color) => { const el = $("#autoStatus"); 
    el.textContent = text; el.style.color = color; };

  /* ===== 1. 지도 ===== */
  const map = L.map(mapEl).setView([36.3504, 127.3845], 14);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19, attribution: "&copy; OpenStreetMap contributors",
  }).addTo(map);

  const droneIcon = L.divIcon({ className: "", html: '<div class="drone-icon">🚁</div>', iconSize: [24, 24], iconAnchor: [12, 12] });

  let selectedNo = mapEl.dataset.selected ? Number(mapEl.dataset.selected) : null;
  const farmMarkers = {};

  // 팝업 글자는 textContent 로 (안전)
  const popupOf = (lines) => {
    const box = document.createElement("div");
    lines.forEach((line, i) => {
      if (i > 0) box.append(document.createElement("br"));
      const el = document.createElement(i === 0 ? "b" : "span");
      el.textContent = line;
      box.append(el);
    });
    return box;
  };

  const renderFarmMarkers = () => {
    Object.values(farmMarkers).forEach((m) => map.removeLayer(m));
    Object.keys(farmMarkers).forEach((k) => delete farmMarkers[k]);

    farms.forEach((f) => {
      if (f.lat == null || f.lng == null) return;
      const sel = f.no === selectedNo;
      const m = L.circleMarker([f.lat, f.lng], {
        radius: sel ? 12 : 8, fillColor: farmColor(f),
        color: sel ? "#000" : "#fff", weight: sel ? 3 : 2, fillOpacity: 0.9,
      }).addTo(map);
      const lines = [`🌱 ${f.address}`, `면적: ${Number(f.area || 0).toLocaleString()}㎡`, `상태: ${farmLabel(f)}`];
      m.bindPopup(popupOf(lines));
      m.on("click", () => selectFarm(f.no, false));
      if (sel) m.bringToFront();
      farmMarkers[f.no] = m;
    });
  };
/* ===== 2. 농지 선택 → 목록 표시 + 배정 칸 채우기 ===== */
  const panel = $("#assign-panel");
  const cancelForm = $("#ap-cancel-form");
  const note = $("#ap-note");
  const goAssignBtn = $("#ap-go-assign"); // 바로가기 버튼
  const currentDashboardPolygon = null;

  // [작업 진행중] 상세 패널을 표시할 위치
  const workingPanelSlot = $("#working-assign-panel-slot");
  const unassignedPanelSlot = $("#unassigned-assign-panel-slot");

  // 1. 하단 패널 내용 채우기 함수
  function fillPanel(f) {
    if (!panel) return;

    // 농지 상태에 따라 상세 패널 위치 이동
    const targetSlot = f.status === "IN_PROGRESS"
        ? workingPanelSlot
        : unassignedPanelSlot;

    if (targetSlot && panel.parentElement !== targetSlot) {
        targetSlot.appendChild(panel);
    }

    panel.hidden = false;
    if ($("#ap-title")) $("#ap-title").textContent = `🌱 ${f.address}`;
    if ($("#ap-status")) {
      $("#ap-status").textContent = `● ${farmLabel(f)}`;
      $("#ap-status").style.color = farmColor(f);
    }
    if ($("#ap-area")) $("#ap-area").textContent = `${Number(f.area || 0).toLocaleString()}㎡`;
    if ($("#ap-date")) $("#ap-date").textContent = f.assignedAt || "-";

    const canAssign = f.status === "READY" || f.status === "ASSIGNED";

    // 바로가기 버튼에 농지 번호 달아주기
    if (goAssignBtn) {
      goAssignBtn.hidden = !canAssign;
      if (canAssign) {
        goAssignBtn.href = `/leader/task-assignment?landNo=${f.no}`;
      }
    }

    if (cancelForm) {
      cancelForm.hidden = f.status !== "ASSIGNED";
      if (f.status === "ASSIGNED" && $("#ap-cancel-no")) {
        $("#ap-cancel-no").value = f.no;
      }
    }

    if (note) {
      note.hidden = canAssign || f.status === "IN_PROGRESS";
     
      if (f.status === "COMPLETED") note.textContent = "완료된 작업입니다.";
    }

    // 선택한 농지의 다각형 외곽선 지도에 렌더링
    if (currentDashboardPolygon && map) {
      map.removeLayer(currentDashboardPolygon);   // 이전 밭 다각형 지우기
      currentDashboardPolygon = null;
    }
    
    if (f.polygonCooredinates && f.polygonCooredinates.trim() !== "") {
      try {
        const coords = JSON.parse(f.polygonCooredinates);
        if (Array.isArray(coords) && coords.length >= 3 && map) {
          currentDashboardPolygon = L.polygon(coords, {
            color: f.color || "#238451", // 농지 상태 색상 (READY면 초록, ASSIGNED 면 노랑 등)
            weight: 3, 
            dashArray: "4, 4",
            fillColor: f.color || "#54a77b",
            fillOpacity: 0.18
          }).addTo(map);
        }
      } catch (e) {
        console.error("단장 대시보드 다각형 파싱 에러: ", e)
      }
    }
  }

  // 2. 👈 [복구 완료] 농지 선택 함수
  function selectFarm(no, moveMap = true) {
    selectedNo = Number(no);
    document.querySelectorAll("#unassigned-farm-list .side-item #leader-working-list .side-item").forEach((li) =>
      li.classList.toggle("selected", Number(li.dataset.no) === selectedNo));
    if (typeof renderFarmMarkers === "function") renderFarmMarkers();

    const f = typeof findFarm === "function" ? findFarm(selectedNo) : null;
    if (!f) return;
    fillPanel(f); // 👈 여기서 팝업 패널을 띄웁니다!
    if (typeof farmMarkers !== "undefined") {
      const m = farmMarkers[f.no];
      if (moveMap && m && map) map.flyTo([f.lat, f.lng], 16, { duration: 0.8 });
      if (m) m.openPopup();
    }
  }

  // 3. 클릭 및 키보드 엔터 선택 이벤트 등록 함수
  function onActivate(listSel, handler) {
    const list = $(listSel);
    if (!list) return;
    list.addEventListener("click", (e) => { 
      const li = e.target.closest(".side-item"); 
      if (li) handler(li); 
    });
    list.addEventListener("keydown", (e) => {
      if (e.key !== "Enter" && e.key !== " ") return;
      const li = e.target.closest(".side-item");
      if (li) { 
        e.preventDefault(); 
        handler(li); 
      }
    });
  }

  // 4. 미배정 농지 목록 클릭
  onActivate("#unassigned-farm-list", (li) => selectFarm(li.dataset.no));


  // 작업 중인 농지 목록 클릭
  onActivate("#leader-working-list", (li) => selectFarm(li.dataset.no));

  /* ===== 3. 조종사 실시간 위치 (웹소켓) ===== */
  const LIVE_TIMEOUT_MS = 15000;
  const pilotMarkers = {};
  const pilotLastSeen = {};
  let stompClient = null;
  let followPilots = false;
  let selectedPilot = null;
  let pilotFocusAnimating = false;

  const focusPilot = (latlng) => {
    // 진행 중인 지도 이동 애니메이션 정리
    map.stop();

    pilotFocusAnimating = true;

    map.once("moveend", () => {
        pilotFocusAnimating = false;
    });

    // 헬기를 중앙에 배치하고 줌 18로 확대
    map.flyTo(latlng, 18, {
        duration: 0.8
    });
};

  const isOnline = (name) => pilotLastSeen[name] && Date.now() - pilotLastSeen[name] < LIVE_TIMEOUT_MS;

  // 상태 드롭다운: 고른 상태의 조종사만 보여줌
  const pilotStateFilter = $("#pilot-state-filter");

  const applyPilotFilter = () => {
    const want = pilotStateFilter ? pilotStateFilter.value : "all";
    const items = document.querySelectorAll("#pilot-status-list .side-item");
    let shown = 0;

    items.forEach((li) => {
      const badge = li.querySelector(".pilot-state");
      const state = badge.classList.contains("busy") ? "busy"
        : badge.classList.contains("ready") ? "ready"
        : "offline";
      const show = want === "all" || want === state;
      li.style.display = show ? "" : "none";
      if (show) shown++;
    });

    const countEl = $("#pilot-count");
    if (countEl) countEl.textContent = `${shown}명`;

    const emptyEl = $("#pilot-filter-empty");
    if (emptyEl) emptyEl.style.display = items.length > 0 && shown === 0 ? "" : "none";

    // 작업 중 → 대기 → 오프라인 순으로 정렬 (같은 상태끼리는 기존 순서 유지)
    const rank = (li) => {
      const b = li.querySelector(".pilot-state");
      return b.classList.contains("busy") ? 0 : b.classList.contains("ready") ? 1 : 2;
    };
    const list = $("#pilot-status-list");
    const sorted = [...items].sort((a, b) => rank(a) - rank(b));
    const changed = sorted.some((li, i) => li !== items[i]);
    if (changed) sorted.forEach((li) => list.insertBefore(li, emptyEl));
  };
  if (pilotStateFilter) pilotStateFilter.addEventListener("change", applyPilotFilter);

  // 작업 중이 아닌 조종사만: 위치가 오면 "대기", 끊기면 "오프라인"
  const refreshPilotStates = () => {
    document.querySelectorAll("#pilot-status-list .side-item").forEach((li) => {
      if (li.dataset.working === "true") return;
      const online = isOnline(li.dataset.name);
      const badge = li.querySelector(".pilot-state");
      badge.classList.toggle("ready", online);
      badge.classList.toggle("offline", !online);
      badge.lastChild.textContent = online ? "대기" : "오프라인";
    });
    applyPilotFilter();
  };
  setInterval(refreshPilotStates, 5000);
  refreshPilotStates(); // 처음 로딩 때 바로 정렬

  const pilotAnimations = {};
  const pilotTracks = {};
  // 조종사별 고유 비행선 컬러 팔레트 (선명하고 겹치지 않는 색상군)
  const TRACK_COLORS = [
    "#0284c7", // 선명한 테크 블루 (조종사 1)
    "#16a34a", // 에메랄드 그린 (조종사 2)
    "#ea580c", // 생동감 있는 오렌지 (조종사 3)
    "#9333ea", // 퍼플 (조종사 4)
    "#e11d48", // 로즈 레드 (조종사 5)
    "#0d9488"  // 딥 틸 (조종사 6)
  ];

  function getPilotColor(name) {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5 ) - hash );
    }
    const index = Math.abs(hash) % TRACK_COLORS.length;
    return TRACK_COLORS[index];
  }

  const updatePilotMarker = (name, lat, lng) => {
    pilotLastSeen[name] = Date.now();

    // 1. 해당 조종사의 실시간 비행선이 없으면 생성
    if (!pilotTracks[name]) {
      const pilotColor = getPilotColor(name); // 보유 
      
      pilotTracks[name] = L.polyline([], {
        color: pilotColor,
        weight: 4,
        opacity: 0.9
      }).addTo(map);
    }

    // 핵심: 새 좌표가 오자마자 비행선에 점을 무조건 즉시 추가! (선이 끊기지 않음)
    pilotTracks[name].addLatLng([lat, lng]);
    console.log(`📍[${name} 궤적 점 누적]:`, pilotTracks[name].getLatLngs().length, "개");

    // 2. 마커가 처음 생성되는 경우
    if (!pilotMarkers[name]) {
    const marker = L.marker([lat, lng], {
        icon: droneIcon,
        zIndexOffset: 1000
    }).addTo(map);

    marker.bindPopup(popupOf([`🚁 ${name}`]), {
        autoPan: false
    });

    // 헬기 마커 클릭 시 개별 포커싱 또는 해제
    marker.on("click", () => {

        // 이미 선택된 조종사를 다시 누르면 전체 보기
        if (selectedPilot === name) {
            marker.closePopup();
            fitAll();
            return;
        }

        // 조종사 목록에서도 해당 조종사 선택 처리
        const pilotItem = [...document.querySelectorAll("#pilot-status-list .side-item")]
            .find((li) => li.dataset.name === name);

        if (pilotItem) {
            selectPilot(pilotItem);
        } else {
            selectedPilot = name;
            focusPilot(marker.getLatLng());
            marker.openPopup();
        }
    });

    pilotMarkers[name] = marker;

    if (name === selectedPilot) {
        map.panTo([lat, lng]);
    }


    } else {
      // 3. 이미 마커가 있는 경우: 선형 보간(Lerp)으로 부드럽게 이동!
      if (pilotAnimations[name]) {
        cancelAnimationFrame(pilotAnimations[name]);
      }

      const marker = pilotMarkers[name];
      const startLatLng = marker.getLatLng();
      const startLat = startLatLng.lat;
      const startLng = startLatLng.lng;
      const startTime = performance.now();
      const duration = 1800; // 1.8초 동안 부드럽게 이동

      const animate = (currentTime) => {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1.0);

        const ease = 1 - Math.pow(1 - progress, 3);
        const curLat = startLat + (lat - startLat) * ease;
        const curLng = startLng + (lng - startLng) * ease;

        marker.setLatLng([curLat, curLng]);

        if (followPilots) {
          const pts = Object.values(pilotMarkers).map((m) => m.getLatLng());
          if (pts.length === 1) map.panTo(pts[0]);
          
          if (pts.length === 1) {
              map.panTo(pts[0]);
          } else {
            map.fitBounds(pts, { 
                padding: [40, 40],
                maxZoom: map.getZoom()
            });
            }
          }

        if (progress < 1.0) {
    pilotAnimations[name] = requestAnimationFrame(animate);
    } else {
    delete pilotAnimations[name];

    // 선택된 조종사의 위치를 지도 중앙으로 유지
    if (
        name === selectedPilot &&
        !pilotFocusAnimating &&
        !followPilots
    ) {
        map.panTo(marker.getLatLng());
    }
}
      };

      pilotAnimations[name] = requestAnimationFrame(animate);
    }

    const onlineCount = Object.keys(pilotLastSeen).filter(isOnline).length;
    setStatus(`🚁 조종사 ${onlineCount}명 실시간 위치 수신 중`, "#238451");
    refreshPilotStates();
  };

  // [추가] 새로고침 시 서버 메모리에 보관된 이전 실시간 궤적들을 지도에 복원하는 함수
  const loadActiveTracks = async (channelKey) => {
    try {
      const res = await fetch(`/api/drone/active-tracks?teamCode=${channelKey}`);
      if (!res.ok) return;
      const activeTracks = await res.json();

      Object.entries(activeTracks).forEach(([name, coords]) => {
        if (!coords || coords.length === 0) return;

        if (!pilotTracks[name]) {
          const pilotColor = getPilotColor(name);
          pilotTracks[name] = L.polyline(coords, {
            color: pilotColor,
            weight: 4,
            opacity: 0.9
          }).addTo(map);
        } else {
          pilotTracks[name].setLatLngs(coords);
        }

        const lastCoord = coords[coords.length - 1];
        updatePilotMarker(name, lastCoord[0], lastCoord[1]);
        console.log(`🔄 [${name} 이전 궤적 복원 성공]:`, coords.length, "개 점 복원됨");
      });
    } catch (err) {
      console.error("이전 비행 궤적 복원 실패:", err);
    }
  };

  const connect = () => {
    if (typeof SockJS === "undefined" || typeof Stomp === "undefined") return;
    
    const channelKey = $("#teamChannelKey")?.value?.trim();

    if (!channelKey) {
      const status = $("#status");
      status.textContent = "⚠️ 소속 방제단 정보가 없어 관제 채널을 열 수 없습니다.";
      status.style.color = "#dc3545";
      return;
    }

    if (stompClient && stompClient.connected) stompClient.disconnect();

    stompClient = Stomp.over(new SockJS("/ws-drone"));
    stompClient.debug = null;
  
    stompClient.connect({}, () => {
      $("#status .status-text").textContent = "방제단 관제 연결됨";
      loadActiveTracks(channelKey);
      
       $("#status").style.color = "#238451";

      stompClient.subscribe("/sub/team/" + channelKey, (message) => {
        const d = JSON.parse(message.body);
        console.log("🚁 [단장 화면 실시간 수신 성공!]:", d);
        updatePilotMarker(d.pilotName, d.latitude, d.longitude);
      });

    }, () => {
      $("#status .status-text").textContent = "연결 끊김 (3초 후 재연결...)";
       $("#status").style.color = "#f2b705";

      setTimeout(connect, 3000);
    });
  };

  // 조종사 선택 → 실시간 위치, 없으면 작업 중 농지-로
  const selectPilot = (li) => {
       // 조종사 개별 선택 시 전체 따라가기 해제
    if (followPilots) {
        followPilots = false;
        updateFollowButton();
    }

    selectedPilot = li.dataset.name;
    document.querySelectorAll("#pilot-status-list .side-item").forEach((x) =>
      x.classList.toggle("selected", x === li));

    const marker = pilotMarkers[selectedPilot];
    if (marker) {
      focusPilot(marker.getLatLng());
      marker.openPopup();
      setStatus(`🚁 ${selectedPilot} 조종사 위치로 이동했습니다.`, "#238451");
      return;
    }
    const working = farms.find((f) => f.pilotName === selectedPilot && f.status === "IN_PROGRESS");
    if (working && farmMarkers[working.no]) {
      map.flyTo([working.lat, working.lng], 16, { duration: 0.8 });
      farmMarkers[working.no].openPopup();
      setStatus(`📍 ${selectedPilot} 조종사의 실시간 위치가 없어 작업 중인 농지로 이동했습니다.`, "#f2b705");
    } else {
      setStatus(`⚠️ ${selectedPilot} 조종사의 위치 정보가 없습니다.`, "#6c757d");
    }
  };
  onActivate("#pilot-status-list", selectPilot);

  /* ===== 4. 지도 버튼 (HTML onclick 에서 부르므로 window 에 연결) ===== */
  const fitAll = () => {
    // 개별 조종사 추적 해제
    selectedPilot = null;
    
    // 전체 따라가기 모드로 해제
    followPilots = false;
    updateFollowButton();
    
    // 조종사 목록의 선택 표시 해제
    document.querySelectorAll("#pilot-status-list .side-item").forEach((li) => {
        li.classList.remove("selected");
    });
    
    const pts = farms.filter((f) => f.lat != null).map((f) => [f.lat, f.lng]);
    Object.values(pilotMarkers).forEach((m) => pts.push(m.getLatLng()));
    if (pts.length) { map.fitBounds(pts, { padding: [30, 30], maxZoom: 16 });
     }  
  };


  // 따라가기 시작: 조종사만 대상으로 하고, 한 명이면 현재 줌을 유지한 채 이동만 함
  const followNow = () => {
    const pts = Object.values(pilotMarkers).map((m) => m.getLatLng());
    if (pts.length === 0) return;                       // 조종사 위치가 아직 없으면 지도를 건드리지 않음
    if (pts.length === 1) map.panTo(pts[0]);            // 줌 유지
    else map.fitBounds(pts, { padding: [40, 40], maxZoom: map.getZoom() });  // 현재 줌보다 더 확대하지는 않음
  };
   
  const updateFollowButton = () => {
    const btn = $("#follow-btn");
    if (!btn) return;

    btn.innerHTML = followPilots
        ? '<i data-lucide="locate-off"></i><span>따라가기 중지</span>'
        : '<i data-lucide="locate-fixed"></i><span>조종사 따라가기</span>';

    btn.classList.toggle("btn-real", !followPilots);
    btn.classList.toggle("btn-stop", followPilots);

    if (window.lucide) {
        lucide.createIcons();
    }
  };


  const toggleFollow = () => {
    followPilots = !followPilots;

    updateFollowButton();

    if (followPilots) { followNow();
      }
    };



  window.connect = connect;
  window.fitAll = fitAll;
  window.toggleFollow = toggleFollow;

  /* ===== 처음 실행 ===== */
  renderFarmMarkers();
  // 처음엔 전체 축소 대신 확대된 기본 화면(대전 기준, 줌 14)으로 시작
  // 전체가 보고 싶으면 [전체 보기] 버튼(fitAll)을 누르면 됨
  addHazardMarkers(map);
  if (selectedNo) selectFarm(selectedNo, false);   // 배정 후 돌아왔으면 그 농지 선택
  setTimeout(() => map.invalidateSize(), 100);
  connect();
});