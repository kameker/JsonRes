# JSON Restructure

## Описание
Инструмент для трансформации JSON-данных по XML-конфигурации маппинга.

## Использование

### В коде
```java
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static String jsonFileName = "input.json";
    static String CONFIG = "config.xml";

    static void main() throws Exception {
        JsonRes res = new JsonRes(CONFIG);
        JsonNode input = MAPPER.readTree(new File(jsonFileName));
        JsonNode result = res.restructure(input);

        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }