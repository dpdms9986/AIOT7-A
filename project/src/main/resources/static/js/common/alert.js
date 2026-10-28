

/**
 * 기본 알림창
 * @param {*} title 
 * @param {*} text 
 * @param {*} icon 
 */
function $alert( title, text, icon, confirmButtonText='확인' ) {
    if (typeof title === 'object') {
        text = title.text;
        icon = title.icon;
        title = title.title;
    }

    return Swal.fire({
        title: title,
        text: text,
        icon: icon,
        confirmButtonText: title.confirmButtonText ?? confirmButtonText,
    })
}

/**
 * 기본 알림창 (콜백 지원)
 * @param {*} title 
 * @param {*} text 
 * @param {*} icon 
 * @param {*} confirmButtonText 
 * @returns Promise
 */
function $alert_(title, text, icon, confirmButtonText = '확인') {
    if (typeof title === 'object') {
        text = title.text;
        icon = title.icon;
        confirmButtonText = title.confirmButtonText ?? confirmButtonText;
        title = title.title;
    }
    return Swal.fire({
        title: title,
        text: text,
        icon: icon,
        confirmButtonText: confirmButtonText,
    });
}

/**
 * HTML 지원 알림창
 * @param {*} title 
 * @param {*} html 
 * @param {*} icon 
 * @param {*} confirmButtonText 
 * @returns 
 */
function $alertHTML(title, html, icon, confirmButtonText = '확인') {
    return Swal.fire({
        title: title,
        html: html,
        icon: icon,
        confirmButtonText: confirmButtonText,
        animation: true
    })
}


/**
 * 기본 confirm 알림창
 * @param {*} title 
 * @param {*} text 
 * @param {*} icon 
 * @param {*} confirmButtonText 
 * @param {*} cancelButtonText 
 * @returns 
 */
function $confirm( title, text, icon, confirmButtonText, cancelButtonText, confirmButtonColor='#3085d6', cancelButtonColor='#d33' ) {
    return Swal.fire({
        title: title,
        text: text,
        icon: icon,
        showCancelButton: true,
        confirmButtonColor: confirmButtonColor,
        cancelButtonColor: cancelButtonColor,
        confirmButtonText: confirmButtonText,
        cancelButtonText: cancelButtonText
    })
}


function $confirmDeny( title, text, icon, confirmButtonText, denyButtonText, cancelButtonText, confirmButtonColor='#3085d6', denyButtonColor='#d33',  cancelButtonColor='#666' ) {
    return Swal.fire({
        title: title,
        text: text,
        icon: icon,
        showDenyButton: true,
        showCancelButton: true,
        confirmButtonColor: confirmButtonColor,
        denyButtonColor: denyButtonColor,
        cancelButtonColor: cancelButtonColor,

        confirmButtonText: confirmButtonText,
        denyButtonText: denyButtonText,
        cancelButtonText: cancelButtonText
    })
}


function $confirmHTML( title, html, icon, confirmButtonText, cancelButtonText, confirmButtonColor='#3085d6', cancelButtonColor='#d33' ) {
    return Swal.fire({
        title: title,
        html: html,
        icon: icon,
        showCancelButton: true,
        confirmButtonColor: confirmButtonColor,
        cancelButtonColor: cancelButtonColor,
        confirmButtonText: confirmButtonText,
        cancelButtonText: cancelButtonText,
        animation: true
    })
}

/**
 * 기본 토스트
 * @param {*} obj 
 * obj = {
 *  timer: 3000,
 *  title: 'title',
 *  icon: 'success',
 *  position: 'top-end',
 *  showConfirmButton: false,
 *  timerProgressBar: true
 * }
 * 
 */
async function $toast(obj = {}) {
    const Toast = Swal.mixin({
        toast: true,
        timer: obj.timer ?? 3000,
        position: obj.position ?? "top-end",
        showConfirmButton: obj.showConfirmButton ?? false,
        timerProgressBar: obj.timerProgressBar ?? true,
        didOpen: (toast) => {
            toast.onmouseenter = Swal.stopTimer;
            toast.onmouseleave = Swal.resumeTimer;
    }
    });
    Toast.fire({
        icon: obj.icon ?? "success",
        title: obj.title ?? "success",
    }).then((result) => {
        if (result.dismiss === Swal.DismissReason.timer) {
            console.log('toast is closed by timer');
        }
    });
}




/**
 * 토스트 콜백
 * @param {*} obj 
 * obj = {
 *  timer: 3000,
 *  title: 'title',
 *  icon: 'success',
 *  position: 'top-end',
 *  showConfirmButton: false,
 *  timerProgressBar: true
 * }
 * 
 */
function $toast_(obj = {}) {
    const Toast = Swal.mixin({
        toast: true,
        timer: obj.timer ?? 3000,
        position: obj.position ?? "top-end",
        showConfirmButton: obj.showConfirmButton ?? false,
        timerProgressBar: obj.timerProgressBar ?? true,
        didOpen: (toast) => {
            toast.onmouseenter = Swal.stopTimer;
            toast.onmouseleave = Swal.resumeTimer;
    }
    });

    return Toast.fire({
        icon: obj.icon ?? "success",
        title: obj.title ?? "success",
    })
}



/**
 * 로그인 필요 알림창 - 회원가입, 로그인
 */
function alertLogin() {

    Swal.fire({
        title: "로그인 필요",
        text: "로그인 후 이용 가능합니다.",
        icon: "warning",
        showDenyButton: true,
        showCancelButton: false,
        confirmButtonText: "회원가입",
        confirmButtonColor: '#0a56f7',
        denyButtonText: `로그인`,
        denyButtonColor: '#0066cc',
      }).then((result) => {
        if (result.isConfirmed) {
          location.href = "/auth/join";
        } else if (result.isDenied) {
          location.href = "/auth/login";
        }
    });

}