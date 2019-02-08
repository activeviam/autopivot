var baseUrl = window.location.href.split('/ui')[0];

window.env = {
  serverUrls: {
    activePivot: baseUrl,
    activeMonitor: baseUrl,
    content: baseUrl
  }
};
