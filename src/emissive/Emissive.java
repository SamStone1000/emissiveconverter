package emissive;

import emissive.LightmapToBloom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Emissive {
    public static void main(String[] args) {
	Path root = Path.of(args[0]);
	Path model = Path.of(args[1]);
	LightmapToBloom walker = new LightmapToBloom(root);
	try (Stream<Path> models = Files.walk(root.resolve(model));) {
	    models
		.filter(p -> p.getFileName().toString().endsWith(".json"))
		.forEach(walker);
	} catch (IOException e) {
	    System.out.println("Error while walking files!");
	}
    }
}
