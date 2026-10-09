package com.hexaware.osb.protocolbridge.adapter.outbound;

import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;

/**
 * OSB Business Service Adapter Interface (Outbound Protocol Binding).
 */
public interface BusinessServiceAdapter {
    ProtocolType getSupportedProtocol();
    void invokeBusinessService(CanonicalMessage context);
}
