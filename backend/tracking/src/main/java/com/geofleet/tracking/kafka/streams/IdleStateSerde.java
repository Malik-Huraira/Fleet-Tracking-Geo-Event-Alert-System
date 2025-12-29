package com.geofleet.tracking.kafka.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IdleStateSerde implements Serde<IdleState> {

    private final ObjectMapper objectMapper;

    @Override
    public Serializer<IdleState> serializer() {
        return new Serializer<IdleState>() {
            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
            }

            @Override
            public byte[] serialize(String topic, IdleState data) {
                try {
                    return objectMapper.writeValueAsBytes(data);
                } catch (IOException e) {
                    throw new RuntimeException("Error serializing IdleState", e);
                }
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public Deserializer<IdleState> deserializer() {
        return new Deserializer<IdleState>() {
            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
            }

            @Override
            public IdleState deserialize(String topic, byte[] data) {
                try {
                    if (data == null || data.length == 0) {
                        return new IdleState();
                    }
                    return objectMapper.readValue(data, IdleState.class);
                } catch (IOException e) {
                    throw new RuntimeException("Error deserializing IdleState", e);
                }
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }
}