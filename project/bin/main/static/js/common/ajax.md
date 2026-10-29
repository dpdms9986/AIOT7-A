# `$ajax` 함수 문서

## 설명

`$ajax` 함수는 jQuery의 `$.ajax`를 래핑하여 CSRF 토큰 자동 처리, FormData 지원, 에러 핸들링을 제공하는 비동기 함수입니다.

---

## 함수 시그니처

```js
async function $ajax(obj)
```

---

## 사용법

### 기본 사용 예시

```js
// GET 요청 예시
const response = await $ajax({
    url: '/api/items',
    type: 'GET',
    data: { id: 1 }
});
console.log(response);

// POST 요청 예시 (JSON 데이터)
const response = await $ajax({
    url: '/api/items',
    type: 'POST',
    data: { name: 'item1', price: 1000 }
});
console.log(response);

// DELETE 요청 예시
const response = await $ajax({
    url: '/api/items/1',
    type: 'DELETE',
    data: { id: 1 }
});
```

### FormData 전송 예시

```js
const $form = document.getElementById('form');
const formData = new FormData($form);

const response = await $ajax({
    url: '/api/upload',
    type: 'POST',
    data: formData
});
console.log(response);

// 또는 직접 FormData 생성
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('name', 'filename');

await $ajax({
    url: '/api/upload',
    type: 'POST',
    data: formData
});
```

### 콜백 함수 사용

```js
$ajax({
    url: '/api/items',
    type: 'POST',
    data: { name: 'item1' },
    success: function(response) {
        console.log('성공:', response);
    },
    error: function(xhr, status, error) {
        console.log('에러:', error);
    }
});
```

---

## 파라미터

`obj` 객체는 다음 속성을 포함할 수 있습니다:

| 이름    | 타입              | 필수 | 설명                                    |
|---------|-------------------|------|----------------------------------------|
| url     | string            | ✓    | 요청할 URL                              |
| type    | string            | ✓    | HTTP 메서드 (GET, POST, PUT, DELETE 등) |
| data    | object/FormData   | ✓    | 전송할 데이터                           |
| success | function          |      | 성공 콜백 함수                          |
| error   | function          |      | 에러 콜백 함수                          |

---

## 반환값

- **성공:** 서버 응답 데이터 (Promise)
- **실패:** 문자열 `"FAIL"`

---

## 주요 기능

### 1. CSRF 토큰 자동 처리
함수는 페이지의 메타 태그에서 CSRF 토큰을 자동으로 추출하여 요청 헤더에 포함합니다.

```html
<meta name="_csrf" content="token-value">
<meta name="_csrf_header" content="X-CSRF-TOKEN">
```

### 2. 데이터 타입별 처리
- **GET/DELETE 요청:** 데이터를 쿼리 파라미터로 전송
- **POST/PUT 등:** 데이터를 JSON 문자열로 변환하여 body에 전송
- **FormData:** 자동 감지하여 적절한 설정 적용

### 3. FormData 자동 처리
FormData 객체 전송 시 `contentType`과 `processData`를 자동으로 `false`로 설정합니다.

### 4. 디버깅 로그
개발 시 유용한 콘솔 로그를 자동으로 출력합니다:
- 요청 데이터
- 요청 URL
- 요청 타입

---

## 에러 처리

에러 발생 시 콘솔에 에러 메시지를 출력하고 `"FAIL"` 문자열을 반환합니다.

```js
try {
    const response = await $ajax({
        url: '/api/items',
        type: 'POST',
        data: { name: 'item1' }
    });
    
    if (response === "FAIL") {
        console.log('요청 실패');
    } else {
        console.log('성공:', response);
    }
} catch (error) {
    console.error('예외 발생:', error);
}
```

---

## 참고사항

- 이 함수는 jQuery의 `$.ajax`에 의존합니다
- CSRF 보호가 활성화된 Spring Security 환경에서 사용하도록 설계되었습니다
- FormData 사용 시 `contentType`과 `processData`를 수동으로 설정할 필요가 없습니다
- 비동기 함수이므로 `await` 또는 `.then()`으로 결과를 처리해야 합니다
