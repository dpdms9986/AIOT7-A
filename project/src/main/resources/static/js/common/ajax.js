/**
 * jQuery $.ajax 기반 공통 비동기 요청 함수 (CSRF 토큰 자동 첨부)
 * @param {*} obj
 * obj = {
 *  url: '/api/items',       // 요청 URL
 *  method: 'GET'|'POST'|'PUT'|'DELETE',
 *  data: {} | FormData,     // 요청 데이터 (JSON 객체 또는 FormData)
 *  success: (response) => {},
 *  error: (response) => {}
 * }
 * @returns Promise (성공/실패 응답 데이터)
 */
async function $ajax( obj ) {
    console.log('data:', obj.data);
    console.log('url:', obj.url);
    obj.method = obj.method ?? obj.type
    console.log('method:', obj.method);
    
    try {
        // 💎 CSRF TOKEN
        const csrfToken = $('meta[name="_csrf"]').attr('content');
        const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

        // 요청 메소드에 따라 데이터 형식을 다르게 처리
        // GET/DELETE는 데이터를 그대로 전달 (jQuery가 쿼리 문자열로 변환)
        // POST/PUT은 FormData면 그대로, 아니면 JSON 문자열로 변환
        let data
        if(obj.method == 'GET' || obj.method == 'DELETE') { data = obj.data; }
        else { data = obj.data instanceof FormData ? obj.data : JSON.stringify(obj.data)}
        let response = await $.ajax({
            beforeSend: function(xhr) {
                // 모든 요청 헤더에 CSRF 토큰 첨부
                xhr.setRequestHeader(csrfHeader, csrfToken);
            },
            url: obj.url,
            method: obj.method,
            data: data,
            contentType: obj.data instanceof FormData ? false : 'application/json; charset=utf-8',        // FormData는 브라우저가 boundary를 자동 설정하도록 false 지정
            processData: obj.data instanceof FormData ? false : true,                                    // 데이터를 쿼리 문자열로 변환하지 않음
            // dataType: 'json'              // 서버의 응답 데이터 타입 (단순 SUCCESS, FAIL 문자열일 경우 생략)
        });

        if (obj.success) obj.success(response);
        return response;
    } catch (jqXHR) {
        console.log('[ajax.js] error -------------------------');

        // 서버가 내려준 에러 응답 본문을 그대로 반환하여 호출부에서 사용 가능하게 함
        const response = jqXHR.responseJSON !== undefined ? jqXHR.responseJSON : jqXHR.responseText;

        if (obj.error) obj.error(response);
        return response;
    }   

}