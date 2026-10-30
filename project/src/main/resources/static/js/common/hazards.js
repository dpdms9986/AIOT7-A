/* ===== 위험 구역 (hazards.js) =====
 * 표는 서버(타임리프)가 그림. 여기서는
 *  1) 행의 data- 값을 읽어 지도에 마커 찍기
 *  2) 행/마커 클릭 → 지도 이동 + 상세 보여 주기
 *  3) 신고 팝업 열기/닫기 + 지도 클릭으로 위도·경도 채우기
 */
document.addEventListener("DOMContentLoaded", () => {
  if (typeof L === "undefined") return;           // 지도 라이브러리가 없으면 중단

  const DEFAULT_POS = [36.3504, 127.3845];        // 기본 위치 (대전)
  const COLORS = { "고위험": "#d93025", "중위험": "#f29900", "저위험": "#1e8e3e" };
  const RESOLVED_COLOR = "#9aa0a6";
  const TILE = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
  const TILE_OPT = { maxZoom: 19, attribution: "&copy; OpenStreetMap" };

  const badgeClass = (type) => (type === "고위험" ? "hz-high" : type === "중위험" ? "hz-mid" : "hz-low");

  /* ---------- 1) 큰 지도 + 마커 ---------- */
  const mapEl = document.getElementById("hz-map");
  const rows = [...document.querySelectorAll(".hz-row")];
  const markers = new Map();                      // 행 → 마커
  let map = null;

  if (mapEl) {
    map = L.map(mapEl).setView(DEFAULT_POS, 12);
    L.tileLayer(TILE, TILE_OPT).addTo(map);

    rows.forEach((row) => {
      const d = row.dataset;
      const lat = parseFloat(d.lat);
      const lng = parseFloat(d.lng);
      if (Number.isNaN(lat) || Number.isNaN(lng)) return;

      const resolved = d.status === "RESOLVED";
      const marker = L.circleMarker([lat, lng], {
        radius: 9,
        color: "#ffffff",
        weight: 2,
        fillColor: resolved ? RESOLVED_COLOR : (COLORS[d.type] || COLORS["중위험"]),
        fillOpacity: 0.9,
      }).addTo(map);

      marker.bindTooltip(d.title || "위험 구역");
      marker.on("click", () => select(row, true));
      markers.set(row, marker);
    });

    // 마커가 있으면 전부 보이게 화면 맞추기
    if (markers.size > 0) {
      const bounds = L.latLngBounds([...markers.values()].map((m) => m.getLatLng()));
      map.fitBounds(bounds, { padding: [30, 30], maxZoom: 15 });
    }
  }

  /* ---------- 2) 선택 → 지도 이동 + 상세 ---------- */
  const detail = document.getElementById("hz-detail");
  const hint = document.getElementById("hz-detail-hint");
  const $ = (id) => document.getElementById(id);

  function select(row, fromMarker) {
    rows.forEach((r) => r.classList.toggle("selected", r === row));
    const d = row.dataset;

    const marker = markers.get(row);
    if (map && marker) {
      map.setView(marker.getLatLng(), 15);
      marker.openTooltip();
    }

    // 상세 채우기 (글자는 textContent 로 안전하게)
    $("hz-d-title").textContent = d.title || "-";
    $("hz-d-type").textContent = d.type || "-";
    $("hz-d-type").className = "hz-badge " + badgeClass(d.type);
    const active = d.status === "ACTIVE";
    $("hz-d-status").textContent = active ? "주의 요망" : "해소됨";
    $("hz-d-status").className = "hz-status " + (active ? "active" : "resolved");
    $("hz-d-author").textContent = d.author || "-";
    $("hz-d-date").textContent = d.date || "-";
    $("hz-d-coord").textContent = `${d.lat}, ${d.lng}`;
    $("hz-d-content").textContent = d.content || "";

    if (detail) detail.hidden = false;
    if (hint) hint.hidden = true;
    if (fromMarker) row.scrollIntoView({ block: "nearest", behavior: "smooth" });
  }

  rows.forEach((row) => {
    row.addEventListener("click", (e) => {
      if (e.target.closest("form, button")) return;   // 버튼 누를 땐 선택 안 함
      select(row, false);
    });
  });

  /* ---------- 2-1) 상세보기 팝업 (내용이 길어도 전부 보이게) ---------- */
  const viewModal = document.getElementById("hz-view-modal");

  function openView(row) {
    const d = row.dataset;
    $("hz-v-title").textContent = d.title || "-";
    $("hz-v-type").textContent = d.type || "-";
    $("hz-v-type").className = "hz-badge " + badgeClass(d.type);
    const active = d.status === "ACTIVE";
    $("hz-v-status").textContent = active ? "주의 요망" : "해소됨";
    $("hz-v-status").className = "hz-status " + (active ? "active" : "resolved");
    $("hz-v-author").textContent = d.author || "-";
    $("hz-v-date").textContent = d.date || "-";
    $("hz-v-coord").textContent = `${d.lat}, ${d.lng}`;
    $("hz-v-content").textContent = d.content || "";
    viewModal.hidden = false;
  }
  const closeView = () => { if (viewModal) viewModal.hidden = true; };

  document.querySelectorAll(".hz-view-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      const row = btn.closest(".hz-row");
      select(row, false);      // 지도도 같이 이동
      openView(row);
    });
  });
  viewModal?.querySelectorAll("[data-hz-view-close]").forEach((b) => b.addEventListener("click", closeView));
  viewModal?.addEventListener("click", (e) => { if (e.target === viewModal) closeView(); });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && viewModal && !viewModal.hidden) closeView(); });

  /* ---------- 3) 신고 팝업 ---------- */
  const modal = document.getElementById("hz-modal");
  const openBtn = document.getElementById("hz-open-btn");
  const latInput = document.getElementById("hz-lat");
  const lngInput = document.getElementById("hz-lng");
  let pickMap = null;
  let pickMarker = null;

  function setPick(lat, lng) {
    latInput.value = lat.toFixed(6);
    lngInput.value = lng.toFixed(6);
    if (pickMarker) pickMarker.setLatLng([lat, lng]);
  }

  function openModal() {
    if (!modal) return;
    modal.hidden = false;

    // 팝업 안 작은 지도 (처음 열 때 한 번만 만들기)
    if (!pickMap) {
      const start = map ? map.getCenter() : L.latLng(DEFAULT_POS);
      pickMap = L.map("hz-pick-map").setView(start, 13);
      L.tileLayer(TILE, TILE_OPT).addTo(pickMap);
      pickMarker = L.marker(start).addTo(pickMap);
      setPick(start.lat, start.lng);
      pickMap.on("click", (e) => setPick(e.latlng.lat, e.latlng.lng));
    }
    // 숨겨져 있던 지도는 크기를 다시 계산해야 제대로 보임
    setTimeout(() => pickMap.invalidateSize(), 0);
    modal.querySelector("input[name='title']")?.focus();
  }

  function closeModal() {
    if (modal) modal.hidden = true;
  }

  // 위도·경도를 직접 고치면 작은 지도 마커도 이동
  [latInput, lngInput].forEach((input) => {
    input?.addEventListener("change", () => {
      const lat = parseFloat(latInput.value);
      const lng = parseFloat(lngInput.value);
      if (Number.isNaN(lat) || Number.isNaN(lng) || !pickMap) return;
      pickMarker.setLatLng([lat, lng]);
      pickMap.setView([lat, lng]);
    });
  });

  openBtn?.addEventListener("click", openModal);
  modal?.querySelectorAll("[data-hz-close]").forEach((b) => b.addEventListener("click", closeModal));
  modal?.addEventListener("click", (e) => { if (e.target === modal) closeModal(); });   // 바깥 클릭
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && modal && !modal.hidden) closeModal(); });

    /* ---------- 4) 주소 검색 → 위도·경도 자동 입력 ---------- */
  const addrInput = document.getElementById("hz-addr");
  const addrBtn = document.getElementById("hz-addr-btn");
  const addrMsg = document.getElementById("hz-addr-msg");

  // 주소 → 좌표 (OpenStreetMap Nominatim, 무료)
  async function geocode(address) {
    const url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=kr&q="
              + encodeURIComponent(address);
    const res = await fetch(url, { headers: { "Accept-Language": "ko" } });
    if (!res.ok) return null;
    const list = await res.json();
    return list.length ? { lat: parseFloat(list[0].lat), lng: parseFloat(list[0].lon) } : null;
  }

  // 못 찾으면 주소 끝부분(번지 등)을 하나씩 떼면서 다시 시도 → 동네라도 맞추기
  async function geocodeLoose(address) {
    let words = address.trim().split(/\s+/);
    while (words.length >= 2) {
      const pos = await geocode(words.join(" "));
      if (pos) return { pos, exact: words.join(" ") === address.trim() };
      words = words.slice(0, -1);
    }
    return null;
  }

  async function moveToAddress(roadAddr, jibunAddr) {
    addrMsg.textContent = "위치를 찾는 중이에요...";
    const found = (await geocodeLoose(roadAddr)) || (jibunAddr ? await geocodeLoose(jibunAddr) : null);

    if (!found) {
      addrMsg.textContent = "주소 위치를 찾지 못했어요. 지도에서 직접 위치를 눌러 주세요.";
      if (window.showPopup) showPopup("주소 위치를 찾지 못했어요.\n지도에서 직접 위치를 눌러 주세요.", "warn");
      return;
    }

    const { lat, lng } = found.pos;
    setPick(lat, lng);
    pickMap.setView([lat, lng], found.exact ? 17 : 15);
    addrMsg.textContent = found.exact
      ? "주소 위치로 맞췄어요. 필요하면 지도를 클릭해 미세 조정하세요."
      : "정확한 번지는 못 찾아서 근처로 맞췄어요. 지도를 클릭해 정확한 위치를 찍어 주세요.";
  }

  addrBtn?.addEventListener("click", () => {
    // 다음(카카오) 우편번호 검색 (script.html 에서 이미 불러옴)
    if (typeof daum === "undefined" || !daum.Postcode) {
      if (window.showPopup) showPopup("주소 검색을 불러오지 못했어요. 지도에서 직접 위치를 눌러 주세요.", "warn");
      return;
    }
    new daum.Postcode({
      oncomplete: (data) => {
        const road = data.roadAddress || data.address;
        addrInput.value = road;
        moveToAddress(road, data.jibunAddress || data.autoJibunAddress);
      },
    }).open();
  });

});