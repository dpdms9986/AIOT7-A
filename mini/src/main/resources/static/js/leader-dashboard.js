// 단장 메인 화면: 방제단 농지 지도 + 미배정 농지 배정 + 소속 조종사 실시간 위치

/* ===== 상태 이름 → 화면 글자와 색 ===== */
const STATUS = {
  READY:       { text: "미배정",  color: "#238451" },
  ASSIGNED:    { text: "배정",    color: "#f2b705" },
  IN_PROGRESS: { text: "진행 중", color: "#2f80ed" },
  COMPLETED:   { text: "완료",    color: "#8a8f8c" },
};

/* ===== 서버 API (백엔드와 맞춰서 수정) ===== */
const TASKS_API   = "/api/leader/tasks";   // GET  [{id, farm, area, date, status, lat, lng, pilotId, pilotName}]
const PILOTS_API  = "/api/leader/pilots";  // GET  [{id, name, team}]
const assignUrl   = (id) => `/api/leader/tasks/${id}/assign`;   // POST {pilotId, date}
const unassignUrl = (id) => `/api/leader/tasks/${id}/unassign`; // POST

const LIVE_TIMEOUT_MS = 15000; // 15초 이상 위치가 안 오면 오프라인

/* ===== 상태 ===== */
let tasks = [];
let pilots = [];
let selectedId = null;         // 선택한 농지 id
let selectedPilot = null;      // 선택한 조종사 이름
const farmMarkers = {};        // 농지 id → 마커
const pilotMarkers = {};       // 조종사 이름 → 마커
const pilotLastSeen = {};      // 조종사 이름 → 마지막 위치 수신 시각
let stompClient = null;
let followPilots = false;

/* ===== 화면 요소 ===== */
const farmList = document.querySelector("#unassigned-farm-list");
const pilotList = document.querySelector("#pilot-status-list");
const assignPanel = document.querySelector("#assign-panel");

// XSS 방어
const esc = (v) =>
  String(v ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

/* ===== 지도 ===== */
const map = L.map("leader-map").setView([36.3504, 127.3845], 14); // 대전 시청
L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 19,
  attribution: "&copy; OpenStreetMap contributors",
}).addTo(map);

const droneIcon = L.divIcon({
  className: "",
  html: '<div class="drone-icon">🚁</div>',
  iconSize: [24, 24],
  iconAnchor: [12, 12],
});

/* ===== 공통 계산 ===== */
const isActive = (t) => t.status === "ASSIGNED" || t.status === "IN_PROGRESS";
const activeCountOf = (pilotId) => tasks.filter((t) => t.pilotId === pilotId && isActive(t)).length;
const workingTaskOf = (pilotId) => tasks.find((t) => t.pilotId === pilotId && t.status === "IN_PROGRESS");
const isOnline = (name) => pilotLastSeen[name] && Date.now() - pilotLastSeen[name] < LIVE_TIMEOUT_MS;

/* ===== 요약 카드 ===== */
function renderStats() {
  const count = (s) => tasks.filter((t) => t.status === s).length;
  $("#stat-ready").html(`${count("READY")}<span>건</span>`);
  $("#stat-assigned").html(`${count("ASSIGNED")}<span>건</span>`);
  $("#stat-progress").html(`${count("IN_PROGRESS")}<span>건</span>`);
  $("#stat-completed").html(`${count("COMPLETED")}<span>건</span>`);
}

/* ===== 미배정 농지 카드 ===== */
function renderFarmList() {
  const ready = tasks.filter((t) => t.status === "READY");
  $("#farm-count").text(`${ready.length}개`);

  if (ready.length === 0) {
    farmList.innerHTML = `<li class="side-empty">미배정 농지가 없습니다.</li>`;
    return;
  }
  farmList.innerHTML = ready.map((t) => `
    <li class="side-item ${t.id === selectedId ? "selected" : ""}" data-farm-id="${t.id}" tabindex="0">
      <div class="item-main">
        <strong>${esc(t.farm)}</strong>
        <small>면적 ${Number(t.area || 0).toLocaleString()}㎡ · 농지 번호 ${t.id}</small>
      </div>
      <span class="status-pill">미배정</span>
    </li>`).join("");
}

