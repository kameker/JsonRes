package kameker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class JsonRes {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final List<Mapping> mappings;

    public JsonRes(String configFile) throws Exception {
        this.mappings = parseConfigXml(new File(configFile));
    }

    public JsonRes(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    public JsonNode restructure(JsonNode input) {
        ObjectNode result = MAPPER.createObjectNode();
        for (Mapping mapping : mappings) {
            applyMapping(input, result, mapping);
        }
        return result;
    }

    private void applyMapping(JsonNode input, ObjectNode result, Mapping mapping) {
        List<Object> sourceValues = extractValues(input, mapping.src);
        List<String> destPath = parsePath(mapping.dst);

        for (Object value : sourceValues) {
            if (value instanceof JsonNode) {
                JsonNode node = (JsonNode) value;
                if (mapping.multiply != null && node.isNumber()) {
                    double val = node.asDouble() * mapping.multiply;
                    if (mapping.scale != null) {
                        val = round(val, mapping.scale.intValue());
                    }
                    node = MAPPER.valueToTree(val);
                } else if (mapping.scale != null && node.isNumber()) {
                    double val = round(node.asDouble(), mapping.scale.intValue());
                    node = MAPPER.valueToTree(val);
                }
                setValue(result, destPath, node);
            }
        }
    }

    private double round(double value, int places) {
        if (places < 0) return value;
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private List<Object> extractValues(JsonNode node, String path) {
        List<Object> results = new ArrayList<>();
        String[] parts = path.split("\\.");

        List<JsonNode> currentNodes = Collections.singletonList(node);

        for (String part : parts) {
            List<JsonNode> nextNodes = new ArrayList<>();
            boolean isArray = part.contains("[]");
            String fieldName = part.replace("[]", "");

            if (isArray) {
                for (JsonNode currentNode : currentNodes) {
                    JsonNode arrayNode = currentNode.get(fieldName);
                    if (arrayNode != null && arrayNode.isArray()) {
                        for (JsonNode item : arrayNode) {
                            nextNodes.add(item);
                        }
                    }
                }
            } else {
                for (JsonNode currentNode : currentNodes) {
                    JsonNode childNode = currentNode.get(part);
                    if (childNode != null) {
                        nextNodes.add(childNode);
                    }
                }
            }
            currentNodes = nextNodes;
            if (currentNodes.isEmpty()) break;
        }

        results.addAll(currentNodes);
        return results;
    }

    private List<String> parsePath(String path) {
        return Arrays.asList(path.split("\\."));
    }

    private void setValue(ObjectNode root, List<String> path, JsonNode value) {
        if (path.isEmpty()) return;

        ObjectNode current = root;
        for (int i = 0; i < path.size() - 1; i++) {
            String segment = path.get(i);
            boolean isArray = segment.contains("[]");
            String fieldName = segment.replace("[]", "");

            if (isArray) {
                ArrayNode arrayNode;
                if (current.has(fieldName)) {
                    arrayNode = (ArrayNode) current.get(fieldName);
                } else {
                    arrayNode = current.putArray(fieldName);
                }
                if (arrayNode.size() == 0) {
                    arrayNode.add(MAPPER.createObjectNode());
                }
                current = (ObjectNode) arrayNode.get(arrayNode.size() - 1);
            } else {
                if (!current.has(fieldName)) {
                    current.putObject(fieldName);
                }
                current = (ObjectNode) current.get(fieldName);
            }
        }

        String lastSegment = path.get(path.size() - 1);
        boolean isArray = lastSegment.contains("[]");
        String lastFieldName = lastSegment.replace("[]", "");

        if (isArray) {
            ArrayNode arrayNode;
            if (current.has(lastFieldName)) {
                arrayNode = (ArrayNode) current.get(lastFieldName);
            } else {
                arrayNode = current.putArray(lastFieldName);
            }
            arrayNode.add(value);
        } else {
            current.set(lastFieldName, value);
        }
    }

    private List<Mapping> parseConfigXml(File configFile) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(configFile);

        doc.getDocumentElement().normalize();
        NodeList mappingNodes = doc.getElementsByTagName("mapping");

        List<Mapping> mappings = new ArrayList<>();
        for (int i = 0; i < mappingNodes.getLength(); i++) {
            Node node = mappingNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                Mapping mapping = new Mapping();
                mapping.src = getTagContent(element, "src");
                mapping.dst = getTagContent(element, "dst");

                String multiply = getTagContent(element, "multiply");
                if (multiply != null) mapping.multiply = Double.parseDouble(multiply);

                String scale = getTagContent(element, "scale");
                if (scale != null) mapping.scale = Integer.parseInt(scale);

                mappings.add(mapping);
            }
        }
        return mappings;
    }

    private String getTagContent(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    public static class Mapping {
        public String src;
        public String dst;
        public Double multiply;
        public Integer scale;
    }
}