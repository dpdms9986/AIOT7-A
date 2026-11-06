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
  const assignForm = $("#ap-assign-form");
  const cancelForm = $("#ap-cancel-form");
  const note = $("#ap-note");
  const pilotSelect = $("#ap-pilot-select");

  const fillPanel = (f) => {
    panel.hidden = false;
    $("#ap-title").textContent = `🌱 ${f.address}`;
    $("#ap-status").textContent = `● ${f.statusLabel}`;
    $("#ap-status").style.color = f.color;
    $("#ap-area").textContent = `${Number(f.area || 0).toLocaleString()}㎡`;
    $("#ap-date").textContent = f.assignedAt || "-";
    $("#ap-pilot").textContent = f.pilotName || "미배정";

    // 상태에 따라 보여 줄 것
    const canAssign = f.status === "READY" || f.status === "ASSIGNED";
    assignForm.hidden = !canAssign;
    cancelForm.hidden = f.status !== "ASSIGNED";
    note.hidden = canAssign;

    if (canAssign) {
      $("#ap-land-no").value = f.no;
      // 작업배정 페이지로 이동할 때 선택한 농지가 미리 체크되도록 번호를 붙임
      $("#ap-go-assign").href = `${ctx}/leader/task-assignment?landNo=${f.no}`;
      $("#ap-submit").textContent = f.status === "READY" ? "✅ 배정하기" : "🔄 재배정";
      // 이미 배정된 조종사 미리 선택
      const opt = [...pilotSelect.options].find((o) => o.dataset.name === f.pilotName);
      pilotSelect.value = opt ? opt.value : "";
    }
    if (f.status === "ASSIGNED") $("#ap-cancel-no").value = f.no;
    if (f.status === "IN_PROGRESS") note.textContent = `🚁 ${f.pilotName} 조종사가 작업 중입니다.`;
    if (f.status === "COMPLETED") note.textContent = "완료된 작업입니다.";
  };

  const selectFarm = (no, moveMap = true) => {
    selectedNo = Number(no);
    document.querySelectorAll("#unassigned-farm-list .side-item").forEach((li) =>
      li.classList.toggle("selected", Number(li.dataset.no) === selectedNo));
    renderFarmMarkers();

    const f = findFarm(selectedNo);
    if (!f) return;
    fillPanel(f);
    const m = farmMarkers[f.no];
    if (moveMap && m) map.flyTo([f.lat, f.lng], 16, { duration: 0.8 });
    if (m) m.openPopup();
  };

  // 클릭 / 엔터로 선택 (목록 공통)
  const onActivate = (listSel, handler) => {
    const list = $(listSel);
    list.addEventListener("click", (e) => { const li = e.target.closest(".side-item"); if (li) handler(li); });
    list.addEventListener("keydown", (e) => {
      if (e.key !== "Enter" && e.key !== " ") return;
      const li = e.target.closest(".side-item");
      if (li) { e.preventDefault(); handler(li); }
    });
  };
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

  const updatePilotMarker = (name, lat, lng) => {
    pilotLastSeen[name] = Date.now();
    if (pilotMarkers[name]) {
      pilotMarkers[name].setLatLng([lat, lng]);
    } else {
      pilotMarkers[name] = L.marker([lat, lng], { icon: droneIcon, zIndexOffset: 1000 })
        .addTo(map).bindPopup(popupOf([`🚁 ${name}`]));
    }
    if (followPilots) {
      const pts = Object.values(pilotMarkers).map((m) => m.getLatLng());
      if (pts.length === 1) map.panTo(pts[0]); else map.fitBounds(pts, { padding: [40, 40], maxZoom: map.getZoom() });
    } else if (name === selectedPilot) {
      map.panTo([lat, lng]);
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
        updatePilotMarker(d.pilotName, d.latitude, d.longitude);
      });

    }, () => {
      $("#status .status-text").textContent = "연결 끊김 (3초 후 재연결...)";
       $("#status").style.color = "#f2b705";

      setTimeout(connect, 3000);
    });
  };

  // 조종사 선택 → 실시간 위치, 없으면 작업 중 농지로
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