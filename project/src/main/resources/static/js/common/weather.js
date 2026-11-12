// 날씨 표시 (Open-Meteo, API 키 불필요)
//  data-weather="header"  : 헤더용 한 줄 요약
//     + data-weather-alert="inline" 이면 주의 문구도 헤더에 같이 표시 (조종사 헤더)
//  data-weather="notice"  : 대시보드용 안내 박스 (양호/주의/위험)
(function () {
  "use strict";

  // 기본 위치 (화면에서 농지 좌표를 못 찾았을 때만 사용): 대전 유성구
  const DEFAULT_LAT = 36.3622;
  const DEFAULT_LNG = 127.3562;
  const CACHE_MIN = 10;   // 같은 값을 몇 분간 재사용할지 (화면 이동 때마다 호출 방지)

  // 주의 기준 (임시값 - 팀에서 정한 기준으로 수정하세요)
  const T = {
    WIND_CAUTION: 5,   // 풍속(m/s) 이상이면 주의
    WIND_DANGER: 8,    // 풍속(m/s) 이상이면 위험
    GUST_CAUTION: 8,   // 돌풍(m/s) 이상이면 주의
    GUST_DANGER: 12,   // 돌풍(m/s) 이상이면 위험
    RAIN_DANGER: 100,  // 강수량(mm) 이상이면 위험
  };

  // 날씨 코드 → 아이콘/글자
  function describe(code) {
    if (code === 0) return ["☀️", "맑음"];
    if (code === 1 || code === 2) return ["🌤️", "구름 조금"];
    if (code === 3) return ["☁️", "흐림"];
    if (code === 45 || code === 48) return ["🌫️", "안개"];
    if (code >= 51 && code <= 57) return ["🌦️", "이슬비"];
    if (code >= 61 && code <= 67) return ["🌧️", "비"];
    if (code >= 71 && code <= 77) return ["🌨️", "눈"];
    if (code >= 80 && code <= 82) return ["🌦️", "소나기"];
    if (code === 85 || code === 86) return ["🌨️", "눈 소나기"];
    if (code >= 95) return ["⛈️", "뇌우"];
    return ["🌡️", "날씨"];
  }

  // 수치 → ok / caution / danger 판정
  function judge(w) {
    const reasons = [];
    let level = "ok";
    const up = (l) => {
      if (l === "danger" || (l === "caution" && level === "ok")) level = l;
    };

    if (w.wind >= T.WIND_DANGER) { reasons.push("바람 : 강함"); up("danger"); }
    else if (w.wind >= T.WIND_CAUTION) { reasons.push("바람 : 다소 강함"); up("caution"); }

    if (w.gust >= T.GUST_DANGER) { reasons.push("돌풍 : 강함"); up("danger"); }
    else if (w.gust >= T.GUST_CAUTION) { reasons.push("돌풍 : 다소 강함"); up("caution"); }

    const c = w.code;
    if (w.rain >= T.RAIN_DANGER) { reasons.push("강수량 : 100mm 이상"); up("danger"); }
    if (c === 3) { reasons.push("날씨 : 흐림"); up("caution"); }
    if (c >= 95) { reasons.push("날씨 : 뇌우"); up("danger"); }
    else if ((c >= 51 && c <= 67) || (c >= 80 && c <= 82) || w.rain > 0) {
      reasons.push("날씨 : 비"); up("caution");
    } else if ((c >= 71 && c <= 77) || c === 85 || c === 86) {
      reasons.push("날씨 : 눈"); up("caution");
    }
    return { level, reasons };
  }

  // 시연용: 주소 뒤에 ?wxtest=caution / danger / off 를 붙이면 가짜 날씨로 확인 가능
  function testKind() {
    try {
      const q = new URLSearchParams(location.search).get("wxtest");
      if (q === "off") sessionStorage.removeItem("wx-test");
      else if (q) sessionStorage.setItem("wx-test", q);
      return sessionStorage.getItem("wx-test");
    } catch (e) { return null; }
  }
  function fake(kind) {
    if (kind === "danger") return { temp: 21, wind: 9.5, gust: 14, rain: 120, code: 95 };
    if (kind === "caution") return { temp: 20, wind: 6.2, gust: 9, rain: 0, code: 3 };
    return null;
  }

  // 위치 결정 (서버 수정 없이, 화면에 이미 있는 농지 좌표를 사용)
  //  1순위: 지금 화면의 농지 좌표 평균
  //         - 단장 대시보드: FARMS (농지 목록)
  //         - 조종사 대시보드: 내 작업 목록(.pilot-task)의 data-lat / data-lng
  //  2순위: 마지막으로 찾았던 위치 (다른 화면 헤더에서도 같은 위치 사용)
  //  3순위: 기본 위치
  function round2(n) { return Math.round(n * 100) / 100; }   // 약 1km 단위

  function average(points) {
    const ok = points.filter((p) => isFinite(p.lat) && isFinite(p.lng));
    if (!ok.length) return null;
    return {
      lat: round2(ok.reduce((s, p) => s + p.lat, 0) / ok.length),
      lng: round2(ok.reduce((s, p) => s + p.lng, 0) / ok.length),
    };
  }

  function findPagePos() {
    const points = [];

    try {
      if (typeof FARMS !== "undefined" && Array.isArray(FARMS)) {
        FARMS.forEach((f) => {
          if (f.lat != null && f.lng != null) {
            points.push({ lat: Number(f.lat), lng: Number(f.lng) });
          }
        });
      }
    } catch (e) { /* FARMS 가 없는 화면이면 건너뜀 */ }

    document.querySelectorAll(".pilot-task[data-lat][data-lng]").forEach((el) => {
      points.push({ lat: parseFloat(el.dataset.lat), lng: parseFloat(el.dataset.lng) });
    });

    return average(points);
  }

  function getPos() {
    const found = findPagePos();
    try {
      if (found) {
        localStorage.setItem("wx-pos", JSON.stringify(found));
        return found;
      }
      const saved = JSON.parse(localStorage.getItem("wx-pos") || "null");
      if (saved && isFinite(saved.lat) && isFinite(saved.lng)) return saved;
    } catch (e) { /* 저장소를 못 쓰면 기본값 */ }
    return found || { lat: DEFAULT_LAT, lng: DEFAULT_LNG };
  }

  async function load() {
    const test = fake(testKind());
    if (test) return test;

    const pos = getPos();
    const key = pos.lat + "," + pos.lng;   // 위치가 바뀌면 캐시도 새로

    try {
      const raw = sessionStorage.getItem("wx-cache");
      if (raw) {
        const c = JSON.parse(raw);
        if (c.key === key && Date.now() - c.t < CACHE_MIN * 60000) return c.data;
      }
    } catch (e) { /* 캐시 읽기 실패는 무시 */ }

    const url = "https://api.open-meteo.com/v1/forecast"
      + "?latitude=" + pos.lat + "&longitude=" + pos.lng
      + "&current=temperature_2m,precipitation,weather_code,wind_speed_10m,wind_gusts_10m"
      + "&wind_speed_unit=ms&timezone=Asia%2FSeoul";

    const res = await fetch(url);
    if (!res.ok) throw new Error("weather " + res.status);
    const cur = (await res.json()).current;

    const data = {
      temp: cur.temperature_2m,
      wind: cur.wind_speed_10m,
      gust: cur.wind_gusts_10m,
      rain: cur.precipitation,
      code: cur.weather_code,
    };
    try { sessionStorage.setItem("wx-cache", JSON.stringify({ t: Date.now(), key: key, data: data })); } catch (e) { /* 무시 */ }
    return data;
  }

  const fmt = (n) => (Math.round(n * 10) / 10).toFixed(1);

  // 헤더: 날씨 한 줄 (inline 모드면 주의 문구도 붙임)
  function renderHeader(el, w, j) {
    const d = describe(w.code);
    const inline = el.dataset.weatherAlert === "inline";

    el.textContent = "";
    el.classList.remove("wx-ok", "wx-caution", "wx-danger", "wx-off");
    el.classList.add("wx-" + j.level);

    const wxText = document.createElement("span");
    wxText.className = "wx-text";
    const wxMain = document.createElement("span");
    wxMain.textContent = d[0] + " " + d[1];
    const wxSub = document.createElement("span");
    wxSub.className = "wx-sub";
    wxSub.textContent = Math.round(w.temp) + "° · " + fmt(w.wind) + "m/s";
    wxText.append(wxMain, wxSub);
    el.append(wxText);

    if (inline) {
      const chip = document.createElement("b");
      chip.className = "wx-chip";

      let face, msg;
      if (j.level === "ok") {
        face = "😊";
        msg = "작업하기 좋아요";
      } else if (j.level === "caution") {
        face = "😥";
        msg = "작업 시 주의하세요";
      } else {
        face = "🥵";
        msg = "작업을 중단하세요";
      }

      const faceEl = document.createElement("span");
      faceEl.className = "wx-face";
      faceEl.textContent = face;

      const msgEl = document.createElement("span");
      msgEl.className = "wx-msg";
      msgEl.textContent = " " + msg;

      chip.title = msg;                 // 작은 화면에서 마우스를 올리면 문구 확인
      chip.append(faceEl, msgEl);
      el.classList.add("wx-has-chip");  // 이모지 칩이 있는 헤더(조종사)만 줄이기 위한 표시
      el.append(chip);
    }

    // 화면에 날씨 박스가 없는 페이지에서는, 헤더 날씨를 누르면 박스가 팝업으로 뜸
    if (!document.querySelector('[data-weather="notice"]')) {
      el.style.cursor = "pointer";
      el.onclick = (e) => {
        e.stopPropagation();
        togglePop(el, w, j);
      };
    }

    el.title = "풍속 " + fmt(w.wind) + "m/s · 돌풍 " + fmt(w.gust) + "m/s · 강수 " + fmt(w.rain) + "mm\n"
      + "예보 수치 기준 참고 정보 (기상청 공식 특보 아님)\n날씨 데이터: Open-Meteo.com";
  }

  // 헤더 날씨를 누르면 아래에 안내 박스 팝업
  let pop = null;
  function closePop() {
    if (pop) { pop.remove(); pop = null; }
  }
  function togglePop(anchor, w, j) {
    if (pop) { closePop(); return; }
    pop = document.createElement("div");
    document.body.append(pop);
    renderNotice(pop, w, j);
    pop.classList.add("wx-pop");
    const r = anchor.getBoundingClientRect();
    const left = Math.min(r.left, window.innerWidth - pop.offsetWidth - 8);
    pop.style.top = (r.bottom + 8) + "px";
    pop.style.left = Math.max(8, left) + "px";
  }
  document.addEventListener("click", (e) => {
    if (pop && !pop.contains(e.target)) closePop();
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closePop();
  });
  window.addEventListener("resize", closePop);

  // 대시보드: 안내 박스
  function renderNotice(el, w, j) {
    el.hidden = false;
    el.className = "wx-notice wx-" + j.level;
    el.textContent = "";

    const top = document.createElement("div");
    top.className = "wx-top";
    const head = document.createElement("strong");
    const note = document.createElement("small");
    note.textContent = "예보 기준 참고 · 기상청 특보 아님";
    top.append(head, note);

    const msg = document.createElement("div");
    msg.className = "wx-reasons";
    const tip = document.createElement("b");

    if (j.level === "ok") {
      head.textContent = "😊 현재 날씨 양호";
      msg.textContent = "풍속 " + fmt(w.wind) + "m/s · 돌풍 " + fmt(w.gust) + "m/s";
    } else {
      head.textContent = j.level === "caution" ? "😥 날씨 주의" : "🥵 날씨 위험";
      j.reasons.forEach((r) => {
        const tag = document.createElement("span");
        tag.textContent = r;
        msg.append(tag);
      });
      tip.textContent = j.level === "caution"
        ? "작업 전 현장 날씨를 확인하세요."
        : "작업 여부를 신중히 결정하세요.";
    }
    el.append(top, msg);
    if (tip.textContent) el.append(tip);
  }
  async function init() {
    const headers = document.querySelectorAll('[data-weather="header"]');
    const notices = document.querySelectorAll('[data-weather="notice"]');
    if (!headers.length && !notices.length) return;

    try {
      const w = await load();
      const j = judge(w);
      headers.forEach((el) => renderHeader(el, w, j));
      notices.forEach((el) => renderNotice(el, w, j));
    } catch (e) {
      console.warn("[weather] 날씨 조회 실패", e);
      headers.forEach((el) => {
        el.textContent = "날씨 정보 없음";
        el.classList.add("wx-off");
      });
    }
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
  else init();
})();