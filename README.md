# JSON Restructure

## Описание
Инструмент для трансформации JSON-данных по XML-конфигурации маппинга.

## Использование
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
    
```
## CONFIG
```xml
<config>
    <mapping>
        <src>user.name</src>
        <dst>userInfo.fullName</dst>
    </mapping>

    <mapping>
        <src>user.age</src>
        <dst>userInfo.age</dst>
    </mapping>

    <mapping>
        <src>user.tags[]</src>
        <dst>tags[]</dst>
    </mapping>

    <mapping>
        <src>user.orders[].amount</src>
        <dst>orders[]</dst>
        <multiply>1.5</multiply>
        <scale>2</scale>
    </mapping>

    <mapping>
        <src>user.orders[].items[].price</src>
        <dst>orderPrice[]</dst>
    </mapping>
</config>
```