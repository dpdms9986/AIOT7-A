// 농지 관리
// - 목록 / 검색 / 등록 / 삭제 처리 : 서버 (타임리프 + 컨트롤러)
// - JS : 상세 패널 + 지도, 삭제 칸 채우기, 등록 모달 열고 닫기
document.addEventListener("DOMContentLoaded", () => {
  const $ = (sel) => document.querySelector(sel);

  /* ===================== 1. 상세 패널 ===================== */
  const drawer = $("#farm-detail-drawer");
  const backdrop = $("#farm-detail-backdrop");
  const closeBtn = $("#close-farm-detail");
  const deleteNoInput = $("#delete-land-no");
  const deleteBtn = $("#delete-farm-button");
  const assignLink = $("#assign-farm-link");
  const drawerStatus = $("#drawer-status");
  const pilotAssignmentsContainer = $("#drawer-pilot-assignments");
  let lastTrigger = null;

  // HTML 출력 시 사용자 입력값 이스케이프
  const escapeHtml = (value) => {
    return String(value).replace(/[&<>"']/g, (char) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#039;"
    })[char]);
  };

  // 중단됨 조종사 카드 밑에 [대상 선택 + 재배정] 폼 붙이기
  const reassignTemplate = document.getElementById("reassign-form-template");

  const appendReassignForm = (card, pilotNo) => {
    if (!reassignTemplate || !card) return;

    const wrap = reassignTemplate.content.cloneNode(true);
    const form = wrap.querySelector("form");

    // 주소 .../team-management/0/reassign 의 0 을 중단된 조종사 번호로 교체
    form.setAttribute("action", form.getAttribute("action").replace("/0/", `/${pilotNo}/`));

    // 중단된 조종사 본인은 대상 목록에서 제외
    form.querySelectorAll("option[data-no]").forEach((opt) => {
      if (opt.dataset.no === String(pilotNo)) opt.remove();
    });

    // 고를 수 있는 조종사가 없으면 안내만 표시
    const select = form.querySelector("select");
    if (select.options.length <= 1) {
      select.options[0].textContent = "배정 가능한 조종사 없음";
      select.disabled = true;
      form.querySelector("button").disabled = true;
    }

    card.appendChild(wrap);
  };

  const openDrawer = (btn) => {
    const d = btn.dataset;
    lastTrigger = btn;

    // 패널 내용 채우기
    $("#drawer-address").textContent = d.address || "-";
    drawerStatus.textContent = d.status || "-";
    drawerStatus.className = `status-badge ${d.statusClass || ""}`;
    $("#drawer-id").textContent = `LAND-${String(d.no || "").padStart(4, "0")}`;
    $("#drawer-coordinates").textContent =
      d.lat && d.lng ? `${d.lat}, ${d.lng}` : "미등록";

    // 다중 조종사 작업 정보 표시
    pilotAssignmentsContainer.innerHTML = "";

    const assignmentData = document.getElementById(
      `pilot-assignments-${d.no}`
    );

    const assignments = assignmentData
      ? assignmentData.querySelectorAll("[data-pilot-name]")
      : [];

    // 조종사 한 명의 정보 카드 생성
    const createPilotCard = (pilotName, statusLabel, statusClass, assignedAt, statusCode, pilotNo) => {
      pilotAssignmentsContainer.insertAdjacentHTML("beforeend", `
        <div class="pilot-assignment-card">
          <dl class="drawer-info">
            <dt>배정 조종사</dt>
            <dd>${escapeHtml(pilotName || "미배정")}</dd>

            <dt>작업 상태</dt>
            <dd>
              <span class="status-badge ${escapeHtml(statusClass || "")}">
                ${escapeHtml(statusLabel || "-")}
              </span>
            </dd>

            <dt>배정 일시</dt>
            <dd>${escapeHtml(assignedAt || "-")}</dd>
          </dl>
        </div>
      `);

      // 중단됨(INTERRUPTED) 상태인 조종사 카드 밑에만 재배정 폼 표시
      if (statusCode === "INTERRUPTED" && pilotNo) {
        appendReassignForm(pilotAssignmentsContainer.lastElementChild, pilotNo);
      }
    };

    // 배정 정보가 있는 경우
    if (assignments.length > 0) {
      assignments.forEach((assignment) => {
        const data = assignment.dataset;

        createPilotCard(
          data.pilotName || "미배정",
          data.statusLabel || "-",
          data.statusClass || "",
          data.assignedAt || "-",
          data.statusCode || "",
          data.pilotNo || ""
        );
      });
    } else {
      // 배정 정보가 없는 경우
      createPilotCard("미배정", "미배정", "", "-");
    }

    // 삭제 폼의 숨은 칸에 번호 넣기 + 대기 또는 완료 상태만 삭제 가능
    deleteNoInput.value = d.no;

    const deletable = d.deletable === "true";
    deleteBtn.disabled = !deletable;
    deleteBtn.title = deletable
      ? ""
      : "대기 또는 완료 상태인 농지만 삭제할 수 있습니다.";

    // 대기가 아니면 [삭제] [작업 배정] 영역 자체를 숨김
    document.querySelector(".drawer-footer").style.display =
      deletable ? "" : "none";

    // 작업 배정 링크 (주소는 타임리프가 만들어 둠)
    assignLink.href = d.assignUrl || "#";

    // 작업 배정 버튼은 대기(READY) 상태에서만 표시
    const assignable = d.statusCode === "READY";
    assignLink.style.display = assignable ? "" : "none";

   // 작업 배정 버튼이 숨겨지면 삭제 버튼을 오른쪽으로 정렬
   document.querySelector(".drawer-footer")
  .classList.toggle("is-delete-only", !assignable);

    // 패널 열기
    drawer.hidden = false;
    backdrop.hidden = false;
    drawer.inert = false;
    drawer.setAttribute("aria-hidden", "false");
    document.body.classList.add("farm-drawer-open");

    requestAnimationFrame(() => {
      drawer.classList.add("is-open");
      closeBtn.focus();
      showMap(d);   // 패널이 열린 다음 지도 표시 (크기 계산 때문)
    });
  };

  const closeDrawer = () => {
    drawer.classList.remove("is-open");
    drawer.inert = true;
    drawer.setAttribute("aria-hidden", "true");
    backdrop.hidden = true;
    document.body.classList.remove("farm-drawer-open");

    setTimeout(() => {
      drawer.hidden = true;
      lastTrigger?.focus();
    }, 260);   // 닫히는 애니메이션 시간
  };

  document.querySelectorAll(".detail-button").forEach((btn) => {
    btn.addEventListener("click", () => openDrawer(btn));
  });

  closeBtn.addEventListener("click", closeDrawer);
  backdrop.addEventListener("click", closeDrawer);

  /* ===================== 2. 지도 (Leaflet) ===================== */
  const DEFAULT_POS = [36.3504, 127.3845];   // 좌표 없을 때 기본 위치 (대전)
  let map = null;
  let marker = null;
  let polyLayer = null;   // 저장된 농지 외곽선

  const showMap = (d) => {
    const status = $("#farm-detail-map-status");

    if (typeof L === "undefined") {
      status.textContent = "지도를 불러오지 못했습니다.";
      return;
    }

    // 지도는 처음 한 번만 만들기
    if (!map) {
      map = L.map("farm-detail-map").setView(DEFAULT_POS, 13);

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors",
      }).addTo(map);
    }

    map.invalidateSize();   // 숨겨져 있던 패널이 열렸으니 크기 다시 계산

    // 이전 마커 및 외곽선 지우기
    if (marker) {
      map.removeLayer(marker);
      marker = null;
    }

    if (polyLayer) {
      map.removeLayer(polyLayer);
      polyLayer = null;
    }

    const lat = Number(d.lat);
    const lng = Number(d.lng);
    const polygon = window.FieldSplit
      ? FieldSplit.parsePolygon(d.polygon)
      : null;   // data-polygon

    if (d.lat && d.lng && Number.isFinite(lat) && Number.isFinite(lng)) {
      map.setView([lat, lng], 16);
      marker = L.marker([lat, lng]).addTo(map).bindPopup(d.address);
      status.textContent = "농지 위치 표시 중";
    } else {
      map.setView(DEFAULT_POS, 13);
      status.textContent = "농지 좌표 미등록";
    }

    // 저장된 농지 외곽선 표시
    if (polygon) {
      polyLayer = L.polygon(polygon, {
        color: "#247a5e",
        weight: 2,
        fillColor: "#54a77b",
        fillOpacity: 0.25,
      }).addTo(map);

      map.fitBounds(polyLayer.getBounds(), {
        padding: [24, 24],
        maxZoom: 18
      });

      status.textContent = "농지 외곽선 표시 중";
    }
  };

  /* ===================== 3. 등록 모달 ===================== */
  const modal = $("#farm-register-modal");
  const regForm = modal.querySelector("form");
  const field = (name) => regForm.querySelector(`[name='${name}']`);
  const addrInput = $("#farm-addr");
  const addrBtn = $("#farm-addr-btn");
  const addrMsg = $("#farm-addr-msg");
  const latInput = field("latitude");
  const lngInput = field("longitude");
  const ADDR_HINT = "주소를 고르면 지도와 위도·경도가 자동으로 맞춰져요. 지도를 클릭해 미세 조정할 수 있어요.";

  // 외곽선 그리기에 필요한 요소들
  const drawBtn = $("#farm-draw-btn");
  const drawStatus = $("#farm-draw-status");
  const polygonInput = $("#farm-polygon");
  const DRAW_HINT = "외곽을 그리지 않으면 위치 마커만 저장돼요.";

  let pickMap = null;
  let pickMarker = null;
  let outlineDrawer = null;    // FieldSplit.createDrawer (지도가 만들어진 뒤 생성)

  const setDrawStatus = (message, state = "") => {
    if (!drawStatus) return;

    drawStatus.textContent = message;
    drawStatus.dataset.state = state;
  };

  // 꼭짓점 좌표([[lat,lng], ...])로 면적(㎡) 계산 (3점 미만이면 null)
  const calcArea = (coords) => {
    if (!window.turf || !window.FieldSplit || !coords || coords.length < 3) {
      return null;
    }

    try {
      const polygon = turf.polygon([
        FieldSplit.closeRing(coords).map(([lat, lng]) => [lng, lat])
      ]);

      return Math.max(1, Math.round(turf.area(polygon)));
    } catch (e) {
      return null;   // 선이 꼬이는 등 계산이 안 될 때
    }
  };

  // 면적 칸에 값 넣기 (null 이면 비움)
  const setArea = (value) => {
    const areaField = field("areaSize");
    if (areaField) areaField.value = value == null ? "" : value;
  };

  // 그린 외곽선 + 관련 입력칸 모두 초기화
  const clearDrawing = () => {
    if (outlineDrawer) outlineDrawer.clear();

    if (polygonInput) polygonInput.value = "";

    if (drawBtn) {
      drawBtn.textContent = "지도에서 외곽 그리기";
      drawBtn.setAttribute("aria-pressed", "false");
    }

    setArea(null);
    setDrawStatus(DRAW_HINT);
  };

  // 위도·경도 칸 + 마커 (+ 지도 이동) 한 번에 맞추기
  const setPick = (lat, lng, zoom) => {
    latInput.value = lat.toFixed(6);
    lngInput.value = lng.toFixed(6);

    if (!pickMap) return;

    if (pickMarker) {
      pickMarker.setLatLng([lat, lng]);
    } else {
      pickMarker = L.marker([lat, lng]).addTo(pickMap);
    }

    if (zoom) pickMap.setView([lat, lng], zoom);
  };

  // 마커 지우고 지도를 기본 위치로
  const resetPick = () => {
    if (!pickMap) return;

    if (pickMarker) {
      pickMap.removeLayer(pickMarker);
      pickMarker = null;
    }

    pickMap.setView(DEFAULT_POS, 13);
  };

  // 그리기가 끝났을 때 처리
  const finishDrawing = () => {
    if (!outlineDrawer) return false;

    const result = outlineDrawer.finish();

    if (drawBtn) {
      drawBtn.textContent = "지도에서 외곽 다시 그리기";
      drawBtn.setAttribute("aria-pressed", "false");
    }

    if (!result.ok) {
      if (polygonInput) polygonInput.value = "";
      return false;
    }

    const coords = result.coordinates;

    if (polygonInput) polygonInput.value = JSON.stringify(coords);

    const center = L.latLngBounds(coords).getCenter();
    setPick(center.lat, center.lng);

    const area = calcArea(coords);
    if (area != null) setArea(area);

    return true;
  };

  const openModal = () => {
    modal.hidden = false;
    document.body.style.overflow = "hidden";
    regForm.reset();                    // 지난번에 입력한 값 지우기
    addrMsg.textContent = ADDR_HINT;
    clearDrawing();                     // 열 때 외곽선 초기화

    // 팝업 안 작은 지도 (처음 열 때 한 번만 만들기)
    if (typeof L !== "undefined") {
      if (!pickMap) {
        pickMap = L.map("farm-pick-map").setView(DEFAULT_POS, 13);

        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
          maxZoom: 19,
          attribution: "&copy; OpenStreetMap",
        }).addTo(pickMap);

        pickMap.on("click", (e) => {
          if (outlineDrawer && outlineDrawer.isActive()) return;   // 그리는 중엔 꼭짓점이 우선
          setPick(e.latlng.lat, e.latlng.lng);
        });

        if (window.FieldSplit) {
          outlineDrawer = FieldSplit.createDrawer(pickMap, {
            onStatus: setDrawStatus,

            // 꼭짓점이 추가될 때마다 면적 자동 계산
            onChange: (coords) => {
              const area = calcArea(coords);
              setArea(area);

              if (area != null) {
                setDrawStatus(
                  `${coords.length}개 꼭짓점 · 현재 면적 약 ${area.toLocaleString()}㎡. 지도를 눌러 다음 점을 선택하세요.`,
                  "active"
                );
              }
            },
          });
        }
      }

      resetPick();
      setTimeout(() => pickMap.invalidateSize(), 0);   // 숨겨져 있던 지도는 크기 다시 계산
    }

    if (addrBtn) addrBtn.focus();
  };

  const closeModal = () => {
    modal.hidden = true;
    document.body.style.overflow = "";
  };

  const registerBtn = $("#register-button");

  if (registerBtn) registerBtn.addEventListener("click", openModal);

  modal.querySelectorAll("[data-close-modal]").forEach((b) => {
    b.addEventListener("click", closeModal);
  });

  // 외곽 그리기 버튼 이벤트
  if (drawBtn) {
    drawBtn.addEventListener("click", () => {
      if (!outlineDrawer) {
        if (window.showPopup) {
          showPopup("지도를 불러오지 못했어요. 페이지를 새로고침해 주세요.", "warn");
        }
        return;
      }

      if (outlineDrawer.isActive()) {          // 그리기 완료
        finishDrawing();
        return;
      }

      if (
        polygonInput &&
        polygonInput.value &&
        !window.confirm("이미 그린 외곽이 있어요. 지우고 새로 그릴까요?")
      ) {
        return;
      }

      clearDrawing();

      if (pickMarker) {
        pickMap.removeLayer(pickMarker);
        pickMarker = null;
      }   // 그리는 동안엔 마커 숨김

      outlineDrawer.start();
      drawBtn.textContent = "그리기 완료";
      drawBtn.setAttribute("aria-pressed", "true");
    });
  }

  // 위도·경도를 직접 고치면 지도 마커도 이동
  [latInput, lngInput].forEach((input) => {
    if (!input) return;

    input.addEventListener("change", () => {
      const lat = parseFloat(latInput.value);
      const lng = parseFloat(lngInput.value);

      if (Number.isNaN(lat) || Number.isNaN(lng)) return;

      setPick(lat, lng, 16);
    });
  });

  // 주소 검색을 안 하고 등록하려 할 때 막기 + 그리는 중 등록 방지
  regForm.addEventListener("submit", (e) => {
    if (outlineDrawer && outlineDrawer.isActive() && !finishDrawing()) {
      e.preventDefault();

      const msg = "농지 외곽을 올바르게 그려 주세요. (꼭짓점 3개 이상, 선이 꼬이지 않게)";

      if (window.showPopup) showPopup(msg, "warn");
      else alert(msg);

      return;
    }

    if (!field("city").value || !field("dong").value || !field("address").value) {
      e.preventDefault();

      const msg = "주소 검색 버튼으로 농지 주소를 먼저 선택해 주세요.";

      if (window.showPopup) showPopup(msg, "warn");
      else alert(msg);
    }
  });

  /* ===================== 3-1. 주소 검색 → 위치 자동 입력 ===================== */
  const SIDO_FULL = {
    "서울": "서울특별시",
    "부산": "부산광역시",
    "대구": "대구광역시",
    "인천": "인천광역시",
    "광주": "광주광역시",
    "대전": "대전광역시",
    "울산": "울산광역시",
    "세종": "세종특별자치시",
    "경기": "경기도",
    "강원": "강원특별자치도",
    "충북": "충청북도",
    "충남": "충청남도",
    "전북": "전북특별자치도",
    "전남": "전라남도",
    "경북": "경상북도",
    "경남": "경상남도",
    "제주": "제주특별자치도",
  };

  async function geocode(address) {
    const url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=kr&q="
      + encodeURIComponent(address);

    const res = await fetch(url, {
      headers: { "Accept-Language": "ko" }
    });

    if (!res.ok) return null;

    const list = await res.json();

    return list.length
      ? {
          lat: parseFloat(list[0].lat),
          lng: parseFloat(list[0].lon)
        }
      : null;
  }

  async function geocodeLoose(address) {
    let words = address.trim().split(/\s+/);

    while (words.length >= 2) {
      const pos = await geocode(words.join(" "));

      if (pos) {
        return {
          pos,
          exact: words.join(" ") === address.trim()
        };
      }

      words = words.slice(0, -1);
    }

    return null;
  }

  async function findCoords(road, jibun) {
    latInput.value = "";
    lngInput.value = "";

    resetPick();
    addrMsg.textContent = "위치를 찾는 중이에요...";

    const found =
      (await geocodeLoose(road)) ||
      (jibun ? await geocodeLoose(jibun) : null);

    if (!found) {
      addrMsg.textContent = "주소 위치를 찾지 못했어요. 지도를 클릭해 위치를 찍어 주세요.";

      if (window.showPopup) {
        showPopup("주소 위치를 찾지 못했어요.\n지도에서 직접 위치를 눌러 주세요.", "warn");
      }

      return;
    }

    setPick(found.pos.lat, found.pos.lng, found.exact ? 17 : 15);

    addrMsg.textContent = found.exact
      ? "주소 위치로 맞췄어요. 필요하면 지도를 클릭해 미세 조정하세요."
      : "정확한 번지는 못 찾아서 근처로 맞췄어요. 지도를 클릭해 정확한 위치를 찍어 주세요.";
  }

  if (addrBtn) {
    addrBtn.addEventListener("click", () => {
      if (typeof daum === "undefined" || !daum.Postcode) {
        if (window.showPopup) {
          showPopup("주소 검색을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.", "warn");
        }
        return;
      }

      new daum.Postcode({
        oncomplete: (data) => {
          const road = data.roadAddress || data.address;
          const jibun = data.jibunAddress || data.autoJibunAddress || "";

          if (addrInput) addrInput.value = road;

          clearDrawing();   // 위치가 바뀌었으니 이전에 그린 외곽선은 지움

          field("city").value = SIDO_FULL[data.sido] || data.sido;
          field("region").value = data.sigungu || "";
          field("dong").value = data.bname1 || data.bname;

          const prefix = [data.sido, data.sigungu].filter(Boolean).join(" ") + " ";
          const base = jibun || road;

          field("address").value = base.startsWith(prefix)
            ? base.slice(prefix.length)
            : base;

          findCoords(road, jibun);
        },
      }).open();
    });
  }

  /* ===================== 4. ESC 로 닫기 ===================== */
  document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;

    if (!modal.hidden) closeModal();
    else if (!drawer.hidden) closeDrawer();
  });
});