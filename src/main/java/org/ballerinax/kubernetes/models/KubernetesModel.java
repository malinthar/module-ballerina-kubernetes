package org.ballerinax.kubernetes.models;

/**
 * Kubernetes Model class.
 */
public abstract class KubernetesModel {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
