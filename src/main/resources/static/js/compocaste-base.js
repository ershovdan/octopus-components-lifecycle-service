function httpGet(url) {
    let xmlHttp = new XMLHttpRequest()
    xmlHttp.open("GET", url, false)
    xmlHttp.send(null)
    return xmlHttp.responseText
}

function httpPost(url) {
    let xmlHttp = new XMLHttpRequest()
    xmlHttp.open("POST", url, false)
    xmlHttp.send()
    if (xmlHttp.status === 406) return null
    return xmlHttp.responseText
}

function httpPut(url) {
    let xmlHttp = new XMLHttpRequest()
    xmlHttp.open("PUT", url, false)
    xmlHttp.send()
    if (xmlHttp.status === 406) return null
    return xmlHttp.responseText
}

function httpDelete(url) {
    let xmlHttp = new XMLHttpRequest()
    xmlHttp.open("DELETE", url, false)
    xmlHttp.send()
    if (xmlHttp.status === 406) return null
    return xmlHttp.responseText
}

document.getElementById("logout_button").onclick = () => {
    window.location.href = "/logout"
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