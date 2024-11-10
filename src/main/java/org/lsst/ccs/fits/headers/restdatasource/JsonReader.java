package org.lsst.ccs.fits.headers.restdatasource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.lsst.ccs.imagenaming.ImageName;

/**
 *
 * @author tonyj
 */
public class JsonReader {

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(".*_(R\\d\\d_S.\\d).json");
    private final Path root;
    
    JsonReader(Path root) {
        this.root = root;
    }
    
    Map<String, Object> read(Path file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Map<String,Object>> valueTypeRef = new TypeReference<Map<String,Object>>(){};
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, Object> readValue = objectMapper.readValue(reader, valueTypeRef);
            readValue.remove("__CONTENT__");
            return readValue;
        } 
    }

    List<String> getComponents(ImageName imageName) throws IOException {
        Path path = root.resolve(imageName.getDateString()).resolve(imageName.toString());
        Stream<String> sorted = Files.list(path)
                .filter(file -> !Files.isDirectory(file))
                .map(Path::getFileName)
                .map(Path::toString)
                .map(s -> FILE_NAME_PATTERN.matcher(s))
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .sorted();
        return Stream.concat(Stream.of("common"), sorted)
                .collect(Collectors.toList());
    }

    private Map<String, Object> getCommonHeaders(ImageName imageName) throws IOException {
        // Method 1, read all of the files, and only keep the things which are in common
        Path path = root.resolve(imageName.getDateString()).resolve(imageName.toString());
        List<Path> files = Files.list(path)
                .filter(file -> !Files.isDirectory(file))
                .filter(file -> FILE_NAME_PATTERN.matcher(file.getFileName().toString()).matches()).collect(Collectors.toList());
        Map<String, Object> result = null;
        for (Path file : files) {
            Map<String, Object> headers = read(file);
            result = keepCommon(result, headers);
        }
        return result;
                
    }
    
    private static Map<String, Object> keepCommon(Map<String, Object> a, Map<String, Object> b) {
        if (a == null) {
            return b;
        } else {
            b.forEach((key, value) -> {
                if (a.containsKey(key)) {
                    if (!Objects.equals(a.get(key), value)) a.remove(key);
                }
            });    
            return a;
        }
    }
    
    Map<String, Object> getHeadersForComponent(ImageName imageName, String component) throws IOException {
        final Map<String, Object> commonHeaders = getCommonHeaders(imageName);
        if ("common".equals(component)) {
            return commonHeaders;
        } else {
            Path file = root.resolve(imageName.getDateString()).resolve(imageName.toString()).resolve(imageName+"_"+component+".json");
            Map<String, Object> headers = read(file);
            for (String key : commonHeaders.keySet()) {
                headers.remove(key);
            }
            return headers;
        }
    }
    
    static Map<String, Object> suppressNullValues(Map<String, Object> in) {        
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : in.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
            
//        return in.entrySet().stream()
//                .filter(e -> e.getValue() != null)
//                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), null, LinkedHashMap::new));
    }
    
    public static void main(String[] args) throws IOException {
//        Map<String, Object> a = new LinkedHashMap(Map.of("a",1, "b", 2,"c", 3));
//        Map<String, Object> b = new LinkedHashMap(Map.of("a", 3, "b", 2, "c", 1));
//        System.out.println(JsonReader.keepCommon(a,b));
        
        Path path = Paths.get("/home/tonyj/Data/");
        JsonReader reader = new JsonReader(path);
        ImageName imageName = new ImageName("MC_C_20231126_000275");
        List<String> imageNameComponents = reader.getComponents(imageName);
        System.out.println(imageNameComponents);
        
        Map<String, Object> c = reader.read(Paths.get("/home/tonyj/Data/20231126/MC_C_20231126_000275/MC_C_20231126_000275_R03_S10.json"));
        System.out.println(c);
//        Map<String, Object> d = reader.read(Path.of("/home/tonyj/Data/20231126/MC_C_20231126_000275/MC_C_20231126_000275_R13_S10.json"));
//        System.out.println(JsonReader.keepCommon(c,d));
//        
        Map<String, Object> commonHeaders = reader.getHeadersForComponent(imageName, "common");
        System.out.println(suppressNullValues(commonHeaders));
        
        Map<String, Object> uncommonHeaders = reader.getHeadersForComponent(imageName, "R11_S12");
        System.out.println(uncommonHeaders);
    }


}
