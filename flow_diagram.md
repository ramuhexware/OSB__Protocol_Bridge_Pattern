# Oracle Service Bus (OSB) Protocol Bridge Pattern - Application Flow & Architecture

This document provides a comprehensive end-to-end breakdown and visual flow diagrams of the **OSB Protocol Bridge Pattern** application. Built with Java 21 and Spring Boot 3.3.4, this Enterprise Integration Pattern (EIP) middleware decouples heterogeneous clients from legacy backend systems by normalizing incoming requests, executing protocol-level and payload transformations, and dynamically routing messages to target business services.

---

## 1. High-Level Architecture Flowchart

The diagram below illustrates the core architecture and message lifecycle across inbound proxy adapters, the normalized **Canonical Message** model, the 5-stage OSB pipeline, dynamic routing, outbound adapters, backend systems, and real-time audit logging.

```mermaid
flowchart TD
    %% Client Tier
    subgraph Clients["Client Tier / Event Sources"]
        C1["HTTP REST Client<br/>(curl / Postman / UI Workbench)"]
        C2["SOAP Web Service Client<br/>(XML Payload)"]
        C3["JMS Publisher<br/>(osb.inbound.queue)"]
        C4["File System<br/>(./inbound-files - CSV/XML)"]
    end

    %% Inbound Proxy Layer
    subgraph InboundProxies["Inbound Proxy Adapters (Message Reception)"]
        IP1["HttpProxyController<br/>/api/v1/bridge/*"]
        IP2["SoapProxyEndpoint<br/>/proxy/soap/OrderBridgeService"]
        IP3["JmsInboundListener<br/>@JmsListener(osb.inbound.queue)"]
        IP4["FileInboundWatcher<br/>@Scheduled directory scanner"]
    end

    %% Canonical Model Normalization
    subgraph Normalization["Canonical Context Initialization"]
        CM["CanonicalMessage Model<br/>• messageId & correlationId<br/>• sourceProtocol & targetProtocol<br/>• rawInboundPayload<br/>• headers & properties"]
    end

    %% 5-Stage OSB Message Pipeline
    subgraph Pipeline["MessagePipelineProcessor (5-Stage OSB Pipeline)"]
        S1["Stage 1: INBOUND_PROXY<br/>Context capture & trace initialization"]
        S2["Stage 2: HEADER_TRANSFORM<br/>Header mapping & protocol translation<br/>(HTTP Headers ↔ JMS Props ↔ SOAP Headers)"]
        S3["Stage 3: PAYLOAD_TRANSFORM<br/>Format transformation<br/>(JSON ↔ SOAP XML ↔ CSV-to-JSON ↔ JMS Metadata)"]
        S4["Stage 4: PROTOCOL_ROUTER<br/>Dynamic strategy lookup for outbound adapter"]
        S5["Stage 5: OUTBOUND_RESPONSE<br/>Response mapping, latency trace & fault format"]
    end

    %% Outbound Router & Business Service Adapters
    subgraph RoutingAdapters["Outbound Business Service Adapters"]
        PBR["ProtocolBridgeRouter"]
        OA1["RestBusinessServiceAdapter<br/>(HTTP REST Client / RestTemplate)"]
        OA2["HttpSoapBusinessServiceAdapter<br/>(HTTP SOAP Client / RestTemplate)"]
        OA3["JmsBusinessServiceAdapter<br/>(JMS Producer & Sync Request-Reply)"]
        OA4["FileBusinessServiceAdapter<br/>(Outbound File Writer to ./outbound-files)"]
    end

    %% Downstream & Target Services
    subgraph Downstream["Target Services / Backends"]
        BE1["Mock REST Warehouse Service<br/>/mock/rest/orders"]
        BE2["Mock SOAP Order Web Service<br/>/mock/soap/OrderService"]
        BE3["Apache Artemis JMS Queue<br/>osb.backend.queue ➔ osb.backend.reply.queue"]
        BE4["File Storage Directory<br/>./outbound-files/*.json"]
    end

    %% Audit & Monitoring
    subgraph AuditMonitoring["Audit Logging & Real-time Dashboard"]
        ALS["AuditLogService<br/>(ConcurrentLinkedDeque max 100 entries)"]
        DBC["DashboardController<br/>/api/v1/dashboard/logs"]
        UI["Web UI Dashboard<br/>(Pipeline Visualizer & Transaction Log)"]
    end

    %% Connections - Clients to Inbound Proxies
    C1 -->|HTTP POST JSON| IP1
    C2 -->|HTTP POST XML| IP2
    C3 -->|TextMessage| IP3
    C4 -->|Directory Poll| IP4

    %% Connections - Inbound to Canonical Message
    IP1 -->|Wrap Request| CM
    IP2 -->|Wrap Request| CM
    IP3 -->|Wrap Request| CM
    IP4 -->|Wrap Request| CM

    %% Connections - Canonical to Pipeline
    CM -->|Pass Context| S1
    S1 --> S2
    S2 --> S3
    S3 --> S4

    %% Connections - Pipeline to Router & Adapters
    S4 -->|Invoke Route| PBR
    PBR -->|Lookup HTTP_REST| OA1
    PBR -->|Lookup HTTP_SOAP| OA2
    PBR -->|Lookup JMS_QUEUE| OA3
    PBR -->|Lookup FILE_SYSTEM| OA4

    %% Connections - Adapters to Downstream Services
    OA1 -->|HTTP REST POST| BE1
    OA2 -->|HTTP SOAP POST| BE2
    OA3 -->|Send & Receive Reply| BE3
    OA4 -->|Write JSON File| BE4

    %% Connections - Back to Response Pipeline
    BE1 -->|REST Response| S5
    BE2 -->|SOAP Envelope Response| S5
    BE3 -->|JMS Reply Message| S5
    BE4 -->|File Written Ack| S5

    %% Audit Logging Connections
    S5 -->|Record Transaction| ALS
    ALS -->|Supply Logs| DBC
    DBC -->|Render Pipeline & Audit| UI
```

