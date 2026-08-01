package kameker;


import java.io.IOException;

public class Main {
    static void main() throws IOException {
        JsonRes jsr = new JsonRes("settings.json");
        jsr.convert("test.json");
    }
}
