package kameker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JsonRes {
    private Map<String, Object> settings;
    private ObjectMapper mapper;

    public JsonRes(String settingsFile) throws IOException {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.settings = mapper.readValue(
                new File(settingsFile),
                new TypeReference<Map<String, Object>>() {}
        );
    }
    public boolean convert(String inputJsonFileName) throws IOException {
        return convert(inputJsonFileName, null);
    }
    public boolean convert(String inputJsonFileName, String outputFileName) throws IOException {
        Map<String, Object> inputData = mapper.readValue(
                new File(inputJsonFileName),
                new TypeReference<Map<String, Object>>() {}
        );

        if (inputData.isEmpty()) return false;

        Map<String, Object> outputData = new LinkedHashMap<>();

        for (Map.Entry<String, Object> settingEntry : settings.entrySet()) {
            String sourceKey = settingEntry.getKey();
            Object settingValue = settingEntry.getValue();

            Object sourceValue = getValueByPath(inputData, sourceKey);
            if (sourceValue == null) continue;

            if (settingValue instanceof Map) {
                Map<?, ?> settingsMap = (Map<?, ?>) settingValue;
                String targetKey = settingsMap.get("key").toString();

                if (settingsMap.containsKey("index")) {
                    int index = Integer.parseInt(settingsMap.get("index").toString());
                    if (sourceValue instanceof List) {
                        List<?> list = (List<?>) sourceValue;
                        if (index >= 0 && index < list.size()) {
                            sourceValue = list.get(index);
                        } else {
                            continue;
                        }
                    } else if (sourceValue instanceof Object[]) {
                        Object[] array = (Object[]) sourceValue;
                        if (index >= 0 && index < array.length) {
                            sourceValue = array[index];
                        } else {
                            continue;
                        }
                    }
                }

                if (settingsMap.containsKey("nested")) {
                    Map<String, String> nestedMap = (Map<String, String>) settingsMap.get("nested");
                    Map<String, Object> nestedResult = new LinkedHashMap<>();
                    if (sourceValue instanceof Map) {
                        Map<String, Object> sourceMap = (Map<String, Object>) sourceValue;
                        for (Map.Entry<String, String> nestedEntry : nestedMap.entrySet()) {
                            Object nestedValue = getValueByPath(sourceMap, nestedEntry.getKey());
                            if (nestedValue != null) {
                                nestedResult.put(nestedEntry.getValue(), nestedValue);
                            }
                        }
                    }
                    outputData.put(targetKey, nestedResult);
                } else {
                    outputData.put(targetKey, sourceValue);
                }
            } else {
                outputData.put(settingValue.toString(), sourceValue);
            }
        }
        outputFileName = outputFileName == null ? "output" + inputJsonFileName : outputFileName;
        mapper.writeValue(new File(outputFileName), outputData);
        return true;
    }

    private Object getValueByPath(Map<String, Object> data, String path) {
        if (path.contains(".")) {
            String[] parts = path.split("\\.");
            Object current = data;
            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else if (current instanceof List) {
                    try {
                        int index = Integer.parseInt(part);
                        List<?> list = (List<?>) current;
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            return current;
        } else {
            return data.get(path);
        }
    }
}