// CRA dev-server proxy — mirrors the nginx proxy rules from nginx.conf so
// local `npm start` and Docker (nginx) behave identically for API paths.
const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function (app) {
  const AUTH_SERVICE = 'http://localhost:8081';
  const GAME_SERVICE = 'http://localhost:8082';

  const proxyOpts = (target) => ({
    target,
    changeOrigin: true,
    logLevel: 'warn',
  });

  // Auth-service routes (more specific — must come first)
  app.use('/api/auth', createProxyMiddleware(proxyOpts(AUTH_SERVICE)));
  app.use('/api/users', createProxyMiddleware(proxyOpts(AUTH_SERVICE)));
  app.use('/api/admin', createProxyMiddleware(proxyOpts(AUTH_SERVICE)));

  // Game-service routes
  app.use('/api', createProxyMiddleware(proxyOpts(GAME_SERVICE)));
  app.use('/ws', createProxyMiddleware({
    target: GAME_SERVICE,
    changeOrigin: true,
    ws: true,
    logLevel: 'warn',
    onProxyReq: (proxyReq) => {
      proxyReq.setHeader('Origin', GAME_SERVICE);
    },
  }));
};