/* ===== 소속 조종사 카드 ===== */
function renderPilotList() {
  $("#pilot-count").text(`${pilots.length}명`);

  if (pilots.length === 0) {
    pilotList.innerHTML = `<li class="side-empty">소속 조종사가 없습니다.</li>`;
    return;
  }
  pilotList.innerHTML = pilots.map((p) => {
    const working = workingTaskOf(p.id);
    // 작업 중 → 파랑 / 위치 수신 중 → 대기(초록) / 그 외 → 오프라인(회색)
    const state = working ? ["busy", "작업 중"] : isOnline(p.name) ? ["ready", "대기"] : ["offline", "오프라인"];
    const sub = working
      ? `${esc(working.farm)} 작업 중`
      : `${esc(p.team || "")}${p.team ? " · " : ""}배정 ${activeCountOf(p.id)}건`;

    return `
      <li class="side-item ${p.name === selectedPilot ? "selected" : ""}" data-pilot="${esc(p.name)}" tabindex="0">
        <div class="item-main">
          <strong>${esc(p.name)}</strong>
          <small>${sub}</small>
        </div>
        <span class="pilot-state ${state[0]}"><i></i>${state[1]}</span>
      </li>`;
  }).join("");
}

/* ===== 배정 패널 (미배정 농지 카드 아래) ===== */
function renderAssignPanel() {
  const t = tasks.find((x) => x.id === selectedId);
  if (!t) {
    assignPanel.hidden = true;
    return;
  }
  assignPanel.hidden = false;

  const st = STATUS[t.status] || STATUS.READY;
  const today = new Date().toISOString().slice(0, 10);

  let actionHtml = "";
  if (t.status === "READY" || t.status === "ASSIGNED") {
    const options = pilots.map((p) =>
      `<option value="${p.id}" ${p.id === t.pilotId ? "selected" : ""}>${esc(p.name)} (배정 ${activeCountOf(p.id)}건)</option>`
    ).join("");
    actionHtml = `
      <form class="assign-form" id="assign-form">
        <label>조종사
          <select id="assign-pilot" required>
            <option value="">조종사 선택</option>${options}
          </select>
        </label>
        <label>작업 예정일
          <input type="date" id="assign-date" min="${today}" value="${esc(t.date || today)}" required>
        </label>
        <div class="assign-actions">
          <button type="submit" class="dash-btn-primary">${t.status === "READY" ? "✅ 배정하기" : "🔄 재배정"}</button>
          ${t.status === "ASSIGNED" ? `<button type="button" class="dash-btn-outline btn-danger" id="unassign-btn">배정 취소</button>` : ""}
        </div>
      </form>`;
  } else if (t.status === "IN_PROGRESS") {
    actionHtml = `<p class="assign-note">🚁 ${esc(t.pilotName)} 조종사가 작업 중입니다.</p>`;
  } else {
    actionHtml = `<p class="assign-note">완료된 작업입니다.</p>`;
  }

  assignPanel.innerHTML = `
    <h3>🌱 ${esc(t.farm)}</h3>
    <dl>
      <dt>상태</dt><dd><span class="assign-status" style="color:${st.color}">● ${st.text}</span></dd>
      <dt>면적</dt><dd>${Number(t.area || 0).toLocaleString()}㎡</dd>
      <dt>예정일</dt><dd>${esc(t.date || "-")}</dd>
      <dt>조종사</dt><dd>${esc(t.pilotName || "미배정")}</dd>
    </dl>
    ${actionHtml}`;
}

/* ===== 농지 마커 ===== */
function renderFarmMarkers() {
  Object.values(farmMarkers).forEach((m) => map.removeLayer(m));
  Object.keys(farmMarkers).forEach((k) => delete farmMarkers[k]);

  tasks.forEach((t) => {
    if (t.lat == null || t.lng == null) return;
    const st = STATUS[t.status] || STATUS.READY;
    const sel = t.id === selectedId;

    const marker = L.circleMarker([t.lat, t.lng], {
      radius: sel ? 12 : 8,
      fillColor: st.color,
      color: sel ? "#000" : "#fff",
      weight: sel ? 3 : 2,
      opacity: 1,
      fillOpacity: 0.9,
    }).addTo(map);

    marker.bindPopup(
      `<b>🌱 ${esc(t.farm)}</b><br>면적: ${Number(t.area || 0).toLocaleString()}㎡<br>상태: ${st.text}` +
      (t.pilotName ? `<br>조종사: ${esc(t.pilotName)}` : "")
    );
    marker.on("click", () => selectFarm(t.id, false));
    if (sel) marker.bringToFront();
    farmMarkers[t.id] = marker;
  });
}