---

## 2. End-to-End Sequence Diagrams

### 2.1 HTTP REST to SOAP Web Service Bridge Protocol Flow (`REST -> SOAP`)

In this scenario, a modern REST client sends a JSON payload to the bridge. The bridge transforms JSON into a SOAP 1.1 XML Envelope, invokes a legacy SOAP service, transforms the XML response back to JSON, and returns the response to the REST client.

```mermaid
sequenceDiagram
    autonumber
    actor Client as REST Client
    participant Controller as HttpProxyController
    participant Pipeline as MessagePipelineProcessor
    participant HeaderStage as HeaderTransformStage
    participant PayloadStage as PayloadTransformStage
    participant Router as ProtocolBridgeRouter
    participant SoapAdapter as HttpSoapBusinessServiceAdapter
    participant SoapBackend as MockBackendServiceController
    participant Audit as AuditLogService

    Client->>Controller: POST /api/v1/bridge/rest-to-soap (JSON Payload)
    Controller->>Pipeline: executePipeline(CanonicalMessage context)
    
    note over Pipeline: Stage 1: INBOUND_PROXY Init Trace
    Pipeline->>HeaderStage: processHeaderTranslation(context)
    note over HeaderStage: Stage 2: Inject X-Correlation-ID & SOAPAction
    HeaderStage-->>Pipeline: Headers Ready
    
    Pipeline->>PayloadStage: processPayloadTransformation(context)
    note over PayloadStage: Stage 3: jsonToSoapEnvelope() -> &lt;soapenv:Envelope&gt;
    PayloadStage-->>Pipeline: Transformed SOAP XML Payload

    Pipeline->>Router: routeToBusinessService(context)
    note over Router: Stage 4: Match ProtocolType.HTTP_SOAP
    Router->>SoapAdapter: invokeBusinessService(context)
    
    SoapAdapter->>SoapBackend: POST /mock/soap/OrderService (SOAP XML + SOAPAction)
    SoapBackend-->>SoapAdapter: 200 OK (SOAP Response XML Envelope)
    SoapAdapter-->>Pipeline: Set Response Payload & Status Code 200

    note over Pipeline: Stage 5: OUTBOUND_RESPONSE Mapping
    Pipeline->>Audit: recordTransaction(context, response)
    Audit-->>Pipeline: Audit Log Recorded
    Pipeline-->>Controller: BridgeResponse Object
    Controller-->>Client: 200 OK Response (BridgeResponse JSON with pipeline trace)
```

---

### 2.2 HTTP REST to JMS Queue Request-Reply Bridge Protocol Flow (`REST -> JMS`)

In this scenario, an HTTP REST request is converted to an ActiveMQ JMS TextMessage and published to `osb.backend.queue`. The adapter performs a synchronous request-reply using correlation ID matching on `osb.backend.reply.queue`.

