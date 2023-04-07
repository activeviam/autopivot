var baseUrl = window.location.href.split('/ui')[0];

window.env = {
  contentServerUrl: baseUrl,
  contentServerVersion: "6.0.0",
  // WARNING: Changing the keys of activePivotServers will break previously saved widgets and dashboards.
  // If you must do it, then you also need to update each one's serverKey attribute on your content server.
  activePivotServers: {
    "AutoPivot": {
      url: baseUrl,
      version: "6.0.0",
    },
  },
};
