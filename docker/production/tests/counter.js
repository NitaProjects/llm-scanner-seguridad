// Fixture de upstream: no clientes externos, secretos ni cuerpos en logs.
const http = require('node:http');
let writes = 0;
let authorizationSeen = false;
http.createServer((req, res) => {
  res.setHeader('Content-Type', 'application/json');
  if (req.url === '/fixture-count') {
    res.end(JSON.stringify({ writes, authorizationSeen })); return;
  }
  const write = !['GET', 'HEAD', 'OPTIONS'].includes(req.method);
  if (write) { writes++; authorizationSeen = Boolean(req.headers.authorization); }
  req.resume();
  req.on('end', () => {
    if (write) { res.statusCode = 201; res.setHeader('Location', '/api/projects/fixture'); res.end('{"id":"fixture"}'); }
    else if (req.url === '/api/projects') { res.end('[]'); }
    else { res.statusCode = 404; res.end('{"code":"NOT_FOUND"}'); }
  });
}).listen(8080, '0.0.0.0');
