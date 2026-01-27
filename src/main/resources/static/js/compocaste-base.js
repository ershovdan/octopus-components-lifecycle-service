function httpGet(url) {
    let xmlHttp = new XMLHttpRequest()
    xmlHttp.open("GET", url, false)
    xmlHttp.send(null)

    if (xmlHttp.status >= 400) {
        console.error('HTTP GET failed:', url, '-> Status:', xmlHttp.status, 'Response:', xmlHttp.responseText)
    }

    return xmlHttp.responseText
}

function httpPost(url) {
    let xmlHttp = new XMLHttpRequest()
    xmlHttp.open("POST", url, false)
    xmlHttp.send()

    console.log('POST', url, '-> Status:', xmlHttp.status)

    if (xmlHttp.status === 406) return null

    if (xmlHttp.status >= 400) {
        console.error('HTTP POST failed with status:', xmlHttp.status, 'Response:', xmlHttp.responseText)
    }

    return xmlHttp.responseText
}

function httpPut(url) {
    let xmlHttp = new XMLHttpRequest()
    xmlHttp.open("PUT", url, false)
    xmlHttp.send()

    console.log('PUT', url, '-> Status:', xmlHttp.status)

    if (xmlHttp.status === 406) return null

    if (xmlHttp.status >= 400) {
        console.error('HTTP PUT failed with status:', xmlHttp.status, 'Response:', xmlHttp.responseText)
        return xmlHttp.responseText // Return error response so caller can parse it
    }

    return xmlHttp.responseText
}

function httpDelete(url) {
    let xmlHttp = new XMLHttpRequest()
    xmlHttp.open("DELETE", url, false)
    xmlHttp.send()

    console.log('DELETE', url, '-> Status:', xmlHttp.status)

    if (xmlHttp.status === 406) return null

    if (xmlHttp.status >= 400) {
        console.error('HTTP DELETE failed with status:', xmlHttp.status, 'Response:', xmlHttp.responseText)
        return xmlHttp.responseText
    }

    return xmlHttp.responseText
}

document.getElementById("logout_button").onclick = () => {
    // Redirect to Gateway's logout endpoint using absolute URL
    // This ensures we hit https://gateway-host/logout, not /components-lifecycle-service/logout
    window.location.href = window.location.origin + "/logout"
}

function setCookie(name, value, seconds) {
    let expires = ""
    if (seconds) {
        let date = new Date()
        date.setTime(date.getTime() + (seconds * 1000))
        expires = "; expires=" + date.toUTCString()
    }
    document.cookie = name + "=" + (value || "") + expires + "; path=/"
}

function getCookie(name) {
    let nameEQ = name + "="
    let ca = document.cookie.split(';')
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i]
        while (c.charAt(0) === ' ') c = c.substring(1, c.length)
        if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length)
    }
    return null
}

function eraseCookie(name) {
    document.cookie = name + '=; Max-Age=-99999999; path=/';
}