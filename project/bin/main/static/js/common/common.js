function logout() {
  // /logout?redirect={현재경로}
  var currentPath = window.location.pathname;
  window.location.href = '/logout?redirect=' + encodeURIComponent(currentPath);
}