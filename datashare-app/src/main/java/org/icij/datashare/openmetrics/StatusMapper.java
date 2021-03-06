package org.icij.datashare.openmetrics;

import org.icij.datashare.time.DatashareTime;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StatusMapper {
    private final String metricName;
    private final Object status;

    public StatusMapper(String metricName, Object status) {
        this.metricName = metricName;
        this.status = status;
    }

    @Override
    public String toString() {
        if (status == null) return "";
        String header = "# HELP gauge The datashare resources\n" + String.format("# TYPE gauge %s\n", metricName);
        List<Field> declaredFields = Arrays.stream(this.status.getClass().getDeclaredFields()).filter(f -> !f.getName().startsWith("this")).collect(Collectors.toList());

        StringBuilder fieldLines = new StringBuilder();
        for (Field field : declaredFields) {
            try {
                Object value = field.get(status);
                Object numberValue = value;
                String status = "";
                if (String.class.equals(field.getType())) {
                    numberValue = "Nan";
                } else if (boolean.class.equals(field.getType()) || Boolean.class.equals(field.getType())) {
                    numberValue = (Boolean) value ? 1 : 0;
                    status = String.format("status=\"%s\" ", (Boolean) value ? "OK" : "KO");
                }
                fieldLines.append(String.format("%s{%sresource=\"%s\"} %s %d\n", metricName, status, field.getName(), numberValue, DatashareTime.getInstance().currentTimeMillis()));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return header + fieldLines;
    }
}