```mermaid
sequenceDiagram
    autonumber
    actor Client as REST Client
    participant Controller as HttpProxyController
    participant Pipeline as MessagePipelineProcessor
    participant HeaderStage as HeaderTransformStage
    participant PayloadStage as PayloadTransformStage
    participant Router as ProtocolBridgeRouter
    participant JmsAdapter as JmsBusinessServiceAdapter
    participant ActiveMQ as Embedded Artemis JMS Broker
    participant JmsBackend as MockJmsBackendListener
    participant Audit as AuditLogService

    Client->>Controller: POST /api/v1/bridge/rest-to-jms (JSON Payload)
    Controller->>Pipeline: executePipeline(CanonicalMessage context)
    
    Pipeline->>HeaderStage: processHeaderTranslation(context)
    note over HeaderStage: Map Canonical Headers ➔ JMS Properties (JMSCorrelationID)
    HeaderStage-->>Pipeline: Headers Ready

    Pipeline->>PayloadStage: processPayloadTransformation(context)
    note over PayloadStage: Enrich JSON with _osbProtocolBridge Metadata
    PayloadStage-->>Pipeline: Enriched JSON String

    Pipeline->>Router: routeToBusinessService(context)
    Router->>JmsAdapter: invokeBusinessService(context)

    JmsAdapter->>ActiveMQ: send(osb.backend.queue, TextMessage + JMSReplyTo)
    ActiveMQ->>JmsBackend: @JmsListener consumes message
    note over JmsBackend: Process Order & Prepare Reply
    JmsBackend->>ActiveMQ: send(osb.backend.reply.queue, Reply JSON + JMSCorrelationID)

    JmsAdapter->>ActiveMQ: receiveSelected(osb.backend.reply.queue, "JMSCorrelationID = 'CORR-XXX'")
    ActiveMQ-->>JmsAdapter: Reply TextMessage Received
    JmsAdapter-->>Pipeline: Set Response Payload & Status Code 200

    Pipeline->>Audit: recordTransaction(context, response)
    Pipeline-->>Controller: BridgeResponse
    Controller-->>Client: 200 OK (BridgeResponse JSON)
```

---

### 2.3 File System Watcher Batch Inbound Bridge Flow (`FILE -> JMS / REST`)

In this scenario, the `@Scheduled` file watcher scans `./inbound-files` for CSV/XML files, converts CSV rows into structured JSON array records, processes the batch through the OSB pipeline, dispatches to JMS, and archives the file.

```mermaid
sequenceDiagram
    autonumber
    participant FileSys as File System Directory (./inbound-files)
    participant Watcher as FileInboundWatcher (@Scheduled)
    participant Pipeline as MessagePipelineProcessor
    participant PayloadStage as PayloadTransformStage
    participant Router as ProtocolBridgeRouter
    participant JmsAdapter as JmsBusinessServiceAdapter
    participant Archive as Archive Directory (./inbound-files/archive)

    FileSys->>Watcher: Scan detects new file: order_batch.csv
    Watcher->>Pipeline: executePipeline(CanonicalMessage with CSV payload)
    
    Pipeline->>PayloadStage: processPayloadTransformation(context)
    note over PayloadStage: csvToJson() -> Converts CSV lines to JSON array & metadata
    PayloadStage-->>Pipeline: Transformed Batch JSON

    Pipeline->>Router: routeToBusinessService(context)
    Router->>JmsAdapter: invokeBusinessService(context)
    JmsAdapter-->>Pipeline: Dispatch Completed

    Pipeline-->>Watcher: BridgeResponse
    Watcher->>Archive: Move file to proc_[timestamp]_order_batch.csv
```

---

## 3. 5-Stage OSB Message Pipeline Mechanics

The central engine of the application is the `MessagePipelineProcessor`, which executes a strictly ordered 5-stage VETRO (Validate, Enrich, Transform, Route, Operate) pattern:

| Stage # | Stage Name | Component | Core Responsibilities |
| :--- | :--- | :--- | :--- |
| **Stage 1** | **Inbound Proxy Entry** | `MessagePipelineProcessor` | Captures message entry point, generates `messageId` and `correlationId` if missing, starts timer, and logs entry trace. |
| **Stage 2** | **Header Transformation** | `HeaderTransformStage` | Normalizes incoming headers into canonical header map; translates headers into protocol-specific metadata (e.g., `X-Correlation-ID`, `JMSCorrelationID`, `SOAPAction`, `Content-Type`). |
| **Stage 3** | **Payload Transformation** | `PayloadTransformStage` | Performs data conversion (JSON to SOAP XML Envelope, SOAP XML to JSON, CSV to JSON array, JSON metadata enrichment for JMS). |
| **Stage 4** | **Protocol Bridge Routing** | `ProtocolBridgeRouter` | Uses Strategy Pattern (`EnumMap<ProtocolType, BusinessServiceAdapter>`) to select and execute the corresponding outbound adapter (`Rest`, `Soap`, `JMS`, `File`). |
| **Stage 5** | **Outbound Response & Audit** | `AuditLogService` | Captures status code, status message, response payload, updates total execution time (ms), appends pipeline step traces, and logs transaction into in-memory queue. |

