const presets = {
  'rest-to-jms': {
    source: 'HTTP_REST',
    target: 'JMS_QUEUE',
    payload: JSON.stringify({
      orderId: "ORD-99201",
      customer: "Standard Chartered",
      amount: 4500.00,
      currency: "USD",
      items: ["Server Rack", "Fiber Cable"]
    }, null, 2)
  },
  'rest-to-soap': {
    source: 'HTTP_REST',
    target: 'HTTP_SOAP',
    payload: JSON.stringify({
      orderId: "ORD-SOAP-7788",
      customer: "Hexaware Technologies",
      amount: 1250.50
    }, null, 2)
  },
  'soap-to-jms': {
    source: 'HTTP_SOAP',
    target: 'JMS_QUEUE',
    payload: `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ord="http://hexaware.com/osb/order">
  <soapenv:Header>
    <ord:CorrelationId>CORR-SOAP-100</ord:CorrelationId>
  </soapenv:Header>
  <soapenv:Body>
    <ord:ProcessOrderRequest>
      <ord:orderId>ORD-XML-4040</ord:orderId>
      <ord:customer>Barclays Bank</ord:customer>
      <ord:amount>3400.00</ord:amount>
    </ord:ProcessOrderRequest>
  </soapenv:Body>
</soapenv:Envelope>`
  },
  'file-to-jms': {
    source: 'FILE_SYSTEM',
    target: 'JMS_QUEUE',
    payload: `orderId,customer,amount
ORD-CSV-101,Acme Corp,450.00
ORD-CSV-102,Globex Corp,1250.00
ORD-CSV-103,Initech,890.50`
  }
};

function loadPreset(key) {
  document.querySelectorAll('.preset-pills .pill-btn').forEach(btn => btn.classList.remove('active'));
  event.target.classList.add('active');

  const p = presets[key];
  if (p) {
    document.getElementById('sourceProtocol').value = p.source;
    document.getElementById('targetProtocol').value = p.target;
    document.getElementById('payload').value = p.payload;
  }
}

async function executeBridge() {
  const sourceProtocol = document.getElementById('sourceProtocol').value;
  const targetProtocol = document.getElementById('targetProtocol').value;
  const payload = document.getElementById('payload').value;
  const traceBox = document.getElementById('traceBox');
  const responseBox = document.getElementById('responseBox');

  traceBox.innerHTML = '<div class="trace-line">Connecting to OSB Inbound Proxy...</div>';
  responseBox.innerText = 'Processing message transformation...';

  try {
    const res = await fetch('/api/v1/bridge/execute', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sourceProtocol, targetProtocol, payload })
    });

    const data = await res.json();

    // Render pipeline trace
    traceBox.innerHTML = '';
    if (data.pipelineTrace) {
      data.pipelineTrace.forEach(step => {
        const div = document.createElement('div');
        div.className = 'trace-line' + (step.includes('ERROR') || step.includes('FAULT') ? ' error' : '');
        div.innerText = step;
        traceBox.appendChild(div);
      });
    }

    // Pretty print response
    try {
      responseBox.innerText = JSON.stringify(JSON.parse(data.responsePayload), null, 2);
    } catch (e) {
      responseBox.innerText = data.responsePayload || data.statusMessage;
    }

    fetchLogs();

  } catch (err) {
    traceBox.innerHTML += `<div class="trace-line error">Network Error: ${err.message}</div>`;
    responseBox.innerText = 'Failed to execute protocol bridge call.';
  }
}

async function fetchLogs() {
  const tbody = document.getElementById('logsTableBody');
  try {
    const res = await fetch('/api/v1/dashboard/logs');
    const logs = await res.json();

    if (!logs || logs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No audit transactions recorded yet.</td></tr>';
      return;
    }

    tbody.innerHTML = logs.map(l => `
      <tr>
        <td>${new Date(l.timestamp).toLocaleTimeString()}</td>
        <td><code>${l.messageId}</code></td>
        <td><code>${l.correlationId}</code></td>
        <td><strong>${l.sourceProtocol}</strong> ➔ <strong>${l.targetProtocol}</strong></td>
        <td><span class="badge badge-${l.statusCode}">${l.statusCode} ${l.status}</span></td>
        <td>${l.executionTimeMs} ms</td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Failed to fetch audit logs', err);
  }
}

// Initial setup
window.addEventListener('DOMContentLoaded', () => {
  loadPreset('rest-to-jms');
  fetchLogs();
});
