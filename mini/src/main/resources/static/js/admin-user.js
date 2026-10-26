// 관리자 계정 관리: 백엔드 연결 없이 샘플 데이터로 동작

const roleNames = {
    LEADER: "방제단장",
    PILOT: "조종사",
    ADMIN: "관리자"
};

let users = [
    {
        id: 1,
        name: "김조종",
        email: "pilot@test.com",
        phone: "010-1234-5678",
        role: "LEADER",
        team: "대전 유성 방제단",
        status: "ACTIVE",
        lastLogin: "2025-04-28 10:24"
    },
    {
        id: 2,
        name: "이민수",
        email: "lee@test.com",
        phone: "010-2345-6789",
        role: "PILOT",
        team: "대전 유성 방제단",
        status: "ACTIVE",
        lastLogin: "2025-04-28 09:52"
    },
    {
        id: 3,
        name: "박지훈",
        email: "park@test.com",
        phone: "010-3456-7890",
        role: "PILOT",
        team: "대전 유성 방제단",
        status: "ACTIVE",
        lastLogin: "2025-04-28 09:31"
    },
    {
        id: 4,
        name: "최서연",
        email: "seo@test.com",
        phone: "010-4567-8901",
        role: "PILOT",
        team: "대전 유성 방제단",
        status: "INACTIVE",
        lastLogin: "2025-04-27 18:10"
    },
    {
        id: 5,
        name: "관리자",
        email: "admin@test.com",
        phone: "010-5678-9012",
        role: "ADMIN",
        team: "본사",
        status: "ACTIVE",
        lastLogin: "2025-04-28 11:05"
    },
    {
        id: 6,
        name: "한유진",
        email: "yujin@test.com",
        phone: "010-6789-0123",
        role: "LEADER",
        team: "대전 유성 방제단",
        status: "ACTIVE",
        lastLogin: "2025-04-26 14:20"
    },
    {
        id: 7,
        name: "정태호",
        email: "taeho@test.com",
        phone: "010-7890-1234",
        role: "PILOT",
        team: "대전 유성 방제단",
        status: "ACTIVE",
        lastLogin: "2025-04-25 16:42"
    },
    {
        id: 8,
        name: "윤하늘",
        email: "haneul@test.com",
        phone: "010-8901-2345",
        role: "PILOT",
        team: "세종 북부 방제단",
        status: "ACTIVE",
        lastLogin: "2025-04-25 08:16"
    },
    {
        id: 9,
        name: "오준서",
        email: "junseo@test.com",
        phone: "010-9012-3456",
        role: "PILOT",
        team: "대전 유성 방제단",
        status: "INACTIVE",
        lastLogin: "2025-04-24 17:03"
    },
    {
        id: 10,
        name: "강민지",
        email: "minji@test.com",
        phone: "010-0123-4567",
        role: "LEADER",
        team: "세종 북부 방제단",
        status: "ACTIVE",
        lastLogin: "2025-04-24 09:10"
    },
    {
        id: 11,
        name: "문도윤",
        email: "doyun@test.com",
        phone: "010-1122-3344",
        role: "PILOT",
        team: "세종 북부 방제단",
        status: "ACTIVE",
        lastLogin: "2025-04-23 13:05"
    },
    {
        id: 12,
        name: "시스템 관리자",
        email: "sysadmin@test.com",
        phone: "010-2233-4455",
        role: "ADMIN",
        team: "본사",
        status: "ACTIVE",
        lastLogin: "2025-04-22 11:42"
    }
];

let currentPage = 1;
const pageSize = 5;

const $ = id => document.getElementById(id);

function filteredUsers() {
    const keyword = $("user-search").value.trim().toLowerCase();
    const role = $("role-filter").value;
    const status = $("status-filter").value;

    return users.filter(user => {
        const matchesKeyword =
            user.name.toLowerCase().includes(keyword) ||
            user.email.toLowerCase().includes(keyword);

        return matchesKeyword &&
            (!role || user.role === role) &&
            (!status || user.status === status);
    });
}

function renderSummary() {
    $("count-all").textContent = `${users.length}명`;
    $("count-leader").textContent =
        `${users.filter(user => user.role === "LEADER").length}명`;
    $("count-pilot").textContent =
        `${users.filter(user => user.role === "PILOT").length}명`;
    $("count-admin").textContent =
        `${users.filter(user => user.role === "ADMIN").length}명`;
}

