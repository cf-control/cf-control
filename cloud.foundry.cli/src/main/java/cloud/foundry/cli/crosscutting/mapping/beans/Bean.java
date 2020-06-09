package cloud.foundry.cli.crosscutting.mapping.beans;

/**
 * The interface for all 'Bean'-classes. Beans serve as object-representations of yaml-file contents. Beans can easily
 * be constructed from a corresponding yaml representation using the
 * {@link org.yaml.snakeyaml.Yaml#loadAs(String, Class) loadAs} method. They can also be converted back into their yaml
 * representation using the {@link org.yaml.snakeyaml.Yaml#dump(Object) dump} method.
 *
 * Every Bean should at least provide:
 * - a public constructor that takes no arguments
 * - public getters and setters for each attribute that is part of the yaml-file contents
 */
public interface Bean {

}
