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
  const setStatus = (text, color) => { const el = $("#autoStatus"); el.textContent = text; el.style.color = color; };

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
        radius: sel ? 12 : 8, fillColor: f.color,
        color: sel ? "#000" : "#fff", weight: sel ? 3 : 2, fillOpacity: 0.9,
      }).addTo(map);
      const lines = [`🌱 ${f.address}`, `면적: ${Number(f.area || 0).toLocaleString()}㎡`, `상태: ${f.statusLabel}`];
      if (f.pilotName) lines.push(`조종사: ${f.pilotName}`);
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

  // 1. 하단 패널 내용 채우기 함수
  function fillPanel(f) {
    if (!panel) return;
    panel.hidden = false;
    if ($("#ap-title")) $("#ap-title").textContent = `🌱 ${f.address}`;
    if ($("#ap-status")) {
      $("#ap-status").textContent = `● ${f.statusLabel}`;
      $("#ap-status").style.color = f.color;
    }
    if ($("#ap-area")) $("#ap-area").textContent = `${Number(f.area || 0).toLocaleString()}㎡`;
    if ($("#ap-date")) $("#ap-date").textContent = f.assignedAt || "-";
    if ($("#ap-pilot")) $("#ap-pilot").textContent = f.pilotName || "미배정";

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
      note.hidden = canAssign;
      if (f.status === "IN_PROGRESS") note.textContent = `🚁 ${f.pilotName} 조종사가 작업 중입니다.`;
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
    document.querySelectorAll("#unassigned-farm-list .side-item").forEach((li) =>
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

  // 4. 미배정 농지 목록 클릭 시 selectFarm 실행!
  onActivate("#unassigned-farm-list", (li) => selectFarm(li.dataset.no));

  /* ===== 3. 조종사 실시간 위치 (웹소켓) ===== */
  const LIVE_TIMEOUT_MS = 15000;
  const pilotMarkers = {};
  const pilotLastSeen = {};
  let stompClient = null;
  let followPilots = false;
  let selectedPilot = null;

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

  const updatePilotMarker = (name, lat, lng) => {
    pilotLastSeen[name] = Date.now();

    // 1. 해당 조종사의 실시간 비행선이 없으면 생성
    if (!pilotTracks[name]) {
      pilotTracks[name] = L.polyline([], {
        color: "#0284c7",
        weight: 4,
        opacity: 0.9
      }).addTo(map);
    }

    // 핵심: 새 좌표가 오자마자 비행선에 점을 무조건 즉시 추가! (선이 끊기지 않음)
    pilotTracks[name].addLatLng([lat, lng]);
    console.log(`📍[${name} 궤적 점 누적]:`, pilotTracks[name].getLatLngs().length, "개");

    // 2. 마커가 처음 생성되는 경우
    if (!pilotMarkers[name]) {
      pilotMarkers[name] = L.marker([lat, lng], { icon: droneIcon, zIndexOffset: 1000 })
        .addTo(map).bindPopup(popupOf([`🚁 ${name}`]));

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
          else map.fitBounds(pts, { padding: [40, 40], maxZoom: map.getZoom() });
        } else if (name === selectedPilot) {
          map.panTo([curLat, curLng]);
        }

        if (progress < 1.0) {
          pilotAnimations[name] = requestAnimationFrame(animate);
        }
      };

      pilotAnimations[name] = requestAnimationFrame(animate);
    }

    const onlineCount = Object.keys(pilotLastSeen).filter(isOnline).length;
    setStatus(`🚁 조종사 ${onlineCount}명 실시간 위치 수신 중`, "#238451");
    refreshPilotStates();
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
    selectedPilot = li.dataset.name;
    document.querySelectorAll("#pilot-status-list .side-item").forEach((x) =>
      x.classList.toggle("selected", x === li));

    const marker = pilotMarkers[selectedPilot];
    if (marker) {
      map.flyTo(marker.getLatLng(), 17, { duration: 0.8 });
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
    const pts = farms.filter((f) => f.lat != null).map((f) => [f.lat, f.lng]);
    Object.values(pilotMarkers).forEach((m) => pts.push(m.getLatLng()));
    if (pts.length) map.fitBounds(pts, { padding: [30, 30], maxZoom: 16 });
  };
  // 따라가기 시작: 조종사만 대상으로 하고, 한 명이면 현재 줌을 유지한 채 이동만 함
  const followNow = () => {
    const pts = Object.values(pilotMarkers).map((m) => m.getLatLng());
    if (pts.length === 0) return;                       // 조종사 위치가 아직 없으면 지도를 건드리지 않음
    if (pts.length === 1) map.panTo(pts[0]);            // 줌 유지
    else map.fitBounds(pts, { padding: [40, 40], maxZoom: map.getZoom() });  // 현재 줌보다 더 확대하지는 않음
  };
  const toggleFollow = () => {
    followPilots = !followPilots;
    const btn = $("#follow-btn");

    btn.innerHTML = followPilots
        ? '<i data-lucide="locate-off"></i><span>따라가기 중지</span>'
        : '<i data-lucide="locate-fixed"></i><span>조종사 따라가기</span>';

    btn.classList.toggle("btn-real", !followPilots);
    btn.classList.toggle("btn-stop", followPilots);

    // Lucide 아이콘 다시 렌더링
    if (window.lucide) {
        lucide.createIcons();
    }

    if (followPilots) followNow();
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