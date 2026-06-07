package com.ioes.photo.global.common.annotation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TruncateDecimal 애노테이션과 함께 사용하는 Jackson 직렬화기.
 *
 * BeanProperty에서  TruncateDecimal#scale()을 읽어 소수점 자리수를 결정한다.
 *
 * @author 황제연
 */
public class TruncateDecimalSerializer extends StdSerializer<Double> implements ContextualSerializer {

    private final int scale;

    public TruncateDecimalSerializer() {
        super(Double.class);
        this.scale = 1;
    }

    private TruncateDecimalSerializer(int scale) {
        super(Double.class);
        this.scale = scale;
    }

    @Override
    public void serialize(Double value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeNumber(BigDecimal.valueOf(value).setScale(scale, RoundingMode.DOWN));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property != null) {
            TruncateDecimal annotation = property.getAnnotation(TruncateDecimal.class);
            if (annotation == null) {
                annotation = property.getContextAnnotation(TruncateDecimal.class);
            }
            if (annotation != null) {
                return new TruncateDecimalSerializer(annotation.scale());
            }
        }
        return this;
    }
}
