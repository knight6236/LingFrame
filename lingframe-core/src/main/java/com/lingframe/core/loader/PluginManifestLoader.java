package com.lingframe.core.loader;

import com.lingframe.api.config.PluginDefinition;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

public class PluginManifestLoader {

    public static PluginDefinition load(InputStream inputStream) {
        // 1. 配置加载选项
        // 注意：SnakeYAML 2.x 建议显式传入 LoaderOptions
        LoaderOptions options = new LoaderOptions();
        // 允许访问所有类（防止安全限制报错）
        options.setTagInspector(tag -> true);

        // 2. 创建构造器，指定根对象类型为 PluginDefinition
        Constructor constructor = new Constructor(PluginDefinition.class, options);

        // 3. 实例化 Yaml 对象
        Yaml yaml = new Yaml(constructor);

        // 4. 加载并返回
        return yaml.load(inputStream);
    }

}