function renderTable() {
    const result = filteredUsers();
    const pageCount = Math.max(1, Math.ceil(result.length / pageSize));

    currentPage = Math.min(currentPage, pageCount);

    const start = (currentPage - 1) * pageSize;
    const visibleUsers = result.slice(start, start + pageSize);

    $("user-result-count").textContent = `${result.length}명`;

    $("user-table-body").innerHTML = visibleUsers.map(user => {
        const roleClass = `role-${user.role.toLowerCase()}`;
        const statusClass =
            user.status === "ACTIVE" ? "status-active" : "status-inactive";

        const statusText =
            user.status === "ACTIVE" ? "활성" : "비활성";

        const toggleText =
            user.status === "ACTIVE" ? "비활성화" : "활성화";

        return `
            <tr>
                <td>
                    <div class="admin-user-name-cell">
                        <span class="admin-user-avatar">
                            ${user.name.charAt(0)}
                        </span>
                        ${user.name}
                    </div>
                </td>
                <td>${user.email}</td>
                <td>${user.phone}</td>
                <td>
                    <span class="admin-user-badge ${roleClass}">
                        ${roleNames[user.role]}
                    </span>
                </td>
                <td>${user.team}</td>
                <td>
                    <span class="admin-user-badge ${statusClass}">
                        ${statusText}
                    </span>
                </td>
                <td>${user.lastLogin}</td>
                <td>
                    <div class="admin-user-row-actions">
                        <button type="button" data-action="edit" data-id="${user.id}">
                            수정
                        </button>
                        <button type="button" data-action="toggle" data-id="${user.id}">
                            ${toggleText}
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");

    $("user-empty").hidden = result.length > 0;

    renderPagination(pageCount);
}

function renderPagination(pageCount) {
    const nav = $("user-pagination");
    nav.innerHTML = "";

    function addButton(label, page, active = false, disabled = false) {
        const button = document.createElement("button");

        button.type = "button";
        button.textContent = label;
        button.disabled = disabled;

        if (active) {
            button.classList.add("active");
        }

        button.addEventListener("click", () => {
            currentPage = page;
            renderTable();
        });

        nav.appendChild(button);
    }

    addButton("‹", Math.max(1, currentPage - 1), false, currentPage === 1);

    for (let page = 1; page <= pageCount; page++) {
        addButton(String(page), page, page === currentPage);
    }

    addButton("›", Math.min(pageCount, currentPage + 1),
        false, currentPage === pageCount);
}

function render() {
    renderSummary();
    renderTable();
}

$("user-filter-form").addEventListener("submit", event => {
    event.preventDefault();
    currentPage = 1;
    renderTable();
});

$("reset-filter-btn").addEventListener("click", () => {
    $("user-search").value = "";
    $("role-filter").value = "";
    $("status-filter").value = "";
    currentPage = 1;
    renderTable();
});

// 별도 서브 창은 만들지 않음. 사용자 추가는 현재 화면에서 안내만 표시.
$("add-user-btn").addEventListener("click", () => {
    alert("사용자 추가는 현재 시연 화면에만 표시됩니다. 실제 계정 생성은 백엔드 정책 확인 후 연결해야 합니다.");
});

$("user-table-body").addEventListener("click", event => {
    const button = event.target.closest("button[data-action]");

    if (!button) return;

    const user = users.find(item => item.id === Number(button.dataset.id));

    if (!user) return;

    if (button.dataset.action === "edit") {
        alert(`${user.name} 계정 수정은 현재 백엔드 미연결 상태라 실제 저장되지 않습니다.`);
        return;
    }

    user.status = user.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    render();

    alert(`${user.name} 계정 상태가 ${
        user.status === "ACTIVE" ? "활성" : "비활성"
    }으로 변경되었습니다. (화면 예시 데이터)`);
});

$("export-users-btn").addEventListener("click", () => {
    const rows = filteredUsers();

    const headers = [
        "이름", "이메일", "연락처", "역할",
        "소속 방제단", "계정 상태", "최근 접속일"
    ];

    const csvCell = value =>
        `"${String(value ?? "").replace(/"/g, '""')}"`;

    const csvData = [
        headers,
        ...rows.map(user => [
            user.name,
            user.email,
            user.phone,
            roleNames[user.role],
            user.team,
            user.status === "ACTIVE" ? "활성" : "비활성",
            user.lastLogin
        ])
    ];

    const csv = "\uFEFF" +
        csvData.map(row => row.map(csvCell).join(",")).join("\r\n");

    const blob = new Blob([csv], {
        type: "text/csv;charset=utf-8;"
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = "관리자_계정_목록.csv";

    document.body.appendChild(link);
    link.click();
    link.remove();

    URL.revokeObjectURL(url);
});

render();