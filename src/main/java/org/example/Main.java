package org.example;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
public class Main {
    public static void main(String[] args) {
        System.out.println("start working");
        // 项目功能：生成k8s的yaml文件，工程包含多个Dockerfile功能，最终合并成一个yaml文件，允许提供过滤的功能

        String dir = args[0];
        String filters = args[1];
        String tag_prefix= args[3];
        String image_prefix = args[2];
        String freemaker_template_path = args[4];
        List<String> filter_list = Arrays.asList(filters.split(","));
        // walk dir find directory contain Dockerfile
        getDirContainDockerfileRecursively(dir, filter_list).stream()
                .map(dockerfileDir -> genYaml(dockerfileDir, tag_prefix, image_prefix, freemaker_template_path))
                .reduce((a, b) -> a + b)
                .ifPresent(System.out::println);
    }

    private static String genYaml(File dockerfileDir, String tag_prefix,
                                  String image_prefix, String freemaker_template_path) {
        // get parent dir name as service_name
        // image_name = image_prefix + service_name + tag_prefix
        // freemaker template need service_name, image_name
        System.out.println("start freemaker_template_path: " + freemaker_template_path);
        String servicename = dockerfileDir.getName();
        String imagename = image_prefix + servicename + tag_prefix;
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        try {
            System.out.println("freemaker_template_path: " + freemaker_template_path);
            cfg.setDirectoryForTemplateLoading(new File(freemaker_template_path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Template template = cfg.getTemplate("k8s.tpl");
            Map<String, Object> data = new HashMap<>();
            data.put("service_name", servicename);
            data.put("image_name", imagename);
            Writer sb = new StringWriter();
            template.process(data, sb);
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TemplateException e) {
//            throw new RuntimeException(e);
            e.printStackTrace();
        }
        return null;
    }

    private static List<File> getDirContainDockerfileRecursively(String dir, List<String> filterList) {
        List<File> result = new ArrayList<>();
        File root = new File(dir);
        if (!root.exists()) {
            return result;
        }
        Queue<File> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            File file = queue.poll();
            if (file.isDirectory()) {
                System.out.println("searching dir: " + file.getAbsolutePath());
                File[] files = file.listFiles();
                if (files != null) {
                    for (File f : files) {
                        queue.offer(f);
                    }
                }
            } else {
                System.out.println("searching file: " + file.getName());
                // print parent
                System.out.println("searching file parent: " + file.getParentFile().getName());
                if (file.getName().equals("Dockerfile")) {
                    boolean isValid = true;
                    for (String filter : filterList) {
                        if (file.getParentFile().getName().equals(filter)) {
                            System.out.println("filter: " + filter);
                            isValid = false;
                            break;
                        }
                    }
                    if (isValid) {
                        result.add(file);
                    }
                }
            }
        }
        System.out.println("result: " + result);
        return result.stream().map(File::getParentFile).distinct().collect(Collectors.toList());

    }
}