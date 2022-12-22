package com.workshop.operator;

public class ReleaseDetectorStatus {

    /**
     * Last release version deployed on the cluster.
     */
    private String deployedRelase;

    public String getDeployedRelase() {
        return deployedRelase;
    }

    public void setDeployedRelase(String deployedRelase) {
        this.deployedRelase = deployedRelase;
    }
}
