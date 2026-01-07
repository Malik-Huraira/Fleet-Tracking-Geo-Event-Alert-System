package com.geofleet.alert.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Custom Serde for IdleAggregator used in Kafka Streams state store.
 */
public class IdleAggregatorSerde implements Serde<IdleDetectionStream.IdleAggregator> {

    private final ObjectMapper objectMapper;

    public IdleAggregatorSerde(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Serializer<IdleDetectionStream.IdleAggregator> serializer() {
        return (topic, data) -> {
            try {
                return objectMapper.writeValueAsBytes(data);
            } catch (Exception e) {
                return null;
            }
        };
    }

    @Override
    public Deserializer<IdleDetectionStream.IdleAggregator> deserializer() {
        return (topic, data) -> {
            try {
                if (data == null) return new IdleDetectionStream.IdleAggregator();
                return objectMapper.readValue(data, IdleDetectionStream.IdleAggregator.class);
            } catch (Exception e) {
                return new IdleDetectionStream.IdleAggregator();
            }
        };
    }
}