---

## 4. Project Component Mapping & Package Breakdown

```
com.hexaware.osb.protocolbridge
│
├── adapter
│   ├── inbound                     # Inbound Proxy Adapters (Protocol Entry Points)
│   │   ├── HttpProxyController.java      # REST endpoints (/api/v1/bridge/*)
│   │   ├── SoapProxyEndpoint.java        # SOAP endpoint (/proxy/soap/OrderBridgeService)
│   │   ├── JmsInboundListener.java       # JMS queue listener (osb.inbound.queue)
│   │   └── FileInboundWatcher.java       # Scheduled file scanner (./inbound-files)
│   │
│   └── outbound                    # Outbound Business Service Adapters (Target Protocol Bindings)
│       ├── BusinessServiceAdapter.java   # Adapter Strategy Interface
│       ├── RestBusinessServiceAdapter.java       # Outbound HTTP REST Client
│       ├── HttpSoapBusinessServiceAdapter.java   # Outbound HTTP SOAP Client
│       ├── JmsBusinessServiceAdapter.java        # Outbound JMS Producer & Sync Request-Reply
│       └── FileBusinessServiceAdapter.java       # Outbound File Writer
│
├── config
│   └── ActiveMQJmsConfig.java      # Embedded Apache Artemis JMS configuration & queue declarations
│
├── domain                          # Canonical Data Model & Request/Response Contracts
│   ├── CanonicalMessage.java       # Internal normalized OSB message context
│   ├── ProtocolType.java           # Protocol enum (HTTP_REST, HTTP_SOAP, JMS_QUEUE, FILE_SYSTEM)
│   ├── BridgeRequest.java          # REST API request payload model
│   ├── BridgeResponse.java         # REST API response model with pipeline execution trace
│   └── AuditLogEntry.java          # Audit log record model
│
├── exception
│   ├── ProtocolBridgeException.java      # Custom runtime fault with stage identification
│   └── GlobalBridgeExceptionHandler.java # Spring ControllerAdvice fault handler
│
├── mockbackend                     # Mock Downstream Target Services
│   ├── MockBackendServiceController.java # Mock REST (/mock/rest/orders) & SOAP (/mock/soap/OrderService)
│   └── MockJmsBackendListener.java       # Mock JMS consumer on osb.backend.queue
│
├── pipeline                        # OSB Core Pipeline Engine
│   ├── MessagePipelineProcessor.java     # 5-stage pipeline executor
│   ├── HeaderTransformStage.java         # Stage 2: Header translation
│   ├── PayloadTransformStage.java        # Stage 3: Payload conversion
│   └── ProtocolBridgeRouter.java         # Stage 4: Outbound strategy router
│
├── service
│   └── AuditLogService.java        # Sliding window transaction audit logger (max 100 entries)
│
└── ui
    └── DashboardController.java     # REST API serving audit logs & simulation triggers for UI dashboard
```

---

## 5. Protocol Transformation Matrix

| Source Protocol | Target Protocol | Payload Transformation Technique | Header Translation |
| :--- | :--- | :--- | :--- |
| **HTTP REST (JSON)** | **HTTP SOAP (XML)** | Converts JSON keys into XML tags inside a standard `<soapenv:Envelope>` & `<ord:ProcessOrderRequest>`. | Maps HTTP Headers ➔ `SOAPAction` & `Content-Type: text/xml`. |
| **HTTP SOAP (XML)** | **HTTP REST (JSON)** | Parses XML tags (`<orderId>`, `<status>`, `<customer>`, `<amount>`) and constructs structured JSON. | Maps SOAP Headers ➔ HTTP Headers & `Content-Type: application/json`. |
| **HTTP REST (JSON)** | **JMS Queue** | Injects `_osbProtocolBridge` metadata (`correlationId`, `bridgedAt`, `sourceProtocol`) into JSON body. | Maps HTTP Headers ➔ JMS Properties (`JMSCorrelationID`, `JMSType`). |
| **File System (CSV)** | **JMS Queue / REST** | Reads CSV header row and transforms CSV data lines into a JSON array wrapped in a batch payload. | Injects `FileName` and `SourceSystem: FileWatcher` into headers. |
| **JMS Queue** | **HTTP REST (JSON)** | Reads TextMessage body, passes through payload to HTTP REST endpoint. | Maps JMS Message Properties ➔ HTTP Request Headers (`JMS_Property`). |