function renderAll() {
  renderStats();
  renderFarmList();
  renderPilotList();
  renderAssignPanel();
  renderFarmMarkers();
  setTimeout(() => map.invalidateSize(), 100);
}

/* ===== 농지 선택 → 지도 이동 + 마커 강조 + 팝업 ===== */
function selectFarm(id, moveMap = true) {
  selectedId = id;
  selectedPilot = null;
  renderAll();

  const t = tasks.find((x) => x.id === id);
  const marker = farmMarkers[id];
  if (!t || !marker) return;

  if (moveMap) map.flyTo([t.lat, t.lng], 16, { duration: 0.8 });
  marker.openPopup();
}

/* ===== 조종사 선택 → 실시간 위치로 이동 (없으면 작업 중 농지로) ===== */
function selectPilot(name) {
  selectedPilot = name;
  renderPilotList();

  const marker = pilotMarkers[name];
  if (marker) {
    map.flyTo(marker.getLatLng(), 17, { duration: 0.8 });
    marker.openPopup();
    $("#autoStatus").text(`🚁 ${name} 조종사 위치로 이동했습니다.`).css("color", "#238451");
    return;
  }

  const p = pilots.find((x) => x.name === name);
  const working = p && workingTaskOf(p.id);
  if (working && farmMarkers[working.id]) {
    map.flyTo([working.lat, working.lng], 16, { duration: 0.8 });
    farmMarkers[working.id].openPopup();
    $("#autoStatus").text(`📍 ${name} 조종사의 실시간 위치가 없어 작업 중인 농지로 이동했습니다.`).css("color", "#f2b705");
  } else {
    $("#autoStatus").text(`⚠️ ${name} 조종사의 위치 정보가 없습니다.`).css("color", "#6c757d");
  }
}

/* ===== 카드 클릭 / 키보드 ===== */
function onActivate(listEl, selector, handler) {
  listEl.addEventListener("click", (e) => {
    const item = e.target.closest(selector);
    if (item) handler(item);
  });
  listEl.addEventListener("keydown", (e) => {
    if (e.key !== "Enter" && e.key !== " ") return;
    const item = e.target.closest(selector);
    if (item) {
      e.preventDefault();
      handler(item);
    }
  });
}
onActivate(farmList, "[data-farm-id]", (item) => selectFarm(Number(item.dataset.farmId)));
onActivate(pilotList, "[data-pilot]", (item) => selectPilot(item.dataset.pilot));

/* ===== 배정 / 배정 취소 ===== */
async function postJson(url, body) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error(`서버 응답 ${res.status}`);
  return res.status === 204 ? null : res.json().catch(() => null);
}

assignPanel.addEventListener("submit", async (e) => {
  if (e.target.id !== "assign-form") return;
  e.preventDefault();

  const pilotId = Number($("#assign-pilot").val());
  const date = $("#assign-date").val();
  if (!pilotId) return alert("조종사를 선택해주세요.");

  const btn = e.target.querySelector("button[type=submit]");
  btn.disabled = true;
  try {
    await postJson(assignUrl(selectedId), { pilotId, date });
    const t = tasks.find((x) => x.id === selectedId);
    const p = pilots.find((x) => x.id === pilotId);
    Object.assign(t, { status: "ASSIGNED", pilotId, pilotName: p?.name, date });
    alert(`${t.farm} → ${p?.name} 조종사에게 배정했습니다.`);
    selectedId = null; // 배정 끝나면 목록에서 빠지므로 선택 해제
    renderAll();
  } catch (err) {
    console.error("배정 실패:", err);
    alert("배정에 실패했습니다. 다시 시도해주세요.");
    btn.disabled = false;
  }
});

