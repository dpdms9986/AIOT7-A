// 작업 배정 : 농지 다각형 1/N 분할 미리보기 + 구역별 조종사 매핑
// 흐름) 농지 체크 → 모영역(A) 표시 → 투입 조종사 수(N)만큼 분할 → 구역마다 담당 조종사 선택 → 배정 확정
// 의존: field-split.js, Leaflet, Turf.js / task-assignment.js 와 같은 폼(#assign-form)을 사용
document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector("#assign-form");
  if (!form) return;                                   // 현황/이력 탭에서는 아무것도 안 함

  const $ = (sel) => document.querySelector(sel);
  const countInput = $("#splitCount");
  const decBtn = $("#fs-dec");
  const incBtn = $("#fs-inc");
  const countHint = $("#fs-count-hint");
  const errorBox = $("#fs-error");
  const cardsBox = $("#resultCards");
  const emptyBox = $("#fs-empty");
  const fallbackNote = $("#fs-fallback-note");
  const mapEmpty = $("#fs-map-empty");
  const MAP_EMPTY_DEFAULT = mapEmpty.innerHTML;

  const showError = (msg) => { errorBox.textContent = msg; errorBox.hidden = false; };
  const clearError = () => { errorBox.textContent = ""; errorBox.hidden = true; };

  if (!window.L || !window.turf || !window.FieldSplit) {
    showError("지도 또는 공간 연산 라이브러리를 불러오지 못했습니다. 인터넷 연결을 확인해 주세요.");
    return;
  }
  const { COLORS } = FieldSplit;

  /* ===================== 상태 / 입력 읽기 ===================== */
  const landChecks = () => [...form.querySelectorAll(".land-check")];
  const selectedLands = () => landChecks().filter((c) => c.checked);
  const pilotChecks = () => [...form.querySelectorAll(".task-pilot-check")];
  const checkedPilots = () => pilotChecks()
    .filter((c) => c.checked)
    .map((c) => ({
      no: c.value,
      name: c.closest(".task-pilot").querySelector("label > span")?.textContent.trim() || `조종사 ${c.value}`,
    }));

  let targetCheckbox = null;     // 지도에 보여줄 농지 (가장 최근에 체크한 것)
  let pieces = [];               // 분할 결과 (GeoJSON Feature, 서→동 순)
  let pieceLayers = [];          // 조각별 Leaflet 레이어 (툴팁/강조용)
  let assignment = [];           // 조각 index → 조종사 번호 ("" = 미지정)

  /* ===================== 지도 ===================== */
  const map = L.map("fs-map", { scrollWheelZoom: false }).setView([36.3504, 127.3845], 13);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors",
  }).addTo(map);
  let resultLayer = null;
  let originalLayer = null;

  const clearMapLayers = () => {
    if (resultLayer) map.removeLayer(resultLayer);
    if (originalLayer) map.removeLayer(originalLayer);
    resultLayer = null;
    originalLayer = null;
    pieceLayers = [];
  };

  const pad2 = (n) => String(n).padStart(2, "0");
  const pilotNameOf = (pilotNo) => checkedPilots().find((p) => p.no === pilotNo)?.name || "";
  const tooltipText = (index) => {
    const name = pilotNameOf(assignment[index]);
    return `구역 ${pad2(pieces[index].properties.sliceIndex + 1)} · ${name || "담당 미지정"}`;
  };

  const formatArea = (m2) => (m2 >= 1_000_000
    ? { value: (m2 / 1_000_000).toFixed(2), unit: "km²" }
    : { value: Math.round(m2).toLocaleString("ko-KR"), unit: "m²" });

  /* ===================== 조종사 수 ↔ 분할 개수 동기화 ===================== */
  const syncCountToPilots = () => {
    const n = checkedPilots().length;
    const synced = n >= 2;
    if (synced) countInput.value = String(Math.min(12, n));
    countInput.readOnly = synced;
    decBtn.disabled = synced;
    incBtn.disabled = synced;
    countHint.textContent = synced
      ? `투입 조종사 ${n}명에 맞춰 ${Math.min(12, n)}조각으로 나뉩니다.`
      : "투입 조종사를 2명 이상 체크하면 그 인원수에 맞춰 자동으로 바뀝니다.";
  };

  /* 조각별 담당 조종사 기본값: 이전 선택이 아직 유효하면 유지, 비면 안 쓴 조종사를 순서대로 */
  const refreshAssignment = () => {
    const pilots = checkedPilots().map((p) => p.no);
    const next = pieces.map((_, i) => (pilots.includes(assignment[i]) ? assignment[i] : ""));
    const used = new Set(next.filter(Boolean));
    const free = pilots.filter((no) => !used.has(no));
    next.forEach((v, i) => { if (!v && free.length) next[i] = free.shift(); });
    assignment = next;
  };

  /* ===================== 렌더링 ===================== */
  const renderCards = () => {
    cardsBox.replaceChildren();
    emptyBox.hidden = pieces.length > 0;
    cardsBox.hidden = pieces.length === 0;
    $("#fs-result-count").textContent = String(pieces.length);

    const areas = pieces.map((p) => turf.area(p));
    const total = areas.reduce((sum, a) => sum + a, 0);
    const pilots = checkedPilots();

    pieces.forEach((piece, index) => {
      const sliceNo = piece.properties.sliceIndex;
      const color = COLORS[sliceNo % COLORS.length];
      const share = total > 0 ? (areas[index] / total) * 100 : 0;
      const area = formatArea(areas[index]);

      const card = document.createElement("article");
      card.className = "result-card";
      card.dataset.piece = String(index);
      card.innerHTML = `
        <div class="result-card-top">
          <div class="piece-title"><span class="piece-color" style="background:${color.fill}"></span>구역 ${pad2(sliceNo + 1)}</div>
        </div>
        <div class="piece-metrics">
          <span class="piece-area">${area.value}</span>
          <span class="piece-unit">${area.unit}</span>
          <span class="piece-share">${share.toFixed(1)}%</span>
        </div>
        <div class="piece-progress" aria-label="전체 면적의 ${share.toFixed(1)}%"><span style="width:${Math.min(share, 100)}%;background:${color.fill}"></span></div>`;

      if (pilots.length) {
        const select = document.createElement("select");
        select.className = "piece-pilot";
        select.setAttribute("aria-label", `구역 ${pad2(sliceNo + 1)} 담당 조종사`);
        select.append(new Option("담당 조종사 선택", ""));
        pilots.forEach((p) => select.append(new Option(p.name, p.no)));
        select.value = assignment[index] || "";
        select.addEventListener("change", () => onPilotPicked(index, select.value));
        card.append(select);
      } else {
        const hint = document.createElement("div");
        hint.className = "piece-pilot-name";
        hint.textContent = "투입 조종사를 선택하면 여기서 담당자를 고를 수 있어요.";
        card.append(hint);
      }
      cardsBox.append(card);
    });
  };

  const renderMap = (coords) => {
    clearMapLayers();
    resultLayer = L.featureGroup();
    pieces.forEach((feature, index) => {
      const color = COLORS[feature.properties.sliceIndex % COLORS.length];
      const layer = L.geoJSON(feature, {
        style: { color: color.line, weight: 2, opacity: 0.95, fillColor: color.fill, fillOpacity: 0.32 },
        interactive: true,
      });
      layer.bindTooltip(tooltipText(index), { sticky: true, direction: "top" });
      layer.on("click", () => highlightPiece(index));
      layer.addTo(resultLayer);
      pieceLayers[index] = layer;
    });
    resultLayer.addTo(map);

    originalLayer = L.polygon(FieldSplit.closeRing(coords), {
      color: "#304f43", weight: 1.5, dashArray: "5 5", fill: false, interactive: false,
    }).addTo(map);

    map.fitBounds(L.latLngBounds(coords), { padding: [30, 30], maxZoom: 18, animate: true });
    window.setTimeout(() => map.invalidateSize(), 100);
  };

  /* 지도의 조각을 누르면 해당 카드 강조 + 담당자 안내 */
  const highlightPiece = (index) => {
    cardsBox.querySelectorAll(".result-card").forEach((c) =>
      c.classList.toggle("is-active", c.dataset.piece === String(index)));
    pieceLayers.forEach((layer, i) => layer.setStyle({ weight: i === index ? 4 : 2 }));
    pieceLayers[index]?.openTooltip?.();
    cardsBox.querySelector(`.result-card[data-piece="${index}"]`)
      ?.scrollIntoView({ block: "nearest", behavior: "smooth" });
  };

  /* 카드에서 조종사를 고르면: 이미 다른 구역이 쓰던 조종사는 서로 바꿔줌(한 명이 두 구역 X) */
  const onPilotPicked = (index, pilotNo) => {
    if (pilotNo) {
      const other = assignment.findIndex((v, i) => i !== index && v === pilotNo);
      if (other >= 0) assignment[other] = assignment[index];
    }
    assignment[index] = pilotNo;
    cardsBox.querySelectorAll(".piece-pilot").forEach((sel, i) => { sel.value = assignment[i] || ""; });
    pieces.forEach((_, i) => pieceLayers[i]?.setTooltipContent(tooltipText(i)));
    highlightPiece(index);
  };

  const resetView = (message = "농지를 선택해 주세요") => {
    pieces = [];
    assignment = [];
    clearMapLayers();
    renderCards();
    mapEmpty.innerHTML = MAP_EMPTY_DEFAULT;
    mapEmpty.hidden = false;
    fallbackNote.hidden = true;
    $("#fs-target").textContent = "농지를 선택하면 모영역(A)이 지도에 표시됩니다.";
    $("#fs-sum-count").textContent = "-";
    $("#fs-sum-area").textContent = "-";
    $("#fs-map-region").textContent = message;
  };

  /* 모영역(A) 불러오기 → 분할 → 그리기 */
  const renderSplit = () => {
    clearError();
    syncCountToPilots();

    if (!targetCheckbox || !targetCheckbox.checked) {
      resetView();
      return;
    }
    const d = targetCheckbox.dataset;
    const saved = FieldSplit.parsePolygon(d.polygon);
    const coords = saved || FieldSplit.fallbackPolygon(Number(d.lat), Number(d.lng), Number(d.area));
    fallbackNote.hidden = !!saved;

    $("#fs-target").textContent = `${d.address} (모영역 A)`;
    $("#fs-map-region").textContent = d.address;
    $("#fs-sum-area").textContent = `${Number(d.area || 0).toLocaleString("ko-KR")} m²`;
    mapEmpty.hidden = true;

    if (!coords) {
      resetView(d.address);
      mapEmpty.hidden = false;
      mapEmpty.textContent = "이 농지는 외곽선도 위치 좌표도 없어 미리보기를 만들 수 없습니다.";
      return;
    }

    const n = Number(countInput.value);
    try {
      pieces = FieldSplit.splitPolygonIntoN(coords, n);
      if (!pieces.length) throw new Error("다각형과 겹치는 조각이 없습니다. 좌표를 확인해 주세요.");
    } catch (error) {
      pieces = [];
      assignment = [];
      renderCards();
      clearMapLayers();
      originalLayer = L.polygon(FieldSplit.closeRing(coords), {
        color: "#304f43", weight: 1.5, dashArray: "5 5", fill: false, interactive: false,
      }).addTo(map);
      map.fitBounds(L.latLngBounds(coords), { padding: [30, 30], maxZoom: 18 });
      $("#fs-sum-count").innerHTML = `${pad2(n)} <small>예정</small>`;
      showError(error.message || "분할 중 문제가 발생했습니다.");
      return;
    }

    refreshAssignment();
    $("#fs-sum-count").innerHTML = `${pad2(pieces.length)} <small>조각</small>`;
    renderCards();
    renderMap(coords);
  };

  /* ===================== 이벤트 ===================== */
  // 농지 체크: 가장 최근에 체크한 농지를 지도에 보여줌
  const pickTarget = () => {
    if (targetCheckbox && targetCheckbox.checked) return;
    const list = selectedLands();
    targetCheckbox = list.length ? list[list.length - 1] : null;
  };

  landChecks().forEach((c) => c.addEventListener("change", () => {
    if (c.checked) targetCheckbox = c;
    else pickTarget();
    renderSplit();
  }));
  $("#check-all")?.addEventListener("change", () => {
    targetCheckbox = null;
    pickTarget();
    renderSplit();
  });

  // 투입 조종사 체크 → 분할 개수 자동 동기화
  pilotChecks().forEach((c) => c.addEventListener("change", renderSplit));

  // 조각 수 직접 조절 (조종사 2명 미만일 때만 가능 = 미리보기용)
  const changeCount = (amount) => {
    const current = Number(countInput.value) || 2;
    countInput.value = String(Math.min(12, Math.max(2, current + amount)));
    renderSplit();
  };
  decBtn.addEventListener("click", () => changeCount(-1));
  incBtn.addEventListener("click", () => changeCount(1));
  countInput.addEventListener("change", renderSplit);

  $("#fs-open-pilots").addEventListener("click", () => $("#open-assign").click());

  /* ----- 배정 확정 -----
     백엔드 규격(AssignRequest: landNos, quotas[조종사번호])은 그대로 사용.
     농지 1건이면 '구역 01 담당 조종사'에게 quota 1, 나머지는 0 으로 맞춘 뒤 기존 폼 전송. */
  $("#fs-confirm").addEventListener("click", () => {
    const lands = selectedLands();
    const pilots = checkedPilots();
    if (!lands.length) { alert("배정할 농지를 먼저 선택하세요."); return; }
    if (!pilots.length) { alert("투입할 조종사를 선택하세요."); return; }

    if (lands.length === 1 && pieces.length) {
      const first = assignment[0] || pilots[0].no;
      pilotChecks().filter((c) => c.checked).forEach((c) => {
        c.closest(".task-pilot").querySelector(".task-quota-input").value = c.value === first ? 1 : 0;
      });
    }
    form.requestSubmit();
  });

  // 농지 1건 + 조종사 여러 명이면, 조각이 저장되지 않는다는 점을 확정 직전에 한 번 더 알림
  form.addEventListener("submit", (e) => {
    if (e.defaultPrevented) return;                       // 기존 검사에서 이미 막힘
    const pilots = checkedPilots();
    if (selectedLands().length !== 1 || pilots.length < 2) return;
    const receiver = pilotChecks()
      .filter((c) => c.checked)
      .find((c) => Number(c.closest(".task-pilot").querySelector(".task-quota-input").value) > 0);
    const name = receiver ? pilotNameOf(receiver.value) : "첫 번째 조종사";
    const ok = window.confirm(
      `현재는 농지 1건을 조종사 1명에게만 저장할 수 있어요.\n` +
      `조각 나누기는 미리보기용이며, 이 농지 전체가 '${name}' 조종사에게 배정됩니다.\n계속할까요?`);
    if (!ok) e.preventDefault();
  });

  /* ===================== 처음 열렸을 때 ===================== */
  // (농지 관리에서 ?landNo= 로 넘어와 미리 체크돼 있으면 바로 모영역을 표시)
  pickTarget();
  renderSplit();
  window.setTimeout(() => map.invalidateSize(), 200);
});