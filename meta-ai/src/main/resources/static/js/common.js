function logout() {
    const sessionId = document.getElementById('sessionId').value;
    window.location.href = '/logout?sessionId=' + sessionId;
}
function chat() {
    const sessionId = document.getElementById('sessionId').value;
    window.location.href = '/chat?sessionId=' + sessionId;
}