package org.ballerinax.kubernetes.test.samples;

import io.fabric8.docker.api.model.ImageInspect;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.ballerinax.kubernetes.KubernetesConstants;
import org.ballerinax.kubernetes.exceptions.KubernetesPluginException;
import org.ballerinax.kubernetes.test.utils.KubernetesTestUtils;
import org.ballerinax.kubernetes.utils.KubernetesUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.ballerinax.kubernetes.KubernetesConstants.DOCKER;
import static org.ballerinax.kubernetes.KubernetesConstants.KUBERNETES;
import static org.ballerinax.kubernetes.test.utils.KubernetesTestUtils.getDockerImage;

public class TestSample2 implements SampleTester {

    private final String sourceDirPath = SAMPLE_DIR + File.separator + "sample2";
    private final String targetPath = sourceDirPath + File.separator + KUBERNETES;
    private final String dockerImage = "hello_world_k8s_config:latest";
    private final String selectorApp = "hello_world_k8s_config";
    private Deployment deployment;
    private Service service;
    private Ingress ingress;

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(KubernetesTestUtils.compileBallerinaFile(sourceDirPath, "hello_world_k8s_config.bal"), 0);
        File yamlFile = new File(targetPath + File.separator + "hello_world_k8s_config.yaml");
        Assert.assertTrue(yamlFile.exists());
        KubernetesClient client = new DefaultKubernetesClient();
        List<HasMetadata> k8sItems = client.load(new FileInputStream(yamlFile)).get();
        for (HasMetadata data : k8sItems) {
            switch (data.getKind()) {
                case "Deployment":
                    deployment = (Deployment) data;
                    break;
                case "Service":
                    service = (Service) data;
                    break;
                case "Ingress":
                    ingress = (Ingress) data;
                    break;
                default:
                    break;
            }
        }
    }

    @Test
    public void validateDeployment() {
        Assert.assertNotNull(deployment);
        Assert.assertEquals("hello-world-k8s-config-deployment", deployment.getMetadata().getName());
        Assert.assertEquals(1, deployment.getSpec().getReplicas().intValue());
        Assert.assertEquals(selectorApp, deployment.getMetadata().getLabels().get(KubernetesConstants
                .KUBERNETES_SELECTOR_KEY));
        Assert.assertEquals(1, deployment.getSpec().getTemplate().getSpec().getContainers().size());
        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        Assert.assertEquals(dockerImage, container.getImage());
        Assert.assertEquals(KubernetesConstants.ImagePullPolicy.IfNotPresent.name(), container.getImagePullPolicy());
        Assert.assertEquals(1, container.getPorts().size());
        Assert.assertEquals(0, container.getEnv().size());
    }

    @Test
    public void validateK8SService() {
        Assert.assertNotNull(service);
        Assert.assertEquals("hello", service.getMetadata().getName());
        Assert.assertEquals(selectorApp, service.getMetadata().getLabels().get(KubernetesConstants
                .KUBERNETES_SELECTOR_KEY));
        Assert.assertEquals(KubernetesConstants.ServiceType.ClusterIP.name(), service.getSpec().getType());
        Assert.assertEquals(1, service.getSpec().getPorts().size());
        Assert.assertEquals(9090, service.getSpec().getPorts().get(0).getPort().intValue());
    }

    @Test(dependsOnMethods = {"validateK8SService"})
    public void validateIngress() {
        Assert.assertNotNull(ingress);
        Assert.assertEquals("helloep-ingress", ingress.getMetadata().getName());
        Assert.assertEquals(selectorApp, ingress.getMetadata().getLabels().get(KubernetesConstants
                .KUBERNETES_SELECTOR_KEY));
        Assert.assertEquals("abc.com", ingress.getSpec().getRules().get(0).getHost());
        Assert.assertEquals("/", ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath());
        Assert.assertEquals(service.getMetadata().getName(), ingress.getSpec().getRules().get(0).getHttp().getPaths()
                .get(0).getBackend()
                .getServiceName());
        Assert.assertEquals(service.getSpec().getPorts().get(0).getPort().intValue(), ingress.getSpec().getRules()
                .get(0).getHttp().getPaths().get(0).getBackend()
                .getServicePort().getIntVal().intValue());
        Assert.assertEquals(2, ingress.getMetadata().getAnnotations().size());
    }

    @Test
    public void validateDockerfile() {
        File dockerFile = new File(targetPath + File.separator + DOCKER + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validateDockerImage() {
        ImageInspect imageInspect = getDockerImage(dockerImage);
        Assert.assertEquals(1, imageInspect.getContainerConfig().getExposedPorts().size());
        Assert.assertEquals("9090/tcp", imageInspect.getContainerConfig().getExposedPorts().keySet().toArray()[0]);
    }

    @AfterClass
    public void cleanUp() throws KubernetesPluginException {
        KubernetesUtils.deleteDirectory(targetPath);
        KubernetesTestUtils.deleteDockerImage(dockerImage);
    }
}
