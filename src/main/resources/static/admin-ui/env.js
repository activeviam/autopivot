var baseUrl = window.location.href.split("/admin/ui")[0];

window.env = {
  contentServerUrl: baseUrl,
  contentServerVersion: "6.0.0",
  activePivotServers: {
    AutoPivot: {
      url: baseUrl,
      version: "6.0.0",
    },
  },
};
