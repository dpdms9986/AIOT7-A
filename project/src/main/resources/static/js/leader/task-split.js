// 작업 배정 : 영역 선택 → N분할 → 구역별 조종사 매핑 → 배정 확정
// 1) 영역 : 등록된 미배정 농지를 체크하거나, 지도에서 직접 꼭짓점을 찍어 그린다
// 2) 분할 : 조각 수 N 은 조종사 수와 상관없이 단장이 정한다 (경도 기준 N등분)
// 3) 매핑 : 구역마다 담당 조종사를 고른다 (한 조종사가 여러 구역 가능 → 숙련도에 따라 넓게/좁게 배분)
// 의존: field-split.js, Leaflet, Turf.js / task-assignment.js 와 같은 폼(#assign-form)
document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector("#assign-form");
  if (!form) return;                                   // 현황/이력 탭에서는 아무것도 안 함

  const $ = (sel) => document.querySelector(sel);
  const countInput = $("#splitCount");
  const decBtn = $("#fs-dec");
  const incBtn = $("#fs-inc");
  const errorBox = $("#fs-error");
  const cardsBox = $("#resultCards");
  const emptyBox = $("#fs-empty");
  const fallbackNote = $("#fs-fallback-note");
  const mapEmpty = $("#fs-map-empty");
  const MAP_EMPTY_DEFAULT = mapEmpty.innerHTML;
  const drawBtn = $("#fs-draw-btn");
  const drawStatus = $("#fs-draw-status");
  const drawnWrap = $("#fs-drawn-wrap");
  const drawnAddress = $("#fs-drawn-address");
  const coordsBox = $("#fs-coords");
  const summaryBox = $("#fs-pilot-summary");
  const copyMsg = $("#fs-copy-msg");
  const pieceInput = $("#piece-assignments-json");
  const hidden = (name) => form.querySelector(`[name='${name}']`);

  const showError = (msg) => { errorBox.textContent = msg; errorBox.hidden = false; };
  const clearError = () => { errorBox.textContent = ""; errorBox.hidden = true; };

  if (!window.L || !window.turf || !window.FieldSplit) {
    showError("지도 또는 공간 연산 라이브러리를 불러오지 못했습니다. 인터넷 연결을 확인해 주세요.");
    return;
  }
  const { COLORS } = FieldSplit;

  // 프롬프트(FieldSplit)의 예제 좌표
  const SAMPLE_COORDINATES = [
    [37.5467, 127.0328], [37.5500, 127.0360], [37.5491, 127.0414], [37.5467, 127.0457],
    [37.5424, 127.0433], [37.5417, 127.0382], [37.5432, 127.0335],
  ];
  const DRAW_HINT = "등록된 농지를 체크하거나, 지도를 눌러 꼭짓점을 찍어 영역을 잡을 수 있어요.";
  const DRAW_LABEL = "✏️ 지도에서 직접 영역 그리기";

  /* ===================== 상태 ===================== */
  const landChecks = () => [...form.querySelectorAll(".land-check")];
  const selectedLands = () => landChecks().filter((c) => c.checked);

  // 배정 가능한 조종사 = 모달 목록의 조종사 중 작업 중(disabled)이 아닌 사람. 체크 여부와 상관없음
  const allPilots = () => [...form.querySelectorAll(".task-pilot-check")]
    .filter((c) => !c.disabled)
    .map((c) => ({
      no: c.value,
      name: c.closest(".task-pilot").querySelector("label > span")?.textContent.trim() || `조종사 ${c.value}`,
    }));

  let targetCheckbox = null;   // 지도에 보여줄 등록 농지 (가장 최근에 체크한 것)
  let drawn = null;            // 직접 그린 영역 [[lat,lng],...]
  let currentCoords = null;    // 지금 지도에 표시 중인 모영역
  let lastKey = "";            // 영역이 바뀌면 담당 매핑을 초기화하기 위한 값
  let pieces = [];             // 분할 결과 (GeoJSON Feature, 서→동 순)
  let pieceLayers = [];
  let assignment = [];         // 구역 index → 조종사 번호 ("" = 미지정)

  /* ===================== 도우미 ===================== */
  const pad2 = (n) => String(n).padStart(2, "0");
  const pilotNameOf = (no) => allPilots().find((p) => p.no === no)?.name || "";
  const formatArea = (m2) => (m2 >= 1_000_000
    ? { value: (m2 / 1_000_000).toFixed(2), unit: "km²" }
    : { value: Math.round(m2).toLocaleString("ko-KR"), unit: "m²" });

  const areaOfCoords = (coords) => {
    try {
      return turf.area(turf.polygon([FieldSplit.closeRing(coords).map(([lat, lng]) => [lng, lat])]));
    } catch { return 0; }
  };

  const toLatLng = (geometry) => {
    const ring = (r) => r.map(([lng, lat]) => [lat, lng]);
    if (geometry.type === "Polygon") return geometry.coordinates.map(ring);
    if (geometry.type === "MultiPolygon") return geometry.coordinates.map((poly) => poly.map(ring));
    return geometry.coordinates;
  };

  let copyTimer = null;
  const setCopyMsg = (msg) => {
    copyMsg.textContent = msg;
    clearTimeout(copyTimer);
    copyTimer = setTimeout(() => { copyMsg.textContent = ""; }, 3000);
  };
  const copyText = (text, okMsg) => {
    const fallback = () => {
      const t = document.createElement("textarea");
      t.value = text; t.setAttribute("readonly", "");
      t.style.position = "fixed"; t.style.opacity = "0";
      document.body.append(t); t.select();
      const ok = document.execCommand("copy");
      t.remove();
      setCopyMsg(ok ? okMsg : "복사하지 못했어요. 브라우저 권한을 확인해 주세요.");
    };
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => setCopyMsg(okMsg)).catch(fallback);
    } else {
      fallback();
    }
  };

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

  const tooltipText = (index) => {
    const name = pilotNameOf(assignment[index]);
    return `구역 ${pad2(pieces[index].properties.sliceIndex + 1)} · ${name || "담당 미지정"}`;
  };

  /* ===================== 1단계: 지도에서 직접 영역 그리기 ===================== */
  const drawer = FieldSplit.createDrawer(map, {
    onStatus: (message, state) => { drawStatus.textContent = message; drawStatus.dataset.state = state || ""; },
  });

  const resetDrawUi = () => {
    drawBtn.textContent = DRAW_LABEL;
    drawBtn.setAttribute("aria-pressed", "false");
  };
  const cancelDrawing = () => {
    if (drawer.isActive()) { drawer.clear(); resetDrawUi(); drawStatus.textContent = DRAW_HINT; }
  };
  const clearDrawn = () => { drawn = null; drawnWrap.hidden = true; };
  const uncheckLands = () => {
    selectedLands().forEach((c) => {
      c.checked = false;
      c.dispatchEvent(new Event("change", { bubbles: true }));   // task-assignment.js 의 선택 합계도 갱신
    });
  };

  const useDrawn = (coords) => {
    cancelDrawing();
    uncheckLands();                                  // 등록 농지 선택은 해제
    drawn = coords.map(([lat, lng]) => [lat, lng]);
    drawnWrap.hidden = false;
    renderSplit();
  };

  drawBtn.addEventListener("click", () => {
    if (drawer.isActive()) {                         // 그리기 완료
      const result = drawer.finish();
      resetDrawUi();
      if (result.ok) { drawer.clear(); useDrawn(result.coordinates); }
      return;
    }
    drawer.clear();                                  // 새로 그리기 시작
    clearDrawn();
    uncheckLands();
    resetView("지도에서 영역을 그리는 중");
    drawer.start();
    drawBtn.textContent = "✔ 그리기 완료";
    drawBtn.setAttribute("aria-pressed", "true");
  });

  $("#fs-coords-apply").addEventListener("click", () => {
    const parsed = FieldSplit.parsePolygon(coordsBox.value.trim());
    if (!parsed) {
      showError("좌표 형식을 확인해 주세요. 예: [[37.54, 127.03], [37.55, 127.04], [37.53, 127.05]]");
      return;
    }
    clearError();
    useDrawn(parsed);
  });

  $("#fs-sample-btn").addEventListener("click", () => {
    coordsBox.value = JSON.stringify(SAMPLE_COORDINATES);
    clearError();
    useDrawn(SAMPLE_COORDINATES);
  });

  /* ===================== 3단계: 구역별 담당 조종사 ===================== */
  const refreshAssignment = () => {
    const valid = new Set(allPilots().map((p) => p.no));
    assignment = pieces.map((_, i) => (valid.has(assignment[i]) ? assignment[i] : ""));
  };

  // 조종사별 담당 구역 · 면적 요약 (숙련도에 따라 넓게/좁게 줬는지 한눈에 보기)
  const renderPilotSummary = () => {
    if (!pieces.length) { summaryBox.textContent = ""; return; }
    const areas = pieces.map((p) => turf.area(p));
    const total = areas.reduce((s, a) => s + a, 0);
    const byPilot = new Map();
    let unassigned = 0;
    pieces.forEach((p, i) => {
      const no = assignment[i];
      if (!no) { unassigned += 1; return; }
      const e = byPilot.get(no) || { zones: [], area: 0 };
      e.zones.push(pad2(p.properties.sliceIndex + 1));
      e.area += areas[i];
      byPilot.set(no, e);
    });
    const lines = [...byPilot.entries()].map(([no, e]) => {
      const a = formatArea(e.area);
      const pct = total > 0 ? ((e.area / total) * 100).toFixed(1) : "0.0";
      return `${pilotNameOf(no)} · 구역 ${e.zones.join(", ")} · ${a.value}${a.unit} (${pct}%)`;
    });
    if (unassigned) lines.push(`담당 미지정 구역 ${unassigned}개`);
    summaryBox.textContent = lines.length ? lines.join("\n") : "구역마다 담당 조종사를 선택해 주세요.";
  };

  const renderCards = () => {
    cardsBox.replaceChildren();
    emptyBox.hidden = pieces.length > 0;
    cardsBox.hidden = pieces.length === 0;
    $("#fs-result-count").textContent = String(pieces.length);

    const areas = pieces.map((p) => turf.area(p));
    const total = areas.reduce((sum, a) => sum + a, 0);
    const pilots = allPilots();

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
        hint.textContent = "배정 가능한 조종사가 없어요.";
        card.append(hint);
      }

      const copyBtn = document.createElement("button");
      copyBtn.type = "button";
      copyBtn.className = "dash-btn-outline task-mini-btn";
      copyBtn.textContent = "좌표 복사";
      copyBtn.addEventListener("click", () => {
        copyText(JSON.stringify(toLatLng(piece.geometry), null, 2), `구역 ${pad2(sliceNo + 1)} 좌표를 복사했어요.`);
      });
      card.append(copyBtn);

      cardsBox.append(card);
    });
    renderPilotSummary();
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

  const highlightPiece = (index) => {
    cardsBox.querySelectorAll(".result-card").forEach((c) =>
      c.classList.toggle("is-active", c.dataset.piece === String(index)));
    pieceLayers.forEach((layer, i) => layer.setStyle({ weight: i === index ? 4 : 2 }));
    pieceLayers[index]?.openTooltip?.();
    cardsBox.querySelector(`.result-card[data-piece="${index}"]`)
      ?.scrollIntoView({ block: "nearest", behavior: "smooth" });
  };

  // 카드에서 조종사를 고르면 그대로 저장 (한 조종사가 여러 구역을 맡을 수 있음)
  const onPilotPicked = (index, pilotNo) => {
    assignment[index] = pilotNo;
    pieces.forEach((_, i) => pieceLayers[i]?.setTooltipContent(tooltipText(i)));
    renderPilotSummary();
    highlightPiece(index);
  };

  const resetView = (message = "농지를 선택해 주세요") => {
    pieces = [];
    assignment = [];
    lastKey = "";
    currentCoords = null;
    clearMapLayers();
    renderCards();
    mapEmpty.innerHTML = MAP_EMPTY_DEFAULT;
    mapEmpty.hidden = false;
    fallbackNote.hidden = true;
    $("#fs-target").textContent = "위 목록에서 농지를 고르거나, 지도에서 직접 영역을 그려 주세요.";
    $("#fs-sum-count").textContent = "-";
    $("#fs-sum-area").textContent = "-";
    $("#fs-map-region").textContent = message;
  };

  /* ===================== 2단계: 모영역 → N분할 → 그리기 ===================== */
  function renderSplit() {
    clearError();

    let coords = null;
    let label = "";
    let areaM2 = 0;
    let isFallback = false;
    let key = "";

    if (drawn) {
      coords = drawn;
      label = "직접 그린 영역";
      areaM2 = areaOfCoords(drawn);
      key = `drawn:${JSON.stringify(drawn)}`;
    } else if (targetCheckbox && targetCheckbox.checked) {
      const d = targetCheckbox.dataset;
      const saved = FieldSplit.parsePolygon(d.polygon);
      coords = saved || FieldSplit.fallbackPolygon(Number(d.lat), Number(d.lng), Number(d.area));
      isFallback = !saved;
      label = d.address;
      areaM2 = Number(d.area || 0);
      key = `land:${targetCheckbox.value}`;
    } else {
      if (!drawer.isActive()) resetView();
      return;
    }

    if (key !== lastKey) { assignment = []; lastKey = key; }   // 영역이 바뀌면 담당 매핑 초기화
    fallbackNote.hidden = !isFallback;
    $("#fs-target").textContent = `${label} (모영역 A)`;
    $("#fs-map-region").textContent = label;
    $("#fs-sum-area").textContent = `${Math.round(areaM2).toLocaleString("ko-KR")} m²`;
    mapEmpty.hidden = true;

    if (!coords) {
      resetView(label);
      mapEmpty.hidden = false;
      mapEmpty.textContent = "이 농지는 외곽선도 위치 좌표도 없어 미리보기를 만들 수 없습니다.";
      return;
    }
    currentCoords = coords;

    const n = Math.min(12, Math.max(2, Math.round(Number(countInput.value)) || 2));
    countInput.value = String(n);
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
  }

  /* ===================== 이벤트 ===================== */
  const pickTarget = () => {
    if (targetCheckbox && targetCheckbox.checked) return;
    const list = selectedLands();
    targetCheckbox = list.length ? list[list.length - 1] : null;
  };

  // 등록 농지 체크: 가장 최근에 체크한 농지를 지도에 보여줌 (직접 그린 영역은 해제)
  landChecks().forEach((c) => c.addEventListener("change", () => {
    if (c.checked) { cancelDrawing(); clearDrawn(); targetCheckbox = c; }
    else pickTarget();
    renderSplit();
  }));
  $("#check-all")?.addEventListener("change", () => {
    if ($("#check-all").checked) { cancelDrawing(); clearDrawn(); }
    targetCheckbox = null;
    pickTarget();
    renderSplit();
  });

  // 조각 수는 단장이 직접 정한다 (조종사 수와 무관)
  const changeCount = (amount) => {
    const current = Number(countInput.value) || 3;
    countInput.value = String(Math.min(12, Math.max(2, current + amount)));
    renderSplit();
  };
  decBtn.addEventListener("click", () => changeCount(-1));
  incBtn.addEventListener("click", () => changeCount(1));
  countInput.addEventListener("change", renderSplit);

  $("#fs-fit").addEventListener("click", () => {
    if (!currentCoords) return;
    map.fitBounds(L.latLngBounds(currentCoords), { padding: [30, 30], maxZoom: 18, animate: true });
  });

  $("#fs-copy-all").addEventListener("click", () => {
    if (!pieces.length) { setCopyMsg("먼저 영역을 분할해 주세요."); return; }
    const output = pieces.map((feature, i) => ({
      zone: pad2(feature.properties.sliceIndex + 1),
      pilot: pilotNameOf(assignment[i]) || null,
      areaSqm: Math.round(turf.area(feature)),
      type: feature.geometry.type,
      coordinates: toLatLng(feature.geometry),
    }));
    copyText(JSON.stringify(output, null, 2), "구역 좌표 JSON을 복사했어요.");
  });

  /* ----- 배정 확정 -----
     구역마다 담당 조종사 + 구역 다각형(pieceAssignmentsJson)을 서버로 보낸다.
     직접 그린 영역이면 영역 정보(drawn*)도 같이 보낸다. */
  $("#fs-confirm").addEventListener("click", () => {
    clearError();
    if (!pieces.length) { alert("먼저 영역을 선택하고 분할해 주세요."); return; }

    const lands = selectedLands();
    if (!drawn && lands.length !== 1) {
      alert(lands.length
        ? "구역별 배정은 농지를 1건만 선택해 주세요. 여러 농지를 한꺼번에 나눌 때는 '분배 설정'을 사용하세요."
        : "배정할 농지를 선택하거나 지도에서 영역을 그려 주세요.");
      return;
    }
    const emptyIdx = assignment.findIndex((v) => !v);
    if (emptyIdx >= 0) {
      alert(`구역 ${pad2(pieces[emptyIdx].properties.sliceIndex + 1)}의 담당 조종사를 선택하세요.`);
      return;
    }

    if (drawn) {
      if (!drawnAddress.value.trim()) {
        alert("직접 그린 영역의 이름(주소)을 입력해 주세요.");
        drawnAddress.focus();
        return;
      }
      const center = L.latLngBounds(drawn).getCenter();
      hidden("drawnPolygon").value = JSON.stringify(drawn);
      hidden("drawnLat").value = center.lat.toFixed(6);
      hidden("drawnLng").value = center.lng.toFixed(6);
      hidden("drawnArea").value = String(Math.max(1, Math.round(areaOfCoords(drawn))));
    } else {
      ["drawnPolygon", "drawnLat", "drawnLng", "drawnArea"].forEach((n) => { hidden(n).value = ""; });
    }

    pieceInput.value = JSON.stringify(pieces.map((piece, i) => ({
      sliceIndex: piece.properties.sliceIndex,
      pilotNo: Number(assignment[i]),
      areaSize: Math.round(turf.area(piece)),
      geometry: piece.geometry,                         // 구역 다각형 (GeoJSON, [경도, 위도])
    })));

    form.dataset.pieceMode = "1";                       // task-assignment.js 의 필지 수 검사를 건너뜀
    form.requestSubmit();
  });

  // 모달의 '배정 확정'(일반 배정)으로 제출하면 구역 정보는 보내지 않는다
  form.addEventListener("submit", (e) => {
    delete form.dataset.pieceMode;
    if (e.submitter) {
      pieceInput.value = "";
      ["drawnPolygon", "drawnLat", "drawnLng", "drawnArea"].forEach((n) => { hidden(n).value = ""; });
    }
  });

  /* ===================== 처음 열렸을 때 ===================== */
  // (농지 관리에서 ?landNo= 로 넘어와 미리 체크돼 있으면 바로 모영역을 표시)
  pickTarget();
  renderSplit();
  window.setTimeout(() => map.invalidateSize(), 200);
});