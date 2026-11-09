# SweetAlert2 알림/토스트 함수 사용법

이 문서는 `Swal`(SweetAlert2) 기반의 커스텀 알림 및 토스트 함수들의 사용법을 안내합니다.

---

## 1. 기본 알림창

```js
$alert('제목', '내용', 'success');
```
또는
```js
$alert({
    title: '제목',
    text: '내용',
    icon: 'success',
    confirmButtonText: '확인'
});
```

---

## 2. 확인(confirm) 알림창

```js
$confirm('제목', '내용', 'warning', '확인', '취소')
    .then(result => {
        if (result.isConfirmed) {
            // 확인 버튼 클릭 시
        }
    });
```

---

## 3. 확인/거부(confirm/deny) 알림창

```js
$confirmDeny('제목', '내용', 'question', '확인', '거부', '취소')
    .then(result => {
        if (result.isConfirmed) {
            // 확인
        } else if (result.isDenied) {
            // 거부
        }
    });
```

---

## 4. HTML 내용 알림창

```js
$confirmHTML('제목', '<b>HTML 내용</b>', 'info', '확인', '취소');
```

---

## 5. 토스트 알림

```js
$toast({
    title: '성공!',
    icon: 'success',
    timer: 2000,
    position: 'top-end'
});
```

---

## 6. 토스트 알림(콜백 사용)

```js
$toast_({
    title: '처리 완료',
    icon: 'success'
}).then(result => {
    // 토스트 닫힘 후 처리
});
```

---

## 7. 로그인 필요 알림

```js
alertLogin();
// 회원가입 또는 로그인 페이지로 이동
```

---

### 아이콘 종류
- 'success'
- 'error'
- 'warning'
- 'info'
- 'question'

---

> SweetAlert2 공식 문서: https://sweetalert2.github.io/
