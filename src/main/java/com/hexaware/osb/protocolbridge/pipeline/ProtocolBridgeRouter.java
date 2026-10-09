package com.hexaware.osb.protocolbridge.pipeline;

import com.hexaware.osb.protocolbridge.adapter.outbound.BusinessServiceAdapter;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.exception.ProtocolBridgeException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProtocolBridgeRouter {

    private final Map<ProtocolType, BusinessServiceAdapter> adapterMap = new EnumMap<>(ProtocolType.class);

    public ProtocolBridgeRouter(List<BusinessServiceAdapter> adapters) {
        for (BusinessServiceAdapter adapter : adapters) {
            adapterMap.put(adapter.getSupportedProtocol(), adapter);
        }
    }

    public void routeToBusinessService(CanonicalMessage context) {
        ProtocolType targetProtocol = context.getTargetProtocol();
        BusinessServiceAdapter adapter = adapterMap.get(targetProtocol);

        if (adapter == null) {
            throw new ProtocolBridgeException(
                "No OSB Business Service Adapter registered for target protocol: " + targetProtocol,
                "PROTOCOL_ROUTER_STAGE", 501);
        }

        adapter.invokeBusinessService(context);
    }
}
