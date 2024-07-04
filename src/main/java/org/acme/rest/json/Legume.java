package org.acme.rest.json;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.ToString;

@ToString
@RegisterForReflection
public class Legume {

    public String name;
    public String description;

    public Legume() {
    }

    public Legume(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
