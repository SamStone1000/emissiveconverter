package emissive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class LightmapToBloom implements Consumer<Path> {

    public final Path assets;
    public static final ObjectMapper mapper = new ObjectMapper();

    public LightmapToBloom(Path root) {
	this.assets = root;
    }

    @Override
    public void accept(Path file) {
	JsonNode root;
	try {
	    root = mapper.readTree(file.toFile());
	} catch (IOException e) {
	    System.out.println("Error while reading file!");
	    return;
	}
	if (!root.has("ae2_uvl_marker")) {
	    return;
	}

	List<JsonNode> emissiveElements = new ArrayList<>();
	Set<String> emissiveTextures = new HashSet<>();
	for (JsonNode element : root.get("elements")) {
	    for (JsonNode face : element.get("faces")) {
		if (face.has("uvlightmap")) {
		    ObjectNode faces = (ObjectNode) element.get("faces");
		    JsonNode emissiveElement = element.deepCopy();
		    emissiveElements.add(emissiveElement);
		    ObjectNode emissiveFaces = (ObjectNode) emissiveElement.get("faces");
		    emissiveTextures.addAll(convert(faces, emissiveFaces));
		    break;
		}
	    }
	}
	
	for (JsonNode element : emissiveElements) {
	    ((ArrayNode) root.get("elements")).add(element);
	}
	
	for (String texture : emissiveTextures) {
	    String base = toResource(root, texture);
	    Path orginal = this.assets.resolve(base + ".png");
	    Path emissivePNG = this.assets.resolve(base + "_bloom.png");
	    Path emissiveMeta = this.assets.resolve("_bloom.png.mcmeta");
	    try {
		Files.deleteIfExists(emissivePNG);
		Files.copy(orginal, emissivePNG);
		JsonNode meta;
		if (Files.exists(emissiveMeta)) {
		    meta = mapper.readTree(emissiveMeta.toFile());
		} else {
		    meta = mapper.createObjectNode();
		}
		ObjectNode ctm = meta.withObject("ctm");
		ctm.put("ctm_version", 1);
		ctm.put("layer", "BLOOM");
		ctm.put("gregtech", true);
		ctm.withObject("extra").put("light", 8);
		
	    } catch (IOException e) {
		System.out.println("Error copying textures!");
		e.printStackTrace();
		return;
	    }
	}
    }

    public Set<String> convert(ObjectNode element, ObjectNode emissive) {
	Set<String> textures = new HashSet<>();
	Iterator<Map.Entry<String, JsonNode>> fields = element.fields();
	while (fields.hasNext()) {
	    Map.Entry<String, JsonNode> field = fields.next();
	    if (field.getValue().has("uvlightmap")) {
		ObjectNode elementFace = (ObjectNode) field.getValue();
		elementFace.remove("uvlightmap");
		ObjectNode emissiveFace = (ObjectNode) emissive.get(field.getKey());
		emissiveFace.remove("uvlightmap");
		textures.add(emissiveFace.get("texture").asText());
		emissiveFace.put("texture", emissiveFace.get("texture") + "_bloom");
	    } else {
		emissive.remove(field.getKey());
	    }
	}
	return textures;
    }

    public String toResource(JsonNode root, String textureVar) {
	return root.get("textures").get(textureVar.substring(1, textureVar.length())).asText().replace(":", "/textures/");
    }
}