assignPanel.addEventListener("click", async (e) => {
  if (e.target.id !== "unassign-btn") return;
  const t = tasks.find((x) => x.id === selectedId);
  if (!t || !confirm(`${t.farm}의 배정을 취소할까요?`)) return;
  try {
    await postJson(unassignUrl(t.id));
    Object.assign(t, { status: "READY", pilotId: null, pilotName: null });
    renderAll();
  } catch (err) {
    console.error("배정 취소 실패:", err);
    alert("배정 취소에 실패했습니다.");
  }
});

/* ===== 웹소켓: 조종사 위치 수신 (단장은 받기만 함) ===== */
function connect() {
  if (stompClient && stompClient.connected) {
    stompClient.disconnect();
  }

  const teamNo = $("#teamNoInput").val();
  const socket = new SockJS("/ws-drone");
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  stompClient.connect({}, () => {
    $("#status").text(`✅ ${teamNo}번 방제단 관제 연결됨`).css("color", "#238451");
    stompClient.subscribe("/sub/team/" + teamNo, (message) => {
      const d = JSON.parse(message.body);
      updatePilotMarker(d.pilotName, d.latitude, d.longitude);
    });
  }, () => {
    $("#status").text("🔄 연결 끊김 (3초 후 재연결...)").css("color", "#f2b705");
    setTimeout(connect, 3000);
  });
}

function updatePilotMarker(name, lat, lng) {
  pilotLastSeen[name] = Date.now();
  if (pilotMarkers[name]) {
    pilotMarkers[name].setLatLng([lat, lng]);
  } else {
    pilotMarkers[name] = L.marker([lat, lng], { icon: droneIcon, zIndexOffset: 1000 })
      .addTo(map)
      .bindPopup(`<b>🚁 ${esc(name)}</b>`);
  }

  if (followPilots) {
    const pts = Object.values(pilotMarkers).map((m) => m.getLatLng());
    if (pts.length === 1) map.panTo(pts[0]);
    else map.fitBounds(pts, { padding: [40, 40], maxZoom: 17 });
  } else if (name === selectedPilot) {
    // 선택한 조종사는 계속 따라감
    map.panTo([lat, lng]);
  }

  const onlineCount = Object.keys(pilotLastSeen).filter(isOnline).length;
  $("#autoStatus").text(`🚁 조종사 ${onlineCount}명 실시간 위치 수신 중`).css("color", "#238451");

  renderPilotList();
}

/* ===== 지도 조작 버튼 ===== */
function fitAll() {
  const pts = tasks.filter((t) => t.lat != null).map((t) => [t.lat, t.lng]);
  Object.values(pilotMarkers).forEach((m) => pts.push(m.getLatLng()));
  if (pts.length) map.fitBounds(pts, { padding: [30, 30], maxZoom: 16 });
}

function toggleFollow() {
  followPilots = !followPilots;
  $("#follow-btn")
    .text(followPilots ? "⏹️ 따라가기 중지" : "🎯 조종사 따라가기")
    .toggleClass("btn-real", !followPilots)
    .toggleClass("btn-stop", followPilots);
  if (followPilots) fitAll();
}

// 위치가 끊긴 조종사를 오프라인으로 바꾸기 위해 5초마다 갱신
setInterval(renderPilotList, 5000);

/* ===== 초기 실행 ===== */
document.addEventListener("DOMContentLoaded", async () => {
  connect();

  try {
    const [taskRes, pilotRes] = await Promise.all([fetch(TASKS_API), fetch(PILOTS_API)]);
    if (!taskRes.ok) throw new Error(`서버 응답 ${taskRes.status}`);
    tasks = await taskRes.json();
    if (pilotRes.ok) pilots = await pilotRes.json();
  } catch (err) {
    console.error("작업 목록 조회 실패:", err);
    farmList.innerHTML = `<li class="side-empty">농지 목록을 불러오지 못했습니다.</li>`;
    pilotList.innerHTML = `<li class="side-empty">조종사 목록을 불러오지 못했습니다.</li>`;
    setTimeout(() => map.invalidateSize(), 100); // API 실패해도 지도는 그리기
    return;
  }

  fitAll();
  renderAll();
});